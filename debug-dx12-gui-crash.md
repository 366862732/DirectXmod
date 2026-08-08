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

## 阶段 10（08-08 复测）：Fix I 未完全解决 → 黑屏确证根因链 = srv 堆 4096 容量不足 → Fix J 扩容 + 诊断

- **现象**：窗口黑色（按钮有声音）；`dx12_dump_backbuf.bmp` 全 `RGBA(28,24,25,0)`（透明近黑）、`dx12_dump_blitsrc.bmp` 全 `RGBA(0,0,0,0)`（透明黑）→ 渲染目标无任何 GUI 元素，只剩 clear 色。
- **srv heap exhausted 复发（两处）**：
  - **t=424821（L9286）**：资源包加载失败 → `Caught error loading resourcepacks, removing all selected resourcepacks`（堆栈：`dx12CreateTextureView → ... → ProfiledReloadInstance`）。资源包移除后 GUI 元素纹理缺失 → `GuiRenderer.draws` 为空 → `draw()` 直接 return（官方 GuiRenderer.java L184-185）→ **GUI 主 pass 在 t=425190.8（L17168）后完全消失**（最后两帧 t=425085.5/425190.8 仍正常 drawIndexed 30+12）。
  - **t=434953.5（L219544）**：窗口 resize → `RenderTarget.createBuffers → RenderTarget.resize → GameRenderer.resize` → `srv heap exhausted` → `ReportedException: Render Frame` 崩溃。
- **Fix I（free-list 槽位复用）为何未完全解决**：free-list 只在 view **已销毁**时归还槽位。资源包加载/resize 的瞬时峰值 = 旧 view 在 `gPendingDeletes` 队列（资源被打开的命令列表引用，submit 前不能归还）+ 新 view 批量创建，**瞬时并发持有 >4096** → 堆耗尽（不是长期泄漏，是容量不够 + 归还滞后）。
- **修复（Fix J，一次性扩容 + 兜底 flush + 诊断）**：
  - `dx12_device.cpp`：`kSrvHeapSize` 4096→**65536**（D3D12 CBV_SRV_UAV 堆上限 1,000,000，内存仅 ~2MB/堆含 CPU 镜像堆）；`kRtvHeapSize` 512→2048；`kDsvHeapSize` 64→256；`kSamplerHeapSize` 256→4096
  - `allocSrvSlot`/`allocSamplerSlot`：堆满时若存在 pending 且无打开命令列表，先 `flushPendingDeletes()` 再重试一次（覆盖"批量 create+destroy 夹在两次 submit 间"的归还滞后）
  - 耗尽错误消息携带画像：`(next=.. free=.. pending=.. openLists=..)`——若 65536 仍耗尽可直接定位泄漏
- **验证**：DLL 重建 + jar 重打，内嵌 DLL 哈希 = `41C5958B67F259607BB494B55A23CBF6` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌，jar 1214082 B，08-08 21:27）
- **待复测判据（下次日志）**：
  - 无 `srv heap exhausted` → 资源包加载成功、GUI 主 pass 持续出现、窗口可见主菜单
  - 调整窗口大小不崩溃
  - 若 65536 仍耗尽 → 错误消息带 `next/free/pending/openLists` 画像，据此定位 Java 侧未 close 的 view

## 阶段 10 追加（08-08 21:31 复测）：Fix J 引入崩溃 = Sampler 堆 4096 超 D3D12 上限 2048

- **现象**：启动 7.3s 崩溃，`EXCEPTION_ACCESS_VIOLATION` @ `dx12_mc.dll+0x8d08`，Java 栈 `dx12CreateSampler → Dx12GpuSampler.<init> → Dx12Device.createSampler → selfTestJavaResources`。游戏日志 `Device probe + resource self-test: ERROR: CreateDescriptorHeap failed`。
- **根因（Fix J 自身引入）**：`kSamplerHeapSize=4096` **超过 D3D12 Sampler 描述符堆硬上限 2048**（所有 feature level）→ `CreateDescriptorHeap` 失败 → `ensureDevice` 中途失败但 `gCtx.device` 已置位 → 下次调用 guard 只看 device 短路返回 true（半初始化）→ `createSampler` 解引用 null `gCtx.samplerHeap` → AV。
- **修复（Fix K）**：
  - `kSamplerHeapSize` 4096→**2048**（D3D12 上限，Sampler 用量实际仅个位数）
  - `ensureDevice` guard 改为**校验全部描述符堆齐备**（srvHeap/srvCpuHeap/rtvHeap/dsvHeap/samplerHeap/drawHeap 非空才返回 true）——同类"半初始化"问题以后返回错误而非 AV
