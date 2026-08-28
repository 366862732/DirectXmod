# DX12 渲染性能优化与 CPU/GPU 多线程架构 Spec

## Why

DX12 后端在拉高区块渲染距离时非常卡顿、远处区块全黑、物品栏不显示物品、物品栏人物模型上下颠倒。日志证据表明卡顿主要来自：**native 层无条件诊断输出（每次 pushDescriptors 必打日志、waitQFence 每条 WARN 日志）+ 每帧 GPU 全队列同步气泡（dbgReadback 内部 deviceWaitIdle，会话内 20,675 次）+ 每条日志 2 次文件 I/O**，而非纯粹的 GPU 算力不足。同时渲染结构仍为单线程命令录制，CPU 帧时间高。本变更同时解决上述渲染症状并落地 CPU/GPU 多线程异步渲染架构（依据《多线程方案.md》）。

## What Changes

- **P30 抑制 native 层诊断输出**：移除/门控所有高频诊断日志（无条件 fprintf、WARN 级 per-call/per-frame 日志、InfoQueue dump），日志镜像文件改为可选且复用句柄。
- **P30 消除每帧 GPU 同步气泡**：移除 vbDbg/ubDbg/projDbg 每 60 次读回、srv/font/color 纹理读回内部的 `deviceWaitIdle` 与 BMP dump；`deviceWaitIdle` 仅在 surface configure/destroy 等生命周期路径保留。
- **P31 修复物品栏**：物品图标不显示（GUI 图集/纹理采样或 GUI pass 绘制问题）→ 诊断并修复。
- **P31 修复人物倒过来**：Y-flip 目前仅注入 `animate_sprite` 管线；GUI 相机 3D 模型渲染（entity 管线）需做正确的坐标翻转（viewport Y 翻转或投影修正）。
- **P32 修复远处全黑**：确认区块网格构建/上传与雾/深度远平面问题并修复，拉高视距后远处区块可正常显示。
- **P33 CPU/GPU 多线程异步渲染架构**：依据《多线程方案.md》第 1-8 章落地——FrameBuilder → 无锁命令队列 → Worker Pool（Bundle 录制）→ 主 Command List 执行器 → GPU 4-5 帧飞行 + Fence 异步通知，接入 `Dx12CommandEncoderBackend`/`Dx12Backend` 渲染循环，降低 CPU 帧时间。

## Impact

- Affected specs: `GpuBackend` / `CommandEncoderBackend` / `GpuSurfaceBackend` 接口实现（DX12 后端）
- Affected code:
  - `native/src/jni_bridge_p6.cpp`（dx12PushDescriptors 无条件 fprintf）
  - `native/src/dx12_device.cpp`（waitQFence/waitFence 日志、pushDescriptors 日志与读回、dbgReadback*、deviceWaitIdle 调用点、setVertexBuffer vbDbg）
  - `native/src/dx12_surface.cpp`（blitSurface 每帧日志、readbackSurfacePixels）
  - `native/src/dx12_device.h`（日志级别/DBG_LOG_DEBUG 门控）
  - `native/src/jni_bridge.cpp` / `jni_bridge_p3.cpp` / `jni_bridge_p5.cpp`（日志级别配置、surface 路径）
  - `fabric/.../Dx12ShaderCompiler.java`（Y-flip 注入范围）
  - `fabric/.../Dx12CommandEncoderBackend.java` / `Dx12Backend.java` / `Dx12GpuSurface.java`（多线程渲染循环接入）
  - 新增（P33）：`fabric/.../async/`（AsyncRenderCommand、FrameData、FrameManager）、`native/src/`（dx12_bundle_recorder.h/cpp、dx12_descriptor_allocator.h、dx12_fence_manager.h/cpp、dx12_main_executor.h/cpp、lock-free 命令队列）

## ADDED Requirements

### Requirement: Native 诊断输出门控（P30）

系统 SHALL 确保所有高频诊断日志（每个 JNI 调用、每帧、per-binding）默认不输出；仅当显式开启诊断开关（环境变量或日志级别 ≥ DEBUG）时输出。

#### Scenario: 正常游戏运行时无日志风暴
- **WHEN** 用户启动游戏并以默认日志级别运行（gLogLevel=1 WARN）
- **THEN** stderr 与 `%TEMP%\dx12-native.log` 不再出现 `[dx12-jni] dx12PushDescriptors`、`waitQFence`、每帧 `blitSurface`、`pushDesc BIND/CBV/UBO_BIND` 等逐调用/逐帧日志；日志行数从数十万级降为每次会话 < 1 千行（不含着色器编译等一次性日志）

