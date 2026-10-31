# StormBreak Cinematic — DirectX 12 光影包开发文档

> **电影级暴风雨风景摄影** — 基于 gl4dx12 HLSL 光影格式的实战开发全记录

---

## 一、概述

**StormBreak Cinematic** 是一个为 Minecraft DirectX 12 光影引擎（gl4dx12）开发的自定义光影包，目标是实现电影级暴风雨前夕的城市峡谷视觉效果。核心特征：

- 暗灰色厚重云层布满天空
- 一道巨大的斜向不规则裂口撕开云层
- 耀眼的暖金色阳光如瀑布般倾泻而下
- 极具戏剧性的丁达尔效应（光束在地面和建筑墙面上形成不规则斑驳光斑）
- 冷暗阴影与金黄高光形成极致视觉对比
- 大气散射、空气透视、高动态范围（HDR）、写实风格

---

## 二、格式规范：gl4dx12 HLSL 光影包

### 2.1 与 Iris/OptiFine 的本质区别

本光影**不是** Iris/OptiFine 格式。着色器是 **HLSL（SM 5.1）**，pass 链由 `pipeline.json` 显式声明。真正的 Iris/OptiFine 包（GLSL `.fsh/.vsh`）无法加载。

| 维度 | gl4dx12（本格式） | Iris/OptiFine |
|---|---|---|
| 语言 | HLSL，D3DCompile → DXBC（SM 5.1） | GLSL，`.fsh/.vsh` |
| pass 定义 | `pipeline.json` 显式声明 | 文件名约定 |
| `#include` | 不支持，单文件自包含 | 支持 |
| 纹理端点 | 仅 `scene_color` / `scene_depth` / `fx:N` | `colortex0-15` 等 |
| 几何数据 | 无（纯后处理，深度反推） | gbuffers 全套 |
| 阴影 | `raytrace` pass（DXR）或无 | 深度阴影贴图 |
| 采样器 | 固定线性 + CLAMP | 可配置 |

### 2.2 包结构

```
StormBreak.zip
└── shaders/
    ├── shaders.properties     # 元数据 + option.* 设置项
    ├── pipeline.json          # pass 链（自定义 JSON）
    ├── composite/sky.hlsl     # 天空 + 体积云 + 光缝 + 丁达尔 + 云影
    └── final/final.hlsl       # HDR 调色 + 光追阴影 PCF + 冷暖分离
```

- **加载器接受两种路径写法**：`"shaders/composite/sky.hlsl"` 或 `"composite/sky.hlsl"`
- **zip 条目名必须用正斜杠** `/`（Windows `Compress-Archive` 会写成反斜杠）
- 文本用 **UTF-8（无 BOM）**

### 2.3 端点与执行模型

只有三种端点：

| 端点 | 说明 |
|---|---|
| `scene_color` | 主渲染目标（已包含原版世界渲染，sRGB 空间） |
| `scene_depth` | 主深度缓冲（reverse-Z：天空/远平面 = 0，几何 > 0） |
| `fx:N` | 中间纹理（RGBA16F），每帧不自动清空 |

执行模型：
- 每帧顺序：世界渲染 → 依序执行 passes → （若最后一个 pass 没写 `scene_color`）内置拷贝贴回主目标
- 每个 pass 是全屏三角形 draw，整体覆盖输出（无混合、无深度测试）
- 某 pass 编译失败时该帧链在其位置中止
- 每帧最多 8 个 pass 拥有独立 uniform 槽位

### 2.4 内置 uniform（b0）

框架自动拼接到每个着色器文件顶部，**包内不要重复声明**：

```hlsl
cbuffer ShaderPackUniforms : register(b0)
{
    float4 Viewport;        // xy = 宽高(像素), zw = 1/宽高
    float4 Time;            // x = 秒, y = 帧序号, z = 帧间隔秒, w = 世界时间秒
    float4 CameraPosition;  // xyz = 相机世界坐标, w = 1
    float4 SunDirection;    // xyz = 太阳方向单位向量, w = 太阳高度
    float4 PassParams;      // 本 pass 的 params[0..3]
    float4 Sky;             // x = 太阳角, y = 星光亮度, z = 天气亮度, w = 天空类型
    float4 SkyColor;        // rgb = 原版天空色
    float4 SunsetColor;     // rgb = 日出日落色
    float4 Misc;            // x = 视距, y = 云层高度, z = 月亮角, w = 星空角
    float4x4 InvViewProj;   // NDC + depth 反投影到相机世界系
    float4x4 ViewRotation;  // 世界 -> 相机（仅旋转）
    float4 Options[6];      // 24 个设置值（按 shaders.properties 顺序）
};
```