- **验证**：DLL 重建 + jar 重打，内嵌 DLL 哈希 = `AEAA8BB7DFB99D142E03A9560A81E91E` ✅（jar 1214104 B，08-08 21:35）
- **待复测判据（下次日志）**：
  - 无 `CreateDescriptorHeap failed` / 无原生 AV，自检全 OK
  - 叠加阶段 10 判据：无 `srv heap exhausted`、GUI 主 pass 持续出现、窗口可见主菜单、resize 不崩溃

## 阶段 11（08-08 21:37 复测）：Fix K 生效，但"冻结"（非崩溃）→ 根因 = 附件状态跨命令列表残留 → 修复 = endRenderPass 显式回切 COMMON

- **现象**：自检全 OK（无 `CreateDescriptorHeap failed`、无 srv heap exhausted、无原生 AV），但约 19 秒后**冻结而非崩溃**：日志在帧 742 `submit: done v=742` → `presentSurface: ok` 后**戛然而止**（t=3194574ms，最后一行 `pushDesc[0]`），无异常无 hs_err。**启动器（PCL2）也每次跟着卡死**。
- **启动器卡死原因**：游戏 GPU 挂起（非正常退出）→ Windows 触发 TDR（GPU 超时检测与恢复）重置显卡 → TDR 期间**所有**使用同一 GPU 渲染的进程（含 PCL2 的硬件加速 UI）全部停滞；同时 PCL2 作为父进程等待游戏退出，游戏挂死不退出 → 启动器一直阻塞。
- **证据链（每帧 8 条 InfoQueue ERROR，全部同类）**：
  - `ResourceBarrier: Before state (0x0: COMMON|PRESENT) of resource (0x...4C60) (subresource 0-4) does not match with the state (0x4: RENDER_TARGET) specified in preceding ResourceBarrier or as InitialState`
  - 同类资源：`0x...67B0`（COPY_SOURCE）、`0x...23DAA0`（COPY_SOURCE）、`0x...C180`（DEPTH_WRITE）
- **根因 16（机制）**：D3D12 中 **RENDER_TARGET / DEPTH_WRITE 属"非可提升状态"，命令列表执行完成时不会隐式 decay 回 COMMON**（只有 COMMON/COPY_SOURCE/COPY_DEST/UAV 等"可提升状态"才会）。代码假设：
  - `beginCommandList` 每次提交后 `ctx->resourceState.clear()`，默认所有资源回到 COMMON
  - `endRenderPass` 只设 `inRenderPass=0`，**从不把附件状态回切 COMMON**
  → 附件每帧以 RENDER_TARGET/DEPTH_WRITE 状态残留到下一 command list，下一帧 `beginRenderPass`/`blitSurface` 写 barrier 的 Before=COMMON 与实际状态错配 → 每帧 8 条验证 ERROR → GPU 状态错乱累积 → TDR 冻结（游戏挂死 + 启动器卡死）。COPY_SOURCE/COPY_DEST 同理：**显式进入**的可提升状态不会 decay（dbgReadbackTexturePixels 的"显式进入+显式退出"配对是已验证合法的模式）。
- **修复（Fix L，附件/拷贝路径显式回切 COMMON）**：
  - `dx12_device.h`：`CommandContext` 增加 `activeColorTargets`（`std::vector<Dx12Object*>`）/ `activeDepthTarget`（`Dx12Object*`）
  - `beginRenderPass`：开头清空上次记录；每个非 null color 附件 push 进 `activeColorTargets`；depth 附件记入 `activeDepthTarget`
  - `endRenderPass`：遍历附件 `transitionTextureTo(ctx, tex, COMMON)`（RENDER_TARGET/DEPTH_WRITE→COMMON，按本 list 跟踪状态幂等回切）后清空
  - `blitSurface`（dx12_surface.cpp）：拷贝后 `transitionTextureTo(ctx, srcTex, COMMON)` 回切源纹理
  - `copyBufferToTexture` / `copyTextureToBuffer` / `copyTextureToTexture`：拷贝后显式回切 dst/src 到 COMMON（显式进入的 COPY_DEST/COPY_SOURCE 不会 decay，回切后下一 list 的 Before=COMMON 假设才成立）
  - `clearColorTexture` / `clearDepthTexture`：clear 后显式回切 COMMON（同类残留）
