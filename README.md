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

- **Mixin 智能捕获** — Surface + chunk 就绪时取消 GL 世界渲染；否则正常 GL 渲染并捕获 framebuffer
- **不创建额外窗口**（直接使用 Minecraft 窗口 HWND）
- **版本通用**（1.21.1 ~ 1.21.11 + 26.x，不依赖 Yarn 映射）
- **三通道渲染**（Surface 模式）：Pass 1 世界渲染 → Pass 2 后处理 (FXAA/tonemapping/gamma) → Pass 3 HUD 叠加
- **双通道 Chunk 渲染**：Opaque Pass (depth_write=true) + Transparent Pass (alpha blending) 处理植被 CUTOUT/TRANSLUCENT
- **ChunkVertex 40 字节**：位置 + 颜色 + 地形 UV + 光照 UV，支持动态 lightmap
- **PBO 异步 HUD 读回**：3×PBO 三缓冲 + glFlush 替代 glFinish，消除 CPU-GPU 同步阻塞
- **Rust wgpu 引擎完全独立于 Minecraft**，通过 JNI 通信，panic 被 catch_unwind 安全捕获

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
| **Surface 模式**（智能渲染） | Pass 1: chunks/entities/particles → post_texture + depth<br>Pass 2: FXAA/tonemapping/gamma → swapchain<br>Pass 3: HUD overlay (alpha blending) → swapchain | World 中，D3D12 直接渲染 MC 场景（有 chunk）或 GL 帧捕获呈现（无 chunk） |
| **Offscreen 模式** | Rust wgpu → staging buffer → byte[] → PBO → OpenGL fullscreen quad | Title screen / 初始化阶段 |

### TDR 问题解决原理

Surface 模式的核心挑战：GL + D3D12 同窗口共存会导致 NVIDIA 驱动 TDR 超时。解决方案通过 5 个 Mixin 协同工作：

