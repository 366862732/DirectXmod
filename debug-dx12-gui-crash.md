# Debug Session: dx12-gui-crash
- **Status**: [OPEN]
- **Issue**: DX12 后端已成功加载并通过全部自检，但首次真实 GUI draw 在 `dx12PushDescriptors` 路径触发原生 `EXCEPTION_ACCESS_VIOLATION`，JVM 崩溃。
- **Debug Server**: N/A（当前会话先基于现有游戏日志与 hs_err 证据收敛；如需新增插桩再启动）
- **Log File**: `D:\dx12-lib-template-26.1.2\游戏日志 - 26.2-Fabric_0.19.3.log` / `D:\dx12-lib-template-26.1.2\hs_err_pid34120.log`

## Reproduction Steps
1. 使用当前 26.2 版 `gl4dx12` 启动游戏。
2. 确认日志出现 `GL4DX12 initializing`、`[dx12] ... self-test OK`、`Using graphics backend DX12`。
3. 在资源重载完成前后进入首次真实 GUI 绘制。
4. 进程在原生代码中崩溃，生成 `hs_err_pid34120.log`。

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | 实际运行的 DLL 不是刚修复过 `SetGraphicsRootSignature` 的版本 | High | Low | ✅ **Confirmed**（运行 jar 内嵌 DLL=19D6760D 旧版；本地修复版=81AE92BA，从未进 jar） |
| B | `dx12SetPipeline` 虽被调用，但当前 draw 复用了未重新绑定 root signature 的 command list 状态 | Medium | Medium | ⏳ Inconclusive（需在修复版 DLL 上复测） |
| C | 崩溃并非缺 root signature，而是 descriptor table / drawHeap 绑定状态在 reset 后失效 | Medium | Medium | ⏳ Inconclusive（需在修复版 DLL 上复测） |
| D | 本次崩溃点与前两次完全相同，说明当前代码路径根本没命中新增修复日志 | High | Low | ✅ **Confirmed**（pid34120/pid14772 PC 同为 0x7ffcd70e8fa3、读 NULL+0x2b88、dx12_mc.dll+0x9eae；游戏日志无 `setPipeline rootSig=` 行） |
| E | `SetGraphicsRootDescriptorTable` 前的 pipeline / root parameter 索引与实际 PSO/rootSig 不一致 | Medium | High | ⏳ Inconclusive（若修复版部署后仍崩则升级为头号假设） |

## Log Evidence
- `hs_err_pid34120.log` / `hs_err_pid14772.log`：`EXCEPTION_ACCESS_VIOLATION` @ PC `0x00007ffcd70e8fa3`，读 `0x2b88`；Native frames `dx12_mc.dll+0x9eae` / `+0x12f53`；Java 栈 `dx12PushDescriptors → Dx12RenderPassBackend.pushDescriptors+381 → drawIndexed → GuiRenderer.executeDraw`；加载 `D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll`。
- `游戏日志 - 26.2-Fabric_0.19.3.log`（13:16 启动）：49 mods 含 gl4dx12；`Using graphics backend DX12`；自检全 OK；崩溃前最后两行 `pushDesc[0]`(CBV 64B) / `pushDesc[1]`(CBV 256B)；**无 `[dx12] setPipeline rootSig=` 行**（修复版才打印）。
- 哈希证据：
  - 运行时 `dx12mod\dx12_mc.dll` = `19D6760D...`
  - 运行 jar（版本隔离 mods）内嵌 DLL = `19D6760D...`
  - 本地修复版 `native\build\bin\Release\dx12_mc.dll` = `81AE92BA...`
  - `fabric\build\libs\gl4dx12-0.1.0.jar` 内嵌 DLL = `19D6760D...`（旧 jar 未重建）

