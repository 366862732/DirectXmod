# Checklist

- [x] T1: `gradlew genSources` 执行成功，找到 `glfwSwapBuffers` 精确位置 — **GlDevice.presentFrame()**
- [x] T2: Mixin 注入正确，surface 模式下 `glfwSwapBuffers` 被跳过 — **GlDeviceMixin 已注入 presentFrame() HEAD**
- [ ] T2: 非 surface 模式（标题界面）下 swap 正常 — **需用户测试**
- [x] T3: `render_surface()` 处理 `SurfaceError::Timeout` 不崩溃 — **已添加 Timeout 分支日志跳过**
- [x] T3: surface configure 失败时降级到 offscreen 模式 — **设置 surface = None 回退**
- [x] T4: Rust 编译无错误
- [x] T4: Java 编译无错误
- [x] T4: DLL + JAR 正确部署 — **run/mods/gl4dx12-0.1.0.jar**
- [ ] T4: 游戏启动不崩溃 — **需用户测试**
- [ ] T4: 进入世界后 surface 模式启动，连续运行 >5 分钟无 TDR — **需用户测试**
- [ ] T4: 回到标题界面后 surface 模式关闭，不卡死 — **需用户测试**
