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
- [架构设计](#架构设计)
- [项目状态](#项目状态)
- [变更日志](#变更日志)
- [技术栈](#技术栈)
- [构建与运行](#构建与运行)
- [配置方法](#配置方法)
- [使用指引](#使用指引)
- [CI/CD](#cicd)
- [路线图](#路线图)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目概述

**GL4DX12** 是一个 Fabric 模组，通过 Rust + wgpu 实现 DirectX 12 渲染后端，利用 JNI（Java Native Interface）桥接 Minecraft 的 Java 层与本地渲染引擎。

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

## 架构设计

### 三层架构

```
┌─────────────────────────────────────────────────────────────┐
│              Minecraft 1.21.1 (Fabric)                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Dx12Mod.java (ClientModInitializer)                 │ │
│  │  • 模组入口点                                         │ │
│  │  • 初始化 JNI 桥接                                    │ │
│  │  • 测试 Rust 通信                                     │ │
│  └─────────────────────┬─────────────────────────────────┘ │
│                        ▼                                   │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  D3D12Bridge.java (JNI 封装层)                       │ │
│  │  • 动态加载 wgpu_mc_jni.dll                           │ │
│  │  • nativeInit() / nativeHello() / nativeTestDeviceInfo()│
│  └─────────────────────┬─────────────────────────────────┘ │
│                        ▼                                   │
└────────────────────────┼──────────────────────────────────┘
                         │ JNI (Java Native Interface)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         wgpu_mc_jni.dll (Rust JNI Bridge)                  │
│  • Java_com_dx12_D3D12Bridge_nativeInit()                  │
│  • Java_com_dx12_D3D12Bridge_nativeHello()                 │
│  • Java_com_dx12_D3D12Bridge_nativeTestDeviceInfo()        │
│  • Java_com_dx12_D3D12Bridge_nativeRenderFrame()           │
│  • Java_com_dx12_D3D12Bridge_nativeSetWindow()             │
│  • Java_com_dx12_D3D12Bridge_nativeResize()                │
│  • 日志: env_logger + log                                  │
└────────────────────────┬──────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         wgpu-mc (Rust 渲染引擎核心)                         │
│  • WmRenderer::create() — wgpu DX12 实例创建               │
│  • WmRenderer::render_frame() — 离屏渲染帧输出              │
│  • WmRenderer::resize() — 窗口尺寸调整                      │
└─────────────────────────────────────────────────────────────┘
```

### 渲染流程 (当前阶段)

```
1. Minecraft 启动 → Dx12Mod.onInitializeClient()
   ↓
2. D3D12Bridge.init() → System.load("wgpu_mc_jni.dll")
   ↓
3. nativeInit() → Rust env_logger::init()
   ↓
4. nativeHello("Hello from Minecraft!") → 返回 "Hello from Rust wgpu! ..."
   ↓
5. nativeTestDeviceInfo() → 检测 DX12 适配器可用性
   ↓
6. 客户端 Tick → 获取 HWND → nativeSetWindow(hwnd)
   ↓
7. 每帧 → nativeRenderFrame() → 获取 RGBA 像素 → 上传至 OpenGL 纹理 → 全屏 Quad 绘制
   ↓
8. 窗口大小变化 → syncWindowSize() → nativeResize(w, h)
```

---

## 项目状态

### 当前阶段：阶段 2 部分完成 ✅

| 状态 | 说明 |
|------|------|
| **阶段 1** | **已完成** — JNI 通信链路打通，Java ↔ Rust 双向通信正常 |
| **阶段 2** | **部分完成** — wgpu 渲染引擎骨架 + 离屏渲染 + 独立测试程序均可用 |
| **阶段 3** | 待开始 — Minecraft Mixin 集成，替换实际游戏渲染 |
| **阶段 4** | 待开始 — 功能完善与优化 |

### 已完成功能

| 模块 | 说明 |
|------|------|
| **Rust Workspace** | `wgpu-mc` (渲染引擎) + `wgpu-mc-jni` (JNI 桥接) 双 crate 结构 |
| **JNI 桥接层** | 6 个 native 方法：`nativeInit`, `nativeHello`, `nativeTestDeviceInfo`, `nativeRenderFrame`, `nativeSetWindow`, `nativeResize` |
| **Java Fabric 模组** | 基于 Fabric Loom 1.10.3，MC 1.21.1，Fabric API 0.116.6 |
| **DLL 自动加载** | 多级路径搜索策略，支持 JAR 同级目录及 `dx12mod/` 目录部署 |
| **GPU 适配器检测** | 通过 wgpu 创建 DX12 后端实例并检测适配器可用性 |
| **日志系统** | Rust `env_logger` + Java SLF4J 双端日志 |
| **离屏渲染** | `WmRenderer::render_frame()` 输出 RGBA 像素缓冲区 |
| **HWND 传递** | Java → Rust 窗口句柄传递，支持 `nativeSetWindow` / `nativeResize` |
| **像素回传** | Rust → Java `byte[]` 像素数据传输 + OpenGL 纹理上传 + 全屏 Quad 绘制 |
| **独立测试程序** | `examples/simple.rs` — winit + wgpu 弹出窗口渲染彩色三角形 |
| **WGSL 着色器** | `triangle.wgsl` (2D 顶点着色器) + `simple.wgsl` (3D 顶点着色器) |
| **预编译 DLL** | `wgpu_mc_jni.dll` 预打包在 `fabric/src/main/resources/` 中 |
| **GL 状态管理** | 完整的 Minecraft GL 状态保存/恢复机制，避免与 MC 渲染冲突 |
| **资源重载检测** | 自动检测 MC 资源重载并延迟渲染，避免 GL 资源失效 |
| **VAO 重建机制** | 检测到 GL 资源丢失时自动重建 VAO/Shader |

### 已完成

| 任务 | 说明 |
|------|------|
| **Surface 绑定 (基础)** | HWND 获取与传递已完成，wgpu Surface 绑定到 MC 窗口 |
| **独立测试程序** | `examples/simple.rs` 可独立运行，弹出 1280×720 窗口渲染三角形 |
| **WGSL 着色器** | 基础三角形着色器已实现 |

### 待开始

- 多 Pass 分层渲染 (GUI/3D 场景分离)
- RenderGraph 配置驱动管线
- 顶点数据压缩 (BlockstateKey)
- Shader 后处理 (抗锯齿、色彩校正)
- Mixin 替换实际游戏渲染 (LevelRenderer 拦截)

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

### Java 端 (Fabric 模组)

| 依赖 | 版本 | 用途 |
|------|------|------|
| `com.mojang:minecraft` | 1.21.1 | Minecraft 游戏代码 |
| `net.fabricmc:fabric-loader` | 0.16.9 | Fabric 模组加载器 |
| `net.fabricmc.fabric-api:fabric-api` | 0.116.6 | Fabric API 基础 |
| `net.fabricmc:sponge-mixin` | 0.17.3 | Mixin 注入框架 |
| `org.slf4j:slf4j-api` | - | Java 日志 |

### Rust 端

| 依赖 | 版本 | 用途 |
|------|------|------|
| `wgpu` | 23 | WebGPU 图形库 |
| `jni` | 0.21 | Java Native Interface |
| `bytemuck` | 1.14 | 安全字节处理 |
| `log` / `env_logger` | 0.4 / 0.10 | 日志系统 |
| `futures` | 0.3 | 异步支持 |
| `winit` | 0.30 | 窗口管理 (独立测试程序) |
| `raw-window-handle` | 0.6 | RWH API 桥接 |
| `windows-sys` | 0.59 | Windows API (Win32) |

### 构建工具

| 工具 | 版本 | 用途 |
|------|------|------|
| Gradle | 8.13 | Java 构建 |
| Fabric Loom | 1.10.3 | Minecraft 模组编译 |
| Cargo | Rust 1.75+ | Rust 构建 |
| JDK | 21 | Java 编译 (JDK 25 不兼容) |

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
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
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
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
```

### 构建步骤

#### 方式 A：分别构建 (推荐调试用)

```powershell
# 1. 构建 Rust DLL
cd rust
cargo build --release

# 生成的 DLL 位于: target\release\wgpu_mc_jni.dll

# 2. 构建 Fabric 模组
cd ..\fabric
gradlew build

# 生成的 JAR 位于: build\libs\gl4dx12-0.1.0.jar
```

#### 方式 B：一键构建

```powershell
# 在项目根目录
cd fabric
gradlew clean build --no-daemon
```

### 部署到 Minecraft

```powershell
# 1. 复制 JAR 到 mods 目录
copy fabric\build\libs\gl4dx12-0.1.0.jar ^
     "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\mods\"

# 2. 复制 DLL 到 dx12mod 目录
copy rust\target\release\wgpu_mc_jni.dll ^
     "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\"

# 3. 启动 Minecraft 1.21.1-Fabric_0.19.3
```

### 验证安装

启动游戏后，检查日志应看到：

```
[INFO] GL4DX12 Mod initializing...
[INFO] Using wgpu + Rust rendering engine (independent surface approach)
[INFO] [D3D12Bridge] Rust JNI library loaded and initialized.
[INFO] Rust responded: Hello from Rust wgpu! You said: Hello from Minecraft!
[INFO] Device info: wgpu-mc-jni loaded. DX12 adapter: AVAILABLE
[INFO] GL4DX12 Mod initialized successfully!
```

---

## 配置方法

### Minecraft 版本配置

编辑 `fabric/gradle.properties`：

```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.9
fabric_version=0.116.6+1.21.1
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

`D3D12Bridge.java` 按以下顺序搜索 DLL：

1. JAR 包同级目录 `wgpu_mc_jni.dll`
2. `dx12mod/wgpu_mc_jni.dll` (相对于 MC 工作目录)
3. `.minecraft/dx12mod/wgpu_mc_jni.dll`
4. `<user.dir>/dx12mod/wgpu_mc_jni.dll`

如需自定义路径，修改 `getDllPath()` 方法。

> **注意**：预编译的 `wgpu_mc_jni.dll` 已打包在 `fabric/src/main/resources/` 中，可直接使用。

### 日志级别控制

```powershell
# Rust 端日志 (通过环境变量)
$env:RUST_LOG = "debug"  # 或 info, warn, error
java -jar minecraft.jar

# Java 端日志 (通过 logback.xml 或 fabric 配置)
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

#### 验证像素回传

模组每帧调用 `nativeRenderFrame()` 返回蓝色像素缓冲区，通过 OpenGL `glTexSubImage2D` 上传至纹理并在全屏 Quad 上绘制。可在游戏中观察到蓝色覆盖层。

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `NoClassDefFoundError: net/minecraft/client/Minecraft` | JAR 版本过旧 | 重新编译并复制最新 JAR |
| `UnsatisfiedLinkError: wgpu_mc_jni.dll` | DLL 路径不正确 | 确认 DLL 在 `dx12mod/` 目录下 |
| `Unsupported class file major version 69` | JDK 版本不匹配 | 使用 JDK 21 编译 (非 JDK 25) |
| `Incompatible mods found!` | fabric.mod.json 版本声明错误 | 确认 `"minecraft": "~1.21.1"` |

---

## CI/CD

### GitHub Actions

项目配置了自动化 CI 流水线 (`.github/workflows/build.yml`)：

- **触发条件**：push / pull_request 到主分支
- **运行环境**：Ubuntu 24.04, JDK 25
- **构建步骤**：
  1. `./gradlew build` — 构建 Fabric 模组 JAR
  2. 上传构建产物作为 Artifact

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

### 阶段 2：wgpu 渲染引擎骨架 🚧 部分完成

| 任务 | 状态 | 说明 |
|------|------|------|
| Surface 绑定 | ✅ 基础完成 | HWND 获取与传递已完成 |
| 独立测试程序 | ✅ 完成 | `examples/simple.rs` 可独立运行渲染三角形 |
| WGSL 着色器 | ✅ 基础完成 | `triangle.wgsl` + `simple.wgsl` |
| 离屏渲染 | ✅ 完成 | `WmRenderer::render_frame()` 输出 RGBA 像素 |
| 像素回传 | ✅ 完成 | Rust → Java byte[] → OpenGL 纹理 → 全屏 Quad |
| 顶点缓冲区 | 🟡 P2 | 顶点数据传输与真实渲染管线 |

### 阶段 3：Minecraft Mixin 集成 ⏳ 待开始

| 任务 | 优先级 | 说明 |
|------|--------|------|
| LevelRenderer 拦截 | 🔴 P0 | 取消 OpenGL 渲染，调用 Rust |
| 窗口句柄传递 | 🟠 P1 | GLFW → HWND → wgpu Surface |
| 顶点数据传递 | 🟠 P1 | MC 顶点 → Rust 缓冲区 |
| 纹理上传 | 🟡 P2 | MC 纹理 → DX12 SRV |

### 阶段 4：功能完善 🗓 持续迭代

| 任务 | 优先级 | 说明 |
|------|--------|------|
| RenderGraph | 🔴 P0 | 配置驱动渲染管线 |
| 半透明排序 | 🟠 P1 | Alpha 混合 + 深度排序 |
| 天空盒渲染 | 🟠 P1 | 昼夜循环 + 云层 |
| 粒子系统 | 🟡 P2 | 帧动画 + 透明度渐变 |
| 实体渲染 | 🟡 P2 | 骨骼蒙皮 + 动画 |
| 后处理特效 | 🟢 P3 | 抗锯齿 + 色彩校正 |

---

## 贡献指南

### 参与方式

##### 暂时不接受任何贡献

### 当前可认领的任务

暂无

### 贡献者激励

暂无

### 代码规范

暂无

## 许可证

MIT License