- **验证**：DLL 重建 + jar 重打，三处哈希一致 = `8924561EC399F15CD71D235035647D2F5507785BA6B503F0130E6DADEDF68524` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌，jar D6217772，已部署 26.2-Fabric_0.19.3\mods）
- **待复测判据（下次日志）**：
  - 每帧 **8 条 `Before state does not match` ERROR 消失**（关键判据）
  - 窗口持续显示、可交互、可 resize，关闭游戏后**启动器不再卡死**
  - readback 出现非纯色像素（GUI 可见）

## 阶段 12（08-08 22:0x 复测）：Fix L 生效（barrier 错误消失）→ 但"关闭流程卡死"→ 可观测性不足 → 修复 = 原生日志镜像到独立文件

- **现象**：启动正常、渲染期正常运行 **约 19 秒**（21:57:46–21:58:12，三大 pass 循环持续生成，**无每帧 `Before state does not match`** → Fix L 关键判据达成 ✅）。但用户报告**启动器彻底卡死**，只能提供 debug.log（1.7MB，5676+ 行，Java 侧 stdout/stderr）。
- **debug.log 时间线**：
  - 21:57:46 Fabric Loader 启动，自检无异常
  - 21:57:46–21:58:12 渲染期：每帧 8 个坑位（854x480 depth=yes 主三通道 + 854x480 CubeMap + 贴图动画 2048/1024/512/256/128/512x256 depth=no）持续生成 → 核心管线运行正常
  - 21:58:12 `Stopping!` → 渲染线程**仍在渲染**（PostPass.process ×6 / GuiRenderer.executeDrawRange，完成当前帧收尾）→ 21:58:13 全部 Worker-Main 线程 shutdown（= `Util.shutdownExecutors()`，销毁流程已启动）→ 渲染线程日志**戛然而止**，进程不退出，启动器死锁
- **关键限制（为何定位不到卡点）**：
  - debug.log 中 `[dx12-java]` **只在 `createRenderPass` 一处打印**（Dx12CommandEncoderBackend.java:154），其余 JNI 调用（`dx12WaitForFence`/`dx12Submit`/`dx12Destroy*`/`dx12EndCommandList`…）**不打 Java 侧日志**
  - 原生 `dbgLog` 写 stderr → 游戏日志（版本目录 `logs\latest.log`），**被启动器死锁锁住丢失**
  - 因此"Stopping! 后渲染线程卡在哪"完全不可见
- **已排查（静态审查）**：
  - 所有 fence 等待均有界：`waitForFenceValue`/`deviceWaitIdle`/`destroyCommandEncoder` 5s 超时；`waitForQueueFenceValue` 超时钳制 0xFFFFFFF0ms（~49.7 天，`awaitCompletion(Long.MAX_VALUE)` 场景 = **近永久等待**）
  - **最大嫌疑**：关闭流程中 vanilla 的 `MappableRingBuffer.rotate/currentBuffer` / `StagedVertexBuffer` pending recycle 等路径调用 `awaitCompletion(Long.MAX_VALUE)` → 目标 = `queueFenceValue+1`，**关闭后不再有 submit 推进 queueFence → 目标永不满足 → 渲染线程近永久阻塞** → 进程不退出 → PCL2 作为父进程一直等待 → 启动器彻底卡死
  - 与阶段 3（黑屏冻结）同机制：一次性 encoder 上 createFence 的 token 依赖"下一次提交"满足，关闭后无下一次提交
