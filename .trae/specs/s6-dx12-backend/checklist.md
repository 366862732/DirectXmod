# S6 Checklist — DX12 后端

## 前置（已完成）
- [x] 26.2 构建通过（fabric-api 0.156.0+26.2、loader 0.19.3、ModMenu 20.0.1、Mojang 映射）
- [x] 官方 GPU 后端架构调研（GpuBackend 接口族、PreferredGraphicsApi、vulkan/ 包、shader 机制）
- [x] spec.md 编写

## P1 挂点验证
- [ ] Rust 侧：新建 `dx12-mc` crate（windows/d3d12 绑定），`dx12_create_device` 创建 ID3D12Device + 打印名称/特性
- [ ] Java 侧：`Dx12Backend implements GpuBackend` 骨架
- [ ] Mixin `PreferredGraphicsApi`：注入 DX12 枚举值 + `getBackendsToTry` 挂载
- [ ] `createDevice` JNI 调通；失败安全 fallback（回退 GL，不崩溃）
- [ ] 游戏启动验证：日志显示 DX12 device 创建成功；设置里出现 Graphics API: DX12

## P2 资源层
- [ ] `GpuDeviceBackend`：createTexture/createTextureView/createBuffer/createSampler → D3D12 resource
- [ ] D3D12 descriptor heap 管理（CBV/SRV/UAV/sampler）
- [ ] 上传堆（upload heap）+ 暂存（staging）机制
- [ ] 资源生命周期与 release queue

## P3 命令层
- [ ] `CommandEncoderBackend`：command allocator/list、submit、fence
- [ ] `RenderPassBackend`：clear / copy / draw 命令录制
- [ ] 资源状态 barrier 自动管理
- [ ] TransientMemory（帧内瞬态缓冲）

## P4 管线与 shader
- [ ] `RenderPipeline` → D3D12 PSO（顶点布局/混合/深度/图元拓扑映射）
- [ ] GLSL→SPIR-V→HLSL→DXIL 编译链路（shaderc/glslang + SPIRV-Cross + DXC）
- [ ] ShaderDefines 支持 + DXIL 缓存（对应 ShaderCompilationKey）
- [ ] `precompilePipeline` 接入

## P5 Surface 呈现
- [ ] DXGI factory/adapter/swapchain 创建（对应 GpuSurfaceBackend）
- [ ] acquireNextTexture / present / configure（PresentMode 映射）
- [ ] 多帧飞行 + fence 等待

## P6 首帧画面
- [ ] 清屏 render pass 跑通，窗口呈现纯色
- [ ] 官方渲染流程至少一帧走完（天空或全屏 quad）

## P7+ 完整渲染
- [ ] 区块（chunk mesh）渲染
- [ ] 实体 / 粒子 / 天空 / 云
- [ ] lightmap / 后处理（FXAA）
- [ ] 对照官方 Vulkan 逐 RenderPass 验证，功能对齐

## 学习沉淀
- [ ] 官方 Vulkan 后端反编译源码存入 `docs/official-vulkan/`
