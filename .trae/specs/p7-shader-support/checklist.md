# Checklist

## Task 1: 类型关键字扩展
- [x] spvc HLSL 后端原生支持所有 D3D12 SM5.1 类型（无需 parseHlslVariables）
- [x] terrain shader cbuffer 解析验证通过（solid_terrain/translucent_terrain/cutout_terrain COMPILE OK）
- [x] entity shader cbuffer 解析验证通过（entity_solid/entity_translucent_emissive/entity_cutout_z_offset/entity_shadow COMPILE OK）
- [x] `gradlew build` 通过

## Task 2: 诊断日志增强
- [x] `compilePipeline` 成功日志：`[dx12-java] minecraft:pipeline/<name> COMPILE OK (handle=...)` 打印
- [x] `compilePipeline` 失败日志：`[dx12-java] <name> COMPILE FAILED: <detail>` 打印
- [x] `addToBindGroup` 失败日志打印管线 location 和原因
- [x] `gradlew build` 通过

## Task 3: 游戏实测验证
- [x] Iris/Sodium 已安装但未干扰（DX12 后端优先，日志确认 "Using graphics backend DX12"）
- [x] JAR 已部署到测试实例
- [x] 游戏启动日志无 `Failed to create backend DX12`
- [x] terrain 管线编译成功（solid_terrain/translucent_terrain/cutout_terrain COMPILE OK）
- [x] entity 管线编译成功（entity_solid/entity_translucent_emissive/entity_cutout_z_offset/entity_shadow COMPILE OK）
- [x] particle 管线编译成功（opaque_particle/translucent_particle COMPILE OK）
- [x] clouds 管线编译成功（clouds/flat_clouds COMPILE OK）
- [x] debug 管线编译成功（debug_points/debug_filled_box/debug_triangle_fan/debug_quads COMPILE OK）
- [x] splash 管线编译成功（mojang_logo COMPILE OK，红色 splash 渲染正常）
- [x] sky 管线编译成功（sky/end_sky COMPILE OK）

## Task 4: 修复编译失败（如有）
- [x] 所有官方管线编译无失败（日志中 0 条 COMPILE FAILED）
- [x] 游戏画面正常渲染（atlas 上传、draw call 正常）