#### Scenario: 诊断模式仍可开启
- **WHEN** 用户设置环境变量（如 `DX12_LOG_VERBOSE=1` 或日志级别 3）
- **THEN** 原有诊断日志完整可用，用于问题排查

### Requirement: 渲染热路径无 GPU 同步气泡（P30）

系统 SHALL 保证普通帧渲染路径（beginCommandList → draw 录制 → submit → present）内不调用 `deviceWaitIdle`，不做任何 buffer/texture 读回与 BMP dump。

#### Scenario: 连续渲染 100 帧无阻塞
- **WHEN** 用户进入游戏并停留视角
- **THEN** 日志中 `deviceWaitIdle: enter` 不再出现在帧渲染期间（仅在 surface 配置/销毁等生命周期路径出现），`waitQFence` 调用为快速非阻塞路径，帧率恢复流畅（拉高视距后无明显卡顿）

### Requirement: 物品栏正常显示（P31）

系统 SHALL 正确渲染物品栏图标与物品栏内的 3D 模型，方向与 OpenGL/Vulkan 参考后端一致。

#### Scenario: 打开物品栏
- **WHEN** 用户按 E 打开物品栏
- **THEN** 物品栏面板、物品图标均正常显示（非黑/非空），玩家模型方向正确（头朝上，不颠倒），与原生渲染方向一致

#### Scenario: 正常视角渲染不受影响
- **WHEN** 用户在世界中观察方块与实体
- **THEN** 主世界 3D 渲染方向保持正确（Y-flip 修正不影响主世界渲染）

### Requirement: 拉高视距后区块正常渲染（P32）

系统 SHALL 在用户提高渲染距离后，远处区块正常构建、上传并绘制，不出现全黑。

#### Scenario: 视距拉高到 10+
- **WHEN** 用户在选项中将渲染距离调到 10 或更高并保持视角
- **THEN** 远处区块在合理时间内出现（有地面/地形，非全黑），游戏不再因性能原因自动把视距降到 2，帧率保持流畅

### Requirement: CPU/GPU 多线程异步渲染（P33）

系统 SHALL 依据《多线程方案.md》实现异步渲染管线：主线程 FrameBuilder 提交渲染命令到无锁队列，Worker Pool 并行录制 Bundle，主 Command List 执行器聚合提交 GPU，支持 4-5 帧飞行与 Fence 异步通知。

#### Scenario: 帧时间显著下降
- **WHEN** 运行渲染负载（拉高视距、包含区块+实体+GUI 的完整帧）
- **THEN** CPU 帧时间显著下降（目标较单线程基线有明显改善，GPU 侧有 4-5 帧并行执行），渲染不卡顿、不黑屏、无设备移除错误

#### Scenario: 资源生命周期安全
- **WHEN** 多帧飞行期间创建/销毁 buffer、texture、surface，窗口 resize
- **THEN** 不出现 use-after-free、DXGI_ERROR_DEVICE_REMOVED、描述符堆越界等错误；延迟销毁按 fence 完成回调执行

## MODIFIED Requirements

### Requirement: 日志级别体系（原 P12/P16/P18-P29 诊断）

原日志体系在 WARN 级别输出逐调用/逐帧诊断，导致 995,549 行/72 秒的日志风暴与严重卡顿。修改为：WARN 仅输出真实错误；INFO 输出生命周期摘要（surface 配置、encoder 创建等低频事件）；DEBUG 输出逐帧/逐调用诊断。`dbgLog` 的 `%TEMP%\dx12-native.log` 镜像改为可选（环境变量开启）且复用已打开句柄，不做逐行 fopen/fclose。

## REMOVED Requirements

### Requirement: 每 60 次调用的 buffer/纹理读回诊断（vbDbg/ubDbg/projDbg、srv/font/color 读回）

**Reason**: 这些诊断在正常游戏路径中触发 `deviceWaitIdle`（20,675 次/会话），制造 GPU 全队列同步气泡，是卡顿的直接原因；其诊断价值已由 P30 门控诊断模式覆盖。
**Migration**: 如需读回诊断，通过 `DX12_LOG_VERBOSE=1`/DEBUG 日志级别启用，且不影响帧渲染性能。
