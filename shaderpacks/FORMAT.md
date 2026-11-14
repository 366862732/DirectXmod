# gl4dx12 HLSL 光影包格式 v1

> **这不是 Iris/OptiFine 格式。** 着色器是 HLSL（SM 5.1），pass 链由 `shaderpacks/<包>/shaders/pipeline.json` 显式声明。
> 真正的 Iris/OptiFine 包（GLSL `.fsh/.vsh` + 文件名约定）无法加载；HLSL 包缺少 `pipeline.json` 也会加载失败。
>
> 本文对应实现（2026-10-18 核对）：
> - `fabric/src/main/java/com/xgdt/dx12/shaderpack/ShaderPack.java` — 包结构与读取
> - `fabric/src/main/java/com/xgdt/dx12/shaderpack/ShaderPackManifest.java` — `pipeline.json` 解析
> - `fabric/src/main/java/com/xgdt/dx12/shaderpack/ShaderPackManager.java` — 扫描 / 启停 / 选项持久化
> - `fabric/src/main/java/com/xgdt/dx12/dx12/Dx12ShaderPackRenderer.java` — uniform 注入、pass 执行、光追、内置 pass
> - `fabric/src/main/java/com/xgdt/dx12/dx12/OfflineRender.java` — ITRP 风格离线渲染

---

## 1. 放置、识别与配置

- 位置：`<游戏目录>/shaderpacks/`，支持 `MyPack.zip` 与已解压目录 `MyPack/` 两种形式。
- `id` = 文件名（去掉 `.zip`）或目录名；显示名来自 `shaders.properties` 的 `name`。
- 无效包会在光影界面列表中显示原因；没有 `pipeline.json` 一律无法加载。
- 启用状态与滑条值保存在 `<游戏目录>/config/gl4dx12-shaderpacks.properties`：

  ```properties
  enabled=true
  active=Sunlit
  option.Sunlit.HAZE=0.25555557
  ```

- UI 入口：**视频设置 → 光影**。底部有「启用开关 / 重载 / 打开文件夹 / 完成」。
  改完包后按「重载」即可生效（会重建全部管线与中间纹理，无需重启游戏）。
- 每个设置项：滑条拖动 / 点击开关，实时写入 uniform 并持久化到上面的配置文件。

## 2. 包内结构

```
MyPack.zip
└── shaders/
    ├── shaders.properties     # 元数据 + option.* 设置项
    ├── pipeline.json          # pass 链（自定义格式）
    └── **/*.hlsl              # 每个 pass 一个单文件 HLSL（不支持 #include）
```

- 加载器同时接受带 `shaders/` 前缀与不带前缀的写法：`"fragment": "shaders/composite/sky.hlsl"` 或 `"composite/sky.hlsl"` 都可以。
- **打包注意**：zip 条目名必须用正斜杠 `/`（Windows 的 `Compress-Archive` 会写成反斜杠，导致加载器找不到文件）；建议用手工打包（见 §11 的脚本）。文本用 UTF-8（无 BOM）。

## 3. shaders.properties

```properties
name=Sunlit Realistic
author=xgdt
version=0.1.0
description=一行简介

option.BRIGHTNESS=1.0
option.BRIGHTNESS.min=0.6
option.BRIGHTNESS.max=1.6
option.BRIGHTNESS.comment=整体亮度
```

- 元数据：`name` / `author` / `version` / `description`。
- 设置项：`option.<NAME>=默认值`（数值；写 `true`/`false` 会显示为复选框），`.min`/`.max` 是滑条范围，`.comment` 是说明文字。
- **声明顺序 = uniform 顺序**：第 n 个设置 → `Options[n/4][n%4]`；最多 24 个（`Options[6]`），之后的项 UI 仍显示但不会传入着色器。
- 选项名按大写存储；**顺序不可随意调整**，调整顺序会让所有值错位。

## 4. pipeline.json

```json
{
  "version": 1,
  "replaceVanillaSky": true,
  "passes": [
    { "name": "sky",
      "fragment": "shaders/composite/sky.hlsl",
      "inputs": ["scene_color", "scene_depth"],
      "output": "fx:0" },
    { "name": "shadow", "type": "raytrace",
      "output": "fx:1", "params": [1.0, 512.0, 0.06, 0.0] },
    { "name": "final",
      "fragment": "shaders/final/final.hlsl",
      "inputs": ["fx:0", "fx:1", "scene_depth"],
      "output": "scene_color", "params": [0.85, 0.0, 0.0, 0.0] }
  ]
}
```

根字段：

| 字段 | 默认 | 说明 |
|---|---|---|
| `version` | 1 | 目前支持 ≤ 1，更高版本拒绝加载 |
| `replaceVanillaSky` | false | true = 屏蔽原版方块云（原版天空渐变仍会先绘制，天空像素由你的 pass 覆盖） |
| `passes` | 必填 | pass 数组，按顺序执行 |

