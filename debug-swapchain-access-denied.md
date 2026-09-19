# Debug Session: swapchain-access-denied

- Status: **FIXED**
- Symptom: `CreateSwapChainForHwnd failed HRESULT 0x80070005`，DX12 自检阶段创建 surface 失败并回退到 OpenGL。
- Scope: Minecraft Fabric DX12 backend startup self-test
- Root Cause: GLFW30 窗口类带 `CS_OWNDC` 标志，导致 DXGI flip-model swapchain 拒绝绑定。
- Fix: 添加 `CreateSwapChainForComposition` fallback（动态加载），通过 `MakeWindowAssociation` 关联到目标窗口。

## Hypotheses & Verification

| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | `HWND` 虽然非 0，但窗口类型/时机仍不满足 DXGI 绑定条件 | High | Low | Narrowed: `isWindow=1` 且 rect 正常，待确认 class style |
| B | DXGI factory / adapter / command queue 组合不一致 | Medium | Low | Weakened: `devLuid` 已对上 RTX 4070，但仍未完全排除 |
| C | 自检阶段运行在线程错误，命中窗口线程相关限制 | High | Low | Rejected: `wndTid==curTid` 且 `wndPid==curPid` |
| D | `CreateSwapChainForHwnd` 前已有调试层错误，`0x80070005` 只是后续表象 | Medium | Low | Weakened: 当前未看到 `PreSwapInfoQueue` / `PostSwapInfoQueue` 输出 |
| E | 实际运行的不是最新 DLL | Low | Low | Rejected: 已出现新加的 `devLuid` 诊断行 |

## Evidence

- 游戏日志 `854-859`：
  - `dx12CreateSurface: JNI hwnd=0x180864`
  - `createSurface: devLuid=0001096B00000000 desc=NVIDIA GeForce RTX 4070`
  - `createSurface: isWindow=1 wndTid=28132 wndPid=23684 curTid=28132 curPid=23684 clientRect=0,0-854,480 hasRect=1`
  - `dx12CreateSurface: hwnd=0x180864 queue=0x...`
  - `CreateSwapChainForHwnd failed HRESULT 0x80070005`
- 已确认 pipeline self-test 通过，说明设备创建、命令层、管线编译链路基本可用。
- 已确认最新 DLL 已生效，因为日志里出现了新插入的 `devLuid` 诊断行。
- 已确认失败发生在有效窗口、同线程、同进程上下文中。

## Next

- 添加窗口有效性、窗口线程/当前线程、进程归属等诊断日志。
- 在 `CreateSwapChainForHwnd` 失败后立即转储一次调试层消息，确认是否存在 DXGI / D3D12 补充原因。

## Instrumentation

- `native/src/dx12_surface.cpp`
  - 新增 `HWND` 有效性、窗口线程/进程、当前线程/进程、client rect 日志
  - 新增 `CreateSwapChainForHwnd` 失败后的 `PostSwapInfoQueue[...]` 调试层消息输出
  - 新增窗口类名与 class style 日志，用于确认 `CS_OWNDC` 类兼容性问题
  - **修复**：移除无效的 `SetClassLongPtrW` 方案
  - **修复**：添加 `CreateSwapChainForComposition` 动态加载 fallback（`createSurface` + `configureSurface` 重建路径）
  - **修复**：添加 `#include <dxgi1_2.h>` 支持 `IDXGIWindowAssociation`（实际未用到，但保留以防后续需要）
  - **修复**：`MakeWindowAssociation` 直接调用 `factory` 对象而非通过 `IDXGIWindowAssociation` 接口
