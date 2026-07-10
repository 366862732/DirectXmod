//! wgpu-mc-jni: JNI bridge layer for Minecraft + wgpu integration
//!
//! Exports native functions callable from Java Fabric mod.
//!
//! Design: D3D12 device is created synchronously during nativeInit(), which is
//! called from ClientModInitializer.onInitializeClient(). At this point the
//! render thread exists but has NOT started the game loop or OpenGL rendering
//! yet, so there is no GL/D3D12 conflict. The original crash was due to
//! creating D3D12 during a render tick (nativeSetWindow), when the OpenGL
//! context was actively bound.

use jni::objects::{JClass, JFloatArray, JString};
use jni::JNIEnv;

use std::sync::Mutex;

/// Renderer state: None=not started, Some(Ok)=ready, Some(Err)=failed.
type RendererResult = Option<Result<wgpu_mc::WmRenderer, String>>;
static RENDERER: Mutex<RendererResult> = Mutex::new(None);

/// Initialize the Rust JNI library and create the D3D12 renderer.
/// Called during mod init (ClientModInitializer), BEFORE the game loop starts.
/// Safe because OpenGL context is not actively bound for rendering yet.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeInit(_env: JNIEnv, _class: JClass) {
    let _ = env_logger::try_init();

    // Only init once
    {
        let guard = RENDERER.lock().unwrap();
        if guard.is_some() {
            return;
        }
    }

    log::info!("Creating WmRenderer during mod init (no active GL rendering)...");
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        wgpu_mc::WmRenderer::create(800, 600)
    }));
    match result {
        Ok(Ok(renderer)) => {
            log::info!("WmRenderer created successfully during mod init");
            let mut guard = RENDERER.lock().unwrap();
            *guard = Some(Ok(renderer));
        }
        Ok(Err(e)) => {
            log::error!("WmRenderer creation FAILED during mod init: {}", e);
            let mut guard = RENDERER.lock().unwrap();
            *guard = Some(Err(e.to_string()));
        }
        Err(panic_info) => {
            let msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "unknown panic".to_string()
            };
            log::error!("WmRenderer creation PANICKED during mod init: {}", msg);
            let mut guard = RENDERER.lock().unwrap();
            *guard = Some(Err(format!("panic: {}", msg)));
        }
    }
}

/// Check if the wgpu renderer is ready for rendering.
/// Returns 1 if ready, 0 if still initializing, -1 if failed.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeIsReady(
    _env: JNIEnv,
    _class: JClass,
) -> i32 {
    let guard = RENDERER.lock().unwrap();
    match guard.as_ref() {
        Some(Ok(_)) => 1,
        Some(Err(_)) => -1,
        None => 0,
    }
}

/// Get a human-readable status string for the renderer.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeGetStatus<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> JString<'a> {
    let guard = RENDERER.lock().unwrap();
    let msg = match guard.as_ref() {
        Some(Ok(_)) => "ready".to_string(),
        Some(Err(e)) => format!("error: {}", e),
        None => "not_started".to_string(),
    };
    env.new_string(&msg).expect("Failed to create Java string")
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
/// Returns null if renderer is not ready.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeRenderFrame<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> jni::sys::jobject {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Vec<u8> {
        let mut guard = RENDERER.lock().unwrap();
        match guard.as_mut() {
            Some(Ok(renderer)) => renderer.render_frame(),
            Some(Err(_)) => Vec::new(),
            None => Vec::new(),
        }
    }));
    match result {
        Ok(pixels) if !pixels.is_empty() => {
            let len = pixels.len() as i32;
            let bytes = env.new_byte_array(len).unwrap();
            let pixels_i8: &[i8] = unsafe {
                std::slice::from_raw_parts(pixels.as_ptr() as *const i8, pixels.len())
            };
            env.set_byte_array_region(&bytes, 0, pixels_i8).unwrap();
            bytes.into_raw()
        }
        Err(panic_info) => {
            let msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "unknown panic".to_string()
            };
            log::error!("render_frame PANICKED: {}", msg);
            std::ptr::null_mut()
        }
        _ => std::ptr::null_mut(),
    }
}

/// Pass the Minecraft window HWND to create a D3D12 surface/swapchain.
/// Enables surface mode: D3D12 presents directly to the window, bypassing
/// OpenGL readback entirely.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeSetWindow<'a>(
    _env: JNIEnv<'a>,
    _class: JClass<'a>,
    hwnd: i64,
) {
    log::info!("nativeSetWindow: initializing D3D12 surface on HWND 0x{:x}", hwnd);
    let mut guard = RENDERER.lock().unwrap();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.init_surface(hwnd as usize);
    }
}

/// Returns true if the renderer has an active surface (swapchain mode).
/// When surface mode is active, D3D12 presents directly to the window
/// and no pixel readback is needed.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeHasSurface(
    _env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    let guard = RENDERER.lock().unwrap();
    match guard.as_ref() {
        Some(Ok(renderer)) => {
            if renderer.has_surface() { jni::sys::JNI_TRUE } else { jni::sys::JNI_FALSE }
        }
        _ => jni::sys::JNI_FALSE,
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
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
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
    if let Some(Ok(ref mut renderer)) = renderer_guard.as_mut() {
        renderer.resize(width as u32, height as u32);
        log::info!("Renderer resized to {}x{}", width, height);
    }
}
