#![no_std]

#[derive(Debug)]
pub struct EventData {
    pub addr: u32,
    pub port: u32,
    pub size: u32,
    pub direct: Direct,
}

#[derive(Debug)]
#[repr(u32)]
pub enum Direct {
    TX,
    RX,
}

