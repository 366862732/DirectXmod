# Checklist

- [x] WmRenderer::from_hwnd() 完整实现已替换 from_hwnd_placeholder
- [x] winit 包装层正确创建 wgpu Surface 绑定到 MC 窗口
- [x] nativeRenderFrame() 实际调用 renderer.render_frame() 渲染一帧
- [x] RENDERER 单例线程安全，支持跨 JNI 调用复用
- [x] 无效 HWND 正确返回错误并记录日志
- [x] 多次 nativeSetWindow 调用不会重复创建 renderer
- [x] cargo build --package wgpu-mc-jni --release 编译通过
- [x] cargo run --example simple 独立测试程序仍可正常运行
- [x] gradle clean build Fabric 模组编译通过
- [x] cargo clippy -- -D warnings 零警告
- [x] cargo fmt -- --check 格式检查通过
- [ ] MC 1.21.1 启动后日志显示 "WmRenderer created from HWND"
- [ ] MC 窗口中可见 wgpu 渲染输出（蓝色清屏或三角形）
