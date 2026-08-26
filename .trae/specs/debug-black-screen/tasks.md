# Tasks

## 第一阶段：验证 Backbuffer 通路

- [x] Task 1.1: 修改 `blitSurface` 清空 Backbuffer 为纯红色
  - [x] 在 `blitSurface` 函数开头调用 `ClearRenderTargetView`，颜色 (1.0, 0.0, 0.0, 1.0)
  - [x] 重新编译 native 库并运行游戏
  - [ ] 确认屏幕是否为纯红色（需用户测试）
  - [ ] 若变红，记录结果并继续 Task 1.2；若仍黑屏，跳到第三阶段

- [ ] Task 1.2: 用已知测试纹理复制到 Backbuffer
  - [ ] 在 `blitSurface` 中创建或加载一个青色三角形测试纹理
  - [ ] 使用 `CopyTextureRegion` 将该纹理复制到 Backbuffer
  - [ ] 确认屏幕是否显示青色三角形
  - [ ] 若成功，Backbuffer 通路正常，跳到第二阶段；若失败，跳到第三阶段

## 第二阶段：验证 src 纹理内容

- [ ] Task 2.1: 读回 src 纹理像素
  - [ ] 在 `blitSurface` 中添加读回逻辑，读取 src 纹理前 16 个像素
  - [ ] 将像素值 (RGBA) 打印到日志文件
  - [ ] 确认像素值是否非零
  - [ ] 若全零，渲染结果为空，检查 beginRenderPass 绑定的 RenderTarget
  - [ ] 若有颜色，继续 Task 2.2

- [ ] Task 2.2: 检查 src 纹理格式
  - [x] 在 `blitSurface` 中获取 src 纹理的 DXGI_FORMAT（已有 fmt=%d 日志）
  - [ ] 若格式不是 `DXGI_FORMAT_R8G8B8A8_UNORM` 等彩色格式，打印警告
  - [ ] 若格式不对，定位调用方并确保传入正确的彩色输出纹理

## 第三阶段：验证交换链与 Present

- [x] Task 3.1: 确认 SwapChain 格式
  - [x] 在 `configureSurface` 中打印 `swapChainDesc.BufferDesc.Format`
  - [ ] 确认是否为 `DXGI_FORMAT_R8G8B8A8_UNORM`

- [x] Task 3.2: 检查 Present 时 Backbuffer 状态
  - [x] 在 `presentSurface` 中调用 `GetDesc` 打印 Backbuffer 格式和尺寸
  - [ ] 确认与窗口尺寸一致

- [ ] Task 3.3: 检查 ResizeBuffers 调用
  - [x] 已在 configureSurface 中添加日志（Retry/ok 日志）
  - [ ] 确认窗口尺寸变化时正确调用且无错误

## 第四阶段：修复 Blit 根签名与管线分配（2026-08-26 根因定位）

根因：`D3D12SerializeRootSignature` 每帧返回 E_INVALIDARG（0x80070057），`initBlitPipeline` 设置 err → `blitSurface` 提前 return false → backbuffer 永不被绘制 → 黑屏。
E_INVALIDARG 原因：根描述符表同时包含 SRV(t0) 与 SAMPLER(s0) 两种类型 range（D3D12 禁止混用），且 table 的 sampler range 与 static sampler(s0) 寄存器冲突。

- [x] Task 4.1: 移除 `initBlitPipeline` 根签名 descriptor table 中的 SAMPLER range，仅保留 SRV(t0)；static sampler(s0) 继续提供采样器
  - [x] 修改 `dx12_device.cpp` 第 3247-3252 行：删除 `samRange`
  - [x] `table.NumDescriptorRanges = 1`，`ranges[] = { srvRange }`
- [x] Task 4.2: 分配 `gCtx.blitPipeline`（dx12_device.h:111 只声明未分配，写 `->vertBuf` 会 null 解引用）
  - [x] `initBlitPipeline` 内改用本地 `std::unique_ptr<BlitPipeline> bp` 构建全部成员
  - [x] 成功后 `gCtx.blitPipeline = std::move(bp);`
- [x] Task 4.3: `blitSurface` 失败路径补 dbgLog（initBlitPipeline / blitBindSourceTexture 失败时输出错误字符串）
- [x] Task 4.4: 重新编译 native DLL（VS 18 2026），同步 resources/ 与 dx12mod/
- [x] Task 4.5: 重建 JAR 并部署到 deploy/
- [x] Task 4.6: 用户运行游戏，确认日志出现 `blitSurface: set blit rootSig` / `drawIndexed done`，画面不再黑屏
  - 注：blit 链路已打通（drawIndexed done 出现），但画面仍黑 → 进入第五阶段

## 第五阶段：描述符堆/资源状态修复（P24，2026-08-26 黑屏直接根因）

根因（dx12-native.log 658/659 行两类每帧 D3D12 验证 ERROR + 代码交叉验证）：