1. **GameRendererMixin** — HEAD: Surface+chunks → cancel GL 世界渲染（保留 HUD）/ TAIL: HUD capture via PBO async DMA；Surface+无chunks → FBO-aware GL frame capture；Offscreen → PBO upload
2. **MinecraftMixin** — `runTick` TAIL: `glfwMakeContextCurrent(0)` 分离 GL 上下文，调用 D3D12 Present() 后恢复
3. **GlDeviceMixin** — 取消 `GlDevice.presentFrame()` 的 GL buffer swap
4. **SectionCompilerMixin** — 拦截 MC 区块网格 `compile()` RETURN，上传 chunk mesh 到 D3D12
5. **TextureAtlasMixin** — 拦截 MC terrain atlas 像素，上传到 D3D12 + CPU mip chain

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
│  │  nativeUploadTerrainAtlas(buffer, w, h) → 上传 terrain 纹理图集 + mip chain  │  │
│  │  nativeHasChunkGeometry() → 返回当前是否已上传区块网格 |  │
│  └───────────────────────────────────────────────────────┘  │
│                           ↕                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              wgpu-mc (Rust)                            │  │
│  │  wgpu::Instance(DX12) → Adapter → Device + Queue       │  │
│  │  render_frame() → 3D 场景渲染（地面 + 立方体 + 深度）  │  │
│  │  WGSL shader + camera_pos + 共享 IB + 深度测试        │  │
│  │  CHUNK_SHADER_SRC: chunk 渲染 + texture atlas UV       │  │
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
│   │   ├── Dx12Mod.java            # 模组入口，注册事件回调（tick: 相机提取、lightmap、fog、entities、particles）
│   │   ├── D3D12Bridge.java        # JNI 桥接层（16+ native 方法封装）
│   │   ├── ModMenuIntegration.java # ModMenuApi 实现，配置界面集成
│   │   ├── config/
│   │   │   └── Dx12Config.java     # 配置持久化（aa_mode 等）
│   │   ├── gui/
│   │   │   └── Dx12SettingsScreen.java # AA 模式设置 GUI
│   │   └── mixin/
│   │       ├── MinecraftMixin.java      # runTick TAIL: GL解除→D3D12 Present
│   │       ├── GameRendererMixin.java   # HEAD pass through / TAIL HUD/FBO capture + PBO 异步读回
│   │       ├── GlDeviceMixin.java       # HEAD 抑制 GL swap
│   │       ├── SectionCompilerMixin.java# 拦截区块网格 compile() → 上传 chunk mesh
│   │       └── TextureAtlasMixin.java   # 捕获 terrain atlas sprite 像素
│   ├── src/main/resources/
│   │   ├── fabric.mod.json         # Fabric 模组描述（client + modmenu entrypoints）
│   │   └── gl4dx12.mixins.json     # Mixin 配置（5 个 mixins）
│   ├── libs/
│   │   └── modmenu-18.0.0.jar     # ModMenu 本地依赖（MC 26.1.2）
│   ├── build.gradle                # Gradle 构建配置 (loom 1.10.3)
│   └── gradle.properties           # 版本参数
├── rust/
│   ├── Cargo.toml                  # Workspace 配置
│   ├── wgpu-mc/                    # 核心渲染库 (~3400 lines)
│   │   ├── src/lib.rs              # WmRenderer + Surface/Offscreen 双模 + chunk + HUD + FXAA
│   │   └── Cargo.toml              # wgpu 23, raw-window-handle, bytemuck, image
│   └── wgpu-mc-jni/                # JNI 桥接层 (~657 lines)
│       ├── src/lib.rs              # 16+ 个 native 方法
│       └── Cargo.toml              # jni 0.21, log, env_logger
└── 步骤.md                         # 详细开发步骤文档
```

---

## 项目状态

### 当前阶段：Phase 1-11a 全部完成，Phase 11b-f 进行中

| 阶段 | 状态 | 说明 |
|------|------|------|
| **Phase 1: JNI 通信链路** | ✅ 已完成 (2026-07-09) | 16 个 native 方法，Java ↔ Rust 双向通信 |
| **Phase 2: wgpu 渲染引擎骨架** | ✅ 已完成 (2026-07-09) | DX12 adapter + 3D 几何管线 + Surface/Offscreen 双模 |
| **Phase 3: Fabric 事件系统 + Mixin 集成** | ✅ 已完成 (2026-07-10) | 3 个 Mixin + ClientTickEvents |
| **Phase 4: 3D 场景渲染基础** | ✅ 已完成 (2026-07-10) | WGSL shader + 深度测试 + 背面剔除 |
| **Phase 5: 性能优化** | ✅ 已完成 (2026-07-10) | 三缓冲、懒 resize、lock_or_poisoned、相机 LERP |
| **Phase 6: D3D12 Surface 模式** | ✅ 已完成 (2026-07-11) | 原生 swapchain 直出 MC 窗口 HWND |
| **Phase 7: Chunk 几何渲染** | ✅ 已完成 (2026-07-11~16) | MC 区块网格上传 + Terrain atlas + 植被双通道渲染 |
| **Phase 8: HUD/UI 叠加层** | ✅ 已完成 (2026-07-22) | renderLevel HEAD 取消 + GL readback → D3D12 alpha composite |
| **Phase 9: 光照系统** | ✅ 已完成 (2026-07-22) | 动态光照贴图（lightmap）+ 昼夜循环 |
| **Phase 10: 后处理特效 + 模组设置** | ✅ 已完成 (2026-07-22) | FXAA + Reinhard 色调映射 + Gamma + ModMenu 集成 |
| **Phase 11a: PBO 异步 HUD 读回** | ✅ 已完成 (2026-07-22) | 消除 glFinish 阻塞，~2-5ms 节省 |
| **Phase 11b-f: 更多性能优化** | 🔜 进行中 | GL detach 优化、反射缓存、按需 lightmap 上传、chunk 批处理、视锥体裁剪 |

### 已完成功能

| 模块 | 说明 |
|------|------|
| **Rust Workspace** | `wgpu-mc` (渲染引擎) + `wgpu-mc-jni` (JNI 桥接) 双 crate 结构 |
| **Mixin 框架** | 5 个 Mixin 精确控制 GL/D3D12 共存：GameRendererMixin, MinecraftMixin, GlDeviceMixin, SectionCompilerMixin, TextureAtlasMixin |
| **JNI 桥接层** | 16 个 native 方法完整实现（`nativeInit`, `nativeHello`, `nativeTestDeviceInfo`, `nativeSetWindow`, `nativeRenderFrame`, `nativeResize`, `nativeUpdateCamera`, `nativeUpdateCameraPos`, `nativeSetFramePixels`, `nativeUploadChunkMesh`, `nativeClearChunkSection`, `nativeUploadTerrainAtlas`, `nativeHasSurface`, `nativeHasChunkGeometry`, `nativeIsReady`, `nativeGetStatus`, `nativeSetHudPixels`, `nativeUploadLightmap`, `nativeSetAaMode`） |
| **Java Fabric 模组** | 基于 Fabric Loom 1.10.3，MC 26.1.2，Fabric API 0.154.2 |
| **DLL 自动加载** | 从 JAR 提取到 `{user.dir}/dx12mod/wgpu_mc_jni.dll`，支持版本隔离 |
| **GPU 适配器检测** | 通过 wgpu 创建 DX12 后端实例并检测适配器可用性 |
| **日志系统** | Rust `env_logger` + Java SLF4J 双端日志 |
| **Surface 模式** | 智能渲染：chunk 就绪 → D3D12 直接渲染 MC 场景 / chunk 未就绪 → GL 帧捕获 → D3D12 纹理 present |
| **Offscreen 模式** | Triple-buffer 异步读回 + PBO 纹理上传（title screen / 初始化阶段） |
| **Chunk 几何上传** | SectionCompilerMixin 拦截 MC 区块网格 → JNI 传输 → D3D12 TriangleList 渲染 |
| **Terrain 纹理图集上传** | TextureAtlasMixin 捕获 sprite 像素 → CPU mip chain → GPU 采样 |
| **Chunk 双通道渲染** | Opaque Pass (depth_write=true) + Transparent Pass (alpha blending) 处理 CUTOUT/TRANSLUCENT 植被 |
| **光照系统** | 动态 lightmap 纹理上传 + WGSL fragment shader 采样 + 昼夜循环 |
| **指数雾效** | WGSL fog uniform + `abs(clip_w)` 距离计算 + 天气自适应密度 |
| **实体渲染** | 反射提取 Entity Bounding Box + 哈希颜色 + 36 顶点/实体 Box Mesh |
| **粒子系统** | PointList 点精灵 + WGSL 软圆 discard + MC ParticleEngine 反射提取 |
| **HUD/UI 叠加** | renderLevel HEAD 取消 + PBO 异步读回 + D3D12 alpha blending 合成 |
| **PBO 异步 HUD 读回** | 3×PBO 三缓冲 + glFlush 替代 glFinish，消除 CPU-GPU 同步阻塞 |
| **相机 MVP 传递** | Java 提取 MC 相机视角 → `nativeUpdateCamera(float[])` → Rust LERP 平滑 |
| **相机位置传递** | Java 提取 MC 玩家世界坐标 → `nativeUpdateCameraPos()` → Rust 偏移几何体 |
| **3D 几何管线** | WGSL 着色器 + camera_pos + 深度测试 + 背面剔除 |
| **地面平面网格** | 200x200 绿色平面（y=0） |
| **彩色立方体网格** | 5 个立方体，每个独立 VB（预烘焙偏移），共享 IB |
| **深度缓冲区** | `Depth32Float` 格式，支持正确遮挡关系 |
| **FXAA 抗锯齿** | 4 采样点边缘检测 + 方向子像素混合 + Reinhard 色调映射 + Gamma 校正 |
| **三通道渲染架构** | Pass 1: 世界渲染 → Pass 2: 后处理 (FXAA/tonemapping/gamma) → Pass 3: HUD 叠加 |
| **模组设置界面** | Dx12Config (Properties 持久化) + Dx12SettingsScreen (MC GUI) + ModMenu 集成 |
| **AaMode 枚举** | None=0 / FXAA=1 / SMAA=2 (placeholder) / TAA=3 (placeholder) |
| **ChunkVertex 格式** | 40 字节（位置 xyz + 颜色 rgba + UV xy + light_uv xy） |
| **AA 模式切换** | Java → JNI → Rust 标记 pipeline 失效 → 下次渲染重建 |
| **资源重载检测** | 自动检测 MC 资源重载并延迟渲染 |
| **独立测试程序** | `examples/simple.rs` — winit + wgpu 弹出窗口渲染彩色三角形 |

### 验收结果

- 模组加载成功，无 crash
- **Surface 模式（智能渲染）**：chunk 就绪时 D3D12 直接渲染完整 MC 场景（含 terrain atlas、实体、粒子、雾效、lightmap）；chunk 未就绪 → GL 帧捕获 → D3D12 textured quad
- **Offscreen 模式**：PBO 纹理上传 + OpenGL 全屏 quad 覆盖（title screen 阶段）
- **GL 帧捕获增强**：FBO-aware，从 MC 实际 draw FBO 读取而非 GL_BACK
- **HUD/UI 叠加**：renderLevel HEAD 取消 GL 世界渲染 → HUD/GUI 保留 → PBO async DMA → D3D12 alpha blending 合成
- **Chunk 几何渲染**：双通道（Opaque Pass depth_write + Transparent Pass alpha blending），支持 CUTOUT/TRANSLUCENT 植被
- **动态光照**：lightmap 纹理每帧更新 → WGSL fragment shader 采样 → 昼夜循环/火把光照
- **指数雾效**：天气自适应密度，`abs(clip_w)` 正确距离计算
- **实体渲染**：反射提取 Entity Bounding Box → 哈希颜色 → 36 顶点/实体 box mesh
- **粒子系统**：MC ParticleEngine 反射提取 → PointList 点精灵 + WGSL 软圆 discard
- **FXAA 抗锯齿**：4 采样点边缘检测 + Reinhard 色调映射 + Gamma 校正
- **三通道渲染**：World → Post-process → HUD overlay
- **相机视角** 随 MC 移动实时更新（MVP 矩阵 + LERP 平滑）
- **相机世界坐标** 传递，几何体跟随玩家位置
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
- GL 帧捕获从 MC 自定义 FBO 读取（而非 GL_BACK）导致的黑屏

---

### [2.0.0] - 2026-07-22 (Phases 6-10)

> **重大更新：Surface 模式全功能完成** — Chunk 几何渲染、HUD/UI 叠加、光照系统、后处理特效 + FXAA、模组设置界面、PBO 异步 HUD 读回

#### Added
- **Phase 6: D3D12 Surface 模式完整版**
  - `init_surface(hwnd)` 创建 DXGI swapchain on MC window
  - 三种渲染路径自动切换：chunk 几何 / GL 帧捕获 / 测试场景
  - Surface resize 保护：Lost/Outdated 时自动 reconfigure（不在 resize() 中调用 configure）
  - 深度缓冲区：每帧复用 surface_depth texture
  - **植被双通道渲染修复**：Opaque Pass (depth_write=true, no blending) + Transparent Pass (depth_write=false, ALPHA_BLENDING)，解决树叶俯视穿透底部 bug
  - **实体 Box 渲染**：反射提取 Entity Bounding Box + 哈希颜色 + 36 顶点/实体独立 VB
  - **粒子点精灵系统**：PointList 拓扑 + WGSL 软圆 discard + MC ParticleEngine 反射提取
  - **指数雾效**：WGSL fog uniform + `abs(clip_w)` 距离计算 + 天气自适应密度 (normal/raining/thundering)
  - **`lock_or_poisoned()`**：Mutex poison 安全恢复，防止 panic 级联 JVM 崩溃
  - **`catch_unwind`**：所有 native 调用 panic 保护
  - 诊断日志：chunk 顶点 dump、atlas pixel保存为 PNG、surface format 匹配

- **Phase 7: 实际 Minecraft 场景渲染**
  - **SectionCompilerMixin**：拦截 `compile()` RETURN，迭代 `results.renderedLayers`，上传 chunk mesh
  - **TextureAtlasMixin**：拦截 sprite 像素 → 合成完整 atlas → CPU mip chain → GPU 采样
  - **ChunkVertex 40 字节**：新增 `light_uv: [f32; 2]` 字段，支持光照贴图 UV
  - 支持 u16/u32 索引（`index_is_u32` 字段）
  - Chunk 渲染 panic 保护：`catch_unwind` 确保 frame 始终 present

- **Phase 8: HUD/UI 叠加层（方案 C）**
  - **renderLevel HEAD 取消**：Surface+chunks → cancel GL 世界渲染，保留 framebuffer 内容 → HUD/GUI 叠加在上面
  - **`setHudPixels()` + HUD pipeline**：alpha blending 全屏 quad 合成 HUD 到世界画面上方
  - **glFinish 确保 flush**：MC 26.1.2 渲染图系统 (SubmitNodes) 批量命令，glReadPixels 前 flush
  - HUD 纹理与 frame_texture 共享 bind group layout（texture2D + sampler）

- **Phase 9: 光照系统（动态光照贴图）**
  - **Java 端反射提取**：`LightmapTextureManager.textureId` → `glGetTexImage()` → RGBA8 lightmap
  - **`nativeUploadLightmap()`**：每 10 tick 更新（昼夜循环需要）
  - **WGSL lightmap 采样**：`@group(0) @binding(3) var lightmap: texture_2d<f32>`
  - Fragment shader: `light_color = textureSample(lightmap, ..., in.light_uv)` → `tex_color * tint * light_color`
  - **Chunk 顶点 UV2 解析**：offset 24 → 归一化到 [0, 1]

- **Phase 10: 后处理特效 + 模组设置界面**
  - **三通道渲染架构**：Pass 1 World (chunks → post_texture) → Pass 2 Post-process (FXAA → swapchain) → Pass 3 HUD overlay
  - **FXAA 抗锯齿**：4 采样点边缘检测 + Reinhard tone mapping + gamma 校正 (1/2.2)
  - **AaMode 枚举**：None=0 / FXAA=1 / SMAA=2 / TAA=3（SMAA/TAA placeholder）
  - **`set_aa_mode(int)` JNI**：标记 pipeline 失效 → 下次渲染重建
  - **Dx12Config**：Java Properties 持久化到 `config/gl4dx12.properties`
  - **Dx12SettingsScreen**：MC 原生 GUI Screen，Button 循环切换 AA 模式
  - **ModMenu 集成**：`ModMenuApi.getModConfigScreenFactory()` → Dx12SettingsScreen
  - fabric.mod.json 添加 `"modmenu"` entrypoint

- **Phase 11a: PBO 异步 HUD 读回**
  - **3×PBO 环形缓冲**：writeIdx 写入 → readIdx 映射（隔 2 帧），防止 DMA 冲突
  - **`glFlush()` 替代 `glFinish()`**：不等待 GPU 完成，消除 ~2-5ms CPU 阻塞
  - **`glMapBuffer(GL_READ_ONLY)`**：阻塞直到 DMA 完成（~0ms），copy → unmap
  - **同步回退路径**：PBO 初始化失败时降级到原 `glFinish()` + `glReadPixels()` 路径

- **ModMenu 本地依赖**
  - `modImplementation` → `implementation` (Loom 1.15.5 + Gradle 9.x 不兼容)
  - ModMenu 14.0.2 → 18.0.0 (MC 26.1.2 对应版本)
  - 添加 Modrinth Maven + Terraformers Maven + libs/modmenu-18.0.0.jar

#### Changed
- **Surface 模式完整实现**：经过 3 轮迭代方案（抑制 GL swap → 子窗口 → GL 上下文解除）最终方案
- **Chunk mesh 解析格式更新**：MC BLOCK 格式 36 字节 → ChunkVertex 40 字节（新增 light_uv）
- **Two-pass chunk rendering**：opaque (36 verts) + transparent (36 verts) = 72 vertices per chunk layer
- **Three-pass surface rendering**：world (post_texture) → post-process (swapchain) → HUD overlay
- **Lightmap 上传节流**：每 10 frames 上传一次（昼夜循环缓慢变化）
- **`getSkyColor` 反射缓存**：首次尝试失败后缓存标志，避免刷屏

#### Fixed
- **标题界面卡死（9s+）**：`inWorld` 守卫，只在地图中创建 surface
- **C++ EXCEPTION_UNCAUGHT**：懒 resize，不在 resize() 中调用 surface.configure()
- **白屏（子窗口方案）**：回退 GL 上下文解除方案
- **卡加载世界（15s）**：setWindow 移至 MinecraftMixin GL 解除区域内
- **glfwGetCurrentContext()=0**：在解除 GL 之前捕获 HWND
- **Bgra8 != Rgba8 格式 panic**：优先匹配 Rgba8UnormSrgb
- **Surface image already acquired 链式 panic**：格式修复后自动解决
- **测试几何体不可见**：改用全屏三角形 / chunks 渲染后始终可见
- **树叶俯视穿透底部**：双通道渲染修复（Opaque Pass depth_write + Transparent Pass alpha blending）
- **雾效远处蒙黑**：`-clip_w` → `abs(clip_w)` 确保距离为正
- **getSkyColor 反射失败**：缓存 fogReflectionWorks 标志，首次失败后用硬编码回退值
- **纹理闪烁**：窗口 resize 后 texWidth/texHeight 追踪 + 自动重建
- **NVIDIA DMA 页边界 crash**：PBO + 4KB padding

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
| Mixin | SpongePowered Mixin | 0.8.7 (via Fabric 0.17.3) |
| 模组浏览器 | ModMenu | 18.0.0 |
| 语言 | Java | 25 |
| 图形 | wgpu (WebGPU) → DX12 | 23 |
| 语言 | Rust | 2021 edition |
| JNI | jni crate | 0.21 |
| 窗口句柄 | raw-window-handle | 0.6 |
| 图像处理 | image crate | 0.25 (PNG 诊断) |
| 测试 GPU | NVIDIA GeForce RTX 4070 / 3080 | Driver 576.x |

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
[dx12-wm] Surface OK! HWND=0x123456 fmt=Rgba8UnormSrgb 1920x1080
```

