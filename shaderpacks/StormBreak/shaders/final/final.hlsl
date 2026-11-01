// ===========================================================================
// StormBreak - final tone mapping pass
//
// Inputs: t0 = fx:0 (sky), t1 = fx:1 (raytrace shadow), t2 = scene_depth
// Output: scene_color
//
// Pipeline:
//   1. sRGB -> linear
//   2. Raytrace shadow PCF soft edges
//   3. Cold/warm color split (cold shadows vs warm highlights)
//   4. Filmic tone mapping (ACES approx)
//   5. Contrast / saturation / vignette / sharpen
//   6. linear -> sRGB
// ===========================================================================

Texture2D<float4> InColor : register(t0);
SamplerState InSampler : register(s0);
Texture2D<float4> ShadowTex : register(t1);
SamplerState ShadowSampler : register(s1);
Texture2D<float> SceneDepth : register(t2);
SamplerState SceneDepthSampler : register(s2);

// [Options[0..5]] - must match shaders.properties declaration order
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
// Helpers
// ---------------------------------------------------------------------------

float3 srgbToLinear(float3 c)
{
    return pow(c, float3(2.2, 2.2, 2.2));
}

float3 linearToSrgb(float3 c)
{
    return pow(saturate(c), float3(1.0 / 2.2, 1.0 / 2.2, 1.0 / 2.2));
}

// ACES tone mapping fit (Narkowicz 2015). Better HDR headroom than the
// filmic curve: values up to ~5x white still show separation.
float3 filmicToneMap(float3 x)
{
    const float A = 2.51;
    const float B = 0.03;
    const float C = 2.43;
    const float D = 0.59;
    const float E = 0.14;
    // Apply per-channel: ACES fit has enough headroom to keep HDR highlights
    // distinct (clouds can reach 4~8 in linear space before this pass).
    return saturate((x * (A * x + B)) / (x * (C * x + D) + E));
}

// PCF shadow sampling (3x3 soft edges)
float sampleShadowPCF(float2 uv, float depth)
{
    float2 texel = 1.0 / float2(300.0, 300.0);
    // Near = sharp, far = soft
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
// Cold/warm color split
// ---------------------------------------------------------------------------

float3 colorSplit(float3 color, float shadowMask, float warmthAmt)
{
    float3 coldTint = float3(0.88, 0.94, 1.10);  // shadows cool blue
    float3 warmTint = float3(1.08, 1.02, 0.88);  // highlights warm gold

    color = lerp(color, color * coldTint, shadowMask * warmthAmt);
    color = lerp(color, color * warmTint, (1.0 - shadowMask) * warmthAmt * 0.3);

    return color;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

float4 main(VSOutput input) : SV_TARGET
{
    float2 uv = input.uv;

    // 1. Sample inputs
    float4 skyOut = InColor.SampleLevel(InSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);
    float shadow = sampleShadowPCF(uv, depth);

    // 2. sRGB -> linear
    float3 color = srgbToLinear(skyOut.rgb);

    // 3. Raytrace shadow (only for depth > 0)
    float shadowMask = 0.0;
    if (depth > 1e-6)
    {
        shadowMask = 1.0 - shadow;
        float3 shadowColor = float3(0.42, 0.50, 0.68);
        color = lerp(color, color * shadowColor, shadowMask * 0.7);
    }

    // 4. Exposure (slider 0.5–2.0, not clamped)
    color *= O_EXPOSURE;

    // 5. Cold/warm split
    color = colorSplit(color, shadowMask, saturate(O_WARMTH));

    // 6. Contrast S-curve in linear space, before tone mapping
    float contrast = saturate(O_CONTRAST);
    color = lerp(color, (color - 0.5) * (1.0 + contrast * 0.8) + 0.5, contrast);

    // 7. ACES tone mapping (preserves HDR highlights up to ~5x white)
    color = filmicToneMap(color);

    // 8. Saturation
    float sat = saturate(O_SATURATION);
    float lum = dot(color, float3(0.299, 0.587, 0.114));
    color = lerp(float3(lum, lum, lum), color, sat);

    // 9. Vignette
    float vig = saturate(O_VIGNETTE);
    float2 vigUV = (uv - 0.5) * 2.0;
    float vigMask = 1.0 - dot(vigUV, vigUV) * vig * 0.5;
    color *= clamp(vigMask, 0.0, 1.0);

    // 10. Shadow lift
    float lift = saturate(O_SHADOW_LIFT);
    color = max(color, lift * color + lift * 0.01);

    // 11. linear -> sRGB
    color = linearToSrgb(color);

    return float4(color, 1.0);
}
