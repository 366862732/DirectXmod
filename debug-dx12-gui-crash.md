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
