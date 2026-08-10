# DirectXmod

> **一个 Minecraft 26.1.2+ Fabric 模组的 DirectX 12 后端实现。**
> 通过实现 Minecraft 官方的 `GpuBackend` / `GpuDeviceBackend` 接口，用预编译的 C++ DLL（`dx12_mc.dll`）替代原生 OpenGL 渲染路径，使 Minecraft 使用 D3D12 直接渲染游戏画面，同时保留 Mod 扩展点（AA 设置、ModMenu 集成）。

## 概述

本项目采用 **Mixin 注入** 方式修改 Minecraft 的图形 API 选择逻辑，让游戏改用 D3D12 后端渲染，同时通过 Java 层提供渲染状态同步、配置管理和 Mod 兼容层。

### 核心特点

- **Mixin 注入**：仅需 1 个 Mixin（`PreferredGraphicsApiMixin`）将 D3D12 设为首选 API，Minecraft 原生渲染管线直接驱动我们的后端
- **官方 GpuBackend 接口**：完整实现 `GpuBackend` / `GpuDeviceBackend` / `CommandEncoderBackend` / `RenderPassBackend` / `GpuSurfaceBackend` 等官方接口
- **零自定义渲染循环**：不复写 `RenderSystem`，完全接管 Minecraft 官方的渲染流程
- **Shaderc + Spvc 编译链**：GLSL → SPIR-V（shaderc）→ HLSL SM5.1（spvc）→ DXBC（原生 D3DCompile）
- **资源安全**：所有 D3D12 资源通过 Java 包装类（`Dx12Gpu*`）管理，随 Minecraft 生命周期自动释放
- **配置界面**：Fx12Config 持久化配置，Dx12SettingsScreen 设置界面，ModMenu 集成
- **DLL 自动加载**：从 JAR 提取到 `{user.dir}/dx12mod/dx12_mc.dll`，支持版本隔离
- **MC 26.2**：支持 Mojang 官方映射（不依赖 Yarn 映射）

### 当前阶段：Phase P0-P6 全部完成，shader 渲染全链路已通

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0: 原生层基础** | ✅ 已完成 | dx12_mc.dll：D3D12 初始化 + 资源创建 (texture/buffer/sampler/view) |
| **P1: 设备自检** | ✅ 已完成 | Dx12Native.dx12CreateDevice() + 资源 self-test |
| **P2: 资源层** | ✅ 已完成 | Dx12GpuTexture / Dx12GpuBuffer / Dx12GpuSampler / Dx12GpuTextureView |
| **P3: 命令编码层** | ✅ 已完成 | Dx12CommandEncoderBackend：submit/fence/copy/clear/timestamp |
| **P4: 管线编译层** | ✅ 已完成 | shaderc SPIR-V → spvc 反射/rebind → HLSL → D3DCompile DXBC |
| **P5: 交换链层** | ✅ 已完成 | Dx12GpuSurface：DXGI flip-model swapchain (configure/acquire/blit/present) |
| **P6: Draw 全链路** | ✅ 已完成 | Dx12RenderPassBackend：setPipeline + pushDescriptors + draw/drawIndexed |
| **自测通过** | ✅ | GUI + GUI_TEXTURED 管线编译 + surface blit + buffer copy + texture readback 全部通过 |

## 项目结构