1. **Bug B（黑屏直接根因）——backbuffer RTV 被每帧覆盖**：`configureSurface` 调 `allocRtvHandle` 从 `rtvHeap` 槽 0 起创建 backbuffer RTV（存 `s->rtvHandles`）；`beginCommandList` 每帧 `gNextRtv = 0`；`beginRenderPass`/`clearColorTexture` 从槽 0 起分配瞬态 RTV → 覆盖 backbuffer RTV → blit 的 `OMSetRenderTargets(s->rtvHandles[idx])` 实际绑到本帧颜色纹理 → 验证错误 "Resource state (0xC0) is invalid for use as a render target"（659 行）→ 场景画进颜色纹理，真实 backbuffer 从未被写入 → 窗口全程黑。
2. **Bug A（伴生）——blit SRV 堆未设置**：`blitBindSourceTexture` 在 `srvHeap` 分配 SRV 并直接绑根表，但 `beginCommandList` 只 `SetDescriptorHeaps({drawHeap})` → 每帧 "descriptor heap containing handle is different from currently set descriptor heap"（658 行），根表绑定无效。

修复：

- [x] Task 5.1: 新增帧级瞬态 RTV 堆 `gCtx.frameRtvHeap`（kFrameRtvHeapSize=256）
  - [x] `dx12_device.h` DeviceContext 新增 `frameRtvHeap` 成员
  - [x] `dx12_device.cpp` 新增 `gNextFrameRtv` 计数 + `kFrameRtvHeapSize` 常量
  - [x] `ensureDevice` 守卫 + 创建堆
- [x] Task 5.2: `beginCommandList` 不再清零 `gNextRtv`，改清零 `gNextFrameRtv`（backbuffer RTV 持久保留）
- [x] Task 5.3: `beginRenderPass`/`clearColorTexture` 改从 `frameRtvHeap` + `gNextFrameRtv` 分配瞬态 RTV
- [x] Task 5.4: `blitBindSourceTexture` 的 SRV 改写入当前命令列表 drawHeap 瞬态槽位（复用 `ctx->drawHeapSlotBase + ctx->nextDrawSlot`），root table 绑定 drawHeap GPU 句柄
- [x] Task 5.5: native 重新编译通过（零 error）
- [x] Task 5.6: 用户部署后确认日志不再出现 `InfoQueue[ERROR]`（658/659 行两类），P24 两类 ERROR 均已消失（新日志验证）

## 第六阶段：blit SampleMask + 顶点格式修复（P25，2026-08-26 第六轮黑屏根因）

第六轮日志（dx12-native.log）确认 P24 生效（两类 ERROR 消失、rtv 句柄稳定），但画面仍黑。新增两条铁证：

1. **Bug C（blit 不写 backbuffer）**：`dx12-native.log` 632 行
   `InfoQueue[WARNING] ID3D12Device::CreateGraphicsPipelineState: Sample Mask is 0, preventing blend operations for all samples.`
   → `initBlitPipeline` 的 PSO desc 漏设 `SampleMask`（零初始化 = 0）→ blit 全屏四边形所有片元被 sample-mask 测试丢弃 → backbuffer 永不被写入 → 黑屏。
   - [x] Task 6.1: `initBlitPipeline` 的 psoDesc 补 `psoDesc.SampleMask = UINT_MAX;`（场景管线 buildPso 早已正确设置）
2. **Bug D（场景纹理全黑）**：`游戏日志` copyBuf 1639 行 GUI 顶点数据每个顶点 `w=-nan`（`98.00 202.00 0.00 -nan`）→ 齐次除法 NaN → 三角形全被裁剪 → colorTex 只有 clear 值。
   根因：`toDxgiVertexFormat` 把 `RGB32_FLOAT`(46) 展开为 `R32G32B32A32_FLOAT`(16B)，而 MC GUI 顶点实际 stride=16（pos.xyz 12B + 附加 4B），TEXCOORD offset=12 与 POSITION.w 重叠 → POSITION 读到 TEXCOORD/未初始化内存（NaN）。`R32G32B32_FLOAT` 本就是合法 D3D12 顶点输入格式（`dxgiByteSize` 已支持 12B）。
   - [x] Task 6.2: `toDxgiVertexFormat` 改为精确三分量（46→R32G32B32_FLOAT，36→R32G32B32_UINT，37→R32G32B32_SINT），修正错误注释。D3D12 自动为 HLSL float4 语义补 w=1.0；correctedStride 计算（12+4=16、12+8+4=24）不受影响
- [x] Task 6.3: native 重新编译通过（零 error）
- [ ] Task 6.4: 用户部署后验证：日志不再出现 `Sample Mask is 0` WARNING；场景 readback 非黑；画面正常显示

## 清理与回归

- [ ] 移除所有诊断代码，恢复原有实现
- [ ] 根据排查结果修复根本问题
- [ ] 验证修复后画面正常显示