纹理/采样器按 manifest `inputs` 顺序对应 `register(t0..tN-1)` 与 `register(s0..sN-1)`，采样器固定线性 + CLAMP。

---

## 三、Pass 链设计

```json
{
  "version": 1,
  "replaceVanillaSky": true,
  "passes": [
    { "name": "sky", "fragment": "shaders/composite/sky.hlsl",
      "inputs": ["scene_color", "scene_depth"], "output": "fx:0" },
    { "name": "shadow", "type": "raytrace",
      "output": "fx:1", "params": [1.0, 512.0, 0.06, 0.0] },
    { "name": "final", "fragment": "shaders/final/final.hlsl",
      "inputs": ["fx:0", "fx:1", "scene_depth"], "output": "scene_color",
      "params": [0.85, 0.0, 0.0, 0.0] }
  ]
}
```

### 数据流

```
pass 0: sky composite
  读 scene_color + scene_depth
  写 fx:0（RGBA16F：天空渐变/体积云/光缝/丁达尔/云影/大气雾）
       ↓
pass 1: raytrace shadow（内置 DXR）
  读 scene_depth + 世界 TLAS
  写 fx:1（RGBA16F：阴影可见性，1=受光，0=阴影）
       ↓
pass 2: final composite
  读 fx:0 + fx:1 + scene_depth
  写 scene_color（HDR 调色/PCF 阴影/冷暖分离/Filmic/暗角）
```

### 关键设计决策

- **`replaceVanillaSky: true`** — 抑制原版方块云，天空渐变仍会先绘制，由 sky pass 覆盖
- **保留 `raytrace` pass** — DXR 光追阴影比屏幕空间阴影更真实，与电影感匹配
- **`fx:0`/`fx:1` 不碰 `fx:6`/`fx:7`** — 这两个被离线渲染功能占用
- **3 个 pass ≤ 8 上限** — 每个 pass 独占 uniform 槽位

---

## 四、Sky Pass 实现（`sky.hlsl`）

### 4.1 整体架构

sky pass 根据 depth 分叉：

```
depth <= 0 (天空像素)
  → 反投影视线方向 → 天空渐变 + 体积云 + 光缝阳光 + 丁达尔 + 大气雾

depth > 0 (地面/建筑像素)
  → 反投影世界坐标 → 大气雾 + 云影光斑 + 光缝地面投射
```

### 4.2 核心模块

#### 噪声引擎（整数格点哈希）

```hlsl
float ihash12(int2 p)
{
    uint h = uint(p.x) * 374761393u + uint(p.y) * 668265263u + 1442695041u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0xFFFFFFu) * (1.0 / 16777215.0);
}
```

- 使用整数格点哈希而非浮点哈希，**大坐标下稳定不闪烁**
- `ivnoise2`/`ivnoise3`：值噪声（smoothstep 插值）
- `fbm4`：4  octave FBM（分形布朗运动），用于大尺度云层纹理

#### 体积云引擎

```hlsl
float hcCloudDensity(float3 p, float3 wind, float heightFactor)
{
    float sizeScale = rcp(clamp(O_CLOUD_SIZE, 0.2, 1.0));
    float3 q = p + wind;
    q.y -= HC_HEIGHT;
    float d = ivnoise2(q.xz * (0.00030 * sizeScale));
    float d3 = ivnoise3(q * (0.00025 * sizeScale));
    d = d * 0.82 + d3 * 0.18;
    float threshold = lerp(0.55, 0.22, saturate(O_CLOUD_COVER));
    float density = saturate((d - threshold) * 3.8);
    return density * heightFactor;
}
```

- 2D + 3D 噪声混合（`0.82:0.18`），降低 Z 轴重复感
- 暴风雨模式：阈值 `lerp(0.55, 0.22, CLOUD_COVERAGE)`，密度增益 `3.8`（比晴天更厚）
- 云层参数由 `O_CLOUD_HEIGHT × 100` 换算成实际高度

#### 光线步进（Ray Marching）