## Verification Conclusion
- **阶段 1（部署陷阱）已解决**：重建 jar 后（13:22），修复版 DLL（81AE92BA）进 jar，用户部署复测。
- **阶段 2 复测（13:27）**：✅ **UMD 原生 AV 消失**（SetGraphicsRootSignature 修复生效）；❌ 新异常：
  ```
  java.lang.IllegalStateException: dx12WaitForFence: waitForFenceValue: timed out after 0ns
  StagedVertexBuffer$GpuBufferPool$PendingRecycle.tryRecycle → awaitCompletion (Dx12CommandEncoderBackend.java:262)
  ```
- **根因 8**：`jni_bridge_p3.cpp dx12WaitForFence` 把 **fence 超时当错误抛异常**。官方 `GpuFence.awaitCompletion(timeout)` 是轮询语义——`timeout=0` 未完成应返回 `false`，不抛异常；Minecraft 回收暂存 buffer 靠此非阻塞检查。超时（`err` 含 `timed out`）现改为仅返回 `false`，只有参数错误才抛。
- **修复（13:31）**：jni_bridge_p3.cpp 超时不抛异常 → DLL 重建 + jar 重打（1200418 B，13:31:30）→ 内嵌 DLL=633414F1 与本地一致 ✅
- **待复测判据**：
  - 无 `dx12WaitForFence: timed out` 异常 → 阶段 2 通过，继续推进（下一个疑点：真实绘制/描述符/状态）；
  - 若仍出现 fence 相关异常 → 查 `createFence` 捕获值语义（当前 `fenceValue+1` 可能跨帧/跨 encoder 语义偏差，对照官方 `awaitSubmitCompletion`）。

## 阶段 3（13:39 插桩版复测）：黑屏无响应 → 精确卡点确认

- **现象**：窗口黑色无响应，无崩溃。日志 L386-432 冻结点：所有真实 submit 均 1ms 内完成（`submit: done v=1`），随后大量 `createCommandEncoder`（一次性 encoder）与连续 `waitFence: value=1 completed=0` + `TIMEOUT`（0.1ms 高频 = Java `awaitCompletion(0)` 轮询），最终一行 `waitFence: value=1 completed=0` 无 TIMEOUT 同伴 = **阻塞等待**（渲染线程冻结于此）。
- **证据链**：
  - `Dx12CommandEncoderBackend.createFence()` 目标 = `dx12GetFenceValue(ctx)+1` = per-ctx fence 值。
  - 一次性 encoder 从不 submit → per-ctx fence 永远停在 0 → 目标 1 永不 Signal → `SetEventOnCompletion(1)` 永不触发。
  - vanilla 调用方：`RenderSystem.queueFencedTask`（L241）、`StagedVertexBuffer.endFrame`→`PendingRecycle.tryRecycle`（`awaitCompletion(0)` 轮询）、`MappableRingBuffer.rotate/currentBuffer`（`awaitCompletion(Long.MAX_VALUE)` 阻塞）——全部用 `createCommandEncoder().createFence()` 的 fence token。
- **根因 9**：`createFence()` 语义偏差。官方 `VulkanDevice.createCommandEncoder()` 返回**同一个共享 encoder**（VulkanDevice.java L172-173），`createFence()` 捕获其 `currentSubmitIndex`（VulkanCommandEncoder.java L588），fence 在该共享 encoder 的**下一次提交**完成后完成——所以一次性 encoder 上的 token 也随主渲染提交完成。D3D12 实现把 fence 绑到 **per-ctx** fence，一次性 encoder 永不提交 → 永久等待。
- **修复（Fix B，设备级队列 fence）**：
  - `dx12_device.h/.cpp`：`DeviceContext` 增加 `queueFence`/`queueFenceEvent`/`queueFenceValue`；`ensureDevice` 创建全局 fence；`submitCommandList` 每次 `ExecuteCommandLists` 后 `Signal(queueFence, ++queueFenceValue)`；新增 `waitForQueueFenceValue`（每调用独立 event + DWORD 上限钳制防 Long.MAX_VALUE 溢出）/`currentQueueFenceValue`。
  - `jni_bridge_p3.cpp`：`dx12WaitForFence` → `waitForQueueFenceValue`；`dx12GetFenceValue` → `currentQueueFenceValue`。
  - Java：`awaitCompletion` 超时换算加钳制（`timeoutMs > MAX/1e6` 时用 `Long.MAX_VALUE`）。
