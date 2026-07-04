# wgpu-mc — Minecraft D3D12 渲染引擎 (Rust + wgpu)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blueviolet)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://www.minecraft.net/)
[![Rust](https://img.shields.io/badge/Rust-2021-orange)](https://www.rust-lang.org/)
[![wgpu](https://img.shields.io/badge/wgpu-23-blue)](https://wgpu.rs/)

> 为 Minecraft Java Edition 1.21.1 实现的 DirectX 12 渲染后端，通过 Rust + wgpu 渲染引擎 + JNI 桥接，将 OpenGL 渲染替换为 D3D12，以提升图形性能并降低 TDR 风险。

---

## 目录

- [项目概述](#项目概述)
- [架构设计](#架构设计)
- [功能状态](#功能状态)
- [技术栈](#技术栈)
- [构建与运行](#构建与运行)
- [开发路线图](#开发路线图)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目概述

**wgpu-mc** 是一个 Fabric 模组，通过 Rust + wgpu 渲染引擎实现 Minecraft 的 DirectX 12 后端。项目采用三层架构：

```
Java Fabric Mod ↔ JNI Bridge (Rust DLL) ↔ wgpu 渲染引擎
```

### 为什么要用 Rust + wgpu？

- **跨平台**：wgpu 基于 WebGPU 标准，一次编写可在 Windows/Linux/macOS 运行
- **安全性**：Rust 内存安全，避免 C++ 常见的 UAF/野指针问题
- **现代图形 API**：wgpu 自动选择最佳后端（DX12/Vulkan/Metal）
- **低 TDR 风险**：wgpu 内部优化资源加载和 GPU 同步

### 迁移历史

| 阶段 | 技术栈 | 状态 |
|------|--------|------|
| 第一阶段 (已废弃) | C++/D3D12 + JNI | 存在 OpenGL/D3D12 共享 HWND 冲突，导致 GPU 设备移除崩溃 |
| 第二阶段 (当前) | Rust/wgpu + JNI | 独立渲染管线，避免 GPU 冲突 |

---

## 架构设计

### 项目结构

```
DirectXmod-wgpu/
├── fabric/                    # Fabric 模组 (Java)
│   ├── src/main/java/com/dx12/
│   │   ├── Dx12Mod.java      # 模组入口 (ClientModInitializer)
│   │   └── D3D12Bridge.java  # JNI 桥接封装
│   ├── src/main/resources/
│   │   ├── fabric.mod.json   # Fabric 模组配置
│   │   └── mixins.gl4dx12.json
│   ├── build.gradle          # Fabric Loom 构建配置
│   └── gradle.properties
├── rust/                      # Rust 渲染引擎
│   ├── Cargo.toml            # Workspace 配置
│   ├── wgpu-mc/              # 核心渲染库
│   │   ├── src/lib.rs        # WmRenderer 骨架
│   │   └── Cargo.toml
│   └── wgpu-mc-jni/          # JNI 桥接层
│       ├── src/lib.rs        # native 函数导出
│       └── Cargo.toml
└── README.md
```

### 渲染流程

```
1. Minecraft 启动 → Fabric Loader 加载模组
   ↓
2. Dx12Mod.onInitializeClient() 初始化
   ├─ D3D12Bridge.init() → JNI 加载 wgpu_mc_jni.dll
   ├─ D3D12Bridge.sayHello() → 测试 JNI 通信
   └─ D3D12Bridge.getDeviceInfo() → 检测 DX12 适配器
   ↓
3. wgpu 检测 GPU 可用性
   ├─ Instance::new() 创建 wgpu 实例
   ├─ request_adapter() 查找 DX12/Vulkan 适配器
   └─ 返回适配器信息给 Java
   ↓
4. (未来) Mixin 拦截 LevelRenderer.renderLevel()
   ├─ 取消 OpenGL 渲染
   ├─ 传递顶点/纹理数据到 Rust
   └─ wgpu 渲染到独立窗口
```

---

## 功能状态

### ✅ 已完成

| 模块 | 说明 |
|------|------|
| **JNI 通信链路** | Java ↔ Rust 双向通信完全打通 |
| **wgpu 核心骨架** | WmRenderer 初始化 + GPU 适配器检测 |
| **Fabric 模组** | MC 1.21.1 + Fabric Loom 1.10.3 编译通过 |
| **DLL 加载器** | 自动搜索 dx12mod/wgpu_mc_jni.dll 路径 |
| **日志系统** | env_logger + log 跨语言日志 |
| **构建自动化** | Gradle + Cargo 双构建系统 |

### 🟡 开发中 (当前卡点)

| 卡点 | 严重性 | 说明 |
|------|--------|------|
| **MC 窗口集成** | 🔴 最高 | 需要获取 MC 的 GLFW HWND 并创建 wgpu Surface |
| **顶点数据传递** | 🟠 高 | BufferBuilder 拦截 + 顶点格式解析待实现 |
| **渲染管线** | � 高 | terrain/sky/entities/particles 渲染 Pass 未实现 |

### ❌ 未开始

- Shader 系统 (WGSL 着色器)
- RenderGraph 配置驱动管线
- 顶点数据压缩 (BlockstateKey/Palette)
- 后处理效果 (抗锯齿、色彩校正)

---

## 技术栈

### Java 端

| 依赖 | 版本 | 用途 |
|------|------|------|
| `com.mojang:minecraft` | 1.21.1 | Minecraft 游戏代码 |
| `net.fabricmc:fabric-loader` | 0.16.9 | Fabric 模组加载器 |
| `net.fabricmc:fabric-api` | 0.116.6+1.21.1 | Fabric API |
| `org.slf4j:slf4j-api` | - | 日志接口 |

### Rust 端

| 依赖 | 版本 | 用途 |
|------|------|------|
| `wgpu` | 23 | 跨平台图形渲染 |
| `jni` | 0.21 | Java Native Interface |
| `log` / `env_logger` | 0.4 / 0.10 | 跨语言日志 |
| `bytemuck` | 1.14 | 安全字节处理 |
| `futures` | 0.3 | 异步转同步 |

---

## 构建与运行

### 系统要求

- **Windows 10/11** (x64)
- **Java 21+** (推荐 JDK 21)
- **Rust 1.70+** (stable)
- **Gradle 8.13+** (或通过 wrapper)
- **Minecraft 1.21.1** + Fabric Loader 0.19.3

### 快速开始

#### 1. 编译 Rust 端

```bash
cd rust
cargo build --release
```

生成的 DLL 位于：
```
rust\target\release\wgpu_mc_jni.dll
```

#### 2. 编译 Java 端

```bash
cd fabric
gradle clean build
```

生成的 JAR 位于：
```
fabric\build\libs\gl4dx12-0.1.0.jar
```

#### 3. 部署到 Minecraft

```powershell
# 复制 JAR 到 mods 目录
Copy-Item fabric\build\libs\gl4dx12-0.1.0.jar `
  "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\mods\"

# 复制 DLL 到 dx12mod 目录
Copy-Item rust\target\release\wgpu_mc_jni.dll `
  "$env:APPDATA\.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\"
```

#### 4. 启动游戏

使用 PCLCE 或其他 Fabric 启动器加载 `1.21.1-Fabric_0.19.3` 版本。

启动日志应显示：
```
[INFO] GL4DX12 Mod initializing...
[INFO] Rust responded: Hello from Rust wgpu! You said: Hello from Minecraft!
[INFO] Device info: wgpu-mc-jni loaded. DX12 adapter: AVAILABLE
[INFO] GL4DX12 Mod initialized successfully!
```

### 开发模式

#### 启用详细日志

```bash
# Rust 端日志
set RUST_LOG=debug
cargo build --release

# Java 端日志
# 在 Fabric 启动器中添加 JVM 参数:
# -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```

#### 重新编译单个组件

```bash
# 只编译 Rust
cd rust && cargo build --release

# 只编译 Java
cd fabric && gradle build --rerun-tasks
```

---

## 开发路线图

### 第一阶段：JNI 通信链路 ✅ (已完成)

- [x] Rust wgpu 项目初始化
- [x] JNI 桥接层实现
- [x] Fabric 模组编译
- [x] Java ↔ Rust 双向通信验证
- [x] GPU 适配器检测

### 第二阶段：wgpu 渲染引擎骨架 (进行中)

- [ ] WmRenderer 完整实现
- [ ] 独立窗口测试 (winit)
- [ ] WGSL 着色器基础框架
- [ ] 三角形/纯色渲染验证

### 第三阶段：Minecraft 集成 (计划中)

- [ ] Mixin 拦截 LevelRenderer.renderLevel()
- [ ] GLFW HWND 获取与 Surface 绑定
- [ ] 顶点数据传递 (BufferBuilder 拦截)
- [ ] MVP 矩阵同步

### 第四阶段：功能完善 (长期)

- [ ] 区块渲染 (terrain pass)
- [ ] 天空盒渲染 (sky pass)
- [ ] 实体渲染 (entity pass)
- [ ] 粒子系统 (particle pass)
- [ ] 半透明物体深度排序
- [ ] RenderGraph 配置驱动管线

---

## 常见问题

### Q: 启动时提示 `UnsatisfiedLinkError: wgpu_mc_jni.dll`

**A:** 确保 DLL 文件位于：
```
.minecraft\versions\1.21.1-Fabric_0.19.3\dx12mod\wgpu_mc_jni.dll
```

### Q: GPU 适配器检测失败

**A:** 检查：
1. 显卡驱动是否为最新版
2. 是否支持 DX12 (Win10+ 默认支持)
3. 查看 Rust 日志：`set RUST_LOG=debug`

### Q: 编译失败 `Unsupported class file major version 69`

**A:** 在 `fabric/gradle.properties` 中设置：
```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-21.0.10
```

### Q: 为什么从 C++ 迁移到 Rust？

**A:** C++ 方案存在 OpenGL/D3D12 共享同一 HWND 导致的 GPU 设备移除崩溃问题。Rust + wgpu 通过独立的渲染管线避免了这一冲突，同时获得更好的安全性和跨平台能力。

---

## 贡献指南

### 参与方式

1. **技术方案提案**：在 Issues 发布，标题标注 `[SOLUTION提案]`
2. **代码实现**：Fork 仓库提交 PR，关联对应 Issue
3. **BUG 反馈**：在 Issues 提交，包含日志和复现步骤

### 当前可认领的任务

| 任务 | 优先级 | 说明 |
|------|--------|------|
| MC 窗口集成 | 🔴 P0 | 获取 GLFW HWND 并创建 wgpu Surface |
| 顶点数据拦截 | 🔴 P0 | BufferBuilder Mixin 实现 |
| WGSL 着色器 | 🟠 P1 | terrain/sky 基础着色器 |
| 独立窗口测试 | 🟠 P1 | winit 示例程序 |

---

## 许可证

MIT License
