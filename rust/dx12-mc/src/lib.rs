//! dx12-mc — 原生 D3D12 渲染后端（JNI 桥接层）
//!
//! P1 目标：创建 D3D12 device 并返回设备信息，验证官方
//! `GpuBackend` 挂点 + JNI 链路。
//!
//! 设计参考：官方 `com.mojang.blaze3d.vulkan.VulkanBackend#createDevice`
//! （见 docs/official-262/），此处为 D3D12 等价实现。

use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;
use windows::Win32::Graphics::Direct3D::{
    D3D_FEATURE_LEVEL, D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_12_0,
    D3D_FEATURE_LEVEL_12_1,
};
use windows::Win32::Graphics::Direct3D12::{D3D12CreateDevice, ID3D12Device};
use windows::Win32::Graphics::Dxgi::{
    CreateDXGIFactory1, IDXGIAdapter, IDXGIFactory4, DXGI_ADAPTER_DESC,
};

/// Java: `com.dx12.dx12.Dx12Native.dx12CreateDevice() -> String`
///
/// 返回设备信息字符串，如
/// `NVIDIA GeForce RTX 3080 (D3D_FEATURE_LEVEL_12_1)`；
/// 失败时返回以 `ERROR:` 开头的错误信息。
#[no_mangle]
pub extern "system" fn Java_com_dx12_dx12_Dx12Native_dx12CreateDevice(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let msg = match dx12_create_device() {
        Ok(info) => info,
        Err(e) => format!("ERROR: {e}"),
    };
    log::info!("[dx12] createDevice result: {msg}");
    env.new_string(msg)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// 创建 D3D12 device（从高到低尝试 feature level），返回设备描述。
fn dx12_create_device() -> Result<String, String> {
    let levels = [
        D3D_FEATURE_LEVEL_12_1,
        D3D_FEATURE_LEVEL_12_0,
        D3D_FEATURE_LEVEL_11_1,
        D3D_FEATURE_LEVEL_11_0,
    ];
    let mut last_err = String::from("no feature level attempted");
    for level in levels {
        match create_device_at(level) {
            Ok(device) => {
                let name = adapter_name(&device).unwrap_or_else(|e| format!("<name failed: {e}>"));
                return Ok(format!("{name} (D3D_FEATURE_LEVEL {})", level.0 & 0xffff));
            }
            Err(e) => last_err = e,
        }
    }
    Err(format!("D3D12CreateDevice failed: {last_err}"))
}

fn create_device_at(level: D3D_FEATURE_LEVEL) -> Result<ID3D12Device, String> {
    unsafe {
        let mut device: Option<ID3D12Device> = None;
        D3D12CreateDevice(None, level, &mut device).map_err(|e| {
            format!("{e:?} (level {})", level.0 & 0xffff)
        })?;
        device.ok_or_else(|| String::from("device is null"))
    }
}

/// 通过 device 的 LUID 在 DXGI 中枚举对应适配器取设备名。
///
/// 不用 `device.cast::<IDXGIDevice>()`：D3D12 设备的 QueryInterface(IDXGIDevice)
/// 在部分驱动上返回 E_NOINTERFACE（实测 0x80004002）。用
/// `ID3D12Device::GetAdapterLuid` + `IDXGIFactory4::EnumAdapterByLuid` 更稳，
/// 且按 LUID 匹配能正确选中实际创建 device 的适配器（含多 GPU 环境）。
fn adapter_name(device: &ID3D12Device) -> Result<String, String> {
    unsafe {
        let luid = device.GetAdapterLuid();
        let factory: IDXGIFactory4 =
            CreateDXGIFactory1().map_err(|e| format!("CreateDXGIFactory1: {e:?}"))?;
        let adapter: IDXGIAdapter = factory
            .EnumAdapterByLuid(luid)
            .map_err(|e| format!("EnumAdapterByLuid: {e:?}"))?;
        let desc: DXGI_ADAPTER_DESC =
            adapter.GetDesc().map_err(|e| format!("GetDesc: {e:?}"))?;
        let name = String::from_utf16_lossy(&desc.Description);
        let trimmed = name.trim_end_matches('\0').to_string();
        if trimmed.is_empty() {
            Err(String::from("adapter description empty"))
        } else {
            Ok(trimmed)
        }
    }
}