- **验证**：DLL 重建 + jar 重打，三处哈希一致 = `1A6346AF3437A4A2F740B4EF14F58BFB` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌）。
- **待复测判据**（下次日志）：
  - 冻结区应出现 `waitQFence:`（全局 fence 等待）而非 `waitFence:`；`waitQFence: value=N+1` 随后被下一次 submit 的 `OK value=N+1` 满足 → 渲染线程通过资源重载 → 进入首帧；
  - 若 `waitQFence: TIMEOUT` 仍高频出现 → 说明有等待在"未来 submit"上的阻塞路径，继续对照官方调用时序。

## 阶段 4（15:13 复测）：黑屏消失，但"疯狂闪烁"→ 共享 encoder 修复 + 红/灰画面

- **现象（修复前一次复测）**：黑屏已消失、渲染线程跑通，但窗口"各种颜色疯狂闪烁"（backbuffer 每帧不同随机颜色）。
- **根因 10（闪烁）**：官方 `VulkanDevice.createCommandEncoder()` 返回**共享 encoder**（VulkanDevice.java L172-173），blit 与帧末 `createCommandEncoder().submit()` 用**同一** CommandContext；我们的 `Dx12Device.createCommandEncoder()` 每次 `new Dx12CommandEncoderBackend(this)` 新建独立 CommandContext → 绘制指令记在 ctx A、帧末 submit 提交新建的空 ctx B → GPU 每帧执行空命令列表 → backbuffer 呈现未初始化垃圾（随机颜色）。
- **Fix C（共享单例 encoder）**：`Dx12Device` 增加 `sharedCommandEncoder` 懒加载单例；`createCommandEncoder()` 返回同一实例；`close()` 先销毁共享 encoder 再清管线缓存。`createBuffer(data)`/自检路径用私有 `new` 实例不受影响（`CommandEncoderBackend` 接口无 `close()`，vanilla 不关 encoder）。
- **阶段 4 复测（15:13，Fix C 验证）**：
  - ✅ **闪烁消失**：整个运行期仅一次共享 ctx（`000001DF633565D0`，L127-128 创建），212 帧 submit（v=1..212）全部复用；其余 createCommandEncoder 均为 createBuffer(data) 私有实例。
  - ✅ 帧循环稳定：UBO 上传（copyBuffer 56×2 + waitQFence）→ setPipeline/pushDesc 绘制 → submit → beginCommandList，~17-50ms/帧，无冻结无崩溃。
  - ✅ `Dx12Device.close()` 正确销毁共享 encoder（日志尾部 `destroyCommandEncoder: enter fenceValue=212`）。
  - ❌ **画面内容异常**：用户看到"先红后灰"（约 7 秒后 Stopping，v=212）。blit/present/acquire 此前仅在**失败**时打日志 → 无法确认 blit 是否执行。
- **下一步（进行中）**：给 jni_bridge_p5.cpp 的 `dx12AcquireSurface`/`dx12BlitSurface`/`dx12PresentSurface` 成功路径加 `dbgLog` 插桩（新 DLL MD5 = `11F65AEFB9D0052FADA6C23DB7AE515E`），复测确认完整链路：acquire → blit（源纹理 handle）→ submit → present；据此判断红/灰是 blit 未执行（backbuffer 残留）还是源纹理内容异常（GUI RenderTarget clear/绘制）。

## 阶段 5（16:05 复测）：黑屏-纯色 → draw 内容不可见根因（缺 IASetPrimitiveTopology）

