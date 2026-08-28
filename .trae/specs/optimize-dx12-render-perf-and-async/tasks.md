# Tasks

按「先止血（P30 日志/同步）→ 修画面（P31 GUI/P32 区块）→ 架构（P33 多线程）」顺序执行。P30 与 P31/P32 诊断可并行，P33 依赖 P30 完成后稳定的基线。

- [x] Task 1: 抑制 native 层诊断输出（P30）
  - [x] SubTask 1.1: 移除 `jni_bridge_p6.cpp` `dx12PushDescriptors` 中 L98-108 的无条件 `fprintf` 块（改为 DEBUG 级别门控或删除）
  - [x] SubTask 1.2: `dx12_device.cpp` `waitForQueueFenceValue` L1393 与 `waitForFenceValue` L1375 的 WARN 级日志降为 DEBUG；`submitCommandList` L1316-1320 每 30 帧日志降为 INFO
  - [x] SubTask 1.3: `pushDescriptors` L2671-2689（首 push count + BIND 循环）与 L2740-2758（CBV/UBO_BIND）降为 DEBUG；`setVertexBuffer` L2620 已为 DEBUG（确认）
  - [x] SubTask 1.4: `dx12_surface.cpp` `blitSurface` L352-490 的每帧 dbgLog 降为 DEBUG（保留错误路径）
  - [x] SubTask 1.5: `dbgLog`/`dbgLogInfo`/`dbgLogDebug` 的 `%TEMP%\dx12-native.log` 镜像改为环境变量门控 + 复用打开句柄（去除逐行 fopen/fclose）
  - [x] SubTask 1.6: 验证：默认配置启动游戏，日志量降至每次会话 < 1 千行（除一次性编译日志），帧率明显恢复

- [x] Task 2: 消除渲染热路径 GPU 同步气泡（P30）
  - [x] SubTask 2.1: 移除 `setVertexBuffer` L2624-2627 的 vbDbg 读回与 `#ifdef DIAG_READBACK_COLOR_TEX` 分支
  - [x] SubTask 2.2: 移除 `pushDescriptors` L2706-2731 的 ubDbg/projDbg/sprite_ubo 读回
  - [x] SubTask 2.3: 移除/门控 `pushDescriptors` L2780-2814 的 srv_tex_readback、font_tex_readback（每 30 帧）与 L2904-2911 的 color_after_draw；`dbgReadbackTexturePixels`/`dbgReadbackBufferBytes` 内的 `deviceWaitIdle` 改为仅诊断模式调用
  - [x] SubTask 2.4: `dbgReadbackTexturePixels` 中 `dbgDumpPixelsToFile` BMP 写入移出常规路径（仅显式诊断）
  - [x] SubTask 2.5: 验证：帧渲染期间日志无 `deviceWaitIdle: enter`；`waitQFence` 快速返回；拉高视距后无明显卡顿

- [ ] Task 3: 修复物品栏显示与人物方向（P31）
  - [x] SubTask 3.1: 诊断物品图标不显示：确认 GUI 图集（items atlas）是否上传/采样成功、GUI textured 管线 draw 是否被录制，定位黑图标根因
  - [x] SubTask 3.2: 修复物品图标渲染（纹理上传/SRV 绑定/采样状态任一环节的修复）
  - [x] SubTask 3.3: 诊断人物倒过来：确认 GUI 相机（ortho）下 entity 管线渲染的坐标翻转需求（viewport Y 翻转 vs 投影矩阵修正 vs 着色器注入）
  - [x] SubTask 3.4: 实施修复并保证主世界 3D 渲染方向不变（Y-flip 只影响 GUI 相机渲染）
  - [ ] SubTask 3.5: 验证：打开物品栏，物品图标正常显示、玩家模型头朝上不颠倒；主世界渲染正常
    - 改动：`Dx12ShaderCompiler.compile()` 增加 `boolean flipY`；`injectVertexYFlip` 改为循环注入所有 `gl_Position` 赋值；`Dx12Device` 新增 `pipelineCacheFlipY` 缓存 flipY 变体管线；`Dx12RenderPassBackend.setPipeline(pipeline, flipY)` 按 pass 判别结果选取变体；`Dx12CommandEncoderBackend.createRenderPass` 以 `color[0].usage()==13 && depthTexture!=0` 判别 GUI 离屏 pass 并传递 flipY

- [ ] Task 4: 修复拉高视距远处全黑（P32）
  - [ ] SubTask 4.1: 诊断：拉高视距后记录区块网格构建/上传/绘制情况（SectionRenderDispatcher draw 数、chunk mesh 上传）、雾参数、深度远平面与 reverse-Z 设置
  - [ ] SubTask 4.2: 修复远端区块渲染（网格构建并行化依赖 P33 或雾/深度问题独立修复）
  - [ ] SubTask 4.3: 验证：视距 10+ 时远处区块正常显示非全黑，游戏不再自动降视距

- [ ] Task 5: CPU/GPU 多线程异步渲染架构（P33，依据《多线程方案.md》第 1-8 章）
  - [ ] SubTask 5.1: 核心数据结构：`fabric/.../async/AsyncRenderCommand`、`FrameData`（命令类型、flyweight 复用、帧缓冲）
  - [ ] SubTask 5.2: C++ 线程安全描述符管理：`native/src/dx12_descriptor_allocator.h`（无锁分配器 + 分段 ring）
  - [ ] SubTask 5.3: Bundle 录制系统：`native/src/dx12_bundle_recorder.h/cpp`（每 Worker 独立 recorder）+ Java 侧 Bundle 管理器
  - [ ] SubTask 5.4: 异步命令队列：lock-free 命令队列（Java + C++ 双侧）
  - [ ] SubTask 5.5: Fence 异步通知：`native/src/dx12_fence_manager.h/cpp`（fence 完成回调线程）+ Java 侧异步通知
  - [ ] SubTask 5.6: 主 Command List 执行器：`native/src/dx12_main_executor.h/cpp`（等待上一帧 fence → 重置 allocator → ExecuteBundle → 提交 → Signal）
  - [ ] SubTask 5.7: FrameManager 与渲染循环：Java `FrameManager`（MAX_IN_FLIGHT=4-5）+ 接入 `Dx12CommandEncoderBackend`/`Dx12Backend` 现有渲染循环
  - [ ] SubTask 5.8: 资源生命周期：延迟删除改为按 fence 完成回调执行，覆盖多帧飞行
  - [ ] SubTask 5.9: 验证：完整帧（区块+实体+GUI）CPU 帧时间较单线程基线显著下降，无黑屏/DEVICE_REMOVED/描述符越界；窗口 resize 正常

# Task Dependencies

- [Task 3] 与 [Task 4] 的 GUI/区块诊断可在 [Task 1][Task 2] 完成后并行开展
- [Task 5] 依赖 [Task 1][Task 2]（稳定的性能基线）与 [Task 3][Task 4] 的修复（多线程接入不应引入新渲染错误）