```hlsl
float4 hcRealisticCloud(float3 baseColor, float3 atmosphere, ...)
{
    float3 camPos = CameraPosition.xyz;
    float bottom = HC_HEIGHT;
    float top = HC_CLOUD_TOP_Y;

    // 计算视线与云层区间的交点
    float t0 = (bottom - camPos.y) / worldDir.y;
    float t1 = (top - camPos.y) / worldDir.y;
    tStart = max(min(t0, t1), 0.0);
    tEnd = min(max(t0, t1), HC_MAX_DIST);

    // 亚像素抖动起始点（避免摩尔纹）
    float3 p = camPos + worldDir * (tStart + stepSize * hcStartDither(ditherAnchor));

    // 逐样本：透明度累加 + 颜色累加
    float transmittance = 1.0;
    float3 cloudColor = float3(0.0);
    for (int i = 0; i < steps; i++)
    {
        float4 s = hcSampleRealisticCloud(p, sunDir, atmosphere, dither, hf);
        float sampleTrans = exp2(-s.w * stepSize * DENSITY * 1.44269502) * transmittance;
        cloudColor += (transmittance - sampleTrans) * s.rgb;
        transmittance = sampleTrans;
        if (transmittance < 0.01) break;  // 提前终止
    }
    ...
}
```

- **抖动起始**：`ivnoise3(camPos + worldDir * tStart) * stepSize`，消除摩尔纹
- **提前终止**：`transmittance < 0.01` 时跳出（已完全不透明）
- **每像素 ≤ 32 步**（`O_CLOUD_STEPS` 控制，默认 24）

#### 不规则光缝（God Ray Gap）

```hlsl
float godRayGap(float3 samplePoint, float3 sunDir, float3 camPos)
{
    float2 gapCoord = samplePoint.xz * 0.0004 + samplePoint.y * 0.00008;
    float mainGap = fbm4(gapCoord);          // 大尺度裂口
    float ragged = ivnoise2(samplePoint.xz * 0.003) * 0.3;  // 撕裂边缘
    float gap = mainGap + ragged;

    float gapWidth = lerp(0.15, 0.60, saturate(O_BEAM_SPREAD));
    float center = 0.5 + sin(gapCoord.x * 1.8) * 0.12;
    float dist = abs(gap - center);
    float openness = 1.0 - smoothstep(gapWidth * 0.3, gapWidth, dist);

    // 太阳方向增强
    float sunBoost = saturate(dot(normalize(samplePoint - camPos), sunDir) * 0.5 + 0.5);
    openness *= lerp(0.6, 1.0, sunBoost);
    openness *= 1.0 - saturate(O_CLOUD_COVER) * 0.5;
    return openness;
}
```

- 大尺度 FBM 撕开对角裂口，小尺度噪声制造不规则撕裂边缘
- **世界锚定**：采样点 = `cameraPosition + viewDir × 固定距离`，不随视角晃动
- 云量越大，光缝越窄（`openness *= 1 - CLOUD_COVER × 0.5`）

#### 丁达尔光束（Volumetric God Rays）

```hlsl
float3 godRayBeam(float3 viewDir, float3 sunDir, float3 camPos)
{
    float totalGap = 0.0;
    float stepDist = 2000.0;
    for (int i = 0; i < 5; i++)
    {
        float3 probe = camPos + viewDir * (stepDist * (float)(i + 1));
        totalGap += godRayGap(probe, sunDir, camPos);
    }
    totalGap *= 0.2;  // 平均

    // 锥形衰减：视线与太阳方向夹角越小，光束越亮
    float cosBeam = saturate(dot(viewDir, sunDir));
    float beamProfile = pow(cosBeam, lerp(8.0, 2.0, saturate(O_BEAM_SPREAD * 0.4)));

    // 距离衰减
    float distFade = exp(-stepDist * 0.00012);

    float3 beamColor = float3(1.15, 0.92, 0.55);  // 暖金色
    float intensity = totalGap * beamProfile * distFade * beamAmt * 2.5 * sunUp;
    return beamColor * intensity;
}
```

- 沿视线 march 5 步，采样光缝通透度
- 锥形衰减 + 距离衰减 → 光束从光缝"倾泻"而非均匀发光
- 暖金色 `(1.15, 0.92, 0.55)` 超出 sRGB 范围（HDR 值 > 1）

