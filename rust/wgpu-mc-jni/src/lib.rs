//! wgpu-mc-jni: JNI bridge layer for Minecraft + wgpu integration
//!
//! Exports native functions callable from Java Fabric mod.

use jni::objects::{JClass, JString};
use jni::JNIEnv;

// Global static renderer (initialized once from HWND)
use std::sync::Mutex;
static RENDERER: Mutex<Option<wgpu_mc::WmRenderer>> = Mutex::new(None);

/// Initialize the Rust JNI library. Called once during mod startup.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeInit(_env: JNIEnv, _class: JClass) {
    let _ = env_logger::try_init();
    // Write file to confirm DLL is loaded
    let _ = std::fs::write("C:\\tmp\\wgpu_mc_init.txt", "nativeInit executed\n");
}

/// Test JNI string communication.
///
/// # Safety
/// This function is called from Java via JNI.
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

    let response = format!("Hello from Rust wgpu! You said: {}", input_str);
    env.new_string(&response).expect("Failed to create Java string")
}

/// Test GPU availability.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeTestDeviceInfo<'a>(
    env: JNIEnv<'a>,
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
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeSetWindow(
    _env: JNIEnv,
    _class: JClass,
    hwnd: i64,
) {
    // Write file to confirm this function is called
    let hwnd_hex = format!("0x{:016x}", hwnd);
    let _ = std::fs::write("C:\\tmp\\wgpu_mc_setwindow.txt", format!("nativeSetWindow: {}\n", hwnd_hex));

    // Only create renderer once
    let mut renderer_guard = RENDERER.lock().unwrap();
    if renderer_guard.is_some() {
        return;
    }

    // Create renderer from HWND
    match wgpu_mc::WmRenderer::from_hwnd(hwnd as u64) {
        Ok(renderer) => {
            *renderer_guard = Some(renderer);
            let _ = std::fs::write(
                "C:\\tmp\\wgpu_mc_setwindow.txt",
                format!("nativeSetWindow: {} -> OK\n", hwnd_hex),
            );
        }
        Err(e) => {
            let _ = std::fs::write(
                "C:\\tmp\\wgpu_mc_setwindow.txt",
                format!("nativeSetWindow: {} -> ERROR: {:?}\n", hwnd_hex, e),
            );
        }
    }
}

/// Render a single frame via the wgpu backend.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeRenderFrame(_env: JNIEnv, _class: JClass) {
    // Append every render call with timestamp
    use std::io::Write;
    let mut file = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open("C:\\tmp\\wgpu_mc_render_calls.txt")
        .ok();
    if let Some(f) = &mut file {
        let _ = writeln!(f, "render_frame called at {}", std::time::Instant::now().elapsed().as_millis());
    }
    drop(file);
    
    let renderer_guard = RENDERER.lock().unwrap();
    if let Some(renderer) = &*renderer_guard {
        if let Err(e) = renderer.render_frame() {
            eprintln!("[wgpu-mc-jni] render_frame error: {:?}", e);
        }
    }
}

/// Resize the wgpu renderer to match MC window dimensions.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeResize(
    _env: JNIEnv,
    _class: JClass,
    width: i32,
    height: i32,
) {
    if width <= 0 || height <= 0 {
        return;
    }
    let mut renderer_guard = RENDERER.lock().unwrap();
    if let Some(ref mut renderer) = renderer_guard.as_mut() {
        renderer.resize(width as u32, height as u32);
    }
}
