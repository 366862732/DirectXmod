# 修复 DIAG GREEN shader 读回全黑问题

## Why

DIAG GREEN shader 注入已确认对全部 98 条 pipeline 生效，draw calls 均正常提交（`colorTargetsWritten -> 1`），但 `DIAG_READBACK_COLOR_TEX` 读回 `srcTex` 始终返回全黑 `(0,0,0,0)`。需要定位并修复读回逻辑，使其能正确捕获 shader 输出的绿色像素，从而验证渲染通路。

## 问题分析

### 根本原因

`blitSurface` 中的 `DIAG_READBACK_COLOR_TEX` 块位于 `CopyTextureRegion` **之后**（line 428-437），此时 `srcTex` 已被拷贝"消耗"（内容已通过 COPY_SOURCE 语义被读取）。更重要的是，当此诊断块调用 `dbgReadbackTexturePixels` 时：

1. `dbgReadbackTexturePixels` 内部调用 `deviceWaitIdle`，等待所有 GPU 工作完成
2. 在等待期间及等待之后，`srcTex` 可能被后续帧的 render pass 重新用作 color attachment，处于非 COMMON 状态
3. 读回函数假设 `srcTex` 在 COMMON 状态发起 barrier（`StateBefore = COMMON`），但实际状态可能不匹配，导致 barrier 静默失败或拷贝出零数据

此外，Java 层的 `dx12ReadbackTexturePixels(lastColorTextureHandle)` 每 30 帧触发一次，此时 `lastColorTextureHandle` 可能指向一个已在多帧前被复用的纹理，读回的也可能是陈旧或零内容。

### 已确认的事实

| 检查项 | 结果 |
|---|---|
| DIAG GREEN shader 注入 | ✅ 全部 98 条 pipeline `changed=true` |
| draw calls 提交 | ✅ `colorTargetsWritten -> 1` |
| PSO 创建 | ✅ 无报错 |
| `pushDescriptors` firstView=0 | ✅ splash 阶段无纹理，符合预期 |
| `blitSurface` srcTex format | ✅ `fmt=%d` 日志正常（RGBA8） |
| readback srcTex 内容 | ❌ 全黑 `(0,0,0,0)` |

## What Changes

- **修改 `dx12_surface.cpp`**：将 `DIAG_READBACK_COLOR_TEX` 读回块移到 `CopyTextureRegion` **之前**，在 `srcTex` 仍处于 COMMON 状态且包含 fresh shader 输出时立即读回
- **修改 `dx12_surface.cpp`**：在 `blitSurface` 中添加 `colorTargetsWritten` 和 srcTex 状态诊断日志（每次调用），帮助区分"shader 未写入"vs"读回时机错误"
- **可选清理**：若读回验证通过，可移除部分临时诊断代码

## Impact

- 影响文件：`native/src/dx12_surface.cpp`（核心修复）、`native/src/dx12_device.cpp`（可选增强日志）
- 不涉及 Java 层变更
- 诊断代码保持 `#define` 宏开关可控

## MODIFIED Requirements

### Requirement: DIAG_READBACK_COLOR_TEX 读回时机
- **原行为**：在 `CopyTextureRegion(srcTex→dst)` 完成后读回 `srcTex`
- **新行为**：在 `transitionTextureTo(srcTex, COPY_SOURCE)` 完成后、`CopyTextureRegion` 执行前读回 `srcTex`
- **理由**：`CopyTextureRegion` 是"读取源、写入目标"的原子操作，执行后源纹理内容不可再读；在拷贝前读回确保捕获 shader 输出

### Requirement: 读回同步保证
- 读回在 `transitionTextureTo(srcTex, COPY_SOURCE)` 之后、`CopyTextureRegion` 之前执行
- 此时 srcTex 处于本 command list 跟踪的 COPY_SOURCE 状态，`deviceWaitIdle` + COMMON→COPY_SOURCE barrier 不会产生状态错配
- 读回的 staging buffer 通过 fence 同步保证 CPU 可读