- **修复（Fix M，可观测性）**：原生 `dbgLog` 镜像写入独立文件 `%TEMP%\dx12-native.log`（随写随刷，**不受启动器死锁影响**）——下次复测即使启动器卡死/游戏日志丢失，也能看到关闭序列的最后一条原生调用（fence 等待值 / destroy 路径 / 超时报错），精确定位卡点
- **验证**：DLL 重建 + jar 重打 + 部署，三处哈希一致 = `7B1D4B1D656BE2A5D3CCA14038A60A5D` ✅（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌，已部署 26.2-Fabric_0.19.3\mods）
- **待复测判据（下次日志）**：
  - 若复现卡死：读 `%TEMP%\dx12-native.log` 尾部，最后一条原生日志 = 卡点（预期 `waitQFence: TIMEOUT ...` 或 `waitFence: TIMEOUT ...` → 确认关闭期 fence 近永久等待；据此修 `awaitCompletion` 关闭路径的等待语义）
  - 若正常退出：`dx12-native.log` 尾部应有 `destroyCommandEncoder`/`destroyDevice` 完整序列 → 关闭路径无卡死
  - 顺带观察：readback 是否出现非纯色像素（GUI 空白问题独立于关闭卡死，尚未解决）

## 阶段 12b（08-08 22:1x 复测）：dx12-native.log 立功 → 卡点在 destroyCommandEncoder 之后的 Java 关闭路径 → 加销毁路径日志插桩

- **dx12-native.log 首次拿到**（17MB，用户从 %TEMP% 复制到项目目录），最后关闭序列：
  ```
  t=5401566.6ms presentSurface: ok surface=...196510
  t=5401580.0ms deviceWaitIdle: enter → done          ← destroySurface（Dx12GpuSurface.close）
  t=5401582.4ms destroyCommandEncoder: enter fenceValue=748
  t=5401582.7ms waitFence: value=748 completed=748    ← 最后一行
  ```
- **关键解读**：`completed=748 >= value=748` → `waitForFenceValue` 的 `cv>=value` 分支直接 return（不打 OK 日志，**正常**）。即 GPU 完全空闲（deviceWaitIdle done）、所有提交完成（fence 748 已满足）、destroyCommandEncoder 本体完成。**卡点 = destroyCommandEncoder 返回后的 Java 关闭路径**（`transientMemory.close()` / `clearPipelineCache()` / `compiler.close()` 之一），且**卡死期间没有任何原生日志**（无 waitQFence → 不是 vanilla fence 等待卡死）。
- **静默根因**：原生 `destroyObject` / `destroyDevice` / `destroySurface` **全部不打日志**——destroyCommandEncoder 之后的一切销毁路径在 dx12-native.log 中不可见，这是定位不到卡点的直接原因。
- **修复（Fix M2，销毁路径日志插桩）**：
  - 原生：`destroyObject`（kind/size，可看到 transientMemory.close 逐个 buffer 进度）、`destroyDevice`（enter/done）、`destroySurface`（enter/done）、`destroyCommandEncoder`（delete 后 done）
  - Java（`[dx12-java]` → debug.log）：`Dx12CommandEncoderBackend.close`（begin / after destroyCommandEncoder / after transientMemory.close）、`Dx12TransientMemory.close`（buffer 计数 / done）、`Dx12Device.close`（begin / after sharedEncoder.close / after clearPipelineCache / done）
  - 下次复测卡死时，Java + 原生两侧最后一行日志交叉比对 → 精确定位到具体步骤
- **验证**：DLL 重建 + jar 重打 + 部署，三处哈希一致 = `2CF93BFD4E541DD4454DD0DBA4D5A34D` ✅（已部署 26.2-Fabric_0.19.3\mods）
- **待复测判据（下次日志）**：
  - 读 debug.log 尾部 `[dx12-java] close:` / `transientMemory.close:` 系列 + `%TEMP%\dx12-native.log` 尾部 `destroyObject:`/`destroyCommandEncoder: done` 系列，交叉定位卡点：
    - 无 `destroyCommandEncoder: done` + 无 `close: after destroyCommandEncoder` → JNI 未返回（delete ctx 卡住，极低概率）
    - 有 done 但无 `transientMemory.close: done` → 卡在 transientMemory.close 的某个 buffer（看最后一条 `destroyObject`）
    - 有 `transientMemory.close: done` 但无 `after clearPipelineCache` → 卡在 clearPipelineCache（管线销毁）
    - 有 `device.close: done` → 我们后端关闭完成，卡点在 vanilla 关闭后续（届时看是否有 waitQFence 挂起）

## 阶段 12c（08-08 22:2x 复测）：第二次 dx12-native.log → 卡点收窄到"destroyPipeline 无日志 + Java 关闭路径不可见" → 修复 = Java 双看门狗 + 日志双写 + destroyPipeline 插桩