#### 大气雾（空气透视）

```hlsl
float3 hcAirFog(float3 color, float2 ndc, float depth, float3 sunDir, ...)
{
    // 反投影世界坐标（NDC + depth → 相机系）
    float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));
    float dist = length(rel4.xyz / max(abs(rel4.w), 1e-4));

    // 指数衰减
    float opt = dist * strength * (1.0 + 2.5 * weather * weather);
    float3 absorb = exp(-opt * HC_ASF_BETA);

    // 散射色：暴风雨前冷灰蓝
    float3 scatter = lerp(float3(0.28, 0.32, 0.40), float3(0.45, 0.50, 0.58), skyLight);

    // 太阳方向米氏前向散射（暖光）
    float cosSunDir = dot(worldDir, sunDir);
    float phase = pow(saturate(cosSunDir), 8.0) * 0.55;
    scatter += sunTint * phase * sunUp * skyLight;

    return scatter * (1.0 - absorb) + color * absorb;
}
```

- 距离越远越被冷灰蓝雾色吞没
- 太阳方向有额外暖光（米氏前向散射近似）
- 天气（`Sky.z`）越大雾越浓

#### 云影斑驳光斑（Screen-Space Dapple）

```hlsl
float cloudDapple(float3 worldPos, float3 sunDir)
{
    float shadowShift = sunDir.x * 800.0 - sunDir.z * 600.0;
    float2 dappleCoord = worldPos.xz * 0.0008 + shadowShift;

    float big   = fbm4(dappleCoord);         // 大团块
    float mid   = ivnoise2(dappleCoord * 3.1 + 5.2);  // 中等
    float fine  = ivnoise2(dappleCoord * 8.7 + 13.1); // 细碎

    float dapple = big * 0.55 + mid * 0.30 + fine * 0.15;
    float threshold = 0.42;
    float mask = 1.0 - smoothstep(threshold - 0.06, threshold + 0.04, dapple);

    // 光缝处云影消失（阳光直射）
    float gap = godRayGap(worldPos, sunDir, CameraPosition.xyz);
    mask *= 1.0 - gap * 0.7;
    return mask;
}
```

- 三层 FBM 叠加（大团块 55% + 中等 30% + 细碎 15%）→ 不规则斑驳
- 太阳角度偏移 `shadowShift` 让光斑随太阳位置移动
- 光缝处光斑消失（阳光直接照射）

### 4.3 关键教训

#### 教训 1：`InvViewProj` 只能用于 NDC + 深度反投影

**错误用法**（导致 NaN 传播，画面全黑）：
```hlsl
// 错：把 3D 方向向量乘进 InvViewProj
float4 relH = mul(InvViewProj, float4(worldDir.x, worldDir.y, worldDir.z, 1.0));
```

**正确用法**（仅限 NDC + 深度）：
```hlsl
// 对：天空像素（depth = 0）
float4 rel = mul(InvViewProj, float4(ndc.x, ndc.y, 0.0, 1.0));
// 对：地面像素
float4 rel = mul(InvViewProj, float4(ndc.x, ndc.y, depth, 1.0));
```

#### 教训 2：调试优先 — 纯色测试 pass

当效果"看不到"时，第一步是把 main 替换为纯色输出（红/蓝），确认管线是否执行：

```hlsl
float4 main(VSOutput input) : SV_TARGET
{
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);
    if (depth <= 1e-6) return float4(0.9, 0.1, 0.1, 1.0);  // 天空红
    else               return float4(0.1, 0.1, 0.9, 1.0);  // 地面蓝
}
```

如果画面变色 → 管线 OK，问题在 shader 逻辑
如果画面不变 → 管线没执行（编译失败 / 资源绑定失败）

#### 教训 3：HLSL 结构体名必须与框架一致

内置全屏 VS 输出 `struct VSOutput { float4 position; float2 uv; }`，
片元入口参数必须匹配，否则 D3DCompile 失败 → `pipelineFor` 返回 0 → 整条 pass 链静默跳过。

---

## 五、Final Pass 实现（`final.hlsl`）

### 5.1 处理流程

```
sRGB → 线性 → 光追阴影 PCF → 曝光 → 冷暖分离 → Filmic → 对比度 → 饱和度 → 暗角 → 暗部抬升 → sRGB
```

### 5.2 核心代码

#### sRGB ↔ 线性转换