此时渲染模式自动切换到 **Surface 模式**（world 中）。Chunk geometry + Terrain atlas + Lightmap 上传后，D3D12 直接渲染完整 MC 场景（含植被/实体/粒子/雾效/HUD），FXAA 抗锯齿开启。

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
- `nativeSetFramePixels(byte[], w, h)` — Surface 模式接收 GL 捕获的像素
- `nativeUploadChunkMesh(sectionXYZ, buffer, verts, stride)` — 上传 MC 区块网格
- `nativeClearChunkSection(sectionXYZ)` — 清除旧区块网格
- `nativeUploadTerrainAtlas(buffer, w, h)` — 上传地形纹理图集 + CPU mip chain
- `nativeHasSurface()` — 检测当前是否为 Surface 模式
- `nativeHasChunkGeometry()` — 检测是否已上传 chunk 几何
- `nativeIsReady()` — 返回 1/0/-1 状态码
- `nativeGetStatus()` — 返回人类可读状态字符串
- `nativeSetHudPixels(byte[], w, h)` — HUD 叠加层像素上传
- `nativeUploadLightmap(buffer, w, h)` — 动态光照贴图上传
- `nativeSetAaMode(int)` — AA 模式切换 (None/FXAA/SMAA/TAA)

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
| **GPU TDR 超时（~2s 后崩溃）** | GL + D3D12 同窗口共存导致 WDDM 驱动级冲突 | **5 个 Mixin**：GL 上下文分离 + Mixin 取消 GL swap | ✅ 已修复 |
| 标题界面卡死（9s+） | 创建 surface 驱动争用 | `inWorld` 守卫：只在地图中创建 surface | ✅ 已修复 |
| C++ EXCEPTION_UNCAUGHT | `resize()` 中 `surface.configure()` 触发 DXGI ResizeBuffers 异常 | 懒 resize：不在 resize() 中配置 surface，等 Lost/Outdated 时处理 | ✅ 已修复 |
| 白屏（子窗口方案） | STATIC 类窗口覆盖 MC 窗口 | 回退 GL 上下文解除方案 | ✅ 已修复 |
| 卡加载世界（15s 无响应） | setWindow 在 GL 绑定状态调用 surface.configure() | 移至 MinecraftMixin 的 GL 解除区域内 | ✅ 已修复 |
| `glfwGetCurrentContext()=0` 刷屏 | 先解除 GL 再获取 HWND，依赖 glfwGetCurrentContext() | 在解除 GL 之前捕获 HWND：`GLFWNativeWin32.glfwGetWin32Window(glfwWindow)` | ✅ 已修复 |
| Bgra8 != Rgba8 格式 panic | Pipeline 格式 Rgba8 不匹配 Surface 格式 Bgra8 | 优先匹配 `Rgba8UnormSrgb` surface 格式 | ✅ 已修复 |
| Surface image already acquired 链式 panic | 格式 panic → present() 未执行 → 纹理永久锁定 | 格式修复后自动解决 | ✅ 已修复 |
| 测试几何体不可见（浅蓝色全屏） | 几何体在世界原点，玩家生成在 ~100 单位外 | 改用全屏三角形（NDC 空间），始终可见 | ✅ 已修复 |
| **树叶俯视穿透底部** | Alpha Blending + depth_write 冲突，透明像素错误遮挡后方 | 双通道渲染（Opaque Pass depth_write=true + Transparent Pass alpha blending） | ✅ 已修复 |
| **雾效远处蒙黑** | `-clip_w` 导致负数距离 → fog_factor > 1 | 改用 `abs(clip_w)` 确保距离为正 | ✅ 已修复 |
| **getSkyColor 反射失败** | `Level.getSkyColor(Vec3, float)` 在 MC 26.1.2 不存在 | 缓存 `fogReflectionWorks` 标志，首次失败后用回退值 | ✅ 已修复 |
| **renderLevel 内部 HUD 被清除** | TAIL glClearColor(0,0,0,0) 清除已渲染的 HUD | 改为 HEAD cancel 阻止 GL 世界渲染 | ✅ 已修复 |