- **第二次 dx12-native.log 关闭序列（t=58442xx ms，关键突破）**：
  ```
  t=5844280.5ms destroyCommandEncoder: enter fenceValue=203
  t=5844280.7ms waitFence: value=203 completed=203     ← fence 已满足，正常
  t=5844281.9ms destroyCommandEncoder: done ctx=...     ← JNI 正常返回
  t=5844282.1~3.0ms destroyObject: kind=1 size=56/1024/56/64 ×8   ← transientMemory.close 的最后一帧 buffer
  最后一条：destroyObject: kind=1 size=64              ← 之后无任何原生日志
  ```
- **交叉比对结论**：
  - `destroyCommandEncoder: done` 已出现 → JNI 销毁正常；`transientMemory.close()` 的 8 个 buffer 已逐个销毁（kind=1 size=56/1024/56/64 = 一帧瞬时缓冲）→ **卡点被夹在**：
    1. Java 侧 `transientMemory.close` 之后的 `clearPipelineCache()`（每个 Dx12CompiledRenderPipeline.close → `dx12DestroyPipeline`，**原生无日志**）
    2. Java 侧日志在 Worker shutdown 后不再转发到 debug.log（DebugLoggedPrintStream 失效）→ **Java 关闭日志全部丢失**
  - 即：原生的下一个候选卡点（destroyPipeline 纯 `delete pipeline;` 无日志）与 Java 关闭路径日志（全部不可见）之间——两个盲区重叠处就是卡点。
  - 且 `Util.shutdownExecutors()` 打印 shutdown ≠ 线程终止：非 daemon Worker 若卡在阻塞调用（JNI fence 等待 / 死锁），JVM 不会退出 → PCL2 父进程无限等待 → 启动器彻底卡死。
- **修复（Fix N，最终观测方案，Java + 原生双管齐下）**：
  - 原生：`destroyPipeline` 加 `dbgLog`（pso 地址 / done）——销毁路径至此 100% 有日志
  - Java `Dx12Device.close()`：启动 **5 秒看门狗 daemon 线程**——close() 超时未完成即 dump 全部线程栈到 `%TEMP%\dx12-java.log`（卡死时直接看到渲染线程卡在哪个 Java 方法）
  - Java `Dx12Mod`：新增**全局退出看门狗** daemon 线程——检测 "Render thread" 消失后宽限 15 秒，JVM 仍存活则 dump 全部线程栈（含 daemon 标记，可看到卡死的 Worker-Main 线程）到 `%TEMP%\dx12-java.log` 并 `System.exit(1)`——**保底让启动器恢复正常**，同时留下卡死证据
  - Java 关闭日志双写：`appendJavaLog()`（System.err → debug.log + 文件 `%TEMP%\dx12-java.log`）——debug.log 转发丢失后独立文件仍可见
- **验证**：DLL 重建（含 destroyPipeline 日志）+ `gradlew build` 重新编译 Java class + 重打 jar + 部署，三处哈希一致：
  - DLL（native/build/bin/Release ↔ fabric/src/main/resources ↔ jar 内嵌）= `0DC3D1F437FD5EFF2A90F1C522792FB8` ✅
  - jar（fabric/build/libs ↔ 已部署 mods）= `6B64AD2BD1875BD7E9DB65C8BA323749` ✅
  - 已验证 jar 内含新 `Dx12Mod.class` / `Dx12Device.class` ✅（Java 源码变更已真正重新编译，非仅更新 DLL）
- **待复测判据（下次日志）**：
  - 若复现卡死：读 `%TEMP%\dx12-java.log` 看门狗线程栈 → 精确定位卡死行（预期直接看到渲染线程 / Worker-Main 卡在具体 Java 方法 + 原生栈帧）
  - `%TEMP%\dx12-native.log` 尾部应有完整 `destroyPipeline: pso=... → done` 序列或卡在具体 destroy 对象
  - 看门狗 20 秒兜底 `System.exit(1)` → 启动器不再无限卡死（即使根因未除）
  - 若正常退出：`dx12-java.log` 应有 `device.close: begin → after sharedEncoder.close → after clearPipelineCache → done` 完整序列
  - 顺带观察：GUI 空白问题（blitsrc 全黑）独立于关闭卡死，尚未解决

