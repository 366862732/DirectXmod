# Debug Session: swapchain-access-denied (CS_OWNDC)

- Status: **FIXED**
- Symptom: `CreateSwapChainForHwnd failed HRESULT 0x80070005`，DX12 自检阶段创建 surface 失败并回退到 OpenGL。
- Scope: Minecraft Fabric DX12 backend startup self-test
- Root Cause: GLFW30 窗口类带 `CS_OWNDC` 标志，导致 DXGI flip-model swapchain 拒绝绑定。
- Fix: 多级 fallback 策略——先尝试 FLIP_DISCARD+ALLOW_TEARING，失败后降级到 FLIP_DISCARD 无 TEARING，两者均不兼容时返回错误。

## Hypotheses & Verification

| ID | Hypothesis | Likelihood | Evidence |
|----|------------|------------|----------|
| A | `HWND` 虽然非 0，但窗口类型/时机仍不满足 DXGI 绑定条件 | High | Narrowed: `isWindow=1` 且 rect 正常 |
| B | DXGI factory / adapter / command queue 组合不一致 | Medium | Weakened: `devLuid` 已对上 RTX 4070 |
| C | 自检阶段运行在线程错误 | High | Rejected: `wndTid==curTid` 且 `wndPid==curPid` |
| D | `CreateSwapChainForHwnd` 前已有调试层错误 | Medium | Confirmed: 无额外调试层消息 |
| E | 实际运行的不是最新 DLL | Low | Rejected: 新诊断行已生效 |
| F | `CreateSwapChainForComposition` 不可用 | Medium | Confirmed: GetProcAddress 返回 null |

## Evidence

- 游戏日志确认 `class=GLFW30`, `classStyle=0x23`, `CS_OWNDC=1`（通过后续编译验证）
- `CreateSwapChainForComposition` GetProcAddress 返回 null（Windows 11 26200 / DXGI 未导出该名称）
- 多级 fallback 生效后：`configureSurface: 640x480 mode=IMMEDIATE ok=true`
- `Surface self-test OK (pure green clear + present via JNI on real window)`
- `Using graphics backend DX12, using drivers: D3D12 driver`
- `Using graphics device: NVIDIA GeForce RTX 4070 (D3D12)`

## Final Fix

**文件**: `native/src/dx12_surface.cpp`
- `createSurface`: 替换单一 `CreateSwapChainForHwnd` 调用为两级 tier fallback
  - Tier 1: `FLIP_DISCARD + ALLOW_TEARING`（正常路径）
  - Tier 2: `FLIP_DISCARD` 无 TEARING（CS_OWNDC 窗口降级）
- `configureSurface` 重建路径: 同样使用两级 tier fallback
- 修复了 fallback 成功后 `swapChain1.As(&s->swapChain)` 多余执行的 bug
- 移除了无效的 `CreateSwapChainForComposition` fallback（GetProcAddress 不可用）

## Deployment

- DLL: `D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll` (215040 bytes)
- JAR: `D:\.minecraft\versions\26.2-Fabric_0.19.3\mods\gl4dx12-0.1.0.jar` (1295699 bytes)
- 部署脚本: `deploy/deploy-to-minecraft.bat` 路径已更新
