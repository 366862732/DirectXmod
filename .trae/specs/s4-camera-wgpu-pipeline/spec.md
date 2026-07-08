# Phase 4 Step 1: Camera Data + wgpu Rendering Pipeline Spec

## Why
当前 `render_frame()` 只在 CPU 上生成纯蓝色数组，没有使用 wgpu 的 Device/Queue。要让 Rust 真正利用 GPU 渲染，需要：1) 从 Minecraft 获取摄像机数据传给 Rust，2) Rust 端建立真正的 wgpu 渲染管线（Shader + Buffer + RenderPass），3) 从 GPU 纹理读回像素数据。

这是第四阶段的第一步，验证 Minecraft → JNI → Rust wgpu GPU 渲染 → 像素读回 → Java OpenGL 绘制的完整链路。

## What Changes
- **Java 端**: 新增 tick callback 中提取摄像机 position/rotation/yaw/pitch/FOV，通过新的 JNI 方法 `nativeUpdateCamera(float[16])` 传给 Rust（传入 view-projection 矩阵，16 floats）
- **Rust JNI 层**: 新增 `nativeUpdateCamera` 方法，接收 float[16] 矩阵，存入 Mutex
- **Rust wgpu 层**: 替换 `render_frame()` 的 CPU fill 为真正的 GPU 渲染管线：
  - WGSL vertex shader（接收 uniform MVP 矩阵 + vertex position/color）
  - WGSL fragment shader（直接输出颜色）
  - Vertex buffer（一个彩色三角形的顶点）
  - Uniform buffer（MVP 矩阵）
  - Render pipeline + render pass（渲染到 offscreen 纹理）
  - 纹理 readback（copy_texture_to_buffer + map_buffer 读回 CPU）
- **已移除**: `render_frame` 不再写 `C:\tmp\wgpu_debug.txt` 文件日志。`nativeSetWindow` 不再写 `C:\tmp\wgpu_mc_initialized.txt` / `C:\tmp\wgpu_mc_error.txt` / `C:\tmp\wgpu_debug.txt` 等文件日志。`nativeInit` 不再写 `C:\tmp\wgpu_mc_init.txt`。所有文件 I/O debug 日志全部移除。

## Impact
- Affected specs: 无（这是第一个 Phase 4 spec）
- Affected code: `Dx12Mod.java`（新增摄像机数据提取）、`D3D12Bridge.java`（新增 nativeUpdateCamera 声明）、`wgpu-mc-jni/src/lib.rs`（新增 nativeUpdateCamera）、`wgpu-mc/src/lib.rs`（重构为 GPU 渲染管线）

## ADDED Requirements

### Requirement: Minecraft 摄像机数据提取
系统 SHALL 在每个 tick 回调中从 Minecraft 的 `GameRenderer.getCamera()` 获取摄像机 view-projection 矩阵，并通过 JNI 传递给 Rust。

#### Scenario: 正常游戏视角
- **WHEN** 玩家在游戏中移动视角
- **THEN** Rust 端收到的 MVP 矩阵随视角变化而变化
- **AND** 渲染的三角形在屏幕上移动

#### Scenario: 窗口未就绪
- **WHEN** GLFW context 为 0 或 GameRenderer 未初始化
- **THEN** 跳过摄像机数据提取，不调用 `nativeUpdateCamera`

### Requirement: Rust wgpu GPU 渲染管线
系统 SHALL 使用 wgpu 的 Device/Queue 进行真正的 GPU 渲染，替换当前的 CPU数组填充。

#### Scenario: 渲染彩色三角形
- **WHEN** `nativeRenderFrame()` 被 Java 调用
- **THEN** wgpu 创建 command encoder → render pass（使用 WGSL shader 和 vertex buffer）→ 渲染到离屏纹理 → 拷贝纹理到 staging buffer → 读回为 RGBA byte array
- **AND** 返回的 byte[] 大小 = width * height * 4

#### Scenario: Wgpu 资源复用
- **WHEN** `render_frame()` 被多次调用
- **THEN** pipeline、vertex buffer、shader module 只创建一次，后续帧复用
- **AND** staging buffer 在每帧末尾 map_read + unmap

### Requirement: 移除 Rust 文件 I/O 调试日志
系统 SHALL 移除 `nativeInit`、`nativeSetWindow`、`nativeRenderFrame` 中所有写 `C:\tmp\` 文件的代码。文件日志在开发调试阶段有用，但已验证管线正常后应移除。

#### Scenario: nativeInit 调用
- **WHEN** `nativeInit()` 被调用
- **THEN** 只保留 `env_logger::try_init()`，不写 `C:\tmp\wgpu_mc_init.txt`

#### Scenario: nativeSetWindow 调用
- **WHEN** `nativeSetWindow()` 被调用
- **THEN** 不写 `C:\tmp\wgpu_setwindow_entered.txt` / `C:\tmp\wgpu_mc_initialized.txt` / `C:\tmp\wgpu_mc_error.txt` / `C:\tmp\wgpu_debug.txt`

#### Scenario: nativeRenderFrame 调用
- **WHEN** `nativeRenderFrame()` 被调用
- **THEN** 不写 `C:\tmp\wgpu_debug.txt`，不构造 debug_log 字符串
