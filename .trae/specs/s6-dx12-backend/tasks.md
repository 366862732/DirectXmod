# S6 Tasks — DX12 后端

## P1 挂点验证（当前阶段）

### T1.1 Rust 侧 dx12-mc crate 骨架
- 在 `rust/` workspace 新建 `dx12-mc` crate（`cargo` 依赖：`windows` crate 的 d3d12/win32 特性，或 `d3d12` crate + winapi）
- JNI 导出：`Java_com_dx12_dx12_Dx12Native_dx12CreateDevice` 等
- 实现：`D3D12CreateDevice` 创建 `ID3D12Device`，读取设备名/特性等级，通过 JNI 返回字符串给 Java

### T1.2 Java 侧 Dx12Backend 骨架
- 包 `com.dx12.dx12`：
  - `Dx12Native`（JNI 声明 + DLL 加载，复用 D3D12Bridge 的 DLL 提取逻辑）
  - `Dx12Backend implements GpuBackend`
  - `Dx12Device implements GpuDeviceBackend`（骨架，方法先抛 UnsupportedOperationException）
  - `Dx12SurfaceBackend implements GpuSurfaceBackend`（骨架）
  - `Dx12CommandEncoderBackend implements CommandEncoderBackend`（骨架）
  - `Dx12RenderPassBackend implements RenderPassBackend`（骨架）

### T1.3 Mixin PreferredGraphicsApi
- `PreferredGraphicsApiMixin`：`@Shadow` 注入 `DX12` 枚举常量；`getBackendsToTry()` 里返回数组含 `Dx12Backend`
- 注册到 `gl4dx12.mixins.json`

### T1.4 验证
- 构建 + 部署 DLL/JAR
- 游戏启动：DX12 device 创建成功日志、Graphics API 选项出现 DX12
- 失败时 fallback GL，游戏正常运行

## 验收标准（P1）
1. 日志出现 `[dx12] Device: NVIDIA ... (D3D_FEATURE_LEVEL_12_1)` 或等效
2. 选择 DX12 后不再走 GL 创建路径（createDevice 被官方调用）
3. 不选择 DX12 时行为与 26.2 原版一致