```
dx12-lib-template-26.1.2/
├── fabric/                          # Fabric 模组（Java）
│   ├── src/main/java/com/dx12/
│   │   ├── Dx12Mod.java            # 模组入口，设置图形 API 偏好 + 日志自检信息
│   │   ├── ModMenuIntegration.java # ModMenuApi 实现，配置界面集成
│   │   ├── config/
│   │   │   └── Dx12Config.java     # 配置持久化（aa_mode 等，Properties 格式）
│   │   ├── gui/
│   │   │   └── Dx12SettingsScreen.java # AA 模式设置 GUI（None/FXAA/SMAA/TAA）
│   │   ├── mixin/
│   │   │   └── PreferredGraphicsApiMixin.java  # 将 D3D12 设为首选图形 API
│   │   └── dx12/                    # D3D12 后端核心实现（镜像官方 Vulkan 后端）
│   │       ├── Dx12Native.java        # JNI 桥接层（66 native 方法）
│   │       ├── Dx12Backend.java       # GpuBackend 实现：窗口/设备创建 + 4 轮自测（394 行）
│   │       ├── Dx12Device.java        # GpuDeviceBackend 实现：资源创建 + Shader 编译缓存（533 行）
│   │       ├── Dx12ShaderCompiler.java    # GLSL→SPIR-V→HLSL 编译链（shaderc+spvc，190 行）
│   │       ├── Dx12IntermediaryShaderModule.java  # SPIR-V 反射绑定信息（429 行）
│   │       ├── Dx12CompiledShader.java    # 编译产物（HLSL 源码 + 绑定列表）
│   │       ├── Dx12CompiledRenderPipeline.java  # 编译后的渲染管线
│   │       ├── Dx12BindGroupEntry.java    # 管线绑定条目（UBO/SRV/TexelBuffer）
│   │       ├── Dx12GpuSurface.java      # DXGI swapchain（P5 交换链层）
│   │       ├── Dx12CommandEncoderBackend.java   # 命令编码层（P3 submit/copy/clear，334 行）
│   │       ├── Dx12RenderPassBackend.java     # 渲染通道层（P6 draw 全链路，368 行）
│   │       ├── Dx12TransientMemory.java     # 瞬时内存管理（per-frame 缓冲回收，210 行）
│   │       ├── Dx12GpuTexture.java        # D3D12 纹理资源包装
│   │       ├── Dx12GpuTextureView.java    # D3D12 纹理视图（SRV）
│   │       ├── Dx12GpuBuffer.java         # D3D12 缓冲区资源包装
│   │       ├── Dx12GpuSampler.java        # D3D12 采样器包装
│   │       └── Dx12GpuQueryPool.java      # D3D12 GPU 时间戳查询池
│   ├── src/main/resources/
│   │   ├── fabric.mod.json         # Fabric 模组描述（client + modmenu entrypoints）
│   │   ├── gl4dx12.mixins.json     # Mixin 配置（1 个 mixin）
│   │   └── assets/dx12mod/icon.png # 模组图标（16x16）
│   ├── libs/
│   │   └── modmenu-20.0.1.jar     # ModMenu 本地依赖（MC 26.2）
│   ├── build.gradle                # Gradle 构建配置 (loom 1.15.5)
│   └── gradle.properties           # 版本参数
├── native/                          # D3D12 原生层（C++17）
│   ├── src/
│   │   ├── dx12_device.cpp         # D3D12 设备/资源创建 + 渲染通道 + 时间戳
│   │   ├── dx12_device.h           # DX12DeviceHandle 结构体定义
│   │   ├── dx12_surface.cpp        # DXGI swapchain + blit + present
│   │   └── dx12_native.cpp         # JNI 入口（66 native 方法）
│   ├── CMakeLists.txt              # CMake 构建配置
│   └── build/bin/Release/dx12_mc.dll  # 预编译 DLL（~150KB，从 JAR 提取部署）
└── 步骤.md                         # 详细开发步骤文档
```

## 架构设计

### 渲染管线：完全接管 Minecraft 官方渲染流程

本项目不创建自定义渲染循环，而是实现 Minecraft 26.1+ 引入的 `GpuBackend` 接口族，让游戏引擎自己驱动整个渲染：

```
Minecraft RenderSystem
  │
  ▼
GpuBackend.createDevice(window, shaderSource, ...)   ← Dx12Backend
  │
  ├─ GpuDeviceBackend.createTexture()                ← Dx12Device → Dx12Native.dx12CreateTexture()
  ├─ GpuDeviceBackend.createBuffer()                 ← Dx12Device → Dx12Native.dx12CreateBuffer()
  ├─ GpuDeviceBackend.createSampler()                ← Dx12Device → Dx12Native.dx12CreateSampler()
  ├─ GpuDeviceBackend.createSurface()                ← Dx12Device → Dx12Native.dx12CreateSurface()
  ├─ GpuDeviceBackend.createCommandEncoder()         ← Dx12Device → Dx12CommandEncoderBackend
  │   │
  │   └─ CommandEncoderBackend.beginRenderPass()     ← Dx12CommandEncoderBackend
  │       │
  │       └─ RenderPassBackend                       ← Dx12RenderPassBackend
  │           ├─ setPipeline()      → dx12SetPipeline()
  │           ├─ setVertexBuffer()  → dx12SetVertexBuffer()
  │           ├─ setIndexBuffer()   → dx12SetIndexBuffer()
  │           ├─ pushDescriptors()  → dx12PushDescriptors()
  │           ├─ drawIndexed()      → dx12DrawIndexed()
  │           └─ draw()             → dx12Draw()
  │
  └─ GpuSurfaceBackend                             ← Dx12GpuSurface
      ├─ configure()      → dx12ConfigureSurface()
      ├─ acquireNextTexture() → dx12AcquireSurface()
      ├─ blitFromTexture()  → dx12BlitSurface()
      └─ present()          → dx12PresentSurface()
```

