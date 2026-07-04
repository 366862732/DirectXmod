//! wgpu-mc-jni: JNI bridge layer for Minecraft + wgpu integration
//!
//! Exports native functions callable from Java Fabric mod.

use jni::objects::{JClass, JString};
use jni::JNIEnv;

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
