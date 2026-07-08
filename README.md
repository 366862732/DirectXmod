# GL4DX12 — Minecraft wgpu/DX12 渲染模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blueviolet)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![Rust](https://img.shields.io/badge/Rust-2021-orange)](https://www.rust-lang.org/)
[![wgpu](https://img.shields.io/badge/wgpu-23-blue)](https://wgpu.rs/)

> 为 Minecraft Java Edition 1.21.1 实现的 DirectX 12 渲染后端，通过 Rust + wgpu + JNI 桥接，将 OpenGL 渲染替换为 D3D12/WebGPU，以解决 TDR 崩溃问题并提升图形性能。

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

---

## 项目概述

**GL4DX12** 是一个 Fabric 模组，通过 Rust + wgpu 实现 DirectX 12 渲染后端，利用 JNI（Java Native Interface）桥接 Minecraft 的 Java 层与本地渲染引擎。

### 核心设计原则

- **不使用 Mixin**（避免与 Fabric 渲染器冲突）
- **不创建额外窗口**（直接使用 Minecraft 窗口 HWND）
- **版本通用**（1.21.1 ~ 1.21.11 + 26.x，不依赖 Yarn 映射）
- **后台渲染**：Rust wgpu 在后台线程渲染 → 读回像素 → Java 通过 OpenGL 纹理上传 → 绘制全屏 quad

### 为什么重构为 Rust + wgpu？

| 旧方案 (C++/D3D12) | 新方案 (Rust/wgpu) |
|---------------------|---------------------|
| 手动管理 D3D12 资源 | wgpu 自动资源管理 |
| OpenGL + D3D12 共享 HWND 导致 GPU 设备移除 | 独立表面 (independent surface) 架构 |
| 内存安全依赖开发者 | Rust 编译器保证内存安全 |
| 复杂的 C++ 构建配置 | Cargo 依赖管理 |
| TDR 崩溃频发 | 架构层面规避 TDR |

### 核心优势

- **内存安全**：Rust 编译器在编译期消除 use-after-free、数据竞争等常见 bug
- **跨平台**：wgpu 抽象层支持 DX12/Vulkan/Metal，一次编写多平台运行
- **高性能**：WebGPU 标准驱动的现代 GPU API，接近原生 C++ 性能
- **易维护**：Cargo 生态系统 + 类型系统降低长期维护成本

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Minecraft 1.21.1                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Fabric Loader 0.19.3                      │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │           Fabric API (ClientTickEvents)          │  │  │
│  │  │  Tick Callback → 节流 100ms → Rust renderFrame() │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │         Fabric API (HudRenderCallback)           │  │  │
│  │  │  GL 绘制 → glTexSubImage2D + VAO + Shader → 全屏quad │ │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│                           ↕ JNI                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              wgpu_mc_jni.dll (Rust)                    │  │
│  │  nativeSetWindow(HWND) → 初始化 DX12 Adapter           │  │
│  │  nativeRenderFrame() → 返回 byte[] (RGBA 像素数据)      │  │
│  │  nativeResize(width, height) → 更新窗口尺寸            │  │
│  └───────────────────────────────────────────────────────┘  │
│                           ↕                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              wgpu-mc (Rust)                            │  │
│  │  wgpu::Instance(DX12) → Adapter → Device + Queue       │  │
│  │  render_frame() → 生成纯色/三角形像素数据               │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 项目结构

```
dx12-lib-template-26.1.2/
├── fabric/                          # Fabric 模组（Java）
│   ├── src/main/java/com/dx12/
│   │   ├── Dx12Mod.java            # 模组入口，注册事件回调
│   │   └── D3D12Bridge.java        # JNI 桥接层
│   ├── src/main/resources/
│   │   └── fabric.mod.json         # Fabric 模组描述
│   ├── build.gradle                # Gradle 构建配置
│   └── gradle.properties           # 版本参数
├── rust/
│   ├── Cargo.toml                  # Workspace 配置
│   ├── wgpu-mc/                    # 核心渲染库
│   │   ├── src/lib.rs              # WmRenderer 结构
│   │   └── Cargo.toml              # wgpu 23, futures, raw-window-handle
│   └── wgpu-mc-jni/                # JNI 桥接层
│       ├── src/lib.rs              # nativeSetWindow/renderFrame/resize
│       └── Cargo.toml              # jni 0.21, log, env_logger
```

---

## 项目状态

### 当前阶段：阶段 1-3 已完成，阶段 4 未开始

| 阶段 | 状态 | 说明 |
|------|------|------|
| **第一阶段：JNI 通信链路** | ✅ 已完成 | Java ↔ Rust 双向通信正常 |
| **第二阶段：wgpu 渲染引擎骨架** | ✅ 已完成 | DX12 adapter 初始化 + 离屏渲染 + 像素回传 |
| **第三阶段：Fabric 事件系统集成** | ✅ 已完成 | ClientTickEvents + HudRenderCallback，OpenGL 全屏 quad 绘制正常 |
| **第四阶段：实际 Minecraft 场景渲染** | ❌ 未开始 | 当前为纯色覆盖层（蓝色），待接入真实游戏场景 |

### 已完成功能

| 模块 | 说明 |
|------|------|
| **Rust Workspace** | `wgpu-mc` (渲染引擎) + `wgpu-mc-jni` (JNI 桥接) 双 crate 结构 |
| **JNI 桥接层** | 6 个 native 方法：`nativeInit`, `nativeHello`, `nativeTestDeviceInfo`, `nativeRenderFrame`, `nativeSetWindow`, `nativeResize` |
| **Java Fabric 模组** | 基于 Fabric Loom 1.10.3，MC 1.21.1，Fabric API 0.116.13 |
| **DLL 自动加载** | 从 JAR 提取到 `{user.dir}/dx12mod/wgpu_mc_jni.dll`，支持版本隔离 |
| **GPU 适配器检测** | 通过 wgpu 创建 DX12 后端实例并检测适配器可用性 |
| **日志系统** | Rust `env_logger` + Java SLF4J 双端日志 |
| **离屏渲染** | `WmRenderer::render_frame()` 输出 RGBA 像素缓冲区 |
| **HWND 传递** | Java → Rust 窗口句柄传递，支持 `nativeSetWindow` / `nativeResize` |
| **像素回传** | Rust → Java `byte[]` 像素数据传输 + OpenGL 纹理上传 + 全屏 Quad 绘制 |
| **独立测试程序** | `examples/simple.rs` — winit + wgpu 弹出窗口渲染彩色三角形 |
| **GL 状态管理** | 完整的 Minecraft GL 状态保存/恢复机制，避免与 MC 渲染冲突 |
| **资源重载检测** | 自动检测 MC 资源重载并延迟渲染，避免 GL 资源失效 |
| **VAO 重建机制** | 检测到 GL 资源丢失时自动重建 VAO/Shader |

### 验收结果

- 模组加载成功，无 crash
- 蓝色覆盖层正常显示（1920x1080 @ RGBA）
- 日志输出：`Rendering frame: 1639680 bytes (frame=1/61/121...)`
- 按 Esc 不会 crash
- 进游戏、按 Esc、调设置均无 JVM 崩溃

---

## 变更日志

### [1.0.0] - 2026-07-08

> **注意：此版本为开发预览版，尚未生成 `.jar` 发布文件。** 需手动构建 Fabric 模组（`gradlew build`）方可运行。

#### Added
- 完整的 GL 状态管理机制：保存/恢复 Minecraft VAO、Texture、Program、Blend、Depth 状态
- 资源重载检测：通过 tick 时间间隔判断 MC 资源重新加载，自动重置渲染状态
- VAO/Shader 自动重建：检测到 GL 资源失效时自动重建，无需重启游戏
- 每帧创建新 Texture：避免与 MC 的 shader 加载产生纹理名称冲突
- 启动延迟渲染：10 秒延迟确保 MC 资源加载完成后才启用渲染

#### Changed
- 渲染流程从简单贴图升级为完整的 GL 状态隔离方案
- `Dx12Mod.java` 采用 try-finally 结构确保 GL 状态始终恢复

#### Fixed
- Minecraft 菜单打开时 GL 资源被销毁导致崩溃的问题
- 资源重载期间渲染冲突问题
- 纹理名称重复使用导致的渲染异常

---

### [0.2.0] - 2026-07-07

#### Added
- 6 个 JNI native 方法完整实现 (`nativeRenderFrame`, `nativeSetWindow`, `nativeResize`)
- 每帧渲染循环：Rust 离屏渲染 → byte[] 像素回传 → OpenGL 纹理上传 → 全屏 Quad 绘制
- 窗口句柄 (HWND) 传递机制：Java 反射获取 GLFW 窗口 → `nativeSetWindow`
- 窗口尺寸同步：`syncWindowSize()` 去重 + `nativeResize()` 更新
- 独立测试程序 `examples/simple.rs`：winit + wgpu 弹出窗口渲染彩色三角形
- WGSL 着色器：`triangle.wgsl` (2D) + `simple.wgsl` (3D)
- `winit = "0.30"` + `raw-window-handle = "0.6"` + `windows-sys = "0.59"` 依赖
- 预编译 DLL 打包至 `fabric/src/main/resources/`
- GitHub Actions CI 工作流 (`.github/workflows/build.yml`)

#### Changed
- 渲染流程从纯初始化升级为每帧渲染循环
- JNI 桥接从 3 个方法扩展至 6 个方法
- 架构文档更新为实际的方法名和流程

#### Fixed
- 架构图中 `check_gpu_availability()` → 更正为 `WmRenderer::create()`
- DLL 加载路径描述与实际代码一致 (优先 JAR 同级目录)

---

### [0.1.0] - 2026-07-04

#### Added
- Rust + wgpu 项目结构 (workspace + wgpu-mc + wgpu-mc-jni)
- Fabric 模组项目 (MC 1.21.1 + Fabric Loom 1.10.3)
- JNI 桥接层初始 3 个 native 方法：`nativeInit`, `nativeHello`, `nativeTestDeviceInfo`
- Java 端 `D3D12Bridge` 类：DLL 自动加载 + 路径搜索
- GPU 适配器检测功能
- WGSL 基础着色器模板

#### Changed
- 从 C++/D3D12 方案重构为 Rust/wgpu 方案
- MC 版本从 26.1.2 降级到 1.21.1 (获得完整 Fabric API 支持)
- Gradle 配置：使用 JDK 21 编译 (解决 JDK 25 兼容性问题)

#### Fixed
- OpenGL + D3D12 共享 HWND 导致的 GPU 设备移除崩溃
- Gradle wrapper SSL 证书问题
- JNI 库加载路径问题

#### Removed
- 废弃的 C++ 构建配置 (已归档)

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏 | Minecraft | 1.21.1 |
| 加载器 | Fabric Loader | 0.19.3 |
| API | Fabric API | 0.116.13+1.21.1 |
| 语言 | Java | 21 |
| 图形 | wgpu (WebGPU) → DX12 | 23 |
| 语言 | Rust | 2021 edition |
| JNI | jni crate | 0.21 |
| 渲染 | OpenGL (Java 端) | Core Profile 330 |

---

## 构建与运行

### 系统要求

- **Windows 10/11** (x64)
- **JDK 21** (推荐 BellSoft Liberica JDK 或 Adoptium)
- **Rust 1.75+** (stable)
- **Gradle 8.13** (或通过 wrapper)

### 环境配置

#### 1. 安装 Rust

```powershell
# 从 https://rustup.rs/ 下载安装，或：
rustup default stable
rustup component add rust-analyzer rust-src
```

#### 2. 安装 JDK 21

```powershell
# 确认 Java 版本
java -version
# 应输出 Java 21.x.x

# 如未安装，推荐使用 BellSoft Liberica JDK:
# https://bell-sw.com/pages/downloads/?version=java-21&os=Windows+amd64
```

#### 3. 配置环境变量 (可选)

```powershell
# 设置 JAVA_HOME (如果尚未设置)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x"
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
# 输出: build/libs/gl4dx12-0.1.0.jar
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
     "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\mods\"

# 2. 复制 DLL 到 dx12mod 目录
copy rust\target\release\wgpu_mc_jni.dll ^
     "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\"

# 3. 启动 Minecraft 1.21.1-Fabric_0.19.3
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
[INFO] GL4DX12 Mod initialized!
```

---

## 配置方法

### Minecraft 版本配置

编辑 `fabric/gradle.properties`：

```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.19.3
fabric_version=0.116.13+1.21.1
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
D:\.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\wgpu_mc_jni.dll
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
3. 启动 Minecraft 1.21.1-Fabric_0.19.3
4. 观察控制台日志确认模组加载成功
5. 进入游戏验证 — 当前会显示蓝色渲染覆盖层 (离屏渲染输出)

### 调试技巧

#### 检查 Rust DLL 是否加载

```powershell
# 确认 DLL 文件存在
dir "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\wgpu_mc_jni.dll"

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
- `nativeInit()` — 初始化 Rust 环境
- `nativeHello("Hello from Minecraft!")` — 双向字符串传递
- `nativeTestDeviceInfo()` — GPU 适配器检测
- `nativeSetWindow(hwnd)` — 传递 MC 窗口句柄
- `nativeRenderFrame()` — 每帧渲染并返回 RGBA 像素数据

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
| `Unsupported class file major version 69` | JDK 版本不匹配 | 使用 JDK 21 编译 (非 JDK 25) |
| `Incompatible mods found!` | fabric.mod.json 版本声明错误 | 确认 `"minecraft": "~1.21.1"` |

---

## 已知问题与解决方案

| 问题 | 原因 | 解决方案 | 状态 |
|------|------|----------|------|
| `glTexImage2D(pixels)` 一步完成 crash | NVIDIA 驱动 bug | 改用 `glTexImage2D(null)` + `glTexSubImage2D(pixels)` 两步 | ✅ 已修复 |
| `glTexSubImage2D` 在页边界 ACCESS_VIOLATION | NVIDIA 驱动按页粒度预读，缓冲区非页对齐 | `MemoryUtil.memAlloc` + 4KB 填充，`buf.limit(pixels.length)` | ✅ 已修复 |
| 按 Esc/设置菜单 crash | HUD callback 与 Screen 渲染 GL 状态冲突 | 当前 Screen 不为 null 时跳过 GL 绘制 | ✅ 已修复 |
| 缓冲区大小不匹配导致 nvoglv64 越界 | 帧大小与窗口尺寸不一致 | 用 `bufferBytes` 反推安全 `height` 再调用 glTexSubImage2D | ✅ 已修复 |
| 资源重载后渲染 crash | GL 状态未清理 | 重载检测时重置 `vaoId`/`shaderValid`/`texAllocated`/`pendingPixels` | ✅ 已修复 |
| 每帧调用 Rust 渲染 freeze | GPU 命令队列竞争 | 节流到每 100ms 一次 | ✅ 已修复 |
| 调试日志导致卡顿 | 每 tick 写磁盘 | 移除所有非必要日志 | ✅ 已修复 |
| `setWindow` 重复调用 | JNI 开销 + 日志轰炸 | `lastSetHwnd` 缓存，相同 HWND 直接跳过 | ✅ 已修复 |

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
| 离屏渲染 | ✅ | `render_frame()` 输出 RGBA 像素 |
| 像素回传 | ✅ | Rust → Java byte[] → OpenGL 纹理 → 全屏 Quad |
| 独立测试程序 | ✅ | `examples/simple.rs` 可独立运行渲染三角形 |
| 窗口尺寸同步 | ✅ | `nativeResize()` 更新渲染器尺寸 |

### 阶段 3：Fabric 事件系统集成 ✅ 已完成

| 任务 | 状态 | 说明 |
|------|------|------|
| ClientTickEvents | ✅ | 计时、资源重载检测、调用 Rust 渲染 |
| HudRenderCallback | ✅ | OpenGL 纹理上传 + 全屏 quad 绘制 |
| GL 状态管理 | ✅ | 完整的保存/恢复机制 |
| VAO/Shader 持久化 | ✅ | 首次创建，丢失后自动重建 |

### 阶段 4：实际 Minecraft 场景渲染 ❌ 未开始

当前 `render_frame()` 只生成纯色帧（蓝色）。要实现实际 Minecraft 场景渲染，需要：

| 任务 | 优先级 | 说明 |
|------|--------|------|
| 区块渲染 | 🔴 P0 | 方块网格生成 + 顶点缓冲区 |
| 天空盒与云雾 | 🟠 P1 | 简单着色器即可 |
| 实体渲染 | 🟠 P1 | 模型加载 + 骨骼动画 |
| 粒子系统 | 🟡 P2 | 点精灵 (point sprites) |
| 半透明物体排序 | 🟡 P2 | 深度排序算法 |
| 后期特效 | 🟢 P3 | 泛光、阴影、色调映射 |

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
```

---

## 贡献指南

### 参与方式

##### 暂时不接受任何贡献

### 当前可认领的任务

| 任务 | 优先级 | 说明 |
|------|--------|------|
| 区块渲染 | 🔴 P0 | 方块网格生成 + 顶点缓冲 |
| 天空盒渲染 | 🟠 P1 | 简单着色器即可 |
| RenderGraph | 🔴 P0 | 配置驱动渲染管线 |

---

## 许可证

MIT License