### Shader 编译链（P4）

```
Minecraft ShaderSource (assets/minecraft/shaders/core/*.glsl)
  │
  ▼
Dx12ShaderCompiler.createIntermediary()
  │  shaderc: GLSL → SPIR-V
  │  GlslPreprocessor: 注入 gl_VertexID/gl_InstanceID 宏
  │
  ▼
Dx12IntermediaryShaderModule
  │  spvc 反射: 提取 UBO / Sampler / Output / Input 绑定
  │  spvc rebind: 将 SPIR-V binding decoration 重写为 0..n-1
  │
  ▼
Dx12CompiledShader
  │  spvc HLSL 后端: SPIR-V → HLSL SM5.1
  │
  ▼
Dx12Device.precompilePipeline()
  │  Dx12ShaderCompiler.compilePipeline()
  │  传入 packed descriptor buffer 到原生层
  │
  ▼
dx12_mc.dll: D3DCompile → DXBC + Root Signature + PSO
  │
  ▼
Dx12CompiledRenderPipeline (handle ≠ 0 表示成功)
```

### Mixin 注入点

唯一 Mixin `PreferredGraphicsApiMixin` 在 Minecraft 初始化阶段修改图形 API 偏好：

```java
// PreferredGraphicsApiMixin.java
@Mixin(GdxApplicationParameters.class)
public abstract class PreferredGraphicsApiMixin {
    @Overwrite
    public String getPreferredGraphicsApi() {
        return "D3D12";  // 强制使用 D3D12 而非 OpenGL/Vulkan
    }
}
```

Minecraft 选择后端时优先使用 `D3D12`，进而实例化我们的 `Dx12Backend`。

## 已完成功能

