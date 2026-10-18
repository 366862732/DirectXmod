// ===========================================================================
// Sunlit — 最终调色 pass
//
// 输入：t0 = fx:0（天空/体积云合成结果）
//       t1 = fx:1（光追太阳可见性：1 = 受光，0 = 被遮挡）
//       t2 = scene_depth（reverse-Z，用于阴影软硬的距离分级）
// 输出：scene_color（主渲染目标）
//
// 目标：写实、柔和。高光压缩但不压暗中间调，暗部有轻微抬升，不做死黑。
// ===========================================================================

Texture2D<float4> InColor : register(t0);
SamplerState InSampler : register(s0);
Texture2D<float4> ShadowTex : register(t1);
SamplerState ShadowSampler : register(s1);
Texture2D<float> SceneDepth : register(t2);
SamplerState SceneDepthSampler : register(s2);

#define O_BRIGHTNESS    (Options[0].x)
#define O_SATURATION    (Options[0].y)
#define O_CONTRAST      (Options[0].z)
#define O_VIGNETTE      (Options[0].w)
#define O_CLOUD_COVER   (Options[1].x)
#define O_CLOUD_SPEED   (Options[1].y)
#define O_STAR_BRIGHT   (Options[1].z)
#define O_SUN_SIZE      (Options[1].w)
#define O_SUN_INTENSITY (Options[2].x)
#define O_HAZE          (Options[2].y)
#define O_EXPOSURE      (Options[2].z)
#define O_WARMTH        (Options[2].w)
#define O_SHADOW_LIFT   (Options[3].x)
#define O_NIGHT_LIFT    (Options[3].y)
#define O_SHARPEN       (Options[3].z)

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// 软阴影：8 抽环形采样，半径随距离增大（近处锐利、远处柔和）；
// 每个像素按哈希旋转采样图案，避免出现规则网格状条带。
float sunVisibilityPCF(float2 uv, float radiusPx)
{
    static const float2 kOffsets[8] =
    {
        float2( 0.00,  1.00), float2( 0.71,  0.71),
        float2( 1.00,  0.00), float2( 0.71, -0.71),
        float2( 0.00, -1.00), float2(-0.71, -0.71),
        float2(-1.00,  0.00), float2(-0.71,  0.71)
    };

    float ang = frac(sin(dot(uv * Viewport.xy, float2(12.9898, 78.233))) * 43758.5453) * 6.2831853;
    float cs = cos(ang);
    float sn = sin(ang);
    float2 px = Viewport.zw * radiusPx;

    float sum = 0.0;
    for (int i = 0; i < 8; ++i)
    {
        float2 o = kOffsets[i];
        float2 r = float2(o.x * cs - o.y * sn, o.x * sn + o.y * cs) * px;
        sum += saturate(ShadowTex.SampleLevel(ShadowSampler, uv + r, 0.0).r);
    }
    return sum * 0.125;
}

float4 main(VSOut input) : SV_TARGET
{
    float2 uv = input.uv;
    float3 color = InColor.SampleLevel(InSampler, uv, 0.0).rgb;

    // 轻度锐化（4 邻域）
    float sharpen = saturate(O_SHARPEN);
    if (sharpen > 0.002)
    {
        float2 px = Viewport.zw;
        float3 blur = 0.25 * (
            InColor.SampleLevel(InSampler, uv + float2(px.x, 0.0), 0.0).rgb +
            InColor.SampleLevel(InSampler, uv - float2(px.x, 0.0), 0.0).rgb +
            InColor.SampleLevel(InSampler, uv + float2(0.0, px.y), 0.0).rgb +
            InColor.SampleLevel(InSampler, uv - float2(0.0, px.y), 0.0).rgb);
        color += (color - blur) * sharpen;
    }

    // 原版 scene_color 已经是显示参考（sRGB）值：直接套 ACES 会把中间调整体
    // 抬亮、发灰（实测天空和地形全部发白）。这里先线性化，只做曝光与高光压缩，
    // 再编码回显示空间——中间调基本不变，暗部不死黑，高光柔和收敛。
    float3 lin = pow(max(color, 0.0), 2.2);

    // 光追太阳阴影（P68）：只在白天生效；天空像素的可见性恒为 1，不会被压暗。
    // 阴影不压成死黑——背光最多压到 ~0.6 并偏冷，保留暗部细节。
    // 软硬分级：采样半径随相机距离增大（近锐远柔），边缘自然过渡。
    float shadowStrength = saturate(PassParams.x);
    float sunUp = saturate(SunDirection.y * 6.0);
    if (shadowStrength > 0.001 && sunUp > 0.001)
    {
        float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0.0);
        float vis;
        if (depth <= 1e-6)
        {
            vis = 1.0; // 天空：不接收地形阴影，也不做模糊（避免剪影向天空渗出）
        }
        else
        {
            float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
            float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));
            float dist = length(rel4.xyz / max(rel4.w, 1e-4));
            float radius = lerp(0.7, 4.0, saturate(dist / 260.0));
            vis = sunVisibilityPCF(uv, radius);
        }
        float3 shade = lerp(float3(0.58, 0.63, 0.74), float3(1.0, 1.0, 1.0), vis);
        lin *= lerp(float3(1.0, 1.0, 1.0), shade, shadowStrength * sunUp);
    }

    lin *= max(O_EXPOSURE, 0.05) * max(O_BRIGHTNESS, 0.05);
    lin = lin / (1.0 + lin * 0.35);
    color = pow(saturate(lin), 1.0 / 2.2);

    // 饱和度
    float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
    color = lerp(float3(luma, luma, luma), color, max(O_SATURATION, 0.0));

    // 柔和 S 曲线（强度可调）
    float3 sCurve = color * color * (3.0 - 2.0 * color);
    color = lerp(color, sCurve, saturate(O_CONTRAST) * 0.55);

    // 暖色偏移
    color *= lerp(float3(1.0, 1.0, 1.0), float3(1.04, 1.005, 0.955), saturate(O_WARMTH));

    // 暗部 / 夜间抬升
    float night = saturate(1.0 - smoothstep(-0.12, 0.22, SunDirection.y));
    color += O_SHADOW_LIFT + night * O_NIGHT_LIFT;

    // 暗角
    float2 d = uv - 0.5;
    float vignette = 1.0 - saturate(O_VIGNETTE) * dot(d, d) * 1.5;
    color *= vignette;

    // 抖动，抑制渐变色带
    float dither = (frac(sin(dot(uv * Viewport.xy, float2(12.9898, 78.233))) * 43758.5453) - 0.5)
                 / 255.0;
    color += dither;

    return float4(saturate(color), 1.0);
}
