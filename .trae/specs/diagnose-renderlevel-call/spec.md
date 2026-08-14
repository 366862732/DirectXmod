# 诊断 renderLevel() 调用问题

## Why

P6 阶段 GUI 主菜单渲染已通过验证（纯红→GUI 可见），但进入世界后画面黑屏/无变化。疑似 `GameRenderer.renderLevel()` 未被调用，或调用后渲染结果为空。需要精确定位根因。

## 分析结论（已确认）

从官方代码分析（`docs/official-262/`）：
1. `GameRenderer.render()` 每帧调用，其内部条件判断决定是否调 `renderLevel()`：
   ```java
   boolean shouldRenderLevel = resourcesLoaded && advanceGameTime && minecraft.level != null;
   if (shouldRenderLevel) { renderLevel(deltaTracker); ... }
   ```
2. `Minecraft.renderFrame()` 开头有早退：`if (windowSurface.isAcquired()) return;`
3. 当前 debug mixin 只注入 `render()` 头部，**没有打印 `advanceGameTime`**，无法判断是否通过了条件

## What Changes

- **增强诊断 mixin**：打印 `advanceGameTime` 状态 + 渲染循环帧计数 + render pass 开始/结束标记
- **增强 native 日志**：在关键路径（configure/acquire/blit/present/submit）增加结构化日志
- **验证命令提交完整性**：确认命令列表被正确提交且 fence 信号正常工作

## Impact

- 影响文件：`GameRendererRenderDebugMixin.java`、`Dx12CommandEncoderBackend.java`、native 日志代码
- 不涉及破坏性变更，纯诊断增强
