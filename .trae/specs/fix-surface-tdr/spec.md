# Fix Surface Mode GPU TDR Spec

## Why

Surface 模式在 MC 26.1.2 中运行 ~47 秒后触发 GPU TDR（Timeout Detection & Recovery）：画面冻结但音乐继续。TDR 根因是 `Minecraft.runTick()` 内部在 `GameRenderer.render()` 返回后仍然调用 `glfwSwapBuffers()`（GL swap），随后我们的 TAIL Mixin 又调用 D3D12 `frame.present()`，两个 API 在同一 HWND 上竞态提交 Present 操作，导致 GPU 驱动超时。

## Root Cause Chain

```
runTick() {
    ...
    GameRenderer.render() → HEAD cancel → 无 GL 绘制
    ...
    glfwSwapBuffers(window)   ← GL swap 仍然执行（空帧交换）
    ...
} → TAIL Mixin → D3D12 frame.present()  ← 二次 Present
                     ↓
            GPU 驱动冲突 → ~47s 后 TDR 超时
```

关键点：`glfwSwapBuffers` 不在 `GameRenderer.render()` 内，而在 `runTick()` 方法体后半段，独立于 render 逻辑。HEAD cancel 无法阻止它。

## What Changes

### 1. 反编译 MC 26.1.2 源码确定 swap 精确位置
- 执行 `gradlew genSources` 获取反编译源码
- 搜索 `Minecraft.runTick()` 中 `glfwSwapBuffers` 的精确调用点

### 2. 添加 Mixin 抑制 GL swap
- 在 `MinecraftMixin` 中添加 `@Inject` 或 `@Redirect`，在 surface 模式下跳过 `glfwSwapBuffers` 调用
- 确保仅在 `hasSurface() && inWorld` 时抑制 swap

### 3. 保持 D3D12 Present 为唯一 Present
- `MinecraftMixin` TAIL 注入的 `D3D12Bridge.renderFrame()` → `render_surface()` → `frame.present()` 保持不变
- 此时 D3D12 是唯一对 HWND 做 Present 的 API，消除竞态

### 4. TDR 恢复机制（补充）
- 在 `render_surface()` 中处理 `SurfaceError::Timeout` 场景
- 增加设备丢失后的自动重建路径

### 5. 降级方案（如果抑制 GL swap 不可行）
- `glfwSwapInterval(0)` + `glFinish()` 序列化 GL/D3D12 操作
- 回退到离屏模式（已有稳定实现）

## Impact
- Affected specs: s4-camera-wgpu-pipeline, s5-depth-geometry
- Affected code:
  - `fabric/src/main/java/com/dx12/mixin/MinecraftMixin.java` — 新增 swap 抑制注入
  - `fabric/src/main/java/com/dx12/D3D12Bridge.java` — 可能新增辅助方法
  - `rust/wgpu-mc/src/lib.rs` — render_surface TDR 恢复处理

## ADDED Requirements

### Requirement: Suppress GL Swap in Surface Mode
The system SHALL suppress `glfwSwapBuffers` in `Minecraft.runTick()` when surface mode is active and the player is in-world.

#### Scenario: Enter world with surface mode
- **WHEN** player enters a world and `D3D12Bridge.hasSurface()` returns true
- **THEN** `glfwSwapBuffers` in `Minecraft.runTick()` is skipped
- **AND** D3D12 `Present()` via TAIL inject is the only presentation to the HWND

#### Scenario: Back to title screen
- **WHEN** player exits to title screen
- **THEN** `hasSurface()` returns false (or `inWorld` is false)
- **AND** GL rendering and swap proceed normally

### Requirement: TDR Recovery
The system SHALL handle DXGI swapchain errors gracefully.

#### Scenario: Surface Lost/Timeout
- **WHEN** `get_current_texture()` returns `SurfaceError::Timeout` or device removal
- **THEN** the renderer SHALL attempt to reconfigure the surface
- **AND** fall back to offscreen mode if reconfiguration fails

## MODIFIED Requirements
No existing requirements are modified.

## REMOVED Requirements
No existing requirements are removed.
