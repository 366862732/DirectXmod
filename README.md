# GL4DX12 - Minecraft D3D12 渲染模组

> 为 Minecraft 26.1.2 实现的 D3D12 渲染后端，通过覆盖层窗口 + LevelRenderer 拦截 + 全帧 D3D12 渲染实现完整渲染管线。

---

## 当前状态

**阶段 3 开发中** — 阶段 1+2 已完成并稳定运行，阶段 3 Java 侧已实现（LevelRenderer 拦截 + 多渲染对象捕获），C++ 侧天空/实体/粒子渲染待完成。

好的，我已经仔细阅读了你提供的`README.md`文件。根据我们上一次对话中确认的进展——即**Java端`processMeshData`已成功运行并发送多种顶点数据**，以及**当前核心阻塞点在于C++端的数据接收与渲染**——我来帮你更新README，使其更准确地反映项目当前的真实状态。

以下是更新后的`README.md`相关章节内容，你可以直接替换原有部分：

---

### 当前状态

**阶段 3 开发中** — 阶段 1+2 已完成并稳定运行。阶段 3 的Java侧数据提取与拦截已实现并验证通过，**当前工作重心已转移至C++/D3D12端的顶点数据消费与绘制实现**。

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
| BufferBuilder 数据捕获与提取 | ✅ 已完成 | `build()` RETURN 钩子，支持多种顶点格式 (16/24/28/72字节) 解析并发送至C++端 |
| 天空数据提取 | 🟡 Java 完成 | SkyboxExtractor 提取 skyColor/sunAngle/moonAngle 等，C++端待实现 |
| 粒子数据提取 | 🟡 Java 完成 | ParticleExtractor 反射解析 QuadParticleRenderState，C++端待实现 |
| 实体状态捕获 | 🟡 Java 完成 | EntityRenderDispatcherMixin 捕获 EntityRenderState，C++端待实现 |
| **C++端顶点数据接收与GPU上传** | 🔴 **进行中** | **当前主要瓶颈**。需在`d3d12bridge.cpp`中完善JNI函数，创建顶点缓冲区并上传数据 |
| **C++端绘制命令调用** | 🔴 **进行中** | 需在渲染循环中正确设置顶点缓冲区视图并调用`DrawInstanced` |
| 天空 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeRenderSky 待实现 |
| 实体 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeUploadEntities/nativeRenderEntities 待实现 |
| 粒子 D3D12 渲染 | ❌ 待实现 | C++ 侧 nativeUploadParticles/nativeRenderParticles 待实现 |
| 多 Pass 渲染（半透明/GUI） | ❌ 未开始 | |
| Shader 系统 + 后处理 | ❌ 未开始 | |

### 关键组件

| 组件 | 文件 | 说明 |
|------|------|------|
| C++ 核心 | `d3d12bridge.cpp` | D3D12 设备、SwapChain、PSO、顶点缓冲、纹理、GDI 捕获、多线程渲染。**当前开发焦点** |
| JNI 桥接 | `DX12LibClient.java` | 25 个 native 方法声明（含阶段 3 预留），**需验证数据传递正确性** |
| 业务逻辑 | `D3D12Bridge.java` | 顶点展开、矩阵同步、全帧编排、反射调用。**Java端数据提取已稳定** |
| Mod 入口 | `Dx12Mod.java` | F6 热键、DLL 加载、HWND 传递、启动诊断 |
| 渲染提取器 | `SkyboxExtractor.java` | 从 SkyRenderState 提取天空参数 |
| 渲染提取器 | `ParticleExtractor.java` | 反射解析 QuadParticleRenderState 粒子数据 |
| 库加载 | `NativeUtils.java` | 从 JAR 提取 DLL 到临时目录并加载 |
| Mixin | `LevelRendererMixin.java` | 拦截 `renderLevel()` 取消 OpenGL |
| Mixin | `BufferBuilderMixin.java` | 拦截 `build()` 提取 MeshData，**已支持多种顶点格式** |
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
      2. 动态识别vertexSize(16/24/28/72等)，按偏移量解析位置、颜色、UV
      3. 提取 position(xyz), color(ABGR→RGBA), uv (已通过日志验证)
      4. nativeRecordVertices() → 数据发送至C++端 (下一步需在C++端实现接收与GPU上传)
```

### 已解决的问题

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| 灰色覆盖层 | `WS_EX_LAYERED` + D3D12 flip-model 冲突 | 移除 Layered，改用 GDI BitBlt 捕获 |
| 1/4 色块 | HiDPI 下尺寸不匹配 + MVP identity | 物理尺寸检测 + 手动投影矩阵计算 |
| 红色移动色块 | 顶点颜色 ABGR 序错误 | `readV()` 中交换 R/B 通道 |
| 缩放窗口崩溃 | SwapChain RT 未重建 | `ResizeBuffers()` + RTV/深度缓冲重建 |
| 黑屏残留 | 窗口缩放后未强制重绘 | `InvalidateRect()` + `UpdateWindow()` |
| 投影矩阵缺失 | MC 26.1.2 无 `getProjectionMatrix()` | CameraRenderState 反射 或 FOV 手动计算 |
| 多线程渲染崩溃 | 顶点数据上传和渲染线程竞态 | `std::mutex` + `g_frameReadyEvent` 同步 |
| VertexFormat 反射失败 | MC 26.1.2 类名变化 | **动态识别并硬编码多种顶点布局偏移量（16/24/28/72字节）** |
| F3 闪烁/长文本 | Sodium 多 Pass 触发多次 extractLines | 节流 + 紧凑 GPU 名称 |
| DebugScreenOverlay 类名 | MC 26.1.2 Mojang 映射未知 | 运行时反射枚举全部方法 + fields |
| **Java端数据提取异常** | **索引越界** | **已修复。通过增加日志和动态偏移量适配，成功提取多种顶点格式** |

---

### 🔍 核心更新说明

1.  **状态更精确**：明确阶段3的Java侧工作（数据提取）已完成并通过日志验证，当前瓶颈已转移到**C++端的数据消费**。
2.  **功能进度细化**：新增了“C++端顶点数据接收与GPU上传”和“C++端绘制命令调用”两项，并标注为“进行中”，清晰反映了当前最紧急的任务。
3.  **技术细节更新**：在“顶点数据流”和“已解决的问题”中，补充了`vertexSize`动态识别和多种格式支持的信息，与你的日志输出（16, 24, 28, 72字节）保持一致。
4.  **焦点明确**：在“关键组件”中高亮`d3d12bridge.cpp`为当前开发焦点，帮助团队或协作者快速抓住工作重心。

这份更新后的README能更真实地反映项目进展，特别是清晰地指出了下一个需要攻克的技术高地——C++/D3D12端的数据接收、上传与绘制集成。如果有其他部分需要调整，随时告诉我。