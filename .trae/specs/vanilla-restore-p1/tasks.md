# Tasks

> 实施节奏：每完成一项 → `cargo test` 通过 + 重建部署 → 自检 → **暂停等待用户跑图测试** → 用户确认后再进入下一项。严格按顺序执行，不得跳项或连续推进。

- [ ] Task 1: P1a 太阳/月亮/星星天空天体
  - [ ] 1.1 Java：`Dx12Mod.updateSky`（L315-326）用 `mc.level.getSunAngle(1.0f)` 替代硬编码 `sunAngle=0.0f`，horizon 色传入 `nativeUpdateSky`
  - [ ] 1.2 JNI：`nativeUpdateSky`（wgpu-mc-jni/src/lib.rs:214-229）消费 horizon_* 与 sun_angle，存入 renderer
  - [ ] 1.3 Rust：`WmRenderer` 增 `sky_color_horizon`/`sun_angle` 字段（`set_sky_color` 扩展）；uniform buffer 128B→192B 扩容（`write_camera_uniform` lib.rs:433-458 + 全部 6 个 shader 的 `CameraUniform` 同步，保持内存布局一致）
  - [ ] 1.4 Rust：`SKY_SHADER_SRC`（lib.rs:386-418）fs 增加：太阳盘（`dot(normal, sun_dir)` smoothstep 盘面 + 光晕）、月亮盘（月相由 sun/moon 夹角）、星星（方向 hash 噪声，`daylight` 因子淡入）；夜间 `sky_color_top` 仍取渐变（stars 叠加其上）
  - [ ] 1.5 验证：`cargo test -p wgpu-mc` 通过；DLL/JAR 重建部署；自检日志无 panic；**暂停等用户测试**（白天见太阳、夜晚见月亮与星星、相位正确）

- [ ] Task 2: P1b 云层渲染
  - [ ] 2.1 Rust：`SKY_SHADER_SRC` 或独立 cloud shader 程序化 fbm 噪声云（3 层 octave 求和），云平面几何 y=192（可复用 dome 网格或新平面 mesh，跟随相机 x/z）
  - [ ] 2.2 云滚动动画：uniform 增 `cloud_time`（或复用 camera_pos.x/z 随时间偏移），风卷方向固定 +x
  - [ ] 2.3 云色：以 `sky_color_top` 为底，云密度调制亮度，alpha 混合；方块 pass 之前、天空 pass 之后绘制（depth 不写）
  - [ ] 2.4 验证：`cargo test` 通过 + 重建部署 + 自检；**暂停等用户测试**（晴天白云、卷动自然、不遮挡地形渲染错误）

- [ ] Task 3: P1c 半透明分层渲染
  - [ ] 3.1 Java：`SectionCompilerMixin.onCompileReturn`（L53-67）传 `entry.getKey().ordinal()`（SOLID=0/CUTOUT=1/TRANSLUCENT=2）；`D3D12Bridge.uploadChunkMesh` 增 layer 参数
  - [ ] 3.2 JNI：`nativeUploadChunkMesh`（wgpu-mc-jni/src/lib.rs:84-127）增 `jint layer`
  - [ ] 3.3 Rust：`ChunkMesh`（lib.rs:774-786）增 `layer: u8`；`upload_chunk_mesh`（lib.rs:2068-2360）接收 layer 并存储
  - [ ] 3.4 Rust：`collect_visible_draws`（lib.rs:537-563）增 layer 过滤参数；`draw_chunks`（lib.rs:2879-2968）Pass1 只提 SOLID/CUTOUT、Pass2 只提 TRANSLUCENT（各自收集 draws 或过滤 indirect）
  - [ ] 3.5 验证：`cargo test` 通过 + 重建部署 + 自检；**暂停等用户测试**（玻璃/水半透明正确、透明 pass draw 数显著下降、无遮挡闪烁）

