use std::cell::RefCell;
use std::collections::HashMap;
use std::ffi::{c_char, CString};
use std::net::Ipv4Addr;
use std::sync::{Mutex, RwLock};
use std::time::Duration;

use anyhow::Result;
use aya::maps::perf::AsyncPerfEventArray;
use aya::programs::KProbe;
use aya::{include_bytes_aligned, Bpf, BpfLoader};
use serde::{Serialize, Serializer};
use tikv_bpf_common::EventData;

static THREAD_HANDLE: Mutex<RefCell<bool>> = Mutex::new(RefCell::new(false));

#[derive(Eq, PartialEq, Hash)]
struct IpAddr {
    ip: u32,
    port: u16,
}

impl Serialize for IpAddr {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(
            format!(
                "{}:{}",
                Ipv4Addr::from(u32::from_be(self.ip)),
                u16::from_be(self.port)
            )
            .as_str(),
        )
    }
}

#[derive(serde_derive::Serialize)]
struct Stat {
    tx: u64,
    rx: u64,
}

lazy_static::lazy_static! {
static ref STATS: tokio::sync::Mutex<tokio::sync::RwLock<HashMap<IpAddr,Stat>>>=
    tokio::sync::Mutex::new(tokio::sync::RwLock::new(HashMap::new()));
}
static LAST_STATS: Mutex<RwLock<Option<HashMap<IpAddr, Stat>>>> = Mutex::new(RwLock::new(None));
mod test {
    #[test]
    fn main_test() -> anyhow::Result<()> {
        // env_logger::builder()
        //     .filter_level(log::LevelFilter::Debug)
        //     .init();
        crate::start_monitor()?;
        let mut buf = String::new();
        loop {
            std::io::stdin().read_line(&mut buf)?;
            println!("{}", crate::get_stats());
        }
    }
}

#[no_mangle]
pub extern "C" fn c_start_monitor() -> i32 {
    match start_monitor() {
        Ok(true) => 1,
        Ok(false) => 0,
        Err(_) => -1,
    }
}

#[no_mangle]
pub extern "C" fn c_get_stats() -> *const c_char {
    if let Ok(cstring) = CString::new(get_stats()) {
        let boxed = Box::new(cstring);
        let ptr = boxed.as_ptr();
        std::mem::forget(boxed);
        return ptr;
    }
    return 0 as *const c_char;
}

pub fn start_monitor() -> Result<bool> {
    let bpf = load_bpf()?;
    let mut lock = THREAD_HANDLE.lock().unwrap();
    let thread_handle = lock.get_mut();
    if *thread_handle {
        Ok(false)
    } else {
        std::thread::spawn(move || {
            new_thread_main(bpf).unwrap();
        });
        *thread_handle = true;
        Ok(true)
    }
}

fn new_thread_main(bpf: Bpf) -> Result<()> {
    let rt = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()?;
    let _guard = rt.enter();
    std::mem::forget(_guard);
    rt.block_on(stats_timer(&bpf));
    Ok(())
}

fn load_bpf() -> Result<Bpf> {
    #[cfg(debug_assertions)]
    let bpf_bytes = include_bytes_aligned!("../../target/bpfel-unknown-none/debug/tikv-bpf");
    #[cfg(not(debug_assertions))]
    let bpf_bytes = include_bytes_aligned!("../../target/bpfel-unknown-none/release/tikv-bpf");

    let mut bpf = BpfLoader::new()
        // .set_global("FILTER_PID", &5523)
        .set_global("FILTER_PID", &std::process::id())
        .load(bpf_bytes)?;

    // if let Err(e) = BpfLogger::init(&mut bpf) {
    //     warn!("failed to initialize eBPF logger: {}", e);
    // }
    let program: &mut KProbe = bpf.program_mut("tcp_send").unwrap().try_into()?;
    program.load()?;
    program.attach("tcp_sendmsg", 0)?;
    program.attach("tcp_sendpage", 0)?;
    let program: &mut KProbe = bpf.program_mut("ret_tcp_send").unwrap().try_into()?;
    program.load()?;
    program.attach("tcp_sendmsg", 0)?;
    program.attach("tcp_sendpage", 0)?;

    let program: &mut KProbe = bpf.program_mut("tcp_cleanup_rbuf").unwrap().try_into()?;
    program.load()?;
    program.attach("__tcp_cleanup_rbuf", 0)?;

    Ok(bpf)
}

fn handle_event(bpf: &Bpf) -> Result<()> {
    let mut perf_array: AsyncPerfEventArray<_> = bpf.map_mut("EVENTS")?.try_into()?;
    for cpu_id in aya::util::online_cpus()? {
        let mut buf = perf_array.open(cpu_id, None)?;

        tokio::spawn(async move {
            let mut buffers = (0..10)
                .map(|_| bytes::BytesMut::with_capacity(std::mem::size_of::<EventData>()))
                .collect::<Vec<_>>();

            loop {
                let events = buf.read_events(&mut buffers).await.unwrap();
                for i in 0..events.read {
                    let data =
                        unsafe { (buffers[i].as_ptr() as *const EventData).read_unaligned() };
                    let mut guard = STATS.lock().await;
                    let stat_map = guard.get_mut();
                    let key = IpAddr {
                        ip: data.addr,
                        port: data.port as u16,
                    };
                    if let Some(stat) = stat_map.get_mut(&key) {
                        match data.direct {
                            tikv_bpf_common::Direct::TX => stat.tx += data.size as u64,
                            tikv_bpf_common::Direct::RX => stat.rx += data.size as u64,
                        }
                    } else {
                        stat_map.insert(
                            key,
                            match data.direct {
                                tikv_bpf_common::Direct::TX => Stat {
                                    tx: data.size as u64,
                                    rx: 0,
                                },
                                tikv_bpf_common::Direct::RX => Stat {
                                    tx: 0,
                                    rx: data.size as u64,
                                },
                            },
                        );
                    }
                }
            }
        });
    }
    Ok(())
}

async fn stats_timer(bpf: &Bpf) {
    handle_event(&bpf).unwrap();
    loop {
        tokio::time::sleep(Duration::from_secs(5)).await;
        if let Ok(mut last_stats_lock) = LAST_STATS.lock() {
            let mut stats_lock = STATS.lock().await;
            if let Ok(data) = last_stats_lock.get_mut() {
                *data = Some(std::mem::take(stats_lock.get_mut()));
            }
        }
    }
}

pub fn get_stats() -> String {
    if let Ok(last_stats_lock) = LAST_STATS.lock() {
        if let Ok(data_option) = (*last_stats_lock).try_read() {
            if let Some(ref data) = *data_option {
                return serde_json::to_string(&data).unwrap_or("{\"error\":\"1\"}".to_string());
            }
        }
    }
    "{\"error\":\"2\"}".to_string()
}