---

## 路线图

### Phase 1：JNI 通信链路 ✅ 已完成 (2026-07-09)

| 任务 | 状态 | 说明 |
|------|------|------|
| Rust Workspace 搭建 | ✅ | `wgpu-mc` + `wgpu-mc-jni` 双 crate |
| JNI 桥接层实现 | ✅ | 16+ native 方法，`lock_or_poisoned()` 安全恢复 |
| Java Fabric 模组入口 | ✅ | `ClientModInitializer` + tick callback |
| DLL 自动加载 | ✅ | JAR 提取到 `{user.dir}/dx12mod/` |
| GPU 适配器检测 | ✅ | wgpu DX12 adapter 可用性检查 |
| catch_unwind panic 保护 | ✅ | `nativeInit` + `render_frame` 均捕获 panic |

### Phase 2：wgpu 渲染引擎骨架 ✅ 已完成 (2026-07-09)

| 任务 | 状态 | 说明 |
|------|------|------|
| WmRenderer 创建 | ✅ | wgpu DX12 Instance → Adapter → Device + Queue |
| 3D 几何管线 | ✅ | WGSL 着色器 + camera_pos + 深度测试 + 背面剔除 |
| 地面网格 | ✅ | 200x200 绿色平面 |
| 立方体网格 | ✅ | 5 个独立 VB + 共享 IB |
| 深度缓冲 | ✅ | `Depth32Float` 格式 |
| Pixel Buffer Object (PBO) | ✅ | 绕过 NVIDIA DMA 页边界 crash |
| Triple-buffer 读回 | ✅ | Offscreen 模式三槽环形缓冲 + async map_async |
| 窗口尺寸同步 | ✅ | `nativeResize()` lazy resize |
| 相机 MVP 同步 | ✅ | Java → Rust LERP 平滑 |
| 相机位置同步 | ✅ | 世界坐标偏移几何体 |
| 独立测试程序 | ✅ | `examples/simple.rs` 可独立运行渲染三角形 |
| Push constants baked | ✅ | 模型变换预烘焙到顶点缓冲，兼容所有 GPU |

