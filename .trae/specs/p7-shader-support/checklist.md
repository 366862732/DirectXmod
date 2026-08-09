# Checklist

## Task 1: 类型关键字扩展
- [ ] `parseHlslVariables` 类型关键字列表已扩展（包含 mat4/sampler/uint/bool/double 等）
- [ ] terrain shader cbuffer 解析验证通过（无遗漏、无错误识别）
- [ ] entity shader cbuffer 解析验证通过
- [ ] `gradlew build` 通过

## Task 2: 诊断日志增强
- [ ] `compilePipeline` 成功日志：`[dx12-java] [name] COMPILE OK` 打印
- [ ] `compilePipeline` 失败日志：`[dx12-java] [name] COMPILE FAILED: <detail>` 打印
- [ ] `addToBindGroup` 失败日志打印管线 location 和原因
- [ ] `gradlew build` 通过

## Task 3: 游戏实测验证
- [ ] Iris/Sodium 已禁用（或已知状态已记录）
- [ ] JAR 已部署到测试实例
- [ ] 游戏启动日志无 `Failed to create backend DX12`
- [ ] terrain 管线编译成功（无 COMPILE FAILED）
- [ ] entity 管线编译成功（无 COMPILE FAILED）
- [ ] particle 管线编译成功（无 COMPILE FAILED）
- [ ] clouds 管线编译成功（无 COMPILE FAILED）
- [ ] 地形可见（非黑屏/纯色）
- [ ] 实体可见（非彩色包围盒）

## Task 4: 修复编译失败（如有）
- [ ] 所有官方管线编译无失败
- [ ] 游戏画面正常渲染