```hlsl
float3 srgbToLinear(float3 c)  { return pow(c, float3(2.2, 2.2, 2.2)); }
float3 linearToSrgb(float3 c)  { return pow(saturate(c), float3(1/2.2)); }
```

`scene_color` 是 sRGB 空间，HDR 运算必须在线性空间做。

#### ACES 近似 Filmic 色调映射

```hlsl
float3 filmicToneMap(float3 x)
{
    const float3 A = float3(2.51, 2.43, 2.45);
    const float3 B = float3(0.03, 0.027, 0.025);
    const float3 C = float3(2.43, 2.40, 2.39);
    const float3 D = float3(0.59, 0.55, 0.50);
    const float3 E = float3(0.14, 0.125, 0.108);
    return saturate((x * (A * x + B)) / (x * (C * x + D) + E));
}
```

- HDR 值（光缝阳光可达 10+）被压缩到 [0,1]
- 高动态保留细节，不像 `saturate()` 那样硬截断

#### PCF 软阴影

```hlsl
float sampleShadowPCF(float2 uv, float depth)
{
    float2 texel = 1.0 / float2(300.0, 300.0);
    float blur = lerp(0.7, 4.5, saturate(depth * 5.0));  // 近锐远软
    float2 off = texel * blur;
    float sum = 0.0;
    for (int y = -1; y <= 1; y++)
        for (int x = -1; x <= 1; x++)
            sum += ShadowTex.SampleLevel(ShadowSampler, uv + off * float2(x, y), 0).r;
    return sum / 9.0;
}
```

- 3×3 九点 PCF，近处 0.7px 偏移（锐利），远处 4.5px（柔和）
- 阴影色 `(0.42, 0.50, 0.68)` 偏冷蓝，与暖金高光对比

#### 冷暖色彩分离

```hlsl
float3 colorSplit(float3 color, float shadowMask, float warmthAmt)
{
    float3 coldTint = float3(0.88, 0.94, 1.10);  // 阴影冷蓝
    float3 warmTint = float3(1.08, 1.02, 0.88);  // 高光暖金
    color = lerp(color, color * coldTint, shadowMask * warmthAmt);
    color = lerp(color, color * warmTint, (1.0 - shadowMask) * warmthAmt * 0.3);
    return color;
}
```

- 阴影区偏冷（蓝通道 ×1.10），高光区偏暖（红通道 ×1.08）
- 这是"极致视觉对比"的核心：冷暗 vs 暖亮

---

## 六、光追阴影（raytrace pass）

### 6.1 配置

```json
{ "name": "shadow", "type": "raytrace", "output": "fx:1",
  "params": [1.0, 512.0, 0.06, 0.0] }
```

| Param | 含义 | 值 |
|---|---|---|
| `x` | 未使用 | 1.0 |
| `y` | 最大射线距离 | 512 格 |
| `z` | 沿法线偏移（防自交） | 0.06 |
| `w` | 未使用 | 0.0 |

### 6.2 内置行为

- 由 `scene_depth` 重建世界坐标 + 屏幕空间差分法线
- 天空/无效像素写 `1`（全受光）
- 太阳在地平线下写 `1`
- 否则沿太阳方向发射阴影射线：**命中 = 0、未命中 = 1**（RGB 通道）
- DXR 不可用 / 场景无几何时：输出清为全 `1`（不黑屏）
- 副作用：包内存在 raytrace pass 时自动开启世界几何捕获（区块几何重新上传建加速结构）

### 6.3 性能

光追阴影是最耗时的 pass。RTX 4070 上约 2~5ms/帧（取决于区块数量和距离）。

---

## 七、设置项设计

### 7.1 完整列表（21 项）