- **现象**：游戏 1 分钟无变化，黑屏。readback 插桩证明 backbuffer 全屏**纯色**：
  - 启动加载界面（854x480）：3x3 全 `RGBA(247,48,55,255)`（红）
  - 之后全程：3x3 全 `RGBA(28,24,25,0)`（深灰黑，alpha=0），L2035-2043/L2887+/L3740+ 无任何变化
- **证据链**（L300-315 单帧序列）：
  - `beginRenderPass: color[0] tex=00000267F5B0A720` == `blitSurface src=00000267F5B0A720`（854x480，fmt=6，usage=0xf）✅ 渲染目标正确
  - 每帧 `drawIndexed: indexCount=30` + `indexCount=12`（GUI 加载固定内容）→ `endRenderPass` → blit → submit → present ✅ 指令顺序正确
  - 每帧仅有 UBO 上传（56×2+528）日志；顶点走 `allocateGpuMapped`（UPLOAD heap 直接写，不经 dx12CopyBuffer，无日志）→ 顶点数据通路不能从日志排除
- **根因 11（纯色黑屏）**：**D3D12 命令列表从未调用 `IASetPrimitiveTopology`**。D3D12 命令列表初始 topology = `D3D_PRIMITIVE_TOPOLOGY_UNDEFINED`，任何 draw 前必须显式设置（GL/Vulkan 的 topology 在管线创建时固定，MC RenderPassBackend 不显式调用 → D3D12 移植漏了这条状态）。UNDEFINED 下 GPU 丢弃全部图元 → 渲染目标只有 clear 色 → backbuffer 纯色。
- **Fix E（IASetPrimitiveTopology）**：
  - `dx12_device.h`：`Dx12Pipeline` 增加 `int topology = 4`（MC PrimitiveTopology ordinal）
  - `dx12_device.cpp`：`createGraphicsPipeline` 保存 `desc.topology`；新增 `toPrimitiveTopology(int)`（ordinal→命令列表级 D3D_PRIMITIVE_TOPOLOGY，QUADS 回退 TRIANGLELIST）；`setPipeline` 在 SetPipelineState 后调用 `IASetPrimitiveTopology`（PSO 创建时已用 toTopologyType 限定 PrimitiveTopologyType，两者必须一致）
  - 新增诊断：`setVertexBuffer`/`setIndexBuffer` 打 `dbgLog`（handle/size/offset/stride/heap 类型）；`setPipeline` 日志补 `topoOrdinal/topo`
- **验证**：DLL 重建 + jar 重打，三处哈希一致 = `88F96351E3F1A16DD7C495F7284DDFC3` ✅
- **待复测判据（下次日志）**：
  - `setPipeline ... topoOrdinal=4 topo=4` 行出现（TRIANGLELIST）且 `setVertexBuffer/setIndexBuffer` 每帧 draw 前各一次
  - readback 出现非纯色像素（Mojang logo / 进度条白色）或画面可见 → topology 修复生效
  - 若仍纯色 → 下一个嫌疑：顶点坐标范围与 viewport 不匹配（MC GUI 顶点带 UBO 投影矩阵，检查 UBO 内容/绑定）；或 pushDescriptors 的 SRV 表偏移与 root signature 布局错位

## 阶段 6（17:0x 复测）：kTestShader 决定性实验 + HLSL dump → 真实根因 = debug_points 管线编译失败