| 模块 | 说明 |
|------|------|
| **P0 原生层** | dx12_mc.dll: D3D12 设备创建、texture/buffer/sampler/view 资源管理、命令列表、fence、swapchain |
| **P1 设备自检** | Dx12Native.dx12CreateDevice() 返回适配器名称 + 特性级别 + 自测结果 |
| **P2 资源层** | Dx12GpuTexture/Dx12GpuBuffer/Dx12GpuSampler/Dx12GpuTextureView — 全部通过 JNI 创建真实 D3D12 资源 |
| **P3 命令层** | Dx12CommandEncoderBackend: submit/fence/copyBuffer/copyTexture/writeToTexture/clear/startEndRenderPass/timestamp |
| **P4 管线层** | GLSL→SPIR-V(HLSL→DXBC 编译，Pipeline cache (IdentityHashMap)，Shader source cache (HashMap) |
| **P5 交换链** | Dx12GpuSurface: DXGI flip-model swapchain, 支持 IMMEDIATE/MERCHANTABILITY/RALAXED present mode |
| **P6 Draw 全链路** | Dx12RenderPassBackend: setPipeline + pushDescriptors(UBO/SRV/TexelBuffer) + draw/drawIndexed/drawIndirect |
| **Shader 编译自检** | 内嵌 core/gui + core/position_tex_color 的 GLSL 源码，createDevice 时资源包未加载也可编译 |
| **4 轮自测** | Java资源(self-test)→命令层→管线→Surface 逐轮验证，任何失败立即终止并回退 OpenGL |
| **D3D12 资源管理** | AutoCloseable 模式，close() 时释放所有 D3D12 句柄 |
| **ModMenu 集成** | Dx12Config + Dx12SettingsScreen + ModMenuApi entrypoint |
| **AA 模式持久化** | Properties 格式存储到 `config/gl4dx12.properties`，4 种模式 (None/FXAA/SMAA/TAA) |

## 关键设计原则

- **Mixin 仅修改 API 选择**：不复写任何渲染逻辑，完全依赖 Minecraft 官方 `RenderSystem` 调用我们的后端
- **镜像官方 Vulkan 后端**：每个 Java 类都对应官方 `com.mojang.blaze3d.vulkan.Vulkan*` 的等价实现
- **资源安全释放**：所有 `AutoCloseable`，close 时销毁所有 D3D12 句柄，防止资源泄漏
- **自测驱动开发**：4 轮自测（资源/命令/管线/Surface）在 `createDevice()` 中逐轮验证，失败即终止
- **Lazy shared encoder**：`createCommandEncoder()` 返回同一实例（镜像官方语义），避免每帧新建 CommandContext 导致的命令丢失
- **Shader cache**：Pipeline cache (IdentityHashMap) + Shader source cache (HashMap)，同一 shader 只编译一次
- **DLL 版本隔离**：每次加载都从 JAR 重新提取，确保 DLL 与 JAR 版本一致

## 调试与验证

### 启动日志关键输出

模组加载时依次打印：

```
[dx12] Loading D3D12 mod...
[dx12] Preferred graphics API: D3D12
[dx12] dx12CreateDevice() result: <适配器名称> (D3D_FEATURE_LEVEL <级别>); SELF-TEST OK (...)
[dx12] 创建 GpuBackend: dx12.Dx12Backend
[dx12] Java resource self-test OK (texture/buffer/sampler/view via JNI)
[dx12] Command layer self-test OK (submit/fence/copy/readback via JNI)
[dx12] Pipeline self-test OK (GLSL->SPIR-V->HLSL->DXBC->PSO for core/gui + core/position_tex_color)
[dx12] Surface self-test OK (DXGI swapchain configure/acquire/blit/present via JNI)
[dx12] D3D12 backend initialized successfully.
[dx12] Device name: <适配器名称>
```

### 自测失败时的行为

任何一轮自测失败都会：
1. 关闭已创建的 D3D12 设备
2. 抛出 `BackendCreationException`
3. Minecraft 自动回退到 OpenGL 渲染
4. 日志中打印完整错误堆栈

### 4 轮自测详解

| 轮次 | 测试内容 | 失败原因示例 |
|------|----------|-------------|
| **1. Java 资源** | texture/buffer/sampler/view 创建 + map/unmap 读写 | DLL 加载失败、D3D12 初始化失败 |
| **2. 命令层** | submit + fence wait + buffer copy readback + texture write/readback | 命令列表提交失败、copy 数据不匹配 |
| **3. 管线** | core/gui + core/position_tex_color 全链路编译（GLSL→SPIR-V→HLSL→DXBC→PSO） | shaderc/spvc/D3DCompile 编译失败 |
| **4. Surface** | swapchain 创建 → configure → acquire → blit → submit → present | HWND 无效、DXGI 创建 swapchain 失败 |

## 已知问题与解决方案

| 问题 | 原因 | 解决方案 | 状态 |
|------|------|----------|------|
| **GPU TDR 超时（~2s 后崩溃）** | GL + D3D12 同窗口共存导致 WDDM 驱动级冲突 | **1 个 Mixin**：设置 `D3D12` 为首选 API，Minecraft 不再初始化 GL 后端 | ✅ 已修复 |
| **DLL 版本不匹配** | 旧版 `dx12_mc.dll` 与新版 Java 层 JNI 签名不一致 | 每次启动从 JAR 重新提取到 `{user.dir}/dx12mod/` | ✅ 已修复 |
| **RootSignature 未设置（UMD AV）** | `setPipeline` 只调 `SetPipelineState`，未调 `SetGraphicsRootSignature` | 补 `SetGraphicsRootSignature(pipeline->rootSignature.Get())` | ✅ 已修复 (cd1e029) |
| **命令列表拓扑未设置** | D3D12 初始 topology=UNDEFINED，draw 被 GPU 丢弃 | `setPipeline` 补 `IASetPrimitiveTopology(toPrimitiveTopology(topology))` | ✅ 已修复 |
| **顶点语义硬编码** | `InputElement.semanticName` 硬编码为 `TEXCOORD`，不匹配 HLSL 输入 | 从 pipeline binding 反射提取 SPVC semantic name | ✅ 已修复 (88a9253) |
| **GUI 纯红显示** | 顶点属性与 HLSL 输入语义不匹配 → 顶点数据被丢弃 | 修复 `dx12CreateGraphicsPipeline` 语义映射（`POSITION→SV_Position` 等） | ✅ 已修复 (cb653a7) |
| **HLSL static/const 误判** | 解析变量声明时将 `static`/`const` 误认为类型前缀 | 重构为按类型关键字列表扫描；支持 `const` 修饰符检测 | ✅ 已修复 (86b5f7c/784d331) |
| **Descriptor heap 耗尽** | SRV/Sampler heap 未释放旧 handle → 堆空间耗尽 | `createGraphicsPipeline` 成功后释放旧 handle | ✅ 已修复 (85ea7cf) |
| **命令编码器共享导致渲染异常** | 每帧新建 CommandContext 导致命令丢失 | 懒加载共享单例 encoder（镜像官方语义） | ✅ 已修复 |
| **GPU idle 等待** | 连续 submit 导致 GPU 等待时间过长 | 新增 `d3d12mc_gpu_idle_wait_ms`（默认 2ms） | ✅ 已修复 (85ea7cf) |

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 游戏 | Minecraft | 26.2 |
| 加载器 | Fabric Loader | 0.19.3 |
| API | Fabric API | 0.156.0+26.2 |
| Mixin | SpongePowered Mixin | 0.8.7 (via Fabric 0.17.3) |
| 模组浏览器 | ModMenu | 20.0.1 |
| 语言 | Java | 25 |
| 原生层 | C++ D3D12 | dx12_mc.dll（预编译） |
| Shader 编译 | shaderc (GLSL→SPIR-V) | LWJGL shaderc |
| Shader 反射 | spvc (SPIR-V→HLSL) | LWJGL spvc |
| 语言 | Rust | — (已移除) |

## 路线图

### P0-P6: D3D12 后端核心层 ✅ 全部完成

| 阶段 | 状态 | 说明 |
|------|------|------|
| **P0: 原生层基础** | ✅ | dx12_mc.dll: D3D12 初始化 + 资源创建 |
| **P1: 设备自检** | ✅ | Dx12Native.dx12CreateDevice() + 资源 self-test |
| **P2: 资源层** | ✅ | Dx12GpuTexture/Dx12GpuBuffer/Dx12GpuSampler/Dx12GpuTextureView |
| **P3: 命令编码层** | ✅ | Dx12CommandEncoderBackend: submit/fence/copy/clear/timestamp |
| **P4: 管线编译层** | ✅ | shaderc SPIR-V → spvc 反射/rebind → HLSL → D3DCompile DXBC |
| **P5: 交换链层** | ✅ | Dx12GpuSurface: DXGI flip-model swapchain |
| **P6: Draw 全链路** | ✅ | Dx12RenderPassBackend: setPipeline + pushDescriptors + draw |
| **自测通过** | ✅ | GUI + GUI_TEXTURED 管线 + surface blit + buffer copy + texture readback |

### 🔜 后续优化

| 任务 | 优先级 | 说明 |
|------|--------|------|
| **多帧并行命令队列** | P0 | 当前共享单例 encoder + fence 等待，可升级到三缓冲 |
| **Descriptor heap 优化** | P1 | 当前每帧 push descriptors，可改用 heap 池化（已修复 heap 泄漏） |
| **TransientMemory 块分配器** | P1 | 当前 per-frame committed buffer，可改为 block allocator |
| **完整 Shader 支持** | P2 | 当前自检 core/gui + core/position_tex_color，需支持 terrain/entity/particle 等全量 shader |
| **P6 GUI 渲染修复** | ✅ 已完成 | 修复 RootSignature/Topology/顶点语义/static/const HLSL 等 12 个根因 |
| **性能基准测试** | P3 | 对比 GL/Vulkan/D3D12 的 FPS、内存占用、GPU 利用率 |

### 🎯 最终目标

| 阶段 | 目标 | 状态 |
|------|------|------|
| **过渡方案** | 通过 GpuBackend 接口完全接管 Minecraft 渲染（当前阶段） | ✅ 已完成 |
| **性能优化** | 完整 shader 支持 + descriptor 优化 + 帧并行提交 | 🔜 进行中 |
| **正式发行** | 通过官方渠道发布为 Minecraft 26.2+ 的 D3D12 后端模组 | 🔜 待实现 |

---

## 贡献指南

### 参与方式

> 暂时不接受任何贡献

### 当前可认领的任务

| 任务 | 优先级 | 说明 |
|------|--------|------|
| **完整 Shader 支持** | 🔴 P0 | 编译所有 Minecraft 官方 shader（terrain, entity, particle 等） |
| **天空盒渲染** | 🟠 P1 | 简单着色器即可 |
| **实体渲染** | 🟠 P1 | 模型加载 + 骨骼动画 |
| **粒子系统** | 🟡 P2 | 点精灵 (point sprites) |

---

## 许可证

MIT License