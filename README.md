# GL4DX12 - Minecraft D3D12 渲染模组

> 为 Minecraft 26.1.2 实现的 D3D12 渲染后端，通过覆盖层窗口 + LevelRenderer 拦截 + 全帧 D3D12 渲染实现完整渲染管线。

---

## 当前状态

**阶段 3 开发中** — 阶段 1+2 已完成并稳定运行，阶段 3 Java 侧已实现（LevelRenderer 拦截 + 多渲染对象捕获），C++ 侧天空/实体/粒子渲染待完成。

### 功能进度一览

| 功能 | 状态 | 说明 |
|------|------|------|
| D3D12 覆盖层窗口 | ✅ 已完成 | 无边框、点击穿透、自动追踪 MC 窗口 |
| GDI BitBlt 背景捕获 | ✅ 已完成 | MC 画面作为 D3D12 纹理背景层 |
| 几何叠加层 | ✅ 已完成 | D3D12 渲染方块几何（MVP 变换） |
| ModelView 矩阵同步 | ✅ 已完成 | 反射 `RenderSystem.getModelViewMatrix()` |
| Projection 矩阵 | ✅ 已完成 | 从 CameraRenderState 反射获取或手动计算 |
| 顶点颜色修正 | ✅ 已完成 | ABGR → RGBA 转换 |
| F6 热键切换 | ✅ 已完成 | 实时开关 D3D12 覆盖层 |
| F3 调试信息注入 | ✅ 已完成 | DebugScreenOverlay 替换 OpenGL 为 D3D12 信息 |
| GlDraw/GlBuffer 被动监听 | ✅ 已完成 | GL15/GL11 Mixin 跟踪 GL 状态变化 |
| LevelRenderer 拦截 | ✅ 已完成 | `renderLevel` HEAD 拦截，取消 OpenGL 渲染 |
| BufferBuilder 数据捕获 | ✅ 已完成 | `build()` RETURN 钩子，硬编码顶点布局解析 |
| 天空数据提取 | 🟡 Java 完成 | SkyboxExtractor 提取 skyColor/sunAngle/moonAngle 等 |
| 粒子数据提取 | 🟡 Java 完成 | ParticleExtractor 反射解析 QuadParticleRenderState |
| 实体状态捕获 | 🟡 Java 完成 | EntityRenderDispatcherMixin 捕获 EntityRenderState |
| 天空 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeRenderSky 待实现 |
| 实体 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeUploadEntities/nativeRenderEntities 待实现 |
| 粒子 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeUploadParticles/nativeRenderParticles 待实现 |
| 多 Pass 渲染（半透明/GUI） | ❌ 未开始 | |
| Shader 系统 + 后处理 | ❌ 未开始 | |

---

## 技术架构

### 渲染管线（每帧，阶段 3 目标）

```
LevelRenderer.renderLevel()
  → LevelRendererMixin.onRenderLevelHead() [ci.cancel() 阻止 OpenGL]
    → D3D12Bridge.renderFullFrame(levelState, cameraState, partialTick, modelView)
      1. nativeBeginFrame()
      2. syncMatrices(modelView, cameraState)
      3. renderSky()         → SkyboxExtractor → nativeSetSkyParameters() → nativeRenderSky()
      4. renderTerrain()     → BufferBuilderMixin 已上传顶点 → C++ 端绘制
      5. renderEntities()    → 缓存 EntityRenderState[] → nativeUploadEntities() → nativeRenderEntities()
      6. renderParticles()   → 缓存 ParticlesRenderState → nativeUploadParticles() → nativeRenderParticles()
      7. nativeEndFrame() + nativePresent()
```

### 关键组件

| 组件 | 文件 | 说明 |
|------|------|------|
| C++ 核心 | `d3d12bridge.cpp` | D3D12 设备、SwapChain、PSO、顶点缓冲、纹理、GDI 捕获、多线程渲染 |
| JNI 桥接 | `DX12LibClient.java` | 25 个 native 方法声明（含阶段 3 预留） |
| 业务逻辑 | `D3D12Bridge.java` | 顶点展开、矩阵同步、全帧编排、反射调用 |
| Mod 入口 | `Dx12Mod.java` | F6 热键、DLL 加载、HWND 传递、启动诊断 |
| 渲染提取器 | `SkyboxExtractor.java` | 从 SkyRenderState 提取天空参数 |
| 渲染提取器 | `ParticleExtractor.java` | 反射解析 QuadParticleRenderState 粒子数据 |
| 库加载 | `NativeUtils.java` | 从 JAR 提取 DLL 到临时目录并加载 |
| Mixin | `LevelRendererMixin.java` | 拦截 `renderLevel()` 取消 OpenGL |
| Mixin | `BufferBuilderMixin.java` | 拦截 `build()` 提取 MeshData |
| Mixin | `EntityRenderDispatcherMixin.java` | 拦截 `extractEntity()` 捕获实体状态 |
| Mixin | `ParticleEngineMixin.java` | 拦截 `extract()` 捕获粒子状态 |
| Mixin | `GlBufferMixin.java` | 被动监听 GL15 缓冲操作 |
| Mixin | `GlDrawMixin.java` | 被动监听 GL11 渲染状态 |
| Mixin | `DebugScreenMixin.java` | F3 调试屏幕注入 D3D12 信息 |

### 顶点数据流

```
BufferBuilder.build()
  → BufferBuilderMixin (RETURN 钩子)
    → D3D12Bridge.processMeshData()
      1. 反射读取 DrawState → VertexFormat → ByteBuffer
      2. 根据 vertexSize(24/28/32) 硬编码偏移量解析
      3. 提取 position(xyz), color(ABGR→RGBA), uv
      4. nativeRecordVertices() → C++ upload VB
```

### 天空数据流

