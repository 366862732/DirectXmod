//! wgpu-mc-jni: JNI bridge layer for Minecraft + wgpu integration
//!
//! Exports native functions callable from Java Fabric mod.

use jni::objects::{JClass, JFloatArray, JString};
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
    env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> jni::sys::jobject {
    let mut renderer_guard = RENDERER.lock().unwrap();
    if let Some(renderer) = renderer_guard.as_mut() {
        let pixels = renderer.render_frame();
        let len = pixels.len() as i32;

        let bytes = env.new_byte_array(len).unwrap();
        let pixels_i8: &[i8] = unsafe {
            std::slice::from_raw_parts(pixels.as_ptr() as *const i8, pixels.len())
        };
        env.set_byte_array_region(&bytes, 0, pixels_i8).unwrap();

        return bytes.into_raw();
    }
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

    let width = 800u32;
    let height = 600u32;

    match wgpu_mc::WmRenderer::create(width, height) {
        Ok(renderer) => {
            let mut guard = RENDERER.lock().unwrap();
            *guard = Some(renderer);
            log::info!("WmRenderer created successfully");
        }
        Err(e) => {
            log::error!("Failed: {}", e);
        }
    }
}

/// Update the camera MVP matrix for the wgpu renderer.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeUpdateCamera<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    matrix: JFloatArray<'a>,
) {
    let len = env.get_array_length(&matrix).unwrap();
    if len != 16 {
        return;
    }
    let mut floats = [0f32; 16];
    env.get_float_array_region(&matrix, 0, &mut floats).unwrap();

    let mvp: [[f32; 4]; 4] = [
        [floats[0], floats[1], floats[2], floats[3]],
        [floats[4], floats[5], floats[6], floats[7]],
        [floats[8], floats[9], floats[10], floats[11]],
        [floats[12], floats[13], floats[14], floats[15]],
    ];

    let mut guard = RENDERER.lock().unwrap();
    if let Some(ref mut renderer) = guard.as_mut() {
        renderer.set_camera(mvp);
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
        log::info!("Renderer resized to {}x{}", width, height);
    }
}
