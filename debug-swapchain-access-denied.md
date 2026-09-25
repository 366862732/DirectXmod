# Debug Session: JVM Crash in destroySurface (async render thread race)

- Status: **FIXED**
- Symptom: 自检表面测试通过后，`selfTestSurface` finally 块中调用 `dxSurface.close()` 触发 JVM crash
- Scope: Minecraft Fabric DX12 backend `selfTestSurface` → `destroySurface` → `deviceWaitIdle`
- Root Cause: async render thread 与主线程并发访问同一 swapchain

## Timeline (from game log, run 1)

```
[17:33:58] submit: ASYNC path fence=1          ← self-test 走异步路径
[17:33:58] presentSurface: ok (syncInterval=0)  ← 第1次 present（自测提交）
[17:33:58] acquireSurface: enter ...            ← 渲染线程醒来，acquire surface
[17:33:58] presentSurface: ok                   ← 第2次 present（渲染线程）
[17:33:58] selfTestSurface: presented GREEN     ← self-test 成功
[17:33:58] destroySurface: enter surface=...    ← finally 块开始销毁
[17:33:58] presentSurface: ok                   ← 第3次 present（渲染线程仍在运行！）
# CRASH: deviceWaitIdle → queue->Signal → WaitForSingleObject + 渲染线程正在 Present swapchain
```

## Root Cause Analysis

`destroySurface` 调用链：`dx12DestroySurface(J)` → `destroySurface` → `deviceWaitIdle` → `queue->Signal` + `WaitForSingleObject`

在 async 渲染模式下，自测提交后立即有：
1. 渲染线程醒了（WaitForSingleObject 触发），开始 acquire + present 流程
2. 主线程在 finally 块中立刻调用 `dxSurface.close()` → `destroySurface`
3. `deviceWaitIdle` 的 `queue->Signal` 与渲染线程的 `Present()` **并发访问同一 swapchain**
4. DXGI/D3D12 检测到资源被同时访问 → 崩溃（EXCEPTION_?? 0x87d，KERNELBASE.dll+0xc41ca）

## First Fix Attempt (run 1)

Initial approach: call `destroyAsyncRenderer()` inside `destroySurface` to ensure render thread stops before `deviceWaitIdle`.

**Problem with this approach**: `destroyAsyncRenderer()` permanently destroys the renderer thread. When the game later creates the real surface and calls `asyncRenderBeginFrame`, it fails with "renderer not running" because the thread is gone.

## Timeline (from game log, run 2 — first fix attempt)

```
[18:08:53] selfTestSurface: presented GREEN     ← self-test passes
[18:08:53] destroySurface: enter ...            ← calls destroyAsyncRenderer()
[18:08:53] destroySurface: done                 ← renderer stopped
[18:08:53] Surface self-test OK
[18:08:53] dx12CreateSurface: JNI hwnd=0x701c2  ← creating real game surface
# CRASH: dx12AsyncRenderBeginFrame: renderer not running
```

## Final Fix (run 2)

**Core insight**: Don't destroy the renderer thread. Only synchronize with it (wait for current frame to finish) so there's no concurrent swapchain access during `deviceWaitIdle`.

**New function: `waitForRenderThreadSubmit()`**
- Signals `gEvtBeginFrame` to wake any sleeping render thread
- Waits for `gEvtSubmitDone` (10s timeout) to ensure current frame's submit is complete
- Does NOT set `gRenderRunning=false` or join the thread — the renderer keeps running
- Safe no-op if renderer is not running or already past the wait point

**`destroySurface` now calls `waitForRenderThreadSubmit()` instead of `destroyAsyncRenderer()`**

## Files Changed (final fix)

| 文件 | 修改内容 |
|------|---------|
| `native/src/dx12_device.h` | 添加 `extern std::thread gRenderThread`; `extern bool gRenderRunning`; 声明 `waitForRenderThreadSubmit()` |
| `native/src/dx12_device.cpp` | 将 `gRenderThread`/`gRenderRunning` 移出匿名命名空间至 `dx12mc` 顶层；新增 `waitForRenderThreadSubmit()` 实现；render thread 错误路径改用 `destroySurfaceNoWaitIdle` |
| `native/src/dx12_surface.cpp` | `destroySurface` 改用 `waitForRenderThreadSubmit()` 替代 `destroyAsyncRenderer()`；新增 `destroySurfaceNoWaitIdle` |

## Why This Works

1. Render thread is still running after self-test (game can use it)
2. `waitForRenderThreadSubmit()` ensures no concurrent access to swapchain during destroy
3. `destroyAsyncRenderer()` is still called in `Dx12Device.close()` for proper shutdown
4. `destroySurfaceNoWaitIdle()` prevents deadlock in render thread error paths (where `deviceWaitIdle` would try to signal a queue the thread itself uses)

## Deployment

- DLL: `D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll` (已更新)
- JAR: 无需重新打包（Java 代码无改动）