```
LevelRenderer → SkyRenderState
  → SkyboxExtractor.extractSkyData()
    → [skyColor, sunAngle, moonAngle, starAngle, rainBrightness, starBrightness, sunriseColor]
      → nativeSetSkyParameters() → C++ 天空渲染 (待实现)
```

---

## 已解决的问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 灰色覆盖层 | `WS_EX_LAYERED` + D3D12 flip-model 冲突 | 移除 Layered，改用 GDI BitBlt 捕获 |
| 1/4 色块 | HiDPI 下尺寸不匹配 + MVP identity | 物理尺寸检测 + 手动投影矩阵计算 |
| 红色移动色块 | 顶点颜色 ABGR 序错误 | `readV()` 中交换 R/B 通道 |
| 缩放窗口崩溃 | SwapChain RT 未重建 | `ResizeBuffers()` + RTV/深度缓冲重建 |
| 黑屏残留 | 窗口缩放后未强制重绘 | `InvalidateRect()` + `UpdateWindow()` |
| 投影矩阵缺失 | MC 26.1.2 无 `getProjectionMatrix()` | CameraRenderState 反射 或 FOV 手动计算 |
| 多线程渲染崩溃 | 顶点数据上传和渲染线程竞态 | `std::mutex` + `g_frameReadyEvent` 同步 |
| VertexFormat 反射失败 | MC 26.1.2 类名变化 | 硬编码顶点布局偏移量（24/28/32 字节） |
| F3 闪烁/长文本 | Sodium 多 Pass 触发多次 extractLines | 节流 + 紧凑 GPU 名称 |
| DebugScreenOverlay 类名 | MC 26.1.2 Mojang 映射未知 | 运行时反射枚举全部方法 + fields |

---

## 构建要求

### 环境

- Visual Studio 2022 (MSVC v143)
- Windows SDK 10.0.28000+
- JDK 25 (Minecraft 26.1.2 要求)
- Gradle 8.x + Fabric Loom 1.15.5

### 构建步骤

```batch
# 1. 编译 C++ DLL
cd src\main\native\windows
compile_dll.bat

# 2. 构建 JAR（loom 自动处理 Mixin）
gradlew build

# 3. 输出
build/libs/gl4dx12-1.0.0.jar
```

### JNI 导出函数总览

| 类别 | 函数 | 状态 |
|------|------|------|
| 生命周期 | `nativeInit`, `nativeCleanup`, `nativeResize` | ✅ |
| 帧控制 | `nativeBeginFrame`, `nativeEndFrame`, `nativePresent` | ✅ |
| 顶点 | `nativeRecordVertices`, `nativeRecordColors`, `nativeRecordUV` | ✅ |
| 矩阵 | `nativeSetMvp` | ✅ |
| 调试 | `nativeIsD3D12Active`, `nativeGetD3D12Info` | ✅ |
| 天空 | `nativeSetSkyParameters`, `nativeRenderSky`, `nativeRenderSkybox` | ❌ stub |
| 地形 | `nativeRenderTerrain` | 🟡 复用几何管线 |
| 实体 | `nativeUploadEntities`, `nativeRenderEntities` | ❌ stub |
| 粒子 | `nativeUploadParticles`, `nativeRenderParticles` | ❌ stub |

---

## 日志文件

| 文件 | 内容 |
|------|------|
| `C:\temp\gl4dx12_d3d12.log` | D3D12 初始化、窗口尺寸、MVP 矩阵值、渲染诊断 |
| `C:\temp\gl4dx12_mvp.log` | 矩阵反射结果、ModelView 值、Projection 计算 |
| `latest.log` | Minecraft 日志 + Mod 诊断输出 |

---

## 路线图

| 阶段 | 目标 | 状态 |
|------|------|------|
| 1 | D3D12 覆盖层窗口 + SwapChain 渲染 | ✅ 完成 |
| 2 | GDI 背景捕获 + 几何叠加 + MVP 矩阵同步 | ✅ 完成 |
| 3 | 禁用 OpenGL，LevelRenderer 全帧 D3D12 接管 | 🟡 Java 侧完成，C++ 侧进行中 |
| 4 | 多 Pass 渲染（半透明、GUI、粒子） | ⬜ 未开始 |
| 5 | Shader 系统 + 后处理 | ⬜ 未开始 |

---

## 最近更新

| 日期 | 提交 | 内容 |
|------|------|------|
| 2026-06 | `0d1715d` | 阶段 3: 线程同步修复，几何体正常显示 |
| 2026-06 | `62deb29` | 修复窗口拦截问题 |
| 2026-06 | `ee82489` | 修复 BufferBuilderMixin 导入路径，完善 processMeshData |
| 2026-06 | `d6bd572` | 阶段 3: LevelRenderer 拦截 + 天空/粒子数据提取器 |
| 2026-06 | `81e4868` | 修复 DebugScreenMixin 参数类型 |
| 2026-06 | `4ea24e1` | 修复 F3 闪烁 + Mixin 方法名 |
| 2026-06 | `e063323` | F3 调试屏幕集成 D3D12 信息 |
| 2026-06 | `e85cbdb` | 隐藏最小化覆盖层，过滤非方块几何 |
| 2026-06 | `2e05612` | 顶点格式诊断日志增强 |
| 2026-06 | `908a380` | 手动计算透视投影矩阵 |

---

## 许可证

MIT

---

## 仓库

[https://github.com/366862732/DirectXmod](https://github.com/366862732/DirectXmod)

这个 README 涵盖了：
- 当前完成的状态（阶段 1+2）
- 完整的技术架构图和组件说明
- 已解决的 6 个主要问题及根因
- 构建环境和步骤
- 25 个 JNI 导出函数列表
- 日志文件位置和用途
- 后续路线图（阶段 3-5）