### Phase 3：Fabric 事件系统 + Mixin 集成 ✅ 已完成 (2026-07-10)

| 任务 | 状态 | 说明 |
|------|------|------|
| GameRendererMixin HEAD/TAIL | ✅ | Pass through GL / FBO-aware frame capture |
| MinecraftMixin runTick TAIL | ✅ | GLFW 上下文分离 → D3D12 Present |
| GlDeviceMixin HEAD | ✅ | 抑制 GL swap（避免 DXGI ↔ WGL 争用） |
| GL 状态管理 | ✅ | 完整的保存/恢复机制 |
| VAO/Shader 持久化 | ✅ | 首次创建，丢失后自动重建 |

### Phase 4：Surface 模式（原生 Swapchain）✅ 已完成 (2026-07-11)

| 任务 | 状态 | 说明 |
|------|------|------|
| Surface 创建 | ✅ | `create_surface_from_hwnd()` 基于 raw-window-handle |
| HWND 共享冲突解决 | ✅ | GL 上下文临时解除 + 重绑方案（第 3 轮迭代成功） |
| Swapchain 配置 | ✅ | 匹配 Rgba8UnormSrgb 格式 + depth texture |
| Surface 渲染路径 | ✅ | chunk 几何 / GL 帧捕获 / 测试场景三种智能切换 |
| Surface 自适应 resize | ✅ | Lost/Outdated 时自动 reconfigure |
| Surface 错误恢复 | ✅ | `catch_unwind` 确保 frame 始终 present |
| 双模切换 | ✅ | `nativeSetWindow` 后自动切换到 Surface 模式 |
| inWorld 守卫 | ✅ | 标题界面不创建 surface |