## 阶段 12d（08-08 22:3x 复测）：关闭卡死已解决（决定性证据）

- **用户澄清关键事实**："启动器的情况是画面卡死，但是本身没有未响应"——不是关闭卡死，而是**渲染期画面冻结**（进程活着、按钮有声音、窗口不更新）。
- **dx12-java.log（4 行，决定性证据）**：`device.close: begin → after sharedEncoder.close → after clearPipelineCache → done`——**我们后端关闭路径 100% 完成，无看门狗触发**。
- **debug.log 尾部（22:33:07）**：`Stopping! → Worker-Main shutdown → device.close: done`（最后一行）→ **JVM 正常退出**，关闭卡死已解决（Fix M/M2/N 观测体系生效：Java 双看门狗未触发 + 日志双写可见）。
- **dx12-native.log（17MB 多次运行累积）**：22:32 运行段 t=6546xxx-6558xxx；**最后一次 acquire=6546815、present=6546827，之后 11.6 秒渲染循环继续（draw/submit 每帧）但再无 blit/present** → 画面冻结铁证。
- **javap 反编译 vanilla（minecraft-merged.jar）确认冻结机制**：
  - `Minecraft.renderFrame`：帧首 `if (windowSurface != null && !windowSurface.isAcquired()) { ... blitFromTexture ... present ... }`
  - acquire 抛异常 → 输出 "Couldn't acquire next surface" → **`surfaceIsInvalid = true`** → 之后不再 acquire/blit/present，但渲染循环继续（画面冻结、进程活着）
  - `GameRenderer.renderFrame`：renderLevel → renderLevelPost → `windowSurface.acquireNextTexture()`
- **debug.log 铁证**：`Couldn't create/acquire next surface!` / `This reaction has already been executed` / `Couldn't configure window surface`
- **根因 17（机制）**：MC 在 acquire 与 present 之间被窗口 resize 事件触发 `configureSurface` → `ResizeBuffers` 在**仍持有 acquired backbuffer**（`currentImageIndex` 未释放）时返回 `DXGI_ERROR_NOT_CURRENTLY_AVAILABLE`（0x887A0001）→ 配置失败 → `surfaceIsInvalid=true` → 不再上屏 → 画面冻结（渲染继续）。
- **修复（Fix O，画面冻结）**：
  - `dx12_surface.cpp presentSurface`：present 后**重置 `currentImageIndex = -1`**（backbuffer 所有权已释放；不重置则 configure 的 ResizeBuffers 误判仍有 acquired backbuffer）；真实 HRESULT 日志（OCCLUDED/MODE_CHANGED/FAILED 打 suboptimal，正常 present 清除 suboptimal）
  - `dx12_surface.cpp configureSurface`：ResizeBuffers 前**防御性 `Present(0,0)` 释放残留 acquired backbuffer** + 完整日志
  - `jni_bridge_p5.cpp`：`dx12ConfigureSurface` 失败/成功均打 `dbgLog`（此前失败只 `fprintf(stderr)` 不进 native log，正是 configure 失败不可见的直接原因）；`dx12PresentSurface` 移除无条件 "ok" 日志
- **验证**：DLL 重建 + gradlew build + jar 重打 + 部署，哈希一致：
  - DLL = `1664682EC1AEA896573D6E04F048F03D` ✅（native/build/bin/Release ↔ jar 内嵌 ↔ 已部署 mods）
  - jar = `F68EF739087BD00A5681043885A3956D` ✅（fabric/build/libs ↔ 已部署 mods）
- **待复测判据（下次日志）**：
  - 正常：`%TEMP%\dx12-native.log` 每帧持续 `presentSurface: ok` + `configureSurface: ok` 交替；窗口持续更新、不冻结；`dx12_dump_backbuf.bmp` 有实际画面内容
  - 若仍冻结：读 `%TEMP%\dx12-native.log` 看 `configureSurface: FAILED ... 0x887A0001` 或 `presentSurface: OCCLUDED/MODE_CHANGED`
  - **GUI 内容空白（blitsrc 全黑）独立于冻结**：`dx12_dump_blitsrc.bmp` 纯黑 = 渲染目标无 GUI 内容，真实 shader/绘制问题尚未排查——冻结修复后若仍黑则继续此线

## 阶段 12e（待复测）