- **kTestShader 决定性实验（kTestShader=true）**：固定 vs/ps 输出纯红 `(239,50,61)`，渲染目标 3x3 全红、backbuffer 可见 → **证明 PSO→光栅化→blit→backbuffer 链路 100% 正常，问题 100% 在真实 shader**。副作用：固定 shader 与真实输入布局不匹配 → 主菜单 PSO 创建失败（E_INVALIDARG）→ 黑屏。实验已回退 kTestShader=false。
- **HLSL dump（kTestShader=false）**：真实 vs/ps 结构正常（`mul(Position, mul(ModelViewMat, ProjMat))`、`fragColor = color * ColorModulator`、`discard if w==0`）；`cbuffer Projection register(b0)` / `DynamicTransforms register(b1)`；顶点输入 `TEXCOORD0/1`。
- **读回可靠性**：`dbgReadbackTexturePixels`（dx12_device.cpp L1983）deviceWaitIdle→一次性 copy→Signal→wait→Map，值随场景变化（64x64 黑 / 854x480 红）→ **读回可信，纯红 = 渲染目标真实内容**。
- **决定性根因（根因 12）**：日志 L2509 `[Render thread/ERROR]: Couldn't compile pipeline minecraft:pipeline/debug_points: Couldn't compile HLSL (SPVC_ERROR_UNSUPPORTED_SPIRV)` → **ShaderManager.apply() 抛异常 → "Caught error loading resourcepacks, removing all selected resourcepacks" → 资源包全部移除 → UI 黑屏**。
  - 直接原因：vanilla `debug_point.vsh` 写 `gl_PointSize = LineWidth;`。**SPIRV-Cross HLSL 后端默认不支持 gl_PointSize/gl_PointCoord（SPVC_ERROR_UNSUPPORTED_SPIRV）**，需启用 `HLSL_POINT_SIZE_COMPAT` / `HLSL_POINT_COORD_COMPAT` 选项（映射到 SV_PointSize/PSIZE 语义）。官方 Vulkan 后端直接用 SPIR-V 不经 HLSL 转换，故官方无此问题——这是 D3D12 移植引入的失败点。
  - 佐证：仅 debug_point.vsh 用 gl_PointSize（全 32 个 core vsh 扫描）；rbBuf[vb]/rbBuf[ubo] 全 0 是 UPLOAD 目标 GPU copy 完成前的 CPU 读（读回时机问题，非真实渲染故障）。
- **修复（Fix F）**：`Dx12IntermediaryShaderModule.toHlsl()`：
  - 设置 `SPVC_COMPILER_OPTION_HLSL_POINT_SIZE_COMPAT=1` + `SPVC_COMPILER_OPTION_HLSL_POINT_COORD_COMPAT=1`
  - `spvc_compiler_compile` 失败时读取 `spvc_context_get_last_error_string` 附加到异常（后续若仍有管线失败可直接定位）
  - 仅 Java 侧改动，DLL 不变。
- **待复测判据（下次日志）**：
  - 无 `Couldn't compile pipeline ... debug_points` 错误；无 "Caught error loading resourcepacks"
  - 主菜单 GUI 可见（readback 出现非纯色像素 / 用户肉眼可见标题与按钮）
  - 若仍有 `SPVC_ERROR_UNSUPPORTED_SPIRV` → 新错误消息带 spvc 内部详情，继续修复对应 builtin/指令

## 阶段 7（18:xx 复测）：UI 首现（红窗）+ 新崩溃 = submit Close E_INVALIDARG

- **Fix F 完全生效**：日志无 debug_points 错误、无资源包移除；用户首次看到"红色游戏窗口"（主菜单开始真实渲染，大量 16/4/1024/256/64B copyBuffer = 字体 glyph 上传）。
- **新崩溃（根因 13）**：`java.lang.IllegalStateException: dx12Submit: endCommandList: Close HRESULT 0x80070057`（E_INVALIDARG）。崩溃帧（t=9999300）特征：20+ 次小尺寸 copyBuffer（文字上传）→ beginRenderPass 854x480 → setVertexBuffer(16)/draw 30 → setVertexBuffer(24)/draw 12 → endRenderPass → blit → submit 时 `endCommandList Close` 失败（`submit: JNI done value=0`，未到 ExecuteCommandLists）。
- **诊断手段（Fix G 插桩，DLL 哈希 `E7DA405B...`）**：
  - `endCommandList` Close 失败时附加 `deviceStatusText()`（InfoQueue 最近 24 条验证消息）——Close E_INVALIDARG 是 debug layer 验证错误，InfoQueue 会给确切非法调用（UPLOAD 非法 transition / 未绑定或越界描述符 / root 参数未设置 / PSO-root signature 不匹配等）
  - `beginCommandList` 每帧开头转储并清空 InfoQueue 消息（不崩溃的静默验证错误也能看到）
