# Checklist: Camera Data + wgpu Rendering Pipeline

- [x] `nativeInit` 不再写 `C:\tmp\wgpu_mc_init.txt`
- [x] `nativeSetWindow` 不再写任何 `C:\tmp\` 文件
- [x] `nativeRenderFrame` 不再构造 debug_log 字符串，不写 `C:\tmp\wgpu_debug.txt`
- [x] `D3D12Bridge.java` 中有 `nativeUpdateCamera(float[] matrix)` native 声明
- [x] `D3D12Bridge.java` 中有 `updateCamera(float[] matrix)` 便利方法
- [x] `Dx12Mod.java` tick callback 中正确提取 `GameRenderer.getCamera()`
- [x] MVP 矩阵通过 `D3D12Bridge.updateCamera(mvp)` 传给 Rust
- [x] `WmRenderer` 有 `camera_mvp` 字段和 `set_camera()` 方法
- [x] `WmRenderer` 有持久化的 `pipeline`、`vertex_buffer`、`staging_buffer`（不每帧重创）
- [x] `render_frame()` 走完整的 wgpu render_pass + texture_to_buffer + map_read 流程
- [x] `resize()` 同步重建纹理和 uniform buffer
- [x] `render_frame()` 返回的 byte[] 大小 = width * height * 4
- [x] `cargo build --release` 编译无错误
- [x] `gradle build` 编译无错误 (BUILD SUCCESSFUL)
- [ ] 游戏启动不 crash（无 hs_err 文件）
- [ ] 渲染覆盖层显示彩色三角形（不再是纯蓝色），移动视角时三角形位置有变化