### Phase 5：性能优化 ✅ 已完成 (2026-07-10)

| 任务 | 状态 | 说明 |
|------|------|------|
| 三缓冲异步渲染 | ✅ | Ring(3) 轮转 + map_async + mpsc |
| 相机平滑插值 | ✅ | `mat4_lerp(prev, target, 0.3)` |
| Resize 平滑过渡 | ✅ | 懒 resize + 尺寸不匹配跳过 |
| Poison mutex 恢复 | ✅ | `lock_or_poisoned()` |
| Surface resize 保护 | ✅ | 不在 resize() 中 reconfigure |
| Chunk pipeline 动态格式 | ✅ | `ensure_chunk_pipeline()` 使用 surface_format |
| Chunk 渲染 panic 保护 | ✅ | catch_unwind 确保 frame always present |
| Chunk mesh u32 索引 | ✅ | 大数据区块支持 u32 |

### Phase 6：实际 Minecraft 场景渲染（VulkanMod 级全接管）✅ 已完成 (2026-07-11~16)

| 任务 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| 区块渲染 | 🔴 P0 | ✅ | SectionCompilerMixin 拦截 compile() RETURN，上传 chunk mesh，双通道渲染 |
| Terrain atlas | 🔴 P0 | ✅ | TextureAtlasMixin 捕获 sprite 像素，CPU mip chain，GPU 采样 |
| 植被渲染修复 | 🟠 P1 | ✅ | 双 Pipeline (Opaque + Transparent) 解决 CUTOUT + TRANSLUCENT 同 mesh 渲染冲突 |
| 雾效 | 🟠 P1 | ✅ | 指数雾 WGSL shader + MC sky color 提取 + 天气自适应密度 |
| 实体渲染 | 🟡 P2 | ✅ | 反射 Entity Getter → Bounding Box + 哈希颜色 + 36 顶点/实体 Box |
| 粒子系统 | 🟡 P2 | ✅ | PointList 点精灵 + WGSL 软圆 discard + MC ParticleEngine 反射提取 |

