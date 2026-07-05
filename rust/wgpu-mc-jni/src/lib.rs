//! wgpu-mc-jni: JNI bridge layer for Minecraft + wgpu integration
//!
//! Exports native functions callable from Java Fabric mod.

use jni::objects::{JClass, JString};
use jni::JNIEnv;

// Global static renderer (initialized once from HWND)
use std::sync::Mutex;
static RENDERER: Mutex<Option<wgpu_mc::WmRenderer>> = Mutex::new(None);

#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeInit(_env: JNIEnv, _class: JClass) {
    env_logger::init();
    log::info!("Rust JNI library loaded successfully!");
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeHello<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    input: JString<'a>,
) -> JString<'a> {
    let input_str: String = env
        .get_string(&input)
        .expect("Couldn't get Java string")
        .into();

    log::info!("Java said: {}", input_str);

    let response = format!("Hello from Rust wgpu! You said: {}", input_str);
    env.new_string(&response).expect("Failed to create Java string")
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeTestDeviceInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> JString<'a> {
    let has_dx12 = wgpu_mc::WmRenderer::check_gpu_availability();
    let info = if has_dx12 {
        "wgpu-mc-jni loaded. DX12 adapter: AVAILABLE"
    } else {
        "wgpu-mc-jni loaded. DX12 adapter: NOT FOUND"
    };
    env.new_string(info).expect("Failed to create Java string")
}

/// Set the Minecraft window HWND and create the wgpu renderer.
/// This is called once during game startup.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeSetWindow(
    _env: JNIEnv,
    _class: JClass,
    hwnd: i64,
) {
    log::info!("Setting window HWND: 0x{:016x}", hwnd);

    // Only create renderer once
    let mut renderer_lock = RENDERER.lock().unwrap();
    if renderer_lock.is_some() {
        log::info!("Renderer already initialized, skipping");
        return;
    }

    // Create renderer from HWND using winit wrapper
    // SAFETY: hwnd is a valid Windows window handle passed from Java
    match wgpu_mc::WmRenderer::from_hwnd(hwnd as u64) {
        Ok(renderer) => {
            *renderer_lock = Some(renderer);
            log::info!("WmRenderer created successfully from HWND 0x{:016x}", hwnd);
        }
        Err(e) => {
            log::error!("Failed to create WmRenderer from HWND 0x{:016x}: {:?}", hwnd, e);
        }
    }
}

/// Render a single frame via the wgpu backend.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeRenderFrame(_env: JNIEnv, _class: JClass) {
    let mut renderer_lock = RENDERER.lock().unwrap();
    if let Some(ref mut renderer) = renderer_lock.as_mut() {
        match renderer.render_frame() {
            Ok(_) => {}
            Err(e) => {
                log::error!("render_frame error: {:?}", e);
            }
        }
    } else {
        log::warn!("nativeRenderFrame called but renderer not initialized — skipping frame");
    }
}