- **待复测判据**：下次日志中 `endCommandList: Close ... — GetDeviceRemovedReason=0x00000000 | msg[N](<验证消息>)` 给出确切根因，据此修复后主菜单应持续渲染；若 InfoQueue 无 ERROR → 转查 root table/描述符越界。

## 阶段 8（08-08 复测）：根因 14 确认 = 共享命令列表引用已删资源 → 修复 = 原生延迟销毁（queueForDestroy）

- **日志证据（Fix G 插桩首次完整生效）**：崩溃块 24 条 InfoQueue ERROR，全部 `ID3D12CommandList::Close: An ID3D12Resource object ... was deleted prior to closing the command list`，指向同一命令列表 `0x000001BA50327BD0`（= 共享渲染 encoder ctx 4F942520）。24 个资源 = 12 个字形上传的 UPLOAD staging + DEFAULT gpu buffer 对。
- **时间线**：共享 ctx 4F942520 自 `beginCommandList: fenceValue=28`（t=10353340）后 **2.85 秒不 submit**；期间 vanilla `TrueTypeGlyphProvider.writeToTexture` 经共享命令列表录制 12 次字形上传（copyBufferRegion 引用 staging 对）；崩溃帧 L8353 共享 ctx submit → Close E_INVALIDARG。
- **根因 14（机制）**：vanilla 创建的一次性 staging buffer（`GpuDevice.createBuffer(data)` 等**不经过 Dx12TransientMemory 保护**的路径）由 vanilla 直接 `close()` → Java `Dx12GpuBuffer.close()` → 原生 `destroyObject` **立即 `delete obj`** 释放最后一个 `ComPtr<ID3D12Resource>` → **立即销毁 D3D12 资源**。但打开中的共享命令列表仍引用它 → Close 校验失败（E_INVALIDARG）。官方用 `VulkanGpuBuffer.close() → device.createCommandEncoder().queueForDestroy(this)` **延迟销毁**（VulkanCommandEncoder 的延迟删除队列在提交执行完成后释放），我们的实现缺失此机制。
- **修复（Fix H，原生延迟销毁）**：
  - `dx12_device.cpp`：新增全局 `gPendingDeletes`（`std::vector<Dx12Object*>`）+ `gOpenListCount`（打开未提交的命令列表数）+ `flushPendingDeletes()`（统一 `delete`）
  - `destroyObject` 不再立即 `delete obj`，改为登记 `gPendingDeletes`（unmap 仍立即做）
  - `beginCommandList` 成功后 `++gOpenListCount`
  - `submitCommandList` 同步等待完成后 `--gOpenListCount`，归零则 `flushPendingDeletes()`（此时所有已打开命令列表均已提交并执行完，被删资源不再被引用）
  - `destroyCommandEncoder` 若 ctx 仍打开（未提交即销毁）：递减计数，归零 flush（命令列表已销毁，引用随之失效）
  - `destroyDevice` 兜底 flush（进程退出前释放残留 pending）
  - 与 Dx12TransientMemory 的 rotate 机制互补：TransientMemory 只保护"注册过的"瞬态 buffer，本修复覆盖**全部**销毁路径（含 vanilla 直接 close 的 staging buffer）