| 选项 | 默认 | 范围 | 说明 |
|---|---|---|---|
| BRIGHTNESS | 1.0 | 0.6~1.6 | 整体亮度 |
| SATURATION | 1.0 | 0~2.0 | 色彩饱和度 |
| CONTRAST | 0.45 | 0~1.0 | 柔和 S 曲线强度 |
| VIGNETTE | 0.22 | 0~1.0 | 暗角强度 |
| CLOUD_COVERAGE | 0.70 | 0~1.0 | 云量（越大越厚越暗） |
| CLOUD_SPEED | 1.0 | 0~3.0 | 云移动速度 |
| LIGHT_BEAM | 1.0 | 0~2.5 | 丁达尔光束强度 |
| BEAM_SPREAD | 1.0 | 0.2~2.5 | 光束扩散宽度 |
| SHADOW_STORM | 0.80 | 0~1.5 | 云影光斑强度 |
| SUN_INTENSITY | 2.0 | 0~5.0 | 光缝处阳光亮度 |
| HAZE | 0.65 | 0~1.5 | 大气雾/空气透视 |
| EXPOSURE | 1.10 | 0.5~2.0 | 曝光 |
| WARMTH | 0.50 | 0~1.0 | 冷暖分离强度 |
| SHADOW_LIFT | 0.02 | 0~0.12 | 暗部抬升（防死黑） |
| SHARPEN | 0.10 | 0~1.0 | 轻度锐化 |
| CLOUD_STEPS | 24 | 6~48 | 体积云步数 |
| CLOUD_SIZE | 0.35 | 0.2~1.0 | 云块大小 |
| CLOUD_AMOUNT | 0.70 | 0~1.0 | 云层密度 |
| CLOUD_RAIN | 0.85 | 0~1.0 | 暴风雨程度 |
| CLOUD_HEIGHT | 12.0 | 2~20 | 云层底部高度 |
| CLOUD_THICK | 14.0 | 2~30 | 云层厚度 |

### 7.2 声明顺序规则

```properties
option.BRIGHTNESS=1.0      → Options[0].x
option.SATURATION=1.0      → Options[0].y
option.CONTRAST=0.45       → Options[0].z
option.VIGNETTE=0.22       → Options[0].w
option.CLOUD_COVERAGE=0.70 → Options[1].x
...
```

- 第 n 个设置 → `Options[n/4][n%4]`
- 最多 24 个（`Options[6]`）
- **顺序不可调整**——调整会让所有值错位

---

## 八、打包与部署

### 8.1 正确打包（正斜杠条目名）

```powershell
# 手工 zip（避免 Compress-Archive 的反斜杠问题）
$tmp = "_tmp_build"
if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
New-Item -ItemType Directory -Path $tmp -Force | Out-Null
Copy-Item -Path "shaderpacks\StormBreak\*" -Destination $tmp -Recurse
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $tmp, "shaderpacks\StormBreak.zip",
    [System.IO.Compression.CompressionLevel]::Optimal, $false)
Remove-Item $tmp -Recurse -Force
```

验证条目名（必须是正斜杠）：
```powershell
[System.IO.Compression.ZipFile]::OpenRead("StormBreak.zip").Entries |
    ForEach-Object { $_.FullName }
# 应输出:
#   shaders/pipeline.json
#   shaders/shaders.properties
#   shaders/composite/sky.hlsl
#   shaders/final/final.hlsl
```

### 8.2 部署

```
<游戏目录>/shaderpacks/StormBreak.zip
```

游戏内：**视频设置 → 光影** → 选择 "StormBreak Cinematic" → 启用 → 重载

### 8.3 配置文件

启用状态与滑条值保存在：
```
<游戏目录>/config/gl4dx12-shaderpacks.properties
```

```properties
enabled=true
active=StormBreak
option.BRIGHTNESS=1.0
option.CLOUD_COVERAGE=0.70
...
```

---

## 九、性能优化

### 9.1 体积云

| 优化 | 说明 |
|---|---|
| 亚像素抖动 | `ivnoise3` 起始偏移，消除摩尔纹 |
| 提前终止 | `transmittance < 0.01` 跳出 |
| 步数控制 | `O_CLOUD_STEPS`（默认 24，可调 6~48） |
| 高度因子 | 云区间外直接跳过（`hf > 0.001` 才采样） |

### 9.2 光缝 & 光束

| 优化 | 说明 |
|---|---|
| 世界锚定 | 采样点 = `cameraPosition + viewDir × 固定距离`，不随视角重算 |
| 5 步 march | 丁达尔光束仅 5 步（不是 32 步） |
| 光缝 4-octave FBM | 比云密度 FBM 更便宜 |

### 9.3 光追阴影

| 优化 | 说明 |
|---|---|
| 最大距离 512 格 | `params[1]=512`，超过此距离不发射射线 |
| 法线偏移 0.06 | `params[2]=0.06`，防自交阴影 |
| 3×3 PCF | 9 次采样（不是 4×4=16 次） |