### Phase 7：HUD/UI 叠加层（方案 C）✅ 已完成 (2026-07-22)

| 任务 | 状态 | 说明 |
|------|------|------|
| renderLevel HEAD 取消 | ✅ | 阻止 GL 世界渲染，保留 framebuffer，HUD/GUI 叠加在上面 |
| HUD pipeline | ✅ | Alpha blending 全屏 quad 合成，无 depth write |
| glFinish 确保 flush | ✅ | MC 26.1.2 SubmitNodes 批处理，glReadPixels 前全量提交 |
| HUD 纹理共享 layout | ✅ | 与 frame_texture 共用 texture2D + sampler bind group |

### Phase 8：光照系统（动态光照贴图）✅ 已完成 (2026-07-22)

| 任务 | 状态 | 说明 |
|------|------|------|
| Lightmap 纹理提取 | ✅ | Java 反射 `GameRenderer.lightmapTextureManager.textureId` → glGetTexImage |
| lightmap 上传节流 | ✅ | 每 10 frames 更新一次（昼夜循环缓慢变化） |
| ChunkVertex 光照明 UV | ✅ | 新增 `light_uv: [f32; 2]`，offset 24 归一化到 [0,1] |
| WGSL lightmap 采样 | ✅ | `@group(0) @binding(3) var lightmap: texture_2d<f32>` |
| Fragment lighting | ✅ | `tex_color * tint * light_color` 三色乘法 |
| Two-pass pipeline 更新 | ✅ | Opaque + Transparent pipeline 均包含 lightmap binding |

