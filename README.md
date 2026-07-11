# GL4DX12 — Minecraft wgpu/DX12 渲染模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blueviolet)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-green)](https://www.minecraft.net/)
[![Rust](https://img.shields.io/badge/Rust-2021-orange)](https://www.rust-lang.org/)
[![wgpu](https://img.shields.io/badge/wgpu-23-blue)](https://wgpu.rs/)

> 为 Minecraft Java Edition 26.1.2 实现的 DirectX 12 渲染后端，通过 Rust + wgpu + JNI 桥接，将 OpenGL 渲染替换为 D3D12/WebGPU，以解决 TDR 崩溃问题并提升图形性能。

---

## 📖 目录

- [项目概述](#项目概述)
- [整体架构](#整体架构)
- [项目状态](#项目状态)
- [变更日志](#变更日志)
- [技术栈](#技术栈)
- [构建与运行](#构建与运行)
- [配置方法](#配置方法)
- [使用指引](#使用指引)
- [已知问题与解决方案](#已知问题与解决方案)
- [路线图](#路线图)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

## 项目概述

**GL4DX12** 是一个 Fabric 模组，通过 Rust + wgpu 实现 DirectX 12 渲染后端，利用 JNI（Java Native Interface）桥接 Minecraft 的 Java 层与本地渲染引擎。

### 核心设计原则

- **Mixin 智能捕获** — Surface + chunk 就绪时取消 GL 渲染；否则正常 GL 渲染并捕获 framebuffer
- **不创建额外窗口**（直接使用 Minecraft 窗口 HWND）
- **版本通用**（1.21.1 ~ 1.21.11 + 26.x，不依赖 Yarn 映射）
- **双模渲染**：Surface 模式（GL 帧捕获/D3D12 直接渲染）+ Offscreen 模式（像素读回 + PBO 上传）

### 为什么重构为 Rust + wgpu？

| 旧方案 (C++/D3D12) | 新方案 (Rust/wgpu) |
|---------------------|---------------------|
| 手动管理 D3D12 资源 | wgpu 自动资源管理 |
| OpenGL + D3D12 共享 HWND 导致 GPU 设备移除 | GL 帧捕获 + D3D12 swapchain 呈现 |
| 内存安全依赖开发者 | Rust 编译器保证内存安全 |
| 复杂的 C++ 构建配置 | Cargo 依赖管理 |
| TDR 崩溃频发 | GLFW 上下文分离 + GL 帧捕获 |

### 核心优势

- **内存安全**：Rust 编译器在编译期消除 use-after-free、数据竞争等常见 bug
- **跨平台**：wgpu 抽象层支持 DX12/Vulkan/Metal，一次编写多平台运行
- **高性能**：WebGPU 标准驱动的现代 GPU API，接近原生 C++ 性能
- **易维护**：Cargo 生态系统 + 类型系统降低长期维护成本

### 双模渲染架构

| 模式 | 渲染路径 | 适用场景 |
|------|----------|----------|
| **Surface 模式** | Surface 模式：chunk 就绪 → D3D12 直接渲染 MC 场景 / chunk 未就绪 → GL 帧捕获 → D3D12 纹理 present | World 中，MC 正常 GL 渲染或 D3D12 直接渲染 |
| **Offscreen 模式** | Rust wgpu 渲染 → staging buffer → PBO → OpenGL 全屏 quad | Title screen / 初始化阶段 |

### TDR 问题解决原理

Surface 模式的核心挑战：GL + D3D12 同窗口共存会导致 NVIDIA 驱动 TDR 超时。解决方案通过 3 个 Mixin 协同工作：

1. **GameRendererMixin** — HEAD 注入：Surface + chunk 就绪时取消 GL 渲染；否则让 MC 正常 GL 渲染。TAIL 注入：无 chunk 时捕获 framebuffer（FBO-aware），有 chunk 时跳过
2. **MinecraftMixin** — `runTick` TAIL 注入时 `glfwMakeContextCurrent(0)` 分离 GL 上下文，调用 D3D12 Present() 后恢复
3. **GlDeviceMixin** — 取消 `GlDevice.presentFrame()` 的 GL buffer swap，让 D3D12 Present() 成为唯一呈现

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Minecraft 26.1.2                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Fabric Loader 0.19.3                      │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │           Fabric API (ClientTickEvents)          │  │  │
│  │  │  Tick Callback → 全速渲染 → Rust renderFrame()   │  │  │
│  │  │  + 相机 MVP 矩阵提取 → nativeUpdateCamera()      │  │  │
│  │  │  + 相机位置传递 → nativeUpdateCameraPos()        │  │  │
│  │  │  + 区块网格上传 → nativeUploadChunkMesh()        │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │         Mixin: GameRenderer.render() HEAD/TAIL   │  │  │
│  │  │  Surface+chunks: 取消 GL 渲染                    │  │  │
│  │  │  Surface+无chunks: GL 渲染 → 捕获 framebuffer    │  │  │
│  │  │  Offscreen 模式: PBO 纹理上传 + 全屏 quad        │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │         Mixin: Minecraft.runTick() HEAD          │  │  │
│  │  │  glfwMakeContextCurrent(0) → D3D12 Present()     │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│                           ↕ JNI                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              wgpu_mc_jni.dll (Rust)                    │  │
│  │  nativeSetWindow(HWND) → 初始化 DX12 Surface/Swapchain │  │
│  │  nativeRenderFrame() → Surface 模式直接 present,        │  │
│  │                       Offscreen 模式返回 byte[]         │  │
│  │  nativeResize(width, height) → 更新窗口尺寸            │  │
│  │  nativeUpdateCamera(float[16]) → 同步相机 MVP 矩阵     │  │
│  │  nativeSetFramePixels(byte[], w, h) → Surface 模式接收 │  │
│  │                       GL 捕获的 framebuffer 像素       │  │
│  │  nativeUploadChunkMesh(sectionXYZ, buffer, verts, stride) → 上传 MC 区块网格 │  │
│  │  nativeHasChunkGeometry() → 返回当前是否已上传区块网格 |  │
│  └───────────────────────────────────────────────────────┘  │
│                           ↕                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              wgpu-mc (Rust)                            │  │
│  │  wgpu::Instance(DX12) → Adapter → Device + Queue       │  │
│  │  render_frame() → 3D 场景渲染（地面 + 立方体 + 深度）  │  │
│  │  WGSL shader + camera_pos + 共享 IB + 深度测试        │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │ ✅ Surface 模式: chunk→直接渲染 / 无chunk→GL帧捕获 │  │  │
│  │  │ ✅ Offscreen 模式: triple-buffer 异步读回        │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 项目结构

```
dx12-lib-template-26.1.2/
├── fabric/                          # Fabric 模组（Java）
│   ├── src/main/java/com/dx12/
│   │   ├── Dx12Mod.java            # 模组入口，注册事件回调
│   │   ├── D3D12Bridge.java        # JNI 桥接层
│   │   └── mixin/                  # Mixin 渲染管线控制
│   │       ├── GameRendererMixin.java   # GL 帧捕获 / Offscreen PBO 上传
│   │       ├── MinecraftMixin.java      # GLFW 上下文分离 + D3D12 Present
│   │       └── GlDeviceMixin.java       # 取消 GL buffer swap
│   ├── src/main/resources/
│   │   ├── fabric.mod.json         # Fabric 模组描述
│   │   └── gl4dx12.mixins.json     # Mixin 配置
│   ├── build.gradle                # Gradle 构建配置
│   └── gradle.properties           # 版本参数
├── rust/
│   ├── Cargo.toml                  # Workspace 配置
│   ├── wgpu-mc/                    # 核心渲染库
│   │   ├── src/lib.rs              # WmRenderer + Surface/Offscreen 双模
│   │   └── Cargo.toml              # wgpu 23, futures, bytemuck
│   └── wgpu-mc-jni/                # JNI 桥接层
│       ├── src/lib.rs              # 12 个 native 方法
│       └── Cargo.toml              # jni 0.21, log, env_logger
```

---

## 项目状态

### 当前阶段：阶段 1-5 全部完成

| 阶段 | 状态 | 说明 |
|------|------|------|
| **第一阶段：JNI 通信链路** | ✅ 已完成 | Java ↔ Rust 双向通信正常 |
| **第二阶段：wgpu 渲染引擎骨架** | ✅ 已完成 | DX12 adapter + 3D 几何管线 + 双模渲染 |
| **第三阶段：Fabric 事件系统集成** | ✅ 已完成 | ClientTickEvents + PBO 纹理上传 |
| **第四阶段：Surface 模式（GL 帧捕获 + DX12 呈现）** | ✅ 已完成 | MC 正常 GL 渲染 → 捕获 → D3D12 swapchain present |
| **第五阶段：VulkanMod 级全接管** | 🚧 进行中 | Surface 模式已工作，待接入真实游戏场景 |

### 已完成功能

| 模块 | 说明 |
|------|------|
| **Rust Workspace** | `wgpu-mc` (渲染引擎) + `wgpu-mc-jni` (JNI 桥接) 双 crate 结构 |
| **Mixin 框架** | 3 个 Mixin 精确控制 GL/D3D12 共存：GameRendererMixin, MinecraftMixin, GlDeviceMixin |
| **JNI 桥接层** | 12 个 native 方法：`nativeInit`, `nativeHello`, `nativeTestDeviceInfo`, `nativeRenderFrame`, `nativeSetWindow`, `nativeResize`, `nativeUpdateCamera`, `nativeUpdateCameraPos`, `nativeSetFramePixels`, `nativeUploadChunkMesh`, `nativeHasSurface`, `nativeHasChunkGeometry`, `nativeIsReady`, `nativeGetStatus` |
| **Java Fabric 模组** | 基于 Fabric Loom 1.10.3，MC 26.1.2，Fabric API 0.154.2 |
| **DLL 自动加载** | 从 JAR 提取到 `{user.dir}/dx12mod/wgpu_mc_jni.dll`，支持版本隔离 |
| **GPU 适配器检测** | 通过 wgpu 创建 DX12 后端实例并检测适配器可用性 |
| **日志系统** | Rust `env_logger` + Java SLF4J 双端日志 |
| **Surface 模式** | 智能渲染：chunk 就绪 → D3D12 直接渲染 MC 场景 / chunk 未就绪 → GL 帧捕获 → D3D12 纹理 present | World 中，MC 正常 GL 渲染或 D3D12 直接渲染 |
| **Offscreen 模式** | Triple-buffer 异步读回 + PBO 纹理上传（title screen / 初始化阶段） |
| **Chunk 几何上传** | Java → Rust 区块网格数据（vertex + index），D3D12 直接渲染 MC 场景 |
| **相机位置传递** | Java 提取 MC 玩家世界坐标 → `nativeUpdateCameraPos()` → Rust 偏移测试几何体 |
| **相机 MVP 传递** | Java 提取 MC 相机视角 → `nativeUpdateCamera(float[])` → Rust 实时同步（带 LERP 平滑） |
| **3D 几何管线** | WGSL 着色器 + camera_pos + 共享索引缓冲 + 深度测试 + 背面剔除 |
| **地面平面网格** | 200x200 绿色平面（y=0） |
| **彩色立方体网格** | 5 个立方体，每个独立 VB（预烘焙偏移），共享 IB |
| **深度缓冲区** | `Depth32Float` 格式，支持正确遮挡关系 |
| **PBO 纹理上传** | Pixel Buffer Object 绕过 NVIDIA 驱动 DMA 页边界 crash |
| **GL 状态管理** | 完整的 Minecraft GL 状态保存/恢复机制 |
| **资源重载检测** | 自动检测 MC 资源重载并延迟渲染 |
| **VAO 重建机制** | 检测到 GL 资源丢失时自动重建 |
| **独立测试程序** | `examples/simple.rs` — winit + wgpu 弹出窗口渲染彩色三角形 |

### 验收结果

- 模组加载成功，无 crash
- **Surface 模式（智能渲染）**：chunk 就绪时 D3D12 直接渲染 MC 场景；chunk 未就绪时 GL 帧捕获 → D3D12 swapchain present
- **Offscreen 模式**：PBO 纹理上传 + OpenGL 全屏 quad 覆盖（title screen 阶段）
- **GL 帧捕获增强**：FBO-aware，从 MC 实际 draw FBO 读取而非 GL_BACK
- 相机视角随 MC 移动实时更新（MVP 矩阵 + LERP 平滑）
- 相机世界坐标传递，几何体跟随玩家位置
- 进游戏、按 Esc、调设置均无 JVM 崩溃
- TDR 问题已解决：GL 上下文分离 + Mixin 取消 GL swap

---

## 变更日志

### [1.5.0] - 2026-07-08

> **注意：此版本为开发预览版，尚未生成 `.jar` 发布文件。** 需手动构建 Fabric 模组（`gradlew build`）方可运行。

#### Added
- **Chunk 几何上传**：`nativeUploadChunkMesh()` — Java → Rust 区块网格数据（vertex + index），D3D12 直接渲染 MC 场景
- **Smart Surface 渲染**：`render_surface()` 三种模式 — chunk 几何 → D3D12 直接渲染 / GL 帧捕获 → textured quad / 回退到 3D 测试场景
- **FBO-aware 帧捕获**：GameRendererMixin 从 MC 实际 draw FBO 读取而非 GL_BACK
- **Shader camera_pos**：顶点着色器中 `world_pos = pos + camera.camera_pos`，几何体跟随玩家位置
- **`nativeHasChunkGeometry()`** — Java 端检测是否已上传 chunk 几何
- **纹理格式改回 `Rgba8UnormSrgb`**（pipeline fragment target 格式）
- **Chunk mesh 上传**：MC GL_QUADS → D3D12 TriangleList 转换，世界坐标偏移

#### Changed
- **Surface 模式升级为智能渲染**：不再总是 GL 帧捕获，而是根据 chunk 几何可用性选择渲染路径
- **GameRendererMixin 智能判断**：Surface + chunk 就绪时取消 GL 渲染，否则正常 GL 渲染 + 捕获
- **MinecraftMixin 保持 `runTick` TAIL**（不是 render HEAD）
- **Shader 新增 `camera_pos: vec3<f32>`** uniform，几何体偏移跟随玩家
- **`captureFramebufferForD3D12()` FBO 感知**：从 MC draw FBO 读取，而非固定 GL_BACK
- **纹理格式从 `Bgra8UnormSrgb` 改回 `Rgba8UnormSrgb`**

#### Fixed
- Surface 模式下 D3D12 纹理格式不匹配导致的画面异常

---

### [1.4.0] - 2026-07-08

#### Added
- **Mixin 框架（3 个 Mixin）**：精确控制 GL/D3D12 共存，解决 TDR 问题
  - `GameRendererMixin` — HEAD 取消 MC OpenGL 渲染（TAIL 在 offscreen 模式下上传 PBO）
  - `MinecraftMixin` — TAIL 注入时分离 GL 上下文 (`glfwMakeContextCurrent(0)`)，调用 D3D12 Present() 后恢复
  - `GlDeviceMixin` — 取消 `GlDevice.presentFrame()` 的 GL buffer swap
- **Surface 模式恢复**：world 中 DX12 swapchain 直接呈现，零读回、零 PBO
- **全速渲染**：移除 50ms 节流，`render_frame()` 每 tick 调用
- **`create_cube_mesh_at()`** — 支持指定位置和颜色的立方体生成
- **`create_plane_mesh()`** — 地面平面网格生成（200x200 绿色）
- 纹理格式从 `Rgba8UnormSrgb` 改为 `Bgra8UnormSrgb`

#### Changed
- 渲染架构从"Offscreen 为主"升级为"Surface 模式优先"
- `render_frame()` 根据 Surface 模式返回不同结果（Surface 模式直接 present，Offscreen 模式返回 byte[]）
- `onInitializeClient()` 中相机提取改为 in-world 检测（`mc.player != null && mc.level != null`）
- `renderFrame()` 从 Tick Callback 移到 `MinecraftMixin.runTick TAIL` 调用

#### Fixed
- GPU 驱动 TDR 超时（约 2 秒后 GPU 设备移除）— 通过 Mixin 取消 GL 渲染 + GLFW 上下文分离
- 双重 buffer swap 导致的 GPU 竞争 — GlDeviceMixin 取消 GL swap

---

### [1.3.0] - 2026-07-08

#### Added
- 10 个 JNI native 方法完整实现（新增 `nativeIsReady`, `nativeGetStatus`）
- Rust Mutex poison 处理：`lock_or_poisoned()` 防止 panic 级联崩溃
- 共享索引缓冲区：所有立方体共用一个 IB，减少 GPU 内存
- 顶点预烘焙偏移：`create_cube_mesh_at()` 每个立方体独立 VB，移除 push constants

#### Changed
- **Surface 模式暂停**：GPU 驱动 TDR 问题（GL/D3D12 同窗口共存），需 Mixin 取消 GL 渲染后重新启用
- 移除 push constants：`required_features: Features::empty()` 兼容所有 GPU
- 几何体从"5 个独立 VB + 6 面双色"改为"共享 IB + 每立方体独立 VB"
- 架构从双模渲染回归为 Offscreen 模式为主

#### Fixed
- Panic 在 `render_frame()` 中不再导致后续 JNI 调用级联崩溃
- Push constants 在某些 GPU 上不兼容的问题

---

### [1.2.0] - 2026-07-08

#### Added
- **Surface 模式（DX12 直接呈现）**：`nativeSetWindow` 后创建 swapchain，`render_frame()` 直接 present 到窗口，零读回
- **Offscreen 模式（Triple-buffer 异步读回）**：三槽环形缓冲 + 异步 map_async + Poll 轮询
- **`nativeHasSurface()`**：Java 端检测当前是否为 Surface 模式
- **相机矩阵 LERP 平滑**：`mat4_lerp(camera_prev, camera_target, 0.3)` 避免抖动
- **Surface 自适应 resize**：Surface 模式下自动 reconfigure swapchain
- **Surface 错误恢复**：`SurfaceError::Outdated/Lost` 时自动 reconfigure
- **Offscreen 模式回退**：Surface 模式失败时自动使用 triple-buffer 读回
- 8 个 JNI native 方法完整实现

#### Changed
- `render_frame()` 根据 Surface 模式返回不同结果（Surface 模式返回空 Vec）
- 节流策略保持 50ms（~20fps），Surface 模式下无性能瓶颈
- 架构从单一读回路径升级为双模渲染

#### Fixed
- Surface 模式下不再需要 PBO 纹理上传，消除了 NVIDIA 驱动 DMA 相关的所有 crash
- 相机矩阵抖动（LERP 平滑替代突变）

---

### [1.1.0] - 2026-07-08

#### Added
- 3D 几何管线：WGSL 着色器 + push constants（model matrix）+ 深度测试 + 背面剔除
- 地面平面网格：200x200 绿色地面（y=0）
- 5 个彩色立方体：不同位置摆放，每个面有明暗区分
- 相机 MVP 矩阵传递：Java 提取 MC 相机视角 → `nativeUpdateCamera(float[16])` → Rust 实时同步
- 深度缓冲区：`Depth32Float` 格式，支持正确遮挡关系
- PBO（Pixel Buffer Object）纹理上传：绕过 NVIDIA 驱动 DMA 页边界 crash
- 资源重载检测：自动检测 MC 资源重新加载并重置渲染状态
- VAO/Shader 自动重建：检测到 GL 资源失效时自动重建，无需重启游戏

#### Changed
- `render_frame()` 从纯色背景升级为完整 3D 场景渲染
- JNI 桥接从 6 个方法扩展至 7 个方法（新增 `nativeUpdateCamera`）
- 节流策略调整为 50ms（~20fps）

#### Fixed
- Minecraft 菜单打开时 GL 资源被销毁导致崩溃的问题
- 资源重载期间渲染冲突问题
- 纹理名称重复使用导致的渲染异常

---

### [1.0.0] - 2026-07-08

> **注意：此版本为开发预览版，尚未生成 `.jar` 发布文件。** 需手动构建 Fabric 模组（`gradlew build`）方可运行。

#### Added
- 完整的 GL 状态管理机制：保存/恢复 Minecraft VAO、Texture、Program、Blend、Depth 状态
- 资源重载检测：通过 tick 时间间隔判断 MC 资源重新加载，自动重置渲染状态
- VAO/Shader 自动重建：检测到 GL 资源失效时自动重建，无需重启游戏
- PBO（Pixel Buffer Object）纹理上传：绕过 NVIDIA 驱动 DMA 页边界 crash
- 节流策略：每 50ms 调用一次 Rust 渲染（~20fps）
- 独立测试程序 `examples/simple.rs`：winit + wgpu 弹出窗口渲染彩色三角形
- GitHub Actions CI 工作流 (`.github/workflows/build.yml`)

#### Changed
- 渲染流程从简单贴图升级为完整的 GL 状态隔离方案
- `Dx12Mod.java` 采用 try-finally 结构确保 GL 状态始终恢复
- JNI 桥接从 3 个方法扩展至 6 个方法
- 架构文档更新为实际的方法名和流程

#### Fixed
- Minecraft 菜单打开时 GL 资源被销毁导致崩溃的问题
- 资源重载期间渲染冲突问题
- 纹理名称重复使用导致的渲染异常
- OpenGL + D3D12 共享 HWND 导致的 GPU 设备移除崩溃
- Gradle wrapper SSL 证书问题
- JNI 库加载路径问题

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏 | Minecraft | 26.1.2 |
| 加载器 | Fabric Loader | 0.19.3 |
| API | Fabric API | 0.154.2+26.1.2 |
| 语言 | Java | 25 |
| 图形 | wgpu (WebGPU) → DX12 | 23 |
| 语言 | Rust | 2021 edition |
| JNI | jni crate | 0.21 |
| 渲染 | OpenGL (Java 端，Offscreen 模式) | Core Profile 330 |
| 注入 | SpongePowered Mixin | 通过 Fabric API |

---

## 构建与运行

### 系统要求

- **Windows 10/11** (x64)
- **JDK 25** (推荐 BellSoft Liberica JDK 或 Adoptium)
- **Rust 1.75+** (stable)
- **Gradle 8.13** (或通过 wrapper)

### 环境配置

#### 1. 安装 Rust

```powershell
# 从 https://rustup.rs/ 下载安装，或：
rustup default stable
rustup component add rust-analyzer rust-src
```

#### 2. 安装 JDK 25

```powershell
# 确认 Java 版本
java -version
# 应输出 Java 25.x.x

# 如未安装，推荐使用 BellSoft Liberica JDK:
# https://bell-sw.com/pages/downloads/?version=java-25&os=Windows+amd64
```

#### 3. 配置环境变量 (可选)

```powershell
# 设置 JAVA_HOME (如果尚未设置)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.x"
```

### 构建步骤

#### 方式 A：分别构建 (推荐调试用)

```powershell
# 1. 构建 Rust DLL
cd rust
cargo build --release
# 输出: target/release/wgpu_mc_jni.dll

# 2. 构建 Fabric 模组
cd fabric
gradlew build
# 输出: build/libs/gl4dx12-*.jar
```

#### 方式 B：一键构建

```powershell
cd fabric
gradlew clean build --no-daemon
```

### 部署到 Minecraft

```powershell
# 1. 复制 JAR 到 mods 目录
copy fabric\build\libs\gl4dx12-*.jar ^
     "$env:APPDATA\.minecraft\versions\26.1.2-Fabric_0.19.3\mods\"

# 2. 复制 DLL 到 dx12mod 目录
copy rust\target\release\wgpu_mc_jni.dll ^
     "$env:APPDATA\.minecraft\versions\26.1.2-Fabric_0.19.3\dx12mod\"

# 3. 启动 Minecraft 26.1.2-Fabric_0.19.3
```

> **注意**：模组打包时 DLL 嵌入 JAR，运行时会自动提取到 `{user.dir}/dx12mod/wgpu_mc_jni.dll`。

### 验证安装

启动游戏后，检查日志应看到：

```
[INFO] GL4DX12 Mod initializing...
[D3D12Bridge] Native library loaded from: ...\dx12mod\wgpu_mc_jni.dll
[D3D12Bridge] Rust JNI library initialized.
[INFO] Rust responded: Hello from Rust wgpu! You said: Hello from Minecraft!
[INFO] Device info: wgpu-mc-jni loaded. DX12: READY
[dx12-wm] Creating WmRenderer 800x600 (triple-buffer + surface support)
[dx12-wm] Adapter: NVIDIA GeForce RTX 3080
[dx12-wm] Device created OK
[dx12-wm] Offscreen slots created (800x600)
[INFO] GL4DX12 Mod initialized!
```

进入游戏中，当 HWND 可用后会看到：

```
[dx12-wm] Surface OK! HWND=0x123456 fmt=Bgra8UnormSrgb 1920x1080
```

此时渲染模式自动切换到 **Surface 模式**（world 中），MC 正常 GL 渲染 → 帧捕获 → D3D12 swapchain present。

---

## 配置方法

### Minecraft 版本配置

编辑 `fabric/gradle.properties`：

```properties
minecraft_version=26.1.2
yarn_mappings=26.1.2+build.1
loader_version=0.19.3
fabric_version=0.154.2+26.1.2
```

### Rust 构建配置

编辑 `rust/wgpu-mc-jni/Cargo.toml` 调整依赖：

```toml
[dependencies]
jni = "0.21"
log = "0.4"
env_logger = "0.10"
wgpu-mc = { path = "../wgpu-mc" }
```

### DLL 加载路径

模组运行时自动提取 DLL 到：

```
{user.dir}/dx12mod/wgpu_mc_jni.dll
```

例如版本隔离目录：
```
D:\.minecraft\versions\26.1.2-Fabric_0.19.3\dx12mod\wgpu_mc_jni.dll
```

### 日志级别控制

```powershell
# Rust 端日志 (通过环境变量)
$env:RUST_LOG = "debug"  # 或 info, warn, error
java -jar minecraft.jar
```

---

## 使用指引

### 快速开始

1. 按照 [构建与运行](#构建与运行) 章节完成构建
2. 将 JAR 和 DLL 部署到 Minecraft 目录
3. 启动 Minecraft 26.1.2-Fabric_0.19.3
4. 观察控制台日志确认模组加载成功
5. 进入游戏验证 — Surface 模式下 MC 正常 GL 渲染，D3D12 swapchain 呈现

### 调试技巧

#### 检查 Rust DLL 是否加载

```powershell
# 确认 DLL 文件存在
dir "$env:APPDATA\.minecraft\versions\26.1.2-Fabric_0.19.3\dx12mod\wgpu_mc_jni.dll"

# 检查 DLL 依赖 (需 Dependency Walker 或 dumpbin)
dumpbin /dependents rust\target\release\wgpu_mc_jni.dll
```

#### 查看 Rust 日志

```powershell
# 设置日志级别
$env:RUST_LOG = "debug"

# 运行 Minecraft (日志输出到 latest.log)
```

#### 验证 JNI 通信

模组启动时会自动执行以下测试：
- `nativeInit()` — 初始化 Rust 环境，创建 WmRenderer
- `nativeHello("Hello from Minecraft!")` — 双向字符串传递
- `nativeTestDeviceInfo()` — GPU 适配器检测
- `nativeSetWindow(hwnd)` — 传递 MC 窗口句柄，创建 DX12 swapchain
- `nativeRenderFrame()` — 每帧渲染（Surface 模式直接 present，Offscreen 模式返回 byte[]）
- `nativeUpdateCamera(float[])` — 传递 MC 相机 MVP 矩阵
- `nativeUpdateCameraPos(x, y, z)` — 传递 MC 相机世界坐标
- `nativeHasSurface()` — 检测当前是否为 Surface 模式
- `nativeIsReady()` — 返回 1/0/-1 状态码
- `nativeGetStatus()` — 返回人类可读状态字符串
- `nativeSetFramePixels(byte[], w, h)` — Surface 模式接收 GL 捕获的像素

#### 运行独立测试程序

```powershell
# 在 rust/wgpu-mc 目录下运行
cd rust\wgpu-mc
cargo run --example simple
# 弹出 1280×720 窗口，渲染红绿蓝三色三角形
```

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `NoClassDefFoundError: net/minecraft/client/Minecraft` | JAR 版本过旧 | 重新编译并复制最新 JAR |
| `UnsatisfiedLinkError: wgpu_mc_jni.dll` | DLL 路径不正确 | 确认 DLL 在 `dx12mod/` 目录下 |
| `Unsupported class file major version 69` | JDK 版本不匹配 | 使用 JDK 25 编译 (非 JDK 21) |
| `Incompatible mods found!` | fabric.mod.json 版本声明错误 | 确认 `"minecraft": "~26.1.2"` |

---

## 已知问题与解决方案

| 问题 | 原因 | 解决方案 | 状态 |
|------|------|----------|------|
| `glTexImage2D(pixels)` 一步完成 crash | NVIDIA 驱动 bug | 改用 `glTexImage2D(null)` + `glTexSubImage2D(pixels)` 两步 | ✅ 已修复 |
| `glTexSubImage2D` 在页边界 ACCESS_VIOLATION | NVIDIA 驱动按页粒度 DMA 读取客户端内存不稳定 | **PBO 方案**：`glMapBuffer` + CPU memcpy + `glTexSubImage2D(offset=0)` 从 GPU 内存上传 | ✅ 已修复 |
| `MemoryUtil.memAlloc` crash | LWJGL allocator 页对齐与 nvoglv64 不兼容 | 改用 `BufferUtils.createByteBuffer` | ✅ 已修复 |
| 按 Esc/设置菜单 crash | HUD callback 与 Screen 渲染 GL 状态冲突 | `currentScreen != null` 时跳过 GL 绘制 | ✅ 已修复 |
| 纹理闪烁不完整 | 窗口 resize 后纹理尺寸不匹配 | `texWidth`/`texHeight` 追踪 + 自动重建 | ✅ 已修复 |
| 资源重载后渲染 crash | GL 状态未清理 | 重载检测时重置 `vaoId`/`shaderValid`/`texAllocated`/`pendingPixels` | ✅ 已修复 |
| 调试日志导致卡顿 | 每 tick 写磁盘 | 移除所有 `C:\tmp\` 文件日志 | ✅ 已修复 |
| `setWindow` 重复调用 | JNI 开销 | `lastSetHwnd` 缓存 | ✅ 已修复 |
| **GPU TDR 超时（~2s 后崩溃）** | GL + D3D12 同窗口共存导致 WDDM 驱动级冲突 | **3 个 Mixin**：取消 GL swap + GLFW 上下文分离 | ✅ 已修复 |

---

## 路线图

### 阶段 1：JNI 通信链路 ✅ 已完成

| 任务 | 状态 |
|------|------|
| Rust Workspace 搭建 | ✅ |
| JNI 桥接层实现 | ✅ |
| Java Fabric 模组 | ✅ |
| DLL 自动加载 | ✅ |
| GPU 适配器检测 | ✅ |

### 阶段 2：wgpu 渲染引擎骨架 ✅ 已完成

| 任务 | 状态 | 说明 |
|------|------|------|
| WmRenderer 创建 | ✅ | wgpu DX12 Instance → Adapter → Device |
| 3D 几何管线 | ✅ | WGSL 着色器 + 共享 IB + 深度测试 + 背面剔除 |
| 地面网格 | ✅ | 200x200 绿色平面 |
| 立方体网格 | ✅ | 5 个独立 VB + 共享 IB |
| 深度缓冲 | ✅ | `Depth32Float` 格式 |
| 像素回传 | ✅ | Rust → Java byte[] → PBO → OpenGL 纹理 → 全屏 Quad |
| 独立测试程序 | ✅ | `examples/simple.rs` 可独立运行渲染三角形 |
| 窗口尺寸同步 | ✅ | `nativeResize()` 更新渲染器尺寸 |
| 相机 MVP 同步 | ✅ | Java → Rust 实时传递 MC 相机视角（带 LERP 平滑） |
| 相机位置同步 | ✅ | Java → Rust 实时传递 MC 玩家世界坐标 |

### 阶段 3：Fabric 事件系统集成 ✅ 已完成

| 任务 | 状态 | 说明 |
|------|------|------|
| ClientTickEvents | ✅ | 计时、资源重载检测、相机提取、调用 Rust 渲染 |
| PBO 纹理上传 | ✅ | glMapBuffer + CPU memcpy + glTexSubImage2D(offset=0) |
| GL 状态管理 | ✅ | 完整的保存/恢复机制 |
| VAO/Shader 持久化 | ✅ | 首次创建，丢失后自动重建 |

### 阶段 4：Surface 模式（GL 帧捕获 + DX12 呈现）✅ 已完成

| 任务 | 状态 | 说明 |
|------|------|------|
| Surface 创建 | ✅ | `create_surface_from_hwnd()` 基于 raw-window-handle |
| Swapchain 配置 | ✅ | `present_mode: Immediate` + `desired_maximum_frame_latency: 2` |
| Surface 渲染 | ✅ | `render_surface()` 直接 present 到窗口 |
| Surface 自适应 resize | ✅ | 窗口尺寸变化时 reconfigure |
| Surface 错误恢复 | ✅ | Outdated/Lost 时自动 reconfigure |
| 双模切换 | ✅ | `nativeSetWindow` 后自动切换到 Surface 模式 |
| Triple-buffer 读回 | ✅ | Offscreen 模式三槽环形缓冲 + 异步 map_async |
| **Mixin 框架** | ✅ | 3 个 Mixin 解决 TDR 问题 |
| GLFW 上下文分离 | ✅ | `glfwMakeContextCurrent(0)` 临时分离 GL 上下文 |
| GL 帧捕获 | ✅ | GameRendererMixin TAIL `glReadPixels` → D3D12 纹理 |
| GL swap 取消 | ✅ | GlDeviceMixin 取消 buffer swap |

### 阶段 5：VulkanMod 级全接管 🚧 进行中

当前 Surface 模式已实现 MC 原生 GL 渲染 → 帧捕获 → D3D12 swapchain present。要实现真实 Minecraft 场景渲染（类似 VulkanMod），需要：

| 任务 | 优先级 | 说明 |
|------|--------|------|
| **区块渲染** | 🔴 P0 | 预计算 Chunk Mesh + 顶点缓冲，零读回 |
| **天空盒与云雾** | 🟠 P1 | 简单着色器即可 |
| **实体渲染** | 🟠 P1 | 模型加载 + 骨骼动画 |
| **粒子系统** | 🟡 P2 | 点精灵 (point sprites) |
| **半透明物体排序** | 🟡 P2 | 深度排序算法 |
| **后期特效** | 🟢 P3 | 泛光、阴影、色调映射 |

#### 参考：wgpu-mc RenderGraph 设计

```yaml
passes:
  - name: "sky_pass"
    render_target: "main"
    shader: "sky.wgsl"
    depth_test: false

  - name: "terrain_pass"
    render_target: "main"
    shader: "terrain.wgsl"
    depth_test: true

  - name: "entities_pass"
    render_target: "main"
    shader: "entity.wgsl"
    depth_test: true
    blending: alpha

  - name: "particles_pass"
    render_target: "main"
    shader: "particle.wgsl"
    depth_test: false
    blending: alpha

  - name: "overlay_pass"
    render_target: "main"
    shader: "overlay.wgsl"
    depth_test: false
    # HUD、小地图、Crosshair 等由 MC 原生 OpenGL 渲染
```

---

## 贡献指南

### 参与方式

##### 暂时不接受任何贡献

### 当前可认领的任务

| 任务 | 优先级 | 说明 |
|------|--------|------|
| 区块渲染 | 🔴 P0 | 预计算 Chunk Mesh，DX12 直接渲染 |
| 天空盒渲染 | 🟠 P1 | 简单着色器即可 |
| 实体渲染 | 🟠 P1 | 模型加载 + 骨骼动画 |

---

## 许可证

MIT License
