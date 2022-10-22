#![no_std]
#![no_main]

use aya_bpf::{
    helpers::{bpf_get_current_pid_tgid, bpf_probe_read_kernel},
    macros::{kprobe, kretprobe, map},
    maps::{HashMap, PerfEventArray},
    programs::ProbeContext,
};
mod consts;
use tikv_bpf_common::{Direct, EventData};

type SockPtr = *mut u8;

#[map]
static mut SOCK_MAP: HashMap<u32, SockPtr> = HashMap::with_max_entries(1024, 0);
#[map(name = "EVENTS")]
static mut PERF_EVENTS: PerfEventArray<EventData> = PerfEventArray::with_max_entries(1024, 0);

#[no_mangle]
static FILTER_PID: u32 = 0;

fn get_filter_pid() -> u32 {
    unsafe { core::ptr::read_volatile(&FILTER_PID) }
}

fn tcp_sendentry(ctx: ProbeContext) -> Result<(), i64> {
    let pid_tid = bpf_get_current_pid_tgid();
    let tid = pid_tid as u32;
    let pid = (pid_tid >> 32) as u32;
    if pid != get_filter_pid() {
        return Ok(());
    }
    let sk: SockPtr = ctx.arg(0).ok_or(1i64)?;
    let sk_family = unsafe { bpf_probe_read_kernel(sk.add(16) as *const u16) }?;
    if sk_family != consts::AF_INET {
        return Ok(());
    }
    unsafe { SOCK_MAP.insert(&tid, &sk, 0) }?;

    Ok(())
}

fn tcp_sendstat(ctx: ProbeContext) -> Result<(), i64> {
    let size: u32 = ctx.ret().unwrap_or(0);
    if size == 0 {
        return Ok(());
    }
    let pid_tid = bpf_get_current_pid_tgid();
    let pid = (pid_tid >> 32) as u32;
    let tid = pid_tid as u32;
    if pid != get_filter_pid() {
        return Ok(());
    }
    let sock: SockPtr = *unsafe { SOCK_MAP.get(&tid) }.ok_or(1)?;
    let daddr: u32 = unsafe { bpf_probe_read_kernel(sock as *mut u32) }?;
    let dport: u16 = unsafe { bpf_probe_read_kernel(sock.add(12) as *mut u16) }?;
    unsafe {
        PERF_EVENTS.output(
            &ctx,
            &EventData {
                addr: daddr,
                port: dport.into(),
                size,
                direct: Direct::TX,
            },
            0,
        )
    };
    unsafe { SOCK_MAP.remove(&tid) }?;
    Ok(())
}

fn tcp_cleanup_rbuf(ctx: ProbeContext) -> Result<(), i64> {
    let size: i32 = ctx.arg(1).unwrap_or(0);
    if size <= 0 {
        return Ok(());
    }
    let pid_tid = bpf_get_current_pid_tgid();
    let pid = (pid_tid >> 32) as u32;
    let tid = pid_tid as u32;
    if pid != get_filter_pid() {
        return Ok(());
    }
    let sock: SockPtr = ctx.arg(0).ok_or(1i64)?;
    let saddr: u32 = unsafe { bpf_probe_read_kernel(sock as *mut u32) }?;
    let sport: u16 = unsafe { bpf_probe_read_kernel(sock.add(12) as *mut u16) }?;
    unsafe {
        PERF_EVENTS.output(
            &ctx,
            &EventData {
                addr: saddr,
                port: sport.into(),
                size: size as u32,
                direct: Direct::RX,
            },
            0,
        )
    };
    unsafe { SOCK_MAP.remove(&tid) }?;
    Ok(())
}

#[kprobe(name = "tcp_send")]
pub fn kprobe_tcp_send(ctx: ProbeContext) -> u32 {
    match tcp_sendentry(ctx) {
        Ok(()) => 0,
        Err(ret) => ret as u32,
    }
}

#[kretprobe(name = "ret_tcp_send")]
pub fn kretprobe_tcp_send(ctx: ProbeContext) -> u32 {
    match tcp_sendstat(ctx) {
        Ok(()) => 0,
        Err(ret) => ret as u32,
    }
}

#[kprobe(name = "tcp_cleanup_rbuf")]
pub fn kprobe_tcp_cleanup_rbuf(ctx: ProbeContext) -> u32 {
    match tcp_cleanup_rbuf(ctx) {
        Ok(()) => 0,
        Err(ret) => ret as u32,
    }
}
#[panic_handler]
fn panic(_info: &core::panic::PanicInfo) -> ! {
    unsafe { core::hint::unreachable_unchecked() }
}
