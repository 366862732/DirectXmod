好的，这是根据你的项目当前状态生成的完整 README.md：

---

# DirectXmod — Minecraft D3D12 渲染模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blueviolet)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-green)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)

> 为 Minecraft Java Edition 实现的 DirectX 12 渲染后端，通过 JNI 桥接 + Mixin 拦截，将 OpenGL 渲染替换为 D3D12，以提升图形性能并降低 TDR 风险。支持 26.1.2 版本。

---

## 📖 目录

- [项目概述](#项目概述)
- [架构设计](#架构设计)
- [功能状态](#功能状态)
- [技术栈](#技术栈)
- [构建与运行](#构建与运行)
- [当前卡点与攻坚计划](#当前卡点与攻坚计划)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目概述

**DirectXmod** 是一个 Fabric 模组，通过 JNI（Java Native Interface）将 Minecraft 的 OpenGL 渲染调用拦截并转发到 Windows 本地 C++ DLL，在 DirectX 12 中执行真正的 GPU 渲染。

### 为什么要做这个？

- **性能提升**：D3D12 提供更底层的 GPU 控制，减少驱动开销。
- **TDR 缓解**：通过分阶段资源加载和 GPU 同步，减少超时崩溃。
- **技术验证**：在 Minecraft 这样的大型 Java 应用中验证 D3D12 的可行性。

### 当前状态

| 状态 | 说明 |
|------|------|
| **阶段 3** | 功能完善中。核心渲染管线已打通，粒子/实体/液体/纹理基础框架落地，进入细分功能攻坚阶段。 |
| **整体完成度** | ≈ 85% |
| **稳定性** | OpenGL 回退模式稳定；D3D12 模式处于渐进式恢复中 |

---

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Minecraft 游戏引擎                        │
│                    (LWJGL + GLFW)                          │
└──────────────────────────┬──────────────────────────────────┘
                           │ GLFW 窗口 HWND 共享
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              Fabric Mod (com.dx12)                         │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Mixin 拦截层 (7 个 Mixin)                          │ │
│  │  • LevelRendererMixin  → 取消 OpenGL 渲染          │ │
│  │  • BufferBuilderMixin  → 捕获顶点数据              │ │
│  │  • Entity/Particle 拦截 → 收集渲染状态             │ │
│  └─────────────────────┬─────────────────────────────────┘ │
│                        ▼                                   │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  D3D12Bridge.java (业务逻辑核心)                     │ │
│  │  • 矩阵同步 (MVP)                                   │ │
│  │  • 顶点数据处理 (坐标检测/格式解析)                 │ │
│  │  • 全帧渲染编排                                      │ │
│  └─────────────────────┬─────────────────────────────────┘ │
│                        ▼                                   │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  DX12LibClient.java (JNI 桥接)                       │ │
│  │  35+ native 方法声明                                 │ │
│  └─────────────────────┬─────────────────────────────────┘ │
└────────────────────────┼──────────────────────────────────┘
                         │ JNI
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              C++ D3D12 Bridge (d3d12bridge.dll)            │
│  • D3D12 设备/交换链/命令队列/Fence                        │
│  • 13 阶段异步初始化 (ST_EMPTY → ST_FULL_READY)            │
│  • RenderLoop 独立渲染线程                                 │
│  • GDI BitBlt 背景捕获 + 全屏四边形                        │
│  • 顶点缓冲管理 (DrawChunk 批次提交)                       │
│  • 纹理上传 / SRV 管理                                     │
└─────────────────────────────────────────────────────────────┘
```

### 核心渲染流程

```
1. Minecraft 调用 LevelRenderer.renderLevel()
   ↓
2. LevelRendererMixin @Inject HEAD → ci.cancel()
   ↓
3. D3D12Bridge.renderFullFrame()
   ├── nativeBeginFrame()
   ├── syncMatrices() → nativeSetMvp()
   ├── renderSky() → nativeSetSkyParameters() + nativeRenderSky()
   ├── renderTerrain() → 从 ChunkSectionsToRender 提取 Draw 数据
   ├── renderEntities() → nativeUploadEntities() + nativeRenderEntities()
   ├── renderParticles() → nativeUploadParticles() + nativeRenderParticles()
   └── nativeEndFrame() + nativePresent()
   ↓
4. C++ RenderLoop 线程:
   ├── 等待 g_frameReadyEvent
   ├── 上传顶点到 g_imVB
   ├── CPU 端 MVP 预变换
   ├── DrawInstanced 提交
   ├── Present(0, 0)
   └── Fence 同步
   ↓
5. D3D12 输出到 Minecraft 窗口 (同一 HWND)
```

---

## 功能状态

### ✅ 已完成

| 模块 | 说明 |
|------|------|
| **D3D12 基础设施** | 设备/交换链/命令队列/PSO/Fence 全链路打通，TDR 防护落地 |
| **覆盖层窗口** | 无边框、点击穿透、自动追踪 MC 窗口 |
| **GDI 背景捕获** | BitBlt 抓取 MC OpenGL 输出作为 D3D12 纹理背景 |
| **几何叠加层** | D3D12 渲染方块几何（支持 MVP 变换） |
| **矩阵同步** | ModelView/Projection 矩阵反射提取 + MVP 合并 |
| **顶点数据拦截** | BufferBuilder 拦截 + 多顶点格式解析 (12/16/24/28/72 字节) |
| **顶点类型自动检测** | 6 条规则区分 WORLD/SCREEN/NDC |
| **纹理采样基础** | 纹理上传、SRV 管理、资源状态转换 |
| **粒子/实体/液体框架** | JNI 接口 + C++ 接收框架已落地 |
| **F6 热键切换** | 实时开关 D3D12 覆盖层 |
| **F3 调试注入** | DebugScreenOverlay 替换 OpenGL 为 D3D12 信息 |
| **GlDraw/GlBuffer 监听** | GL15/GL11 Mixin 跟踪 GL 状态变化 |

### 🟡 开发中 (当前卡点)

| 卡点 | 严重性 | 说明 |
|------|--------|------|
| **半透明物体渲染** | 🔴 最高 | 深度排序逻辑未与 D3D12 混合状态联动，玻璃/水/树叶出现透层错乱 |
| **天空盒渲染** | 🟠 高 | 天空参数已提取，但着色器、星图、昼夜动画未整合到渲染管线 |
| **粒子动画** | 🟡 中 | 纹理分块采样、帧动画插值、生命周期透明度渐变待实现 |
| **实体骨骼蒙皮** | 🟡 中 | 静态实体已完成，带骨骼动画的玩家/生物需顶点蒙皮支持 |

### ❌ 未开始

- 多 Pass 分层渲染（GUI/3D 场景分离）
- Shader 系统 + 后处理（抗锯齿、色彩校正）
- 性能优化（顶点批处理、渲染线程调度）

---

## 技术栈

### Java 端

| 依赖 | 用途 |
|------|------|
| `com.mojang:minecraft:26.1.2` | Minecraft 游戏代码 |
| `net.fabricmc:fabric-loader` | Fabric 模组加载器 |
| `net.fabricmc:fabric-api` | Fabric API 基础 |
| `org.joml:joml` | 矩阵运算 |
| `org.lwjgl:lwjgl-glfw` | GLFW 窗口（获取 HWND） |
| `net.fabricmc:sponge-mixin` | Mixin 注入框架 |
| `net.java.dev.jna:jna` | 本地库访问辅助 |

### C++ 端

| 依赖 | 用途 |
|------|------|
| **DirectX 12 SDK** | 核心图形 API (`d3d12.lib`, `d3dcompiler.lib`) |
| **DXGI** | 交换链、适配器枚举 (`dxgi.lib`) |
| **Windows SDK** | 窗口管理、GDI 抓屏 (`user32.lib`, `gdi32.lib`) |
| **JNI** | Java-Native 接口 (`jni.h`) |
| **Microsoft WRL** | ComPtr 智能指针 |
| **d3dx12.h** | 微软官方 D3D12 帮助库 |

---

## 构建与运行

### 系统要求

- **Windows 10/11** (x64)
- **Java 21+**
- **Visual Studio 2022** (含 C++ 桌面开发工具)
- **Gradle** (8.5+，或使用 wrapper)

### 构建步骤

#### 1. 克隆仓库

```bash
git clone https://github.com/366862732/DirectXmod.git
cd DirectXmod
```

#### 2. 编译 Java 端

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

#### 3. 编译 C++ DLL

使用 Visual Studio 2022 打开：

```
src\main\native\windows\d3d12bridge\d3d12bridge.sln
```

选择 `Release x64`，重新生成。

或使用命令行：

```bash
cd src\main\native\windows
compile.bat
```

#### 4. 复制 DLL 到 Minecraft 目录

```bash
# 将生成的 DLL 复制到 dx12mod 目录
copy src\main\native\windows\d3d12bridge\x64\Release\d3d12bridge.dll ^
     %APPDATA%\.minecraft\versions\26.1.2-Fabric_0.19.3\dx12mod\
```

#### 5. 运行游戏

- 使用 Fabric 启动器加载 `26.1.2-Fabric_0.19.3` 版本
- 按 `F6` 键切换 D3D12 覆盖层（默认开启）

---

## 当前卡点与攻坚计划

### 🎯 短期攻坚计划 (1-2 周)

| 任务 | 优先级 | 预计耗时 |
|------|--------|----------|
| 半透明深度排序 + Alpha 混合 | 🔴 P0 | 3-5 天 |
| 天空盒完整渲染 (着色器 + 星图) | 🟠 P1 | 3-4 天 |
| 粒子动画 (帧插值 + 透明度渐变) | 🟡 P2 | 2-3 天 |
| 实体蒙皮预研 (玩家骨骼矩阵提取) | 🟡 P2 | 2-3 天 |

### 📋 已解决的历史遗留问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 0xc010 空指针崩溃 | JNI 在 D3D12 初始化前被调用 | 所有 JNI 函数增加 `g_dev` 空校验 + `g_deviceLost` 检查 |
| 3D 物品误判为 2D GUI | 顶点检测缺少 Z 轴判断 | `detectCoordSpace` 增加 Z 轴范围规则 |
| SRV 堆管理错误 | 纹理共享同一描述符 | 独立 SRV 偏移量，`g_texSlotMap` 管理分配 |
| GPU 超时 (TDR) | 初始化阶段负载集中 | 13 阶段异步加载 + Fence 同步 |

---

## 贡献指南

### 参与方式

1. **技术方案提案**：在 Issues 发布，标题标注 `[SOLUTION提案]`
2. **代码实现**：Fork 仓库提交 PR，关联对应 Issue
3. **BUG 反馈**：在 Issues 提交，包含日志和复现步骤

### 当前可认领的攻坚任务

| 任务 | 难度 | 说明 |
|------|------|------|
| 🟡 半透明物体深度排序 | 高 | 提取 MC 半透明绘制顺序 → D3D12 排序渲染 |
| 🟡 天空盒渲染管线 | 中 | 实现 `nativeRenderSky` 全流程 |
| 🟡 粒子帧动画 | 中 | 纹理分块采样 + 生命周期插值 |
| 🟡 实体骨骼蒙皮 | 高 | 提取骨骼矩阵 + 蒙皮着色器 |
| ❌ 多 Pass 分层渲染 | 中 | GUI / 3D 场景分离渲染 |
| ❌ Shader 后处理框架 | 中 | 抗锯齿、色彩校正等 |

### 贡献者激励

1. **源码永久标注**：贡献者信息记录在模块注释中
2. **README 贡献名单**：统一收录
3. **MC 百科官方词条**：正式发布后，贡献者写入 Mod 开发团队

---

## 许可证

MIT License
