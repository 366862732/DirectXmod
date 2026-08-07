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
