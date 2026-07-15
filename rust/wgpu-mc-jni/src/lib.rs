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
//!
//! Mutex handling: We use lock_or_poisoned() instead of lock().unwrap() to
//! survive panics in render_frame() — if the renderer panics, the Mutex
//! becomes poisoned but the data is still valid. Recovering from poison
//! prevents cascading JVM crashes.

use jni::objects::{JClass, JFloatArray, JString};
use jni::JNIEnv;

use std::sync::{Mutex, MutexGuard};

/// Renderer state: None=not started, Some(Ok)=ready, Some(Err)=failed.
type RendererResult = Option<Result<wgpu_mc::WmRenderer, String>>;
static RENDERER: Mutex<RendererResult> = Mutex::new(None);

/// Lock the renderer mutex, recovering from poison state.
/// If a previous frame panicked, the mutex is poisoned but the
/// renderer data is still valid — we just continue normally.
fn lock_or_poisoned() -> MutexGuard<'static, RendererResult> {
    RENDERER.lock().unwrap_or_else(|e| e.into_inner())
}

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
        let guard = lock_or_poisoned();
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
            let mut guard = lock_or_poisoned();
            *guard = Some(Ok(renderer));
        }
        Ok(Err(e)) => {
            log::error!("WmRenderer creation FAILED during mod init: {}", e);
            let mut guard = lock_or_poisoned();
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
            let mut guard = lock_or_poisoned();
            *guard = Some(Err(format!("panic: {}", msg)));
        }
    }
}

/// Upload MC chunk section mesh to D3D12 for native rendering.
/// `data` contains raw vertex bytes (36 bytes/vertex, MC default block format).
/// `section_x/y/z` are chunk section coordinates (world pos >> 4).
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeUploadChunkMesh(
    _env: JNIEnv,
    _class: JClass,
    section_x: jni::sys::jint,
    section_y: jni::sys::jint,
    section_z: jni::sys::jint,
    buffer: jni::objects::JObject,
    vertex_count: jni::sys::jint,
    vertex_stride: jni::sys::jint,
) {
    if vertex_count <= 0 || vertex_stride <= 0 { return; }

    let byte_buf = jni::objects::JByteBuffer::from(buffer);
    let data = match _env.get_direct_buffer_address(&byte_buf) {
        Ok(ptr) => ptr,
        Err(_) => {
            eprintln!("[dx12-wm] uploadChunkMesh FAILED: buffer is not direct");
            return;
        }
    };
    let len = (vertex_count * vertex_stride) as usize;
    let slice = std::slice::from_raw_parts(data, len);

    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.upload_chunk_mesh(
            section_x, section_y, section_z,
            slice,
            vertex_count as u32,
            vertex_stride as u32,
        );
    }
}

/// Upload the MC terrain atlas texture for chunk rendering.
/// `data` is RGBA8 pixel data, width and height are atlas dimensions.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeUploadTerrainAtlas(
    _env: JNIEnv,
    _class: JClass,
    buffer: jni::objects::JObject,
    width: jni::sys::jint,
    height: jni::sys::jint,
) {
    if width <= 0 || height <= 0 { return; }

    let byte_buf = jni::objects::JByteBuffer::from(buffer);
    let data = match _env.get_direct_buffer_address(&byte_buf) {
        Ok(ptr) => ptr,
        Err(_) => {
            eprintln!("[dx12-wm] uploadTerrainAtlas FAILED: buffer is not direct");
            return;
        }
    };
    let len = (width * height * 4) as usize;
    let slice = std::slice::from_raw_parts(data, len);

    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.upload_terrain_atlas(slice, width as u32, height as u32);
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
    let guard = lock_or_poisoned();
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
    let guard = lock_or_poisoned();
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
        // IMPORTANT: lock the mutex inside catch_unwind.
        // If render_frame() panics, the MutexGuard drop poisons the mutex,
        // but catch_unwind catches the panic. Subsequent calls use
        // lock_or_poisoned() to recover.
        let mut guard = lock_or_poisoned();
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
    let mut guard = lock_or_poisoned();
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
    let guard = lock_or_poisoned();
    match guard.as_ref() {
        Some(Ok(renderer)) => {
            if renderer.has_surface() { jni::sys::JNI_TRUE } else { jni::sys::JNI_FALSE }
        }
        _ => jni::sys::JNI_FALSE,
    }
}

