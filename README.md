# GL4DX12 - Minecraft D3D12 渲染模组

> 为 Minecraft 26.1.2 实现的 D3D12 渲染后端，通过覆盖层窗口 + LevelRenderer 拦截 + 全帧 D3D12 渲染实现完整渲染管线。

---

## 当前状态

**阶段 3 开发中** — 阶段 1+2 已完成并稳定运行。阶段 3 的 Java 侧数据提取与拦截已实现并验证通过，**C++ 侧核心渲染管线已打通**，粒子/实体/液体/纹理基础框架已实现，进入功能完善阶段。

---

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
| BufferBuilder 数据捕获与提取 | ✅ 已完成 | 支持多种顶点格式 (16/24/28/72字节) 解析并发送至C++端 |
| **顶点类型自动检测** | ✅ 已完成 | `autoDetectVertexType` 6条规则区分 WORLD/SCREEN/NDC |
| **纹理采样基础** | ✅ 已完成 | 纹理上传、SRV管理、资源状态转换 |
| **粒子系统基础** | ✅ 已完成 | `nativeUploadParticles`/`nativeRenderParticles` 框架 |
| **实体渲染基础** | ✅ 已完成 | `nativeUploadEntities`/`nativeRenderEntities` 模型矩阵支持 |
| **液体渲染框架** | ✅ 已完成 | `nativeUploadLiquid`/`nativeRenderLiquid` 水和岩浆支持 |
| 天空盒 D3D12 渲染 | 🟡 开发中 | 天空参数已同步，渲染实现中 |
| 半透明物体渲染 | 🟡 开发中 | 深度排序和混合状态待完善 |
| 太阳/月亮/星星渲染 | 🟡 开发中 | 位置计算已实现，渲染待完善 |
| 多 Pass 渲染（半透明/GUI） | ❌ 未开始 | |
| Shader 系统 + 后处理 | ❌ 未开始 | |

---

### 关键组件

| 组件 | 文件 | 说明 |
|------|------|------|
| C++ 核心 | `d3d12bridge.cpp` | D3D12 设备、SwapChain、PSO、顶点缓冲、纹理、GDI 捕获、多线程渲染。**当前开发焦点** |
| JNI 桥接 | `DX12LibClient.java` | 35+ native 方法声明（纹理/实体/粒子/液体/天空） |
| 业务逻辑 | `D3D12Bridge.java` | 顶点展开、矩阵同步、全帧编排、反射调用 |
| Mod 入口 | `Dx12Mod.java` | F6 热键、DLL 加载、HWND 传递、启动诊断 |
| 渲染提取器 | `SkyboxExtractor.java` | 从 SkyRenderState 提取天空参数 |
| 渲染提取器 | `ParticleExtractor.java` | 反射解析 QuadParticleRenderState 粒子数据 |
| 库加载 | `NativeUtils.java` | 从 JAR 提取 DLL 到临时目录并加载 |
| Mixin | `LevelRendererMixin.java` | 拦截 `renderLevel()` 取消 OpenGL |
| Mixin | `BufferBuilderMixin.java` | 拦截 `build()` 提取 MeshData，支持多种顶点格式 |
| Mixin | `EntityRenderDispatcherMixin.java` | 拦截 `extractEntity()` 捕获实体状态 |
| Mixin | `ParticleEngineMixin.java` | 拦截 `extract()` 捕获粒子状态 |

---

### 顶点数据流
BufferBuilder.build()
→ BufferBuilderMixin (RETURN 钩子)
→ D3D12Bridge.processMeshData()

反射读取 DrawState → VertexFormat → ByteBuffer

动态识别vertexSize(16/24/28/72等)，按偏移量解析位置、颜色、UV

提取 position(xyz), color(ABGR→RGBA), uv (已通过日志验证)

autoDetectVertexType() → 自动判定 WORLD/SCREEN/NDC

nativeRecordVertices() → 数据发送至C++端 (每个DrawCall独立存储类型)

C++端根据类型使用透视/正交投影矩阵

text

---

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
| VertexFormat 反射失败 | MC 26.1.2 类名变化 | 动态识别并硬编码多种顶点布局偏移量 |
| F3 闪烁/长文本 | Sodium 多 Pass 触发多次 extractLines | 节流 + 紧凑 GPU 名称 |
| Java端数据提取异常 | 索引越界 | 增加日志和动态偏移量适配 |
| **0xc010 空指针崩溃** | **JNI函数在D3D12初始化前被调用** | **所有JNI函数增加D3D核心对象空校验** |
| **3D物品误判为2D GUI** | **顶点类型检测缺少Z轴深度** | **增加Z轴深度检测规则** |
| **SRV堆管理错误** | **所有纹理共享同一描述符** | **每个纹理独立SRV偏移量** |

---

### 近期更新

| 日期 | 提交 | 内容 |
|------|------|------|
| 2026-06-19 | `v0.3.0` | 粒子/实体/液体/纹理基础框架；顶点类型自动检测；0xc010空指针修复 |
| 2026-06-18 | `v0.2.0` | 阶段2完成：GDI背景捕获、几何叠加、MVP同步 |
| 2026-06-17 | `v0.1.0` | 阶段1完成：D3D12覆盖层窗口、SwapChain渲染 |

---

### 下一步计划

1. **完成天空盒渲染** — `nativeRenderSky` 实现
2. **完成半透明渲染** — 深度排序 + alpha混合
3. **完善粒子系统** — 粒子动画和纹理支持
4. **实体模型完整支持** — 骨骼动画和蒙皮
5. **多Pass渲染** — 半透明/GUI分层渲染

---

## 许可证

MIT

## 仓库

[https://github.com/366862732/DirectXmod](https://github.com/366862732/DirectXmod)
