```markdown
# GL4DX12 - Minecraft D3D12 渲染模组

> 为 Minecraft 26.1.2 (1.21.5) 实现的 D3D12 渲染后端，通过覆盖层窗口 + GDI 捕获 + D3D12 几何叠加实现完整渲染管线。

---

## 当前状态

**阶段 1+2 已完成并稳定运行** — D3D12 成功渲染到 Minecraft 窗口，MVP 矩阵同步正确。

| 功能 | 状态 |
|------|------|
| D3D12 覆盖层窗口 | ✅ 无边框、点击穿透、自动追踪 MC 窗口 |
| GDI BitBlt 背景捕获 | ✅ MC 画面作为 D3D12 纹理背景层 |
| 几何叠加层 | ✅ D3D12 渲染方块几何（MVP 变换） |
| ModelView 矩阵同步 | ✅ 反射 `RenderSystem.getModelViewMatrix()` |
| Projection 矩阵 | ✅ 手动计算透视投影（FOV 70° + aspect） |
| 顶点颜色 | ✅ ABGR 转 RGBA（MC BufferBuilder 小端序） |
| 窗口缩放自适应 | ✅ SwapChain Resize + RTV 重建 |
| F6 热键切换 | ✅ 实时开关 D3D12 覆盖层 |

---

## 技术架构

### 渲染管线（每帧）

```
1. RepositionOverlay()      → 追踪 MC 窗口位置/尺寸
2. CaptureMCFrame()         → GDI BitBlt 捕获 MC 画面 → D3D12 纹理
3. Present Layer 0          → 全屏四边形渲染 MC 背景
4. syncMatrices()           → 反射获取 ModelView + 计算 Projection
5. nativeRecordVertices()   → 上传 MC 顶点数据（世界坐标）
6. Present Layer 1          → D3D12 几何渲染（MVP 变换）
7. Present()                → 显示到覆盖层窗口
```

### 关键组件

| 组件 | 文件 | 说明 |
|------|------|------|
| C++ 核心 | `d3d12bridge.cpp` | D3D12 设备、SwapChain、PSO、顶点缓冲、纹理、GDI 捕获 |
| JNI 桥接 | `DX12LibClient.java` | 25 个 native 方法声明 |
| 业务逻辑 | `D3D12Bridge.java` | 顶点展开、矩阵同步、反射调用 |
| Mod 入口 | `Dx12Mod.java` | F6 热键、初始化、窗口句柄传递 |
| Mixin | `BufferBuilderMixin` | 钩子 `build()` 提取 MeshData |

### 顶点数据流

```
BufferBuilder.build()
  → BufferBuilderMixin (反射读取 MeshData)
    → D3D12Bridge.processMeshData() (quad→tri, strip→tri)
      → DX12LibClient.nativeRecordVertices/UV/Colors()
        → d3d12bridge.cpp: 写入 upload VB + 记录 DrawChunk
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
| 投影矩阵缺失 | MC 26.1.2 无 `getProjectionMatrix()` | 手动计算透视投影（FOV + aspect） |

---

## 构建要求

### 环境

- Visual Studio 2022 (MSVC v143)
- Windows SDK 10.0.28000+
- JDK 21+
- Gradle 8.x

### 构建步骤

```batch
# 1. 编译 DLL
compile_dll.bat

# 2. 构建 JAR
gradlew build

# 3. 输出
build/libs/gl4dx12-1.0.0.jar
```

### DLL 导出函数（25 个）

| 类别 | 函数 |
|------|------|
| 生命周期 | `nativeInit`, `nativeCleanup`, `nativeResize` |
| 渲染 | `nativeBeginFrame`, `nativeEndFrame`, `nativePresent` |
| 顶点 | `nativeRecordVertices`, `nativeRecordUV`, `nativeRecordColors`, `nativeDraw` |
| 状态 | `nativeSetBlendMode`, `nativeSetDepthMode`, `nativeSetCullMode` |
| 矩阵 | `nativeSetMvp` |
| 调试 | `nativeIsD3D12Active`, `nativeGetD3D12Info`, `nativeShowDebugWindow` |
| 纹理 | `nativeCreateTexture`, `nativeUpdateTexture`, `nativeBindTexture` |

---

## 日志文件

| 文件 | 内容 |
|------|------|
| `C:\temp\gl4dx12_d3d12.log` | D3D12 初始化、窗口尺寸、MVP 矩阵值、渲染诊断 |
| `C:\temp\gl4dx12_mvp.log` | 矩阵反射结果、ModelView 值、Projection 计算 |
| `latest.log` | Minecraft 日志 + Mod 诊断输出 |

---

## 待完成（阶段 3-5）

| 阶段 | 目标 | 复杂度 |
|------|------|--------|
| 3 | 禁用 OpenGL 渲染，完全切换 D3D12 | 中 |
| 4 | 多 Pass 渲染（半透明、GUI、粒子） | 中 |
| 5 | Shader 系统 + 后处理 | 高 |

---

## 许可证

MIT

---

## 仓库

[https://github.com/366862732/DirectXmod](https://github.com/366862732/DirectXmod)
```

这个 README 涵盖了：
- 当前完成的状态（阶段 1+2）
- 完整的技术架构图和组件说明
- 已解决的 6 个主要问题及根因
- 构建环境和步骤
- 25 个 JNI 导出函数列表
- 日志文件位置和用途
- 后续路线图（阶段 3-5）