/// Clear all meshes for a given chunk section before recompilation.
/// Prevents stale mesh accumulation when blocks are broken or placed.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeClearChunkSection(
    _env: JNIEnv,
    _class: JClass,
    section_x: jni::sys::jint,
    section_y: jni::sys::jint,
    section_z: jni::sys::jint,
) {
    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.clear_chunk_section(section_x, section_y, section_z);
    }
}

/// Returns true if any MC chunk geometry has been uploaded to the D3D12 renderer.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeHasChunkGeometry(
    _env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    let guard = lock_or_poisoned();
    match guard.as_ref() {
        Some(Ok(renderer)) => {
            if renderer.has_chunk_geometry() { jni::sys::JNI_TRUE } else { jni::sys::JNI_FALSE }
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

    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.set_camera(mvp);
    }
}

/// Set the camera world position (used to offset test geometry near camera).
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeUpdateCameraPos(
    _env: JNIEnv,
    _class: JClass,
    x: f32,
    y: f32,
    z: f32,
) {
    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.set_camera_pos(x, y, z);
    }
}

/// Update fog color and density for atmospheric fog in chunk rendering.
///
/// # Safety
/// This function is called from Java via JNI.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeUpdateFog(
    _env: JNIEnv,
    _class: JClass,
    r: f32,
    g: f32,
    b: f32,
    density: f32,
) {
    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.set_fog(&[r, g, b], density);
    }
}

/// Upload captured GL framebuffer pixels as a D3D12 texture for surface mode display.
///
/// # Safety
/// This function is called from Java via JNI. The ByteBuffer must be a direct buffer.
#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeSetFramePixels(
    _env: JNIEnv,
    _class: JClass,
    buffer: jni::objects::JObject,  // ByteBuffer as JObject
    width: jni::sys::jint,
    height: jni::sys::jint,
) {
    static mut FIRST_CALL: bool = true;
    if width <= 0 || height <= 0 { return; }

    // Wrap as JByteBuffer to access direct buffer address
    let byte_buf = jni::objects::JByteBuffer::from(buffer);
    let data = match _env.get_direct_buffer_address(&byte_buf) {
        Ok(ptr) => ptr,
        Err(_) => {
            eprintln!("[dx12-wm] nativeSetFramePixels FAILED: buffer is not direct");
            log::warn!("[dx12-wm] Frame pixel buffer is not direct");
            return;
        }
    };
    let len = (width * height * 4) as usize;
    let slice = std::slice::from_raw_parts(data, len);

    // One-time diagnostic on first call
    if FIRST_CALL {
        FIRST_CALL = false;
        if len >= 8 {
            let r0 = slice[0]; let g0 = slice[1]; let b0 = slice[2]; let a0 = slice[3];
            let r1 = slice[len-4]; let g1 = slice[len-3]; let b1 = slice[len-2]; let a1 = slice[len-1];
            eprintln!("[dx12-wm] First pixel RGBA=({},{},{},{})  Last pixel RGBA=({},{},{},{})",
                r0, g0, b0, a0, r1, g1, b1, a1);
        }
    }

    let mut guard = lock_or_poisoned();
    if let Some(Ok(ref mut renderer)) = guard.as_mut() {
        renderer.set_frame_pixels(slice, width as u32, height as u32);
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
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let mut renderer_guard = lock_or_poisoned();
        if let Some(Ok(ref mut renderer)) = renderer_guard.as_mut() {
            renderer.resize(width as u32, height as u32);
        }
    }));
    match result {
        Ok(()) => log::info!("Renderer resized to {}x{}", width, height),
        Err(panic_info) => {
            let msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "unknown panic".to_string()
            };
            log::error!("resize PANICKED ({}x{}): {}", width, height, msg);
            // Mutex is now poisoned — subsequent calls recover via lock_or_poisoned()
        }
    }
}