### Phase 9：后处理特效 + 模组设置界面 ✅ 已完成 (2026-07-22)

| 任务 | 状态 | 说明 |
|------|------|------|
| 三通道渲染架构 | ✅ | World → Post-process → HUD overlay |
| FXAA 抗锯齿 | ✅ | 4 采样点边缘检测 + Reinhard tonemapping + gamma (1/2.2) |
| AaMode 枚举切换 | ✅ | None/FXAA/SMAA placeholder/TAA placeholder，pipeline 失效重建 |
| Dx12Config 持久化 | ✅ | Java Properties → `config/gl4dx12.properties` |
| Dx12SettingsScreen GUI | ✅ | MC Screen 子类 + Button 循环切换 |
| ModMenu 集成 | ✅ | `ModMenuApi.getModConfigScreenFactory()` → Dx12SettingsScreen |
| 构建修复 | ✅ | modImplementation→implementation, ModMenu 14→18, Modrinth Maven |

### Phase 10：PBO 异步 HUD 读回 ✅ 已完成 (2026-07-22)

| 任务 | 状态 | 说明 |
|------|------|------|
| 3×PBO 环形缓冲 | ✅ | 每帧轮换写入/读取索引，防止 DMA 冲突 |
| glFlush 替代 glFinish | ✅ | 刷新命令缓冲不等待 (~0ms vs 数 ms) |
| glMapBuffer 异步读回 | ✅ | 读 2 帧前的 DMA 结果 (~0ms) |
| 同步回退路径 | ✅ | PBO 初始化失败时降级到原 glFinish + glReadPixels |
| **预期收益** | — | 消除 ~2-5ms CPU-GPU 同步阻塞（约 10-25% HUD 读回性能提升） |

### 🔜 后续优化（Phase 11b-f 进行中）

| 任务 | 优先级 | 说明 | 状态 |
|------|--------|------|------|
| GL detach/reattach 优化 | P0 | 减少 WDDM 驱动开销 | 🔜 |
| 反射数据提取缓存 | P0 | MethodHandle 查找缓存（entities/particles/lightmap/fog） | 🔜 |
| 光照贴图增量更新 | P1 | 无变化时不上传 GPU write_texture | 🔜 |
| Chunk 渲染批处理 | P1 | 合并同屏 chunks draw call | 🔜 |
| 视锥体裁剪 | P1 | 远离相机的 chunk 不渲染 | 🔜 |

### 🎯 最终目标

| 阶段 | 目标 | 状态 |
|------|------|------|
| 过渡方案 | Surface 模式：MC 正常 GL 渲染 → GameRendererMixin TAIL 捕获 framebuffer → D3D12 纹理 → swapchain present | ✅ 已完成 |
| VulkanMod 级 | Rust/DX12 直接渲染 MC 区块几何，消除 GL 帧捕获中间层，零读回、零纹理上传 | ❌ 待实现 |

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