pass 字段：

| 字段 | 默认 | 说明 |
|---|---|---|
| `name` | `"pass"` | 名称（日志 / 管线缓存用） |
| `type` | `"composite"` | `composite`（HLSL 全屏 pass）、`compute`（HLSL 计算 pass）或 `raytrace`（内置光追） |
| `vertex` | 内置 | 可选自定义顶点着色器路径（仅 composite） |
| `fragment` | — | HLSL 源码路径：composite 为片元着色器（必填），compute 为计算着色器（必填） |
| `inputs` | `[]` | 输入端点数组，按顺序绑定 `t0..tN-1` / `s0..sN-1`（compute 不读纹理，忽略） |
| `output` | `"scene_color"` | 输出端点：`scene_color` 或 `fx:N`；**compute 强制为 null**（不产出图像） |
| `params` | `[0,0,0,0]` | 最多 4 个浮点，透传到该 pass 的 `PassParams`（compute 为 4 个 `b0` root constants） |
| `buffers` | `[]` | SSBO 声明数组（见下），composite 与 compute 均可使用 |
| `groups` | `[0,0,0]` | compute 的线程组数；全 0 = 按视口推导 `(w+63)/64, (h+63)/64, 1` |
| `enabled` | true | false = 整个 pass 跳过 |

`buffers[]` 元素：

| 字段 | 默认 | 说明 |
|---|---|---|
| `name` | 必填 | 名字；**同名即同一个 buffer，跨 pass 共享** |
| `stride` | 16 | 结构化元素字节数（如 32 = 两个 `float4`） |
| `count` | 1 | 元素个数 |
| `access` | `"uav"` | `"uav"` = 可读写（`RWStructuredBuffer`，DEFAULT 堆）；`"srv"` = 只读（`StructuredBuffer`） |

> 同一份数据要「compute 写、composite 读」时，两处必须用**同一个 `name`**；写入方声明 `"access": "uav"`，读取方声明 `"access": "srv"`（框架复用同一块 GPU buffer）。

> JSON 严格解析，不支持注释。

## 5. 端点与执行模型

只有以下端点：

- **`scene_color`**：主渲染目标，已包含原版世界渲染结果。数值是**显示参考（sRGB）空间**，做 HDR 运算请先自行线性化（Sunlit 用 `pow(c, 2.2)`）。
- **`scene_depth`**：主深度缓冲（单通道浮点，用 `Texture2D<float>` 采样，无比较采样器）。**reverse-Z**：天空 / 远平面 = 0，几何 > 0。
- **`fx:N`**：中间纹理，RGBA16F，按需分配、随窗口尺寸自动重建。**每帧不会自动清空**——只能读本帧更早的 pass 已经写入的内容。
  - `fx:6` / `fx:7` 被离线渲染功能占用（乒乓累积，仅离线模式下），自定义 pass 请避开。
- **`history_color`**：**上一帧整条链执行完的最终画面**（RGBA16F）。任何 pass 的 `inputs` 里出现它时框架才分配（首帧清为不透明白），并在链尾自动捕获本帧结果供下一帧使用。用于时域混合 / 运动模糊。

执行模型：

- 每帧顺序：世界渲染完成 → 依序执行 passes（跳过 `enabled=false`）→（若最后一个 pass 没写 `scene_color`）内置拷贝 pass 把其输出贴回主目标 → GUI/HUD（不受光影链影响）。
- `composite` pass 是一次**全屏三角形 draw，整体覆盖输出**（无混合、无深度测试）。
- `compute` pass 发起一次 `Dispatch`，**不产出图像、不改变“当前输出”**，只读写 `buffers[]` 声明的 SSBO。
- 某 pass 编译失败时该帧链在其位置中止（错误见日志 `[shaderpack]` 前缀）。
- 每帧最多 8 个 pass 拥有独立 uniform 槽位：**建议包内 pass 数 ≤ 8**（compute pass 也计在内）。

## 6. pass 类型

### composite

- 缺省顶点着色器是内置全屏三角形（无顶点缓冲，`SV_VertexID` 生成；uv 左上角为 `(0,0)`，已处理 D3D Y 方向）。
- 片元入口固定 `main`，返回 `SV_TARGET`；可指定 `vertex` 用自定义 VS（入口同为 `main`）。
- 编译：片元 `ps_5_1`、顶点 `vs_5_1`（D3DCompile，直接出 DXBC）。

### compute

