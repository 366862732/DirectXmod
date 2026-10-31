// ===========================================================================
// StormBreak — 最终调色 pass
//
// 输入：t0 = fx:0 (sky), t1 = fx:1 (raytrace shadow), t2 = scene_depth
// 输出：scene_color
//
// 处理流程：
//   1. sRGB → 线性空间
//   2. 光追阴影 PCF 软边缘
//   3. 冷暖色彩分离（阴影冷蓝 vs 高光暖金）
//   4. Filmic 色调映射
//   5. 对比度 / 饱和度 / 暗角 / 锐化
//   6. 线性 → sRGB
// ===========================================================================

Texture2D<float4> InColor : register(t0);
SamplerState InSampler : register(s0);
Texture2D<float4> ShadowTex : register(t1);
SamplerState ShadowSampler : register(s1);
Texture2D<float> SceneDepth : register(t2);
SamplerState SceneDepthSampler : register(s2);

// [Options[0..5]] — 与 shaders.properties 声明顺序一致
#define O_BRIGHTNESS    (Options[0].x)
#define O_SATURATION    (Options[0].y)
#define O_CONTRAST      (Options[0].z)
#define O_VIGNETTE      (Options[0].w)
#define O_EXPOSURE      (Options[2].w)
#define O_WARMTH        (Options[3].x)
#define O_SHADOW_LIFT   (Options[3].y)

struct VSOutput
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// 辅助函数
// ---------------------------------------------------------------------------

float3 srgbToLinear(float3 c)
{
    return pow(c, float3(2.2, 2.2, 2.2));
}

float3 linearToSrgb(float3 c)
{
    return pow(saturate(c), float3(1.0 / 2.2, 1.0 / 2.2, 1.0 / 2.2));
}

// ACES 近似 Filmic 色调映射
float3 filmicToneMap(float3 x)
{
    const float3 A = float3(2.51, 2.43, 2.45);
    const float3 B = float3(0.03, 0.027, 0.025);
    const float3 C = float3(2.43, 2.40, 2.39);
    const float3 D = float3(0.59, 0.55, 0.50);
    const float3 E = float3(0.14, 0.125, 0.108);
    return saturate((x * (A * x + B)) / (x * (C * x + D) + E));
}

// PCF 阴影采样（3×3 软边缘）
float sampleShadowPCF(float2 uv, float depth)
{
    float2 texel = 1.0 / float2(300.0, 300.0);
    // 近处阴影边缘锐利，远处柔和
    float blur = lerp(0.7, 4.5, saturate(depth * 5.0));
    float2 off = texel * blur;

    float sum = 0.0;
    for (int y = -1; y <= 1; y++)
    {
        for (int x = -1; x <= 1; x++)
        {
            sum += ShadowTex.SampleLevel(ShadowSampler, uv + off * float2(x, y), 0).r;
        }
    }
    return sum / 9.0;
}

// ---------------------------------------------------------------------------
// 冷暖色彩分离
// ---------------------------------------------------------------------------

float3 colorSplit(float3 color, float shadowMask, float warmthAmt)
{
    float3 coldTint = float3(0.88, 0.94, 1.10);
    float3 warmTint = float3(1.08, 1.02, 0.88);

    float3 result = color;
    result = lerp(result, result * coldTint, shadowMask * warmthAmt);
    result = lerp(result, result * warmTint, (1.0 - shadowMask) * warmthAmt * 0.3);

    return result;
}

// ---------------------------------------------------------------------------
// 主函数
// ---------------------------------------------------------------------------

float4 main(VSOutput input) : SV_TARGET
{
    float2 uv = input.uv;

    // 1. 采样输入
    float4 skyOut = InColor.SampleLevel(InSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);
    float shadow = sampleShadowPCF(uv, depth);

    // 2. sRGB → 线性
    float3 color = srgbToLinear(skyOut.rgb);

    // 3. 光追阴影（仅对深度 > 0 的像素生效）
    float shadowMask = 0.0;
    if (depth > 1e-6)
    {
        shadowMask = 1.0 - shadow;
        float3 shadowColor = float3(0.42, 0.50, 0.68);
        color = lerp(color, color * shadowColor, shadowMask * 0.7);
    }

    // 4. 曝光
    color *= saturate(O_EXPOSURE);

    // 5. 冷暖色彩分离
    color = colorSplit(color, shadowMask, saturate(O_WARMTH));

    // 6. Filmic 色调映射
    color = filmicToneMap(color);

    // 7. 对比度（柔和 S 曲线）
    float contrast = saturate(O_CONTRAST);
    color = lerp(color, (color - 0.5) * (1.0 + contrast * 0.5) + 0.5, contrast);

    // 8. 饱和度
    float sat = saturate(O_SATURATION);
    float lum = dot(color, float3(0.299, 0.587, 0.114));
    color = lerp(float3(lum), color, sat);

    // 9. 暗角
    float vig = saturate(O_VIGNETTE);
    float2 vigUV = (uv - 0.5) * 2.0;
    float vigMask = 1.0 - dot(vigUV, vigUV) * vig * 0.5;
    color *= clamp(vigMask, 0.0, 1.0);

    // 10. 暗部抬升
    float lift = saturate(O_SHADOW_LIFT);
    color = max(color, lift * color + lift * 0.01);

    // 11. 线性 → sRGB
    color = linearToSrgb(color);

    return float4(color, 1.0);
}