### 9.4 性能参考（RTX 4070）

| Pass | 耗时 |
|---|---|
| sky（体积云 24 步 + 光缝 + 光束 + 雾） | ~3ms |
| raytrace shadow（512 格） | ~2~5ms |
| final（PCF + Filmic + 调色） | ~1ms |
| **合计** | **~6~9ms** |

中端显卡（GTX 1060）建议：
- `CLOUD_STEPS` 降到 12
- 移除 raytrace pass（改为屏幕空间阴影或无阴影）
- `HAZE` 降到 0.4（雾计算更便宜）

---

## 十、常见问题排查

### Q1：画面是原版，光影没生效

1. 确认 `shaderpacks/StormBreak.zip` 存在且条目名是**正斜杠**
2. 游戏内 **视频设置 → 光影** 确认 "StormBreak Cinematic" 已选中并启用
3. 检查 `config/gl4dx12-shaderpacks.properties`：`enabled=true` + `active=StormBreak`
4. 点「重载」按钮（重建管线与中间纹理，无需重启游戏）
5. 检查日志 `[shaderpack]` 前缀是否有编译错误

### Q2：云完全不可见

- **云层太高**：`CLOUD_HEIGHT` 默认 12.0（=1200 格），相机在 y≈64 平视时看不到
  - 解法：把 `CLOUD_HEIGHT` 降到 4~6（400~600 格）
- **云量不够**：`CLOUD_COVERAGE` 默认 0.70，可以拉到 1.0
- **天空渐变太亮**：原版天空渐变仍先绘制，sky pass 覆盖它。确认 `replaceVanillaSky=true`

### Q3：光缝/光束太弱

- `SUN_INTENSITY` 默认 2.0，可拉到 3.0~5.0
- `LIGHT_BEAM` 默认 1.0，可拉到 1.5~2.5
- `BEAM_SPREAD` 默认 1.0，拉高让光缝更宽

### Q4：云影光斑看不到

- `SHADOW_STORM` 默认 0.80，可拉到 1.2~1.5
- 云影只在 `depth > 0` 的像素上生效（天空无光斑）
- 需要太阳在地平线上方（`sunUp > 0`）

### Q5：性能不足

- `CLOUD_STEPS` 6~12（从 24 降下来）
- 移除 raytrace pass（从 pipeline.json 删掉 shadow pass）
- `HAZE` 降到 0.3~0.4

---

## 十一、完整代码清单

### `shaders/pipeline.json`

```json
{
  "version": 1,
  "replaceVanillaSky": true,
  "passes": [
    { "name": "sky", "fragment": "shaders/composite/sky.hlsl",
      "inputs": ["scene_color", "scene_depth"], "output": "fx:0" },
    { "name": "shadow", "type": "raytrace",
      "output": "fx:1", "params": [1.0, 512.0, 0.06, 0.0] },
    { "name": "final", "fragment": "shaders/final/final.hlsl",
      "inputs": ["fx:0", "fx:1", "scene_depth"], "output": "scene_color",
      "params": [0.85, 0.0, 0.0, 0.0] }
  ]
}
```

### `shaders/shaders.properties`