- 入口固定 `main`，编译目标 `cs_5_1`；`fragment` 字段承载计算着色器源码路径。
- **不注入 §7 的 `ShaderPackUniforms`**。`register(b0)` 是 4 个 32bit root constants（= `params[0..3]`，`Float.floatToRawIntBits` 传入），需要时自行声明：

  ```hlsl
  cbuffer ComputeParams : register(b0) { float4 Params; };
  ```

- 绑定：只读 SSBO 按 `buffers[]` 中 `access:"srv"` 的出现顺序占 `register(t0..)`；可读写 SSBO 按 `access:"uav"` 的出现顺序占 `register(u0..)`。**必须用 `StructuredBuffer<T>` / `RWStructuredBuffer<T>`，且 `T` 的字节数等于声明的 `stride`**。
- UAV 只支持结构化 buffer；**不能写 `fx:N` 纹理**，compute 的 SRV 也**读不到任何纹理**。需要计算着色器结果参与画面时，写 SSBO，再由后续 composite pass 用 `access:"srv"` 读取。
- `groups` 全 ≤ 0 时按视口推导 `(width+63)/64, (height+63)/64, 1`；显式给出时以 `[x,y,z]` 直接 `Dispatch`。
- `srv` + `uav` 总数为 0 时该 pass 打警告并跳过（不报错）。
- 访问已由 composite pass 声明过的同名 buffer 时，框架复用同一块 GPU buffer，数据可直接跨 pass 传递。

### raytrace（内置 DXR 光追，不需要 HLSL 文件）

- 固定输入：本帧 `scene_depth` + 世界 TLAS（`inputs` 会被忽略）；`output` 必须是 `fx:N`（UAV 写入，RGBA16F），否则该 pass 被跳过并打警告。
- 内置行为（Sunlit 依赖的语义）：由深度重建世界坐标、屏幕空间差分估计法线 → 天空 / 无效像素写 `1`；太阳在地平线下写 `1`；否则沿太阳方向发射阴影射线：**命中 = 0、未命中 = 1**（1 = 受光）写入 RGB。
- `PassParams`（被内置着色器解释）：`y` = 最大射线距离；`z` = 沿法线的偏移量（防自交）。Sunlit 传 `[1.0, 512.0, 0.06, 0.0]`。
- DXR 不可用 / 场景无几何时：输出被清为全 `1`（全受光），不会黑屏。
- 副作用：包内存在 raytrace pass 时会开启世界几何捕获（区块几何重新上传建加速结构）；移除 raytrace pass 后自动关闭。

## 7. 内置 uniform（b0，框架自动注入）

**框架会在编译前把下面这段声明自动拼接到每个着色器文件顶部；包内不要自己再声明 b0（重复声明 = 编译报错）。**

```hlsl
cbuffer ShaderPackUniforms : register(b0)
{
    float4 Viewport;        // xy = 宽高(像素), zw = 1/宽高
    float4 Time;            // x = 秒(离线渲染时冻结; 数值很大, 周期动画自行 fmod), y = 帧序号, z = 帧间隔秒, w = 世界时间秒(游戏刻/20)
    float4 CameraPosition;  // xyz = 相机世界坐标, w = 1
    float4 SunDirection;    // xyz = 太阳方向单位向量(同原版), w = 太阳高度
    float4 PassParams;      // 本 pass 的 params[0..3]
    float4 Sky;             // x = 太阳角(rad), y = 星光亮度, z = 天气亮度系数, w = 天空类型(0=无天空/1=主世界/2=末地)
    float4 SkyColor;        // rgb = 原版天空色, a = 末地闪光强度
    float4 SunsetColor;     // rgb = 日出日落色, a = 1
    float4 Misc;            // x = 视距, y = 云层高度, z = 月亮角(rad), w = 星空角(rad)
    float4x4 InvViewProj;   // inverse(projection * viewRotation)：把 (ndc, depth, 1) 变换到“以相机为原点的世界系”；+ CameraPosition.xyz = 绝对世界坐标
    float4x4 ViewRotation;  // 世界 -> 相机（仅旋转）
    float4 Options[6];      // 24 个设置值（按 shaders.properties 声明顺序）
    float4x4 ViewProj;      // 本帧 projection * viewRotation
    float4x4 PrevViewProj;  // 上一帧的 ViewProj（首帧 = 本帧）
    float4 PrevCameraPosition; // xyz = 上一帧相机世界坐标, w = 1（首帧 = 本帧）
};
```

