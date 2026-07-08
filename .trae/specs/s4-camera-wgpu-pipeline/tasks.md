# Tasks: Camera Data + wgpu Rendering Pipeline

- [x] Task 1: 移除 Rust C:\tmp\ 文件 I/O 调试日志
  - [x] `wgpu-mc-jni/src/lib.rs`: 删除 `nativeInit` 中的 `std::fs::write("C:\\tmp\\wgpu_mc_init.txt", ...)`
  - [x] `wgpu-mc-jni/src/lib.rs`: 删除 `nativeSetWindow` 中的 `std::fs::write("C:\\tmp\\wgpu_setwindow_entered.txt", ...)` / `wgpu_mc_initialized.txt` / `wgpu_mc_error.txt` / `wgpu_debug.txt`
  - [x] `wgpu-mc-jni/src/lib.rs`: 删除 `nativeRenderFrame` 中的 debug_log 构造和 `std::fs::write("C:\\tmp\\wgpu_debug.txt", ...)`
  - [x] `wgpu-mc-jni/src/lib.rs`: 重构 `nativeSetWindow`，移除 closure 包装，直接写主流程
  - [x] 仅在 Rust 端构建，确认无编译错误

- [x] Task 2: 新增 nativeUpdateCamera JNI 方法
  - [x] `wgpu-mc-jni/src/lib.rs`: 新增 `Java_com_dx12_D3D12Bridge_nativeUpdateCamera(env, class, matrix: jfloatArray)` — 接收 16 个 float 的 MVP 矩阵数组
  - [x] `wgpu-mc/src/lib.rs`: 新增 `set_camera(&mut self, mvp: [[f32; 4]; 4])` 方法，将 MVP 矩阵存入 WmRenderer
  - [x] `D3D12Bridge.java`: 新增 `public static native void nativeUpdateCamera(float[] matrix)` 声明
  - [x] `D3D12Bridge.java`: 新增 `public static void updateCamera(float[] matrix)` 便利方法（检查 initialized，转发 native 调用）
  - [x] Rust 端构建成功

- [x] Task 3: Dx12Mod tick callback 中提取摄像机数据
  - [x] 新增 `GameRenderer` 和 `Camera` 的 import
  - [x] 在 tick callback 的节流判断之后、renderFrame 之前，提取摄像机 view-projection 矩阵：
    - `MinecraftClient.getInstance().gameRenderer.getCamera()` 获取 Camera 对象
    - 使用 Camera 的 `getPos()`、`getPitch()`、`getYaw()` 构造 view 矩阵
    - 使用 `Matrix4f` 构造 projection 矩阵（FOV 70°, 宽高比 width/height, near 0.05, far 1000）
    - 计算 MVP = projection * view
    - 展开为 float[16] 调用 `D3D12Bridge.updateCamera(mvp)`
  - [x] 对 camera 为 null 做保护

- [x] Task 4: 重构 WmRenderer 为真正的 wgpu GPU 渲染管线
  - [x] 添加 bytemuck derive 到 Vertex 结构体
  - [x] 添加 WGSL shader 源码（vertex: MVP uniform + position/color input, fragment: 色彩直出）
  - [x] `WmRenderer` 新增字段: `pipeline`, `vertex_buffer`, `index_buffer`, `uniform_buffer`, `bind_group`, `staging_buffer`, `texture`, `sampler`, `camera_mvp: [[f32;4];4]`
  - [x] `create()` 中初始化所有 wgpu 资源（pipeline + buffers + texture 只需创建一次）
  - [x] `render_frame()` 替换 CPU fill
  - [x] `resize()` 中同步重建纹理和 uniform buffer（尺寸变化时）

- [ ] Task 5: 编译并验证
  - [x] `cargo build --release` 成功
  - [x] 复制 DLL 到版本目录（已复制到 fabric/src/main/resources/）
  - [x] `gradle build` 成功 (BUILD SUCCESSFUL, 7 tasks executed)
  - [ ] `./gradlew runClient` 启动，观察蓝色覆盖层是否变为彩色三角形
  - [ ] 移动鼠标视角，三角形在屏幕上位置应有变化

# Task Dependencies
- Task 2 depends on Task 1 (干净的代码基础)
- Task 3 depends on Task 2 (需要 nativeUpdateCamera 声明)
- Task 4 depends on Task 2 (WmRenderer 需要 set_camera 方法)
- Task 5 depends on Task 1, 2, 3, 4 (全部完成后编译测试)
