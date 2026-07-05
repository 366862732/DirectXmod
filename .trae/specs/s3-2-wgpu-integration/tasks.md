# Tasks

- [x] Task 1: 分析 raw-window-handle 0.6 / wgpu 23 兼容性问题
  - 确认 `WindowHandle` 不实现 `HasDisplayHandle` 的根本原因
  - 确定 winit 包装层方案为可行路径

- [x] Task 2: 实现 WmRenderer::from_hwnd() 完整逻辑
  - 在 `rust/wgpu-mc/src/lib.rs` 中替换 `from_hwnd_placeholder()` 为完整实现
  - 使用 winit 的 `Window` 作为中间层创建 wgpu Surface
  - 支持从 HWND 提取 raw-window-handle 信息

- [x] Task 3: 添加 winit 依赖并更新 Cargo.toml
  - 在 `rust/wgpu-mc/Cargo.toml` 中添加 `winit = "0.30"` 作为正常依赖
  - 使用 winit 0.30 API 创建隐藏窗口

- [x] Task 4: 实现 nativeRenderFrame 实际渲染
  - 在 `rust/wgpu-mc-jni/src/lib.rs` 中实现完整的 `nativeRenderFrame()`
  - 确保 renderer 单例在线程安全的情况下被访问
  - 添加错误处理和日志

- [x] Task 5: 更新 Rust JNI 桥接层
  - 实现 `RENDERER` 全局变量的完整生命周期管理
  - 支持 `nativeSetWindow()` 创建 renderer
  - 支持 `nativeRenderFrame()` 调用 renderer.render_frame()

- [x] Task 6: 编译验证 Rust 端
  - 运行 `cargo build --package wgpu-mc-jni --release` ✅ 通过
  - 运行 `cargo clippy -- -D warnings` ✅ 零警告
  - 运行 `cargo run --example simple` ✅ 独立渲染正常工作

- [x] Task 7: 编译验证 Fabric 模组
  - 运行 `gradle clean build` ✅ 通过
  - 确认 Mixin 注入配置正确
  - 确认 JAR 和 DLL 可正常部署

- [x] Task 8: 代码合规性检查
  - `cargo fmt -- --check` ✅ 通过
  - `cargo clippy -- -D warnings` ✅ 零警告
  - `gradle build` ✅ 通过

- [ ] Task 9: 集成测试 - 启动 MC 验证 (需用户手动部署)
  - 部署 JAR 和 DLL 到 MC 1.21.1
  - 启动游戏观察日志输出
  - 验证 wgpu Surface 成功绑定到 MC 窗口

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 4] depends on [Task 2]
- [Task 5] depends on [Task 4]
- [Task 6] depends on [Task 5]
- [Task 7] depends on [Task 6]
- [Task 8] depends on [Task 7]
- [Task 9] depends on [Task 8]
