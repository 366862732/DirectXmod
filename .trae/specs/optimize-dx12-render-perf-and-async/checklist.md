# Checklist

- [ ] 默认配置启动后，native 层不再输出 `[dx12-jni] dx12PushDescriptors`、`waitQFence`、每帧 `blitSurface`、`pushDesc BIND/CBV/UBO_BIND` 等逐调用/逐帧日志（P30 门控生效）
- [ ] `dbgLog` 的 `%TEMP%\dx12-native.log` 镜像仅在诊断模式开启且复用句柄，无逐行 fopen/fclose
- [ ] 帧渲染期间无 `deviceWaitIdle: enter`，`waitQFence` 为快速非阻塞路径（P30 同步气泡消除）
- [ ] 移除 vbDbg/ubDbg/projDbg 每 60 次 buffer 读回、srv/font/color 纹理读回及 BMP dump（P30）
- [ ] 拉高视距后无明显卡顿，帧率恢复正常
- [ ] 物品栏面板与物品图标正常显示（非黑/非空）（P31）
- [ ] 物品栏人物模型方向正确（头朝上，不颠倒），主世界 3D 渲染方向不受影响（P31）
- [ ] 视距 10+ 时远处区块正常显示，非全黑，游戏不再自动降视距（P32）
- [ ] P33 多线程异步渲染：FrameBuilder → 无锁队列 → Worker Pool Bundle 录制 → 主执行器 → GPU 4-5 帧飞行 + Fence 异步通知已落地并接入渲染循环
- [ ] 多线程渲染下无黑屏、DXGI_ERROR_DEVICE_REMOVED、描述符堆越界、use-after-free；窗口 resize 正常（P33）
- [ ] 完整帧（区块+实体+GUI）CPU 帧时间较单线程基线显著下降（P33 性能验证）
