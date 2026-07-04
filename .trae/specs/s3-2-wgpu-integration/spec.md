# S3-2: wgpu 渲染引擎与 MC 窗口 Surface 绑定 Spec

## Why
S3-1 已完成 GameRenderer Mixin 拦截和 JNI 钩子，但 Rust 端的 `nativeSetWindow()` 和 `nativeRenderFrame()` 仍是空壳——wgpu Surface 尚未绑定到 MC 窗口。S3-2 的目标是实现 HWND → wgpu Surface 的完整链路，使 wgpu 能在 MC 窗口中渲染。

## What Changes
- 实现 `WmRenderer::from_hwnd()` 的完整逻辑，解决 raw-window-handle 0.6 / wgpu 23 的 trait 兼容性问题
- 采用 winit 包装层方案：在 Rust 端创建隐藏的 winit Window，通过 winit 的 `HasWindowHandle` trait 创建 wgpu Surface
- 实现 `nativeRenderFrame()` 实际渲染循环
- 更新 Rust JNI 桥接层，存储并复用 WmRenderer 实例

## Impact
- Affected specs: 无（独立于旧 C++ 方案）
- Affected code: `rust/wgpu-mc/src/lib.rs`, `rust/wgpu-mc-jni/src/lib.rs`, `rust/wgpu-mc/Cargo.toml`

## ADDED Requirements

### Requirement: WmRenderer 从 HWND 创建 Surface
系统 SHALL 提供 `WmRenderer::from_hwnd(u64)` 方法，接收 Windows HWND 并创建 wgpu Surface。

#### Scenario: 有效 HWND 创建成功
- **WHEN** Java 调用 `nativeSetWindow(hwnd)` 传入有效的 MC 窗口句柄
- **THEN** Rust 端创建 WmRenderer，Surface 绑定到该窗口，日志输出 "WmRenderer created from HWND 0x..."

#### Scenario: 无效 HWND 失败
- **WHEN** Java 调用 `nativeSetWindow(0)` 或传入空指针
- **THEN** 返回错误并记录 "Invalid HWND" 日志，不创建 Surface

### Requirement: nativeRenderFrame 实际渲染
系统 SHALL 实现 `nativeRenderFrame()` 调用 `WmRenderer.render_frame()` 实际渲染一帧。

#### Scenario: 渲染器已初始化
- **WHEN** Java 调用 `nativeRenderFrame()` 且 renderer 已创建
- **THEN** Rust 端调用 `render_frame()` 渲染一帧，无崩溃

#### Scenario: 渲染器未初始化
- **WHEN** Java 调用 `nativeRenderFrame()` 但 renderer 尚未创建
- **THEN** 记录警告日志 "renderer not initialized"，不崩溃

### Requirement: WmRenderer 单例管理
系统 SHALL 使用线程安全的单例管理 WmRenderer 实例，支持跨 JNI 调用复用。

#### Scenario: 多次 nativeSetWindow 调用
- **WHEN** Java 多次调用 `nativeSetWindow(hwnd)`
- **THEN** 仅在首次调用时创建 renderer，后续调用跳过（避免重复创建）

## MODIFIED Requirements

### Requirement: from_hwnd 兼容性
**之前**: `from_hwnd` 是 placeholder，返回 `SurfaceError::Lost`
**修改后**: 通过 winit 包装层实现完整的 HWND → Surface 绑定

## REMOVED Requirements
### Requirement: from_hwnd_placeholder
**Reason**: 已被完整的 `from_hwnd()` 实现替代
**Migration**: 删除 `from_hwnd_placeholder()` 方法
