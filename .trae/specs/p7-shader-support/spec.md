# P7 — 完整 Shader 支持 Spec

## Why

P6（GUI 首帧渲染）已完成，核心 shader 编译链（GLSL→SPIR-V→HLSL→DXBC→PSO）已验证通。但 Minecraft 26.2 官方包含 **20+ RenderPipeline**（terrain/entity/particle/clouds/debug_* 等），当前自检仅验证了 `core/gui` 和 `core/position_tex_color`。P7 的目标是确保所有官方管线在 D3D12 后端下编译正确、渲染正确，并移除 Iris/Sodium 冲突对测试的干扰。

## What Changes

- **扩展 `parseHlslVariables` 类型关键字**：当前仅支持 `void/float/int/half`，terrain/entity shader 中出现的 `mat4`/`sampler2D`/`uint`/`double` 等类型会导致变量行被跳过，影响 cbuffer 内描述符布局的诊断精度（低风险，但需完善）
- **验证所有官方管线编译**：确保 terrain/entity/particle/clouds/debug_* 等管线在 D3D12 后端下编译成功
- **禁用 Iris/Sodium 冲突模组**：这两个模组接管渲染管线，与 Dx12Backend 冲突，导致测试环境无法验证 D3D12 渲染效果
- **收集 P7 运行日志**：启动游戏后收集 shader 编译日志，确认所有管线无编译失败

## Impact

- Affected specs: s6-dx12-backend（P7 实施阶段）、perf-polish-vanilla-restore（原版画面还原依赖 shader 支持）
- Affected code:
  - `fabric/src/main/java/com/dx12/dx12/Dx12IntermediaryShaderModule.java`：`parseHlslVariables` 类型关键字扩展
  - `fabric/src/main/java/com/dx12/dx12/Dx12ShaderCompiler.java`：新增诊断日志（所有管线编译状态）
  - `fabric/src/main/java/com/dx12/dx12/Dx12Device.java`：`compilePipeline` 失败日志增强
- 不改动：native 层（dx12_mc.dll）、Mixin、配置层

## MODIFIED Requirements

### Requirement: Shader 编译诊断日志
`compilePipeline` 方法 SHALL 对每个管线的编译结果（成功/失败）打印结构化日志，包含管线 location、阶段（vertex/fragment）、错误信息（如有）。

#### Scenario: 管线编译失败
- **WHEN** 某个 shader 管线编译失败（HLSL 语法错误 / D3DCompile 失败）
- **THEN** 日志打印 `[dx12-java] [name] COMPILE FAILED: <detail>`，包含完整错误信息，不再静默跳过

#### Scenario: 管线编译成功
- **WHEN** 某个 shader 管线编译成功
- **THEN** 日志打印 `[dx12-java] [name] COMPILE OK`（仅 gui/debug 管线，其他管线不打印以控制日志量）

## ADDED Requirements

### Requirement: 类型关键字完整支持
`parseHlslVariables` SHALL 支持 D3D12 HLSL SM5.1 规范中所有常见基础类型（`void/float/half/int/uint/bool/double`、`vec2-4/mat2-4` 等价类型、`sampler*` 等采样器类型前缀），确保 cbuffer 内描述符布局的准确诊断。

#### Scenario: terrain shader cbuffer 解析
- **WHEN** terrain.vsh 的 spvc HLSL 输出包含 `cbuffer LightUniforms { sampler2D lightmap; ... }`
- **THEN** `parseHlslVariables` 正确识别 `sampler2D` 为类型、`lightmap` 为变量名，不报错

### Requirement: 游戏实测验证
系统 SHALL 在禁用 Iris/Sodium 的测试环境下启动游戏，确认 terrain/entity/particle/clouds 管线编译无失败，窗口正常呈现游戏画面。

#### Scenario: 首次进入世界
- **WHEN** 游戏启动后进入一个已加载区块的世界
- **THEN** 日志中无 `Couldn't compile pipeline minecraft:pipeline/core/terrain` 等编译失败错误；地形可见、实体可见（即使纹理缺失，几何正确）

#### Scenario: 天气效果
- **WHEN** 下雨或下雪
- **THEN** particle 管线编译成功，雨滴/雪花粒子可见

## REMOVED Requirements

（无删除项）
