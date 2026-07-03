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
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeHello(
    env: JNIEnv,
    _class: JClass,
    input: JString,
) -> JString {
    let input_str: String = env
        .get_string(&input)
        .expect("Couldn't get Java string")
        .into();

    log::info!("Java said: {}", input_str);

    let response = format!("Hello from Rust wgpu! You said: {}", input_str);
    env.new_string(&response).unwrap().into_inner()
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_dx12_D3D12Bridge_nativeTestDeviceInfo(
    env: JNIEnv,
    _class: JClass,
) -> JString {
    let info = format!(
        "wgpu-mc-jni loaded on {}. Graphics: DX12 available via wgpu.",
        cfg!(target_os = "windows")
    );
    env.new_string(&info).unwrap().into_inner()
}