```properties
name=StormBreak Cinematic
author=xgdt
version=1.0.0
description=暴风雨前夕的城市峡谷 / 暗灰厚重云层 / 不规则光缝 / 丁达尔光束 / 大气透视 / HDR 写实

option.BRIGHTNESS=1.0
option.BRIGHTNESS.min=0.6
option.BRIGHTNESS.max=1.6
option.BRIGHTNESS.comment=整体亮度

option.SATURATION=1.0
option.SATURATION.min=0.0
option.SATURATION.max=2.0
option.SATURATION.comment=色彩饱和度

option.CONTRAST=0.45
option.CONTRAST.min=0.0
option.CONTRAST.max=1.0
option.CONTRAST.comment=对比度

option.VIGNETTE=0.22
option.VIGNETTE.min=0.0
option.VIGNETTE.max=1.0
option.VIGNETTE.comment=暗角强度

option.CLOUD_COVERAGE=0.70
option.CLOUD_COVERAGE.min=0.0
option.CLOUD_COVERAGE.max=1.0
option.CLOUD_COVERAGE.comment=云量

option.CLOUD_SPEED=1.0
option.CLOUD_SPEED.min=0.0
option.CLOUD_SPEED.max=3.0
option.CLOUD_SPEED.comment=云移动速度

option.LIGHT_BEAM=1.0
option.LIGHT_BEAM.min=0.0
option.LIGHT_BEAM.max=2.5
option.LIGHT_BEAM.comment=丁达尔光束强度

option.BEAM_SPREAD=1.0
option.BEAM_SPREAD.min=0.2
option.BEAM_SPREAD.max=2.5
option.BEAM_SPREAD.comment=光束扩散宽度

option.SHADOW_STORM=0.80
option.SHADOW_STORM.min=0.0
option.SHADOW_STORM.max=1.5
option.SHADOW_STORM.comment=云影强度

option.SUN_INTENSITY=2.0
option.SUN_INTENSITY.min=0.0
option.SUN_INTENSITY.max=5.0
option.SUN_INTENSITY.comment=光源强度

option.HAZE=0.65
option.HAZE.min=0.0
option.HAZE.max=1.5
option.HAZE.comment=大气雾强度

option.EXPOSURE=1.10
option.EXPOSURE.min=0.5
option.EXPOSURE.max=2.0
option.EXPOSURE.comment=曝光

option.WARMTH=0.50
option.WARMTH.min=0.0
option.WARMTH.max=1.0
option.WARMTH.comment=冷暖分离

option.SHADOW_LIFT=0.02
option.SHADOW_LIFT.min=0.0
option.SHADOW_LIFT.max=0.12
option.SHADOW_LIFT.comment=暗部抬升

option.SHARPEN=0.10
option.SHARPEN.min=0.0
option.SHARPEN.max=1.0
option.SHARPEN.comment=轻度锐化

option.CLOUD_STEPS=24
option.CLOUD_STEPS.min=6
option.CLOUD_STEPS.max=48
option.CLOUD_STEPS.comment=体积云步数

option.CLOUD_SIZE=0.35
option.CLOUD_SIZE.min=0.20
option.CLOUD_SIZE.max=1.0
option.CLOUD_SIZE.comment=云块大小

option.CLOUD_AMOUNT=0.70
option.CLOUD_AMOUNT.min=0.0
option.CLOUD_AMOUNT.max=1.0
option.CLOUD_AMOUNT.comment=云层密度

option.CLOUD_RAIN=0.85
option.CLOUD_RAIN.min=0.0
option.CLOUD_RAIN.max=1.0
option.CLOUD_RAIN.comment=暴风雨程度

option.CLOUD_HEIGHT=12.0
option.CLOUD_HEIGHT.min=2.0
option.CLOUD_HEIGHT.max=20.0
option.CLOUD_HEIGHT.comment=云层底部高度

option.CLOUD_THICK=14.0
option.CLOUD_THICK.min=2.0
option.CLOUD_THICK.max=30.0
option.CLOUD_THICK.comment=云层厚度
```

---

## 十二、开发工具链

| 工具 | 用途 |
|---|---|
| PowerShell `System.IO.Compression` | 打包 zip（正斜杠条目名） |
| `D3DCompile`（自动） | HLSL → DXBC（SM 5.1） |
| `debug.log` / `latest.log` | 查看 `[shaderpack]` 前缀日志 |
| 游戏内 **视频设置 → 光影** | 实时调整设置项 + 重载 |
| F10 | 离线渲染（R2 亚像素抖动累积 64 帧） |

### 调试技巧

1. **纯色测试**：把 main 替换为红/蓝纯色，确认管线执行
2. **D3D 验证层**：设置 `DX12_LOG_VERBOSE=1` 查看 native 日志
3. **单 pass 测试**：临时简化 pipeline.json 只保留 sky pass，确认天空
4. **效果强度**：把 `SUN_INTENSITY` 拉到 5.0 看光缝是否可见
5. **云层高度**：`CLOUD_HEIGHT` 从 12.0 逐步降到 4.0，观察云是否出现

---

## 十三、参考

- **格式规范**：`shaderpacks/FORMAT.md`（gl4dx12 HLSL 光影包格式 v1）
- **参考包**：`shaderpacks/Sunlit/`（天空、体积云、水面、光追阴影、末地、后处理）
- **打包脚本**：`pack_sunlit.ps1`（手工 zip 示例）
- **设计记录**：仓库根目录 `检查代码问题.md` 的「HLSL 光影」章节
