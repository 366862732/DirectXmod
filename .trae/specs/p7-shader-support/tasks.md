# Tasks

- [ ] Task 1: 扩展 `parseHlslVariables` 类型关键字
  - [ ] 1.1 在 `Dx12IntermediaryShaderModule.java` 的 `parseHlslVariables` 中，将类型关键字列表从 `{"void", "float", "int", "half"}` 扩展为完整的 D3D12 SM5.1 基础类型集合：`{"void", "float", "half", "int", "uint", "bool", "double", "mat2", "mat3", "mat4", "sampler", "Texture", "AppendStructuredBuffer", "ConsumeStructuredBuffer"}`
  - [ ] 1.2 验证 `parseHlslVariables` 对 terrain/entity shader 的 spvc HLSL 输出解析正确（无遗漏变量、无错误类型识别）
  - [ ] 1.3 `cd fabric && ..\gradlew.bat build` 通过

- [ ] Task 2: 增强 `compilePipeline` 诊断日志
  - [ ] 2.1 在 `Dx12Device.java` 的 `compilePipeline` 方法中，为每个管线的编译结果添加结构化日志：
    - 成功：`[dx12-java] [name] COMPILE OK`（当前仅 gui/debug 打印，改为所有管线打印，但仅在日志级别 DEBUG 时）
    - 失败：`[dx12-java] [name] COMPILE FAILED: <pipeline location> <detail>`（始终打印）
  - [ ] 2.2 在 `Dx12ShaderCompiler.java` 的 `compile()` 方法中，为 `addToBindGroup` 失败添加日志（打印管线 location 和失败原因）
  - [ ] 2.3 `cd fabric && ..\gradlew.bat build` 通过

- [ ] Task 3: 部署到测试环境并验证游戏运行
  - [ ] 3.1 确认 Iris/Sodium 已禁用（.bak 后缀）；若未禁用，记录状态并告知用户手动操作
  - [ ] 3.2 构建最新 JAR：`cd fabric && ..\gradlew.bat build`
  - [ ] 3.3 部署 JAR 到测试实例：`Copy-Item fabric\build\libs\gl4dx12-0.1.0.jar "d:\.minecraft\versions\xiaozi craft 26.2-Happy-1st-Anniversary-to-xiaozi-craft Extra\mods\" -Force`
  - [ ] 3.4 启动游戏，进入世界，收集 P7 shader 编译日志
  - [ ] 3.5 验证：terrain/entity/particle/clouds 管线无编译失败；地形和实体可见

- [ ] Task 4: 修复发现的编译失败问题（如有）
  - [ ] 4.1 分析 Task 3 收集的日志，定位任何 COMPILE FAILED 错误
  - [ ] 4.2 针对每个失败原因实施修复（参考 Vulkan 官方实现对比）
  - [ ] 4.3 重新构建并部署，验证修复生效
  - [ ] 4.4 重复直到所有管线编译通过

# Task Dependencies

- [Task 1] 无依赖，可与 [Task 2] 并行
- [Task 2] 无依赖，可与 [Task 1] 并行
- [Task 3] depends on [Task 1][Task 2]（需要最新代码构建）
- [Task 4] depends on [Task 3]（需先收集日志才能定位问题）