- 纹理 / 采样器：按 manifest `inputs` 顺序对应 `register(t0..tN-1)` 与 `register(s0..sN-1)`；采样器固定**线性过滤 + CLAMP**，不可配置。
- **只能声明恰好等于 `inputs` 数量的 t/s**——多声明会在绑定阶段失败。若在 `inputs` 之后还要读 SSBO（`access:"srv"`），其 `t` 编号接续纹理编号（`inputs` 长度 + 序号）。
- NDC 约定：`ndc = float2(uv.x * 2 - 1, 1 - uv.y * 2)`。
- 时域重投影（运动模糊 / 时域累积）标准写法：

  ```hlsl
  float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
  float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));  // 相机相对世界
  float3 rel  = rel4.xyz / max(rel4.w, 1e-4);
  float3 prevRel = rel + CameraPosition.xyz - PrevCameraPosition.xyz;
  float4 prevClip = mul(PrevViewProj, float4(prevRel, 1.0));
  float2 prevUv = float2(prevClip.x / prevClip.w * 0.5 + 0.5,
                         0.5 - prevClip.y / prevClip.w * 0.5);
  ```

## 8. 能力边界（写包前先知道）

- **只有后处理**：没有 gbuffers / 几何阶段，拿不到法线、材质、方块 id；地形信息只能从深度反推。
- **无自定义纹理输入**：除端点外不能带 `noisetex` 之类的图，噪声请程序化生成。
- **无 `#include`**：每个 `.hlsl` 必须单文件自包含；跨 pass 共用代码只能复制。
- **无阴影贴图 / 比较采样器**：精细阴影只能用 `raytrace` pass 或屏幕空间近似。
- **有 compute / SSBO**：`type:"compute"` 发起 `Dispatch`，读写结构化 buffer（`buffers[]`，跨 pass 同名共享）。但 UAV **只能写结构化 buffer，不能写 `fx:N` 纹理**，compute 也读不到纹理；想在画面上用计算结果，请写 SSBO 再由 composite pass 读。
- **无 gbuffers / 无运动矢量**：屏幕空间反射需自己从深度重建法线；运动模糊用 `ViewProj` / `PrevViewProj` / `PrevCameraPosition` 重投影（见 §7）。
- 编译 / 运行错误看日志 `[shaderpack]` 前缀；编译错误给的行号是“注入头 + 你的文件”合并后的行号。

## 9. 离线渲染（ITRP 风格，内置功能）

- 快捷键 **F10** 开 / 关；需要先启用光影包；镜头必须保持静止（检测到移动会取消），分辨率变化会重新计数。
- 行为：冻结 `Time.x`、逐帧 R2 序列亚像素抖动（世界光栅与 `InvViewProj` 同步抖动）、`fx:6`/`fx:7` 乒乓按 1/n 累积 64 帧后自动保存 PNG（保存时手部与 GUI 尚未绘制，画面干净）。
- 对包透明：在整条 pass 链之后叠加；包不要占用 `fx:6` / `fx:7`。

## 10. 与 Iris/OptiFine 的差异（速查）

| | gl4dx12（本格式） | Iris/OptiFine |
|---|---|---|
| 语言 / 编译 | HLSL，D3DCompile → DXBC（SM 5.1），入口 `main` | GLSL，`.fsh/.vsh/.glsl` |
| pass 定义 | `pipeline.json` 显式声明输入 / 输出 | 文件名约定（`gbuffers_*` / `deferred*` / `composite*` / `final`） |
| `#include` | 不支持，单文件自包含 | 支持 |
| 纹理端点 | 仅 `scene_color` / `scene_depth` / `fx:N` | `colortex0-15` / `depthtex*` / `shadowtex*` / `noisetex` 等 |
| 几何数据 | 无（纯后处理，深度反推） | gbuffers 全套（法线 / 材质 / 方块 id） |
| 计算着色器 | `type:"compute"` + `buffers[]`（结构化 SSBO），不能写纹理 | compute 着色器 + image load/store |
| 时域数据 | `history_color` 端点 + `PrevViewProj` / `PrevCameraPosition` 重投影 | `colortex` 历史缓冲 + 运动矢量 |
| 阴影 | `raytrace` pass（DXR）或无 | 深度阴影贴图 |
| uniform | 固定 `b0`（见 §7） | 大量自动 uniform + 自定义配置 |
| 选项 | `shaders.properties` 的 `option.*` → `Options[]`（≤24，按序） | 同名风格（能力更丰富） |
| 采样器 | 固定线性 + CLAMP，数量 = 输入数 | 可配置各向异性 / 比较等 |

## 11. 参考

- 完整示例：`shaderpacks/Sunlit/`（天空、体积云、水面、光追阴影、末地、后处理；含 `pipeline.json` 与全部设置项）。
- 扩展能力示例：`shaderpacks/Foxy/`（PBR 材质、compute + SSBO 吸积盘粒子、屏幕空间反射、运动模糊、黑洞天体；6 pass 链）。
- 打包脚本示例：`pack_sunlit.ps1`（手工 zip，正斜杠条目名 + optimal 压缩）。
- 历史设计记录：仓库根目录 `检查代码问题.md` 的「HLSL 光影」相关章节（接入评估与首版说明）。
