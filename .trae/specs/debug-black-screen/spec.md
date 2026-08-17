# 渲染黑屏系统性排查 Spec

## Why

游戏进入世界后画面持续黑屏。需要按系统化清单逐项验证，从 Backbuffer 通路到 src 纹理内容逐步定位断裂点。

## 排查方法

按 `问题.md` 中三个阶段依次执行，每完成一步在 checklist 中标记结果，根据结果决定下一步。

## What Changes

- 在 native 层 (`dx12_surface.cpp`) 中添加诊断代码：
  - 1.1: `blitSurface` 中用 `ClearRenderTargetView` 清 Backbuffer 为纯红色，观察屏幕是否变红
  - 1.2: `blitSurface` 中用已知测试纹理复制到 Backbuffer，观察是否显示
  - 2.1: `blitSurface` 中用 `dbgReadbackTexturePixels` 读回 src 纹理前 16 像素打印到日志
  - 2.2: `blitSurface` 中检查 src 纹理格式，若非 RGBA8 则打印警告
  - 3.1-3.3: 验证交换链格式和 Present 路径

## Impact

- 影响文件：`native/src/dx12_surface.cpp`（临时诊断代码）、日志文件
- 不涉及 Java 层变更
- 诊断完成后移除临时代码，回归原有实现
