# Checklist

## P1a 天空天体
- [ ] sun_angle 全链路打通（Java getSunAngle → JNI → Rust 存储）
- [ ] uniform buffer 128B→192B 扩容，6 个 shader 的 CameraUniform 布局一致
- [ ] 白天可见太阳盘，夜晚可见月亮盘（含月相）与星星，星星夜间淡入
- [ ] 跑图测试通过（用户确认）后才进入 Task 2

## P1b 云层
- [ ] y=192 云平面绘制，fbm 噪声、风卷动画
- [ ] 云色与天空协调、半透明混合正确、不遮挡地形渲染
- [ ] 跑图测试通过（用户确认）后才进入 Task 3

## P1c 半透明分层
- [ ] layer 贯穿 Java/JNI/Rust，ChunkMesh 存储 layer
- [ ] 不透明 pass 只提交 SOLID/CUTOUT，透明 pass 只提交 TRANSLUCENT（透明 pass draw 数显著下降）
- [ ] 玻璃/水半透明混合与深度正确、无闪烁
- [ ] 跑图测试通过（用户确认）后才进入 Task 4

## P1f 粒子贴图
- [ ] particles atlas 捕获并上传（TextureAtlasMixin 扩展）
- [ ] 粒子布局 8→12 float（含 sprite UV），FS 纹理采样
- [ ] topology 修复 PointList→TriangleList（粒子不再画成 1px 点）
- [ ] 跑图测试通过（用户确认）后才进入 Task 5

## P1d 水下效果
- [ ] underwater 标志贯通，水下雾色/雾距生效
- [ ] 潜水时天空 pass 输出水色，出水恢复
- [ ] 跑图测试通过（用户确认）后才进入 Task 6

## P1g 天气
- [ ] 雨天雨滴粒子、雪天雪花粒子、雷暴天空压暗
- [ ] 跑图测试通过（用户确认）后才进入 Task 7

## P1e 实体模型
- [ ] 实体渲染为部件盒体模型（替代彩色包围盒），随移动摆动
- [ ] 跑图测试通过（用户确认）后整个 P1 完成