- **顺带修复（次要 ERROR ①）**：`CopyDescriptorsSimple` 的源必须是 **CPU-only** 描述符堆，此前 texture view 的 cpuHandle 来自 SHADER_VISIBLE srvHeap（每帧 ERROR）。`DeviceContext` 新增 CPU-only `srvCpuHeap`；`createTextureView` 在 CPU-only 堆创建描述符（cpuHandle 指向它，供 pushDescriptors 复制源），同槽位在 SHADER_VISIBLE 堆再创建一份（gpuHandle 供 GPU 直接引用）。
- **验证**：DLL 重建 + jar 重打（手动 zip 替换，Gradle 8.13 不支持 Java 26 class 文件），三处哈希一致 = `EABAA0194019989564EFB75C5245198C` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌）。
- **待复测判据（下次日志）**：
  - 无 `was deleted prior to closing the command list` → 崩溃消除，主菜单持续渲染
  - 无 `CopyDescriptorsSimple ... CPU write only` ERROR（Fix H 附带修复）
  - 仍可能残留每帧 ERROR：`ResourceBarrier Before state (COMMON) ... does not match (COPY_SOURCE)/(DEPTH_WRITE)`（资源 0x48AC8560/0x48AC5320）——DEPTH_WRITE 等状态不隐式 decay 到 COMMON，跨命令列表残留，下一轮在 beginCommandList 时对不 decay 状态显式回退 COMMON；不影响本次崩溃

## 阶段 9（08-08 复测）：Fix H 生效（持续渲染，主菜单可交互）→ 窗口 resize 崩溃 = SRV 堆耗尽

- **Fix H 完全生效**：根因 14 崩溃消失，主菜单持续渲染（日志 7 万行无 `was deleted prior to closing`；每帧稳定 submit + pushDesc + blitSurface），用户可交互操作菜单。
- **新崩溃（根因 15）**：用户调整窗口大小（854x540 → 854x539）→ `RenderTarget.resize → createBuffers` → `dx12CreateTextureView: srv heap exhausted`（`IllegalStateException`）。前一行还有 `dx12ConfigureSurface: ResizeBuffers failed HRESULT 0x887A0001`（DXGI_ERROR_INVALID_CALL，WARN 级，MC 继续）。
- **根因 15（机制）**：SRV 描述符堆槽位 `gNextSrv` 从 0 单调递增**从不回收**（kSrvHeapSize=4096）。长会话中纹理 view 累积分配（字体 glyph/纹理重载每次创建新 view，旧 view close 不还槽位）；窗口 resize 触发 RenderTarget 重建 color/depth 纹理 view → 槽位耗尽崩溃。RTV/DSV 堆每帧 beginCommandList 归零复用（L968-969），无此问题；Sampler 堆（256）同理可能耗尽。
- **修复（Fix I，描述符槽位 free-list 复用）**：
  - `Dx12Object` 增加 `int descSlot = -1`（TextureView → SRV 槽位；Sampler → sampler 槽位）
  - 匿名 namespace 新增 `gFreeSrvSlots` / `gFreeSamplerSlots` + `allocSrvSlot()` / `allocSamplerSlot()`（free-list 优先，空则堆尾递增，仍耗尽时返回错误）
  - `createTextureView` / `createSampler` 改用 allocator 并记录 descSlot
  - `flushPendingDeletes` delete 前按 kind 归还槽位（与延迟销毁共用同一时机，保证资源不再被引用后才复用描述符）
- **顺带修复（ResizeBuffers 0x887A0001）**：
  - 0 尺寸防护：窗口最小化/边框切换瞬间 WM_SIZE 传 0 时 ResizeBuffers 返回 INVALID_CALL——保持旧尺寸直接返回成功（不 ResizeBuffers）
  - 非 0 尺寸偶发失败（拖拽期间 Present 与 ResizeBuffers 竞争）重试一次
  - 错误消息携带 w/h/count/fmt 参数便于下次定位
- **验证**：DLL 重建 + jar 重打（手动 zip 替换），三处哈希一致 = `59C77EB3690AC01388A7563D1BC1D2C8` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌）。
- **待复测判据（下次日志）**：
  - 调整窗口大小不再崩溃（SRV 槽位复用生效；`srv heap exhausted` 不再出现）
  - resize 后画面正确跟随新尺寸；无 `ResizeBuffers failed`（若仍失败，错误消息含参数可定位）
  - 仍可能残留每帧 `ResourceBarrier ... COMMON ... does not match` ERROR（阶段 8 遗留，不崩）