- [ ] Task 4: P1f 粒子贴图
  - [ ] 4.1 Java：`TextureAtlasMixin` 扩展捕获 `textures/atlas/particles.png`（当前硬编码 blocks，L70/L155）；新增 `uploadParticleAtlas`
  - [ ] 4.2 Java：粒子提取（Dx12Mod.java L393-469）读取 `Particle` 的 sprite UV（`getSprite().getU0/U1/V0/V1`）与 `rCol/gCol/bCol/alpha/quadSize`，布局 8→12 float：`[x,y,z,size,r,g,b,a,u0,v0,u1,v1]`
  - [ ] 4.3 JNI：`nativeSetParticles` 按 12 float/粒子解析（JNI L542-564 同步）
  - [ ] 4.4 Rust：`ParticleVertex`（lib.rs:73-97）加 `uv: [f32;4]`（44B，ATTRIBS 同步）；`build_particle_vertices`（L3079-3117）填 UV；粒子管线绑定粒子 atlas（新 BGL 或复用 chunk BGL 扩展）；FS（L370-379）改 `textureSample` + alpha 阈值 discard
  - [ ] 4.5 修复 topology：`ensure_particle_pipeline`（L3354-3388）`PointList→TriangleList`（现 bug：粒子画成 1px 点）
  - [ ] 4.6 验证：`cargo test` 通过 + 重建部署 + 自检（粒子不再画成点）；**暂停等用户测试**（爆炸/火焰/烟粒子显示 MC 纹理）

- [ ] Task 5: P1d 水下效果
  - [ ] 5.1 Java：`Dx12Mod` tick 检测 `mc.player.isUnderWater()`（头部在水中），水下时传水色雾（`nativeUpdateFog` 蓝色高密度）+ 新增 `nativeSetUnderwater(boolean)`（或复用 uniform 标志）
  - [ ] 5.2 JNI：新增/扩展 underwater 接口
  - [ ] 5.3 Rust：uniform 增 `underwater: f32`；天空 shader 在水下时输出水色（替代穹顶渐变）；方块/粒子 fog 增强
  - [ ] 5.4 验证：`cargo test` 通过 + 重建部署 + 自检；**暂停等用户测试**（潜水水色雾、出水恢复、无天空闪烁）

- [ ] Task 6: P1g 天气（雨雪）
  - [ ] 6.1 Java：按 `level.isRaining()/isThundering()` 与生物群系降雪判断，生成雨/雪粒子数据（雨：下落长条、高速；雪：缓慢飘落、白色），复用 `setParticles` 通道或新增天气粒子通道（上限放宽）
  - [ ] 6.2 天空联动：雷暴时 sky/fog 压暗（现有 fog 密度分支增强，雨雪粒子时雾色微灰）
  - [ ] 6.3 Rust：若用独立通道，新增雨/雪粒子 VB 与绘制（复用粒子管线/着色逻辑）
  - [ ] 6.4 验证：`cargo test` 通过 + 重建部署 + 自检；**暂停等用户测试**（雨天落雨、雪天飘雪、雷暴压暗）

- [ ] Task 7: P1e 实体模型（基础版）
  - [ ] 7.1 Java：新增 `EntityRenderDispatcherMixin`（或扩展 LevelRenderer 拦截），从 EntityRenderer 提取模型部件层级（`EntityModel` 的 `ModelPart` 树：盒体尺寸/偏移/枢轴），缓存模型定义（按实体类型 key）
  - [ ] 7.2 Java：每 tick 收集实体 pose：位置/朝向 + 部件旋转角（`ModelPart` 当前 rotation 或按 tick 相位计算摆动），组装部件矩阵（或发送部件旋转角）
  - [ ] 7.3 JNI：新增 `nativeSetEntityModels(...)`（模型定义一次性上传）与扩展 `nativeSetEntities(...)`（每实体含 pose/部件角）
  - [ ] 7.4 Rust：模型部件盒体几何（实例化或逐部件 draw），实体管线替换彩色包围盒（`build_entity_vertices`/`draw_entities` L2973-3074），支持 per-part 矩阵
  - [ ] 7.5 颜色：保留按实体类型着色（现有 hash 色），纹理贴图留待后续
  - [ ] 7.6 验证：`cargo test` 通过 + 重建部署 + 自检（实体显示为部件模型、随移动摆动）；**暂停等用户测试**

# Task Dependencies

- [Task 1] 无依赖（uniform 扩容是后续云/水下的基础，故先行）
- [Task 2] 依赖 [Task 1]（复用扩容后 uniform 与天空色）
- [Task 3] 无依赖（独立核心修复，可与 1/2 并行，但按用户节奏串行执行）
- [Task 4] 依赖 [Task 1] 的 uniform 扩容（新增纹理绑定与粒子 shader 同步 CameraUniform）
- [Task 5] 依赖 [Task 1]（uniform 增 underwater 标志）
- [Task 6] 依赖 [Task 1]（天空联动）与 [Task 4]（粒子通道复用）
- [Task 7] 依赖 [Task 1]（shader uniform 同步）；独立性最强，工程量最大，放最后
