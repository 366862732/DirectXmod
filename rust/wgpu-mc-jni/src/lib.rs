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
    let info = "wgpu-mc-jni loaded. DX12: READY";
    env.new_string(info).expect("Failed to create Java string")
}

/// Render a single frame and return pixel data as byte[].
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeRenderFrame<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> jni::sys::jobject {
    // File-based debug log
    let mut debug_log = "nativeRenderFrame entered\n".to_string();
    
    let mut renderer_guard = RENDERER.lock().unwrap();
    if let Some(renderer) = renderer_guard.as_mut() {
        let pixels = renderer.render_frame();
        let len = pixels.len() as i32;
        debug_log.push_str(&format!("Renderer OK, {} bytes\n", len));
        
        // Create Java byte array and copy pixels into it
        let bytes = env.new_byte_array(len).unwrap();
        // Cast u8 slice to i8 slice for JNI
        let pixels_i8: &[i8] = unsafe {
            std::slice::from_raw_parts(pixels.as_ptr() as *const i8, pixels.len())
        };
        env.set_byte_array_region(&bytes, 0, pixels_i8).unwrap();
        
        // Return the byte array
        let result = bytes.into_raw();
        debug_log.push_str("Returning byte array\n");
        let _ = std::fs::write("C:\\tmp\\wgpu_debug.txt", &debug_log);
        return result;
    }
    debug_log.push_str("ERROR: Renderer is None!\n");
    let _ = std::fs::write("C:\\tmp\\wgpu_debug.txt", &debug_log);
    std::ptr::null_mut()
}

/// Initialize the wgpu renderer with the Minecraft window HWND.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeSetWindow<'a>(
    _env: JNIEnv<'a>,
    _class: JClass<'a>,
    _hwnd: i64,
) {
    log::info!("nativeSetWindow called with hwnd: {}", _hwnd);
    
    let mut debug_log = format!("nativeSetWindow called, hwnd={}\n", _hwnd);
    
    // Try to create the renderer (width/height will be synced later)
    let width = 800u32;
    let height = 600u32;
    
    match wgpu_mc::WmRenderer::create(width, height) {
        Ok(renderer) => {
            let mut guard = RENDERER.lock().unwrap();
            *guard = Some(renderer);
            log::info!("WmRenderer created successfully");
            debug_log.push_str("WmRenderer created OK\n");
            
            // Write file to confirm initialization
            let _ = std::fs::write("C:\\tmp\\wgpu_mc_initialized.txt", "renderer created\n");
        }
        Err(e) => {
            log::error!("Failed to create WmRenderer: {}", e);
            debug_log.push_str(&format!("WmRenderer FAILED: {}\n", e));
            let _ = std::fs::write("C:\\tmp\\wgpu_mc_error.txt", format!("{}\n", e));
        }
    }
    let _ = std::fs::write("C:\\tmp\\wgpu_debug.txt", &debug_log);
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
        log::info!("Renderer resized to {}x{}", width, height);
    }
}
