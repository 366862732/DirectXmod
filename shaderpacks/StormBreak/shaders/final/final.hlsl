// ===========================================================================
// StormBreak - final tone mapping pass
//
// Inputs: t0 = fx:0 (sky, sRGB display space),
//         t1 = fx:1 (raytrace shadow),
//         t2 = scene_depth
// Output: scene_color
//
// The whole pack runs in sRGB display space (Minecraft shader-pack
// convention).  fx:0 is already sRGB-encoded by the sky pass, so this pass
// does a gentle linearisation for physical operations (exposure, contrast),
// then re-encodes to sRGB.  No ACES filmic curve: the image is in 0..1
// display space and a 5x-white tone map would crush it to black.
//
// Pipeline:
//   1. sRGB -> linear (gentle, only for physical ops)
//   2. Raytrace shadow PCF soft edges
//   3. Cold/warm color split
//   4. Exposure
//   5. Contrast S-curve
//   6. Saturation / vignette / shadow lift
//   7. linear -> sRGB
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

// PCF shadow sampling (3x3 soft edges). Offsets in Viewport units so the
// footprint matches the actual render-target size regardless of window.
float sampleShadowPCF(float2 uv, float depth)
{
    float2 texel = float2(Viewport.z, Viewport.w);
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
    return saturate(sum / 9.0);
}

// Mild cold/warm tint.  Applied in sRGB display space (NOT linearised) so a
// fully-shadowed scene can't be washed into a purple cast.
float3 colorSplitSrgb(float3 color, float shadowMask, float warmthAmt)
{
    float3 coldTint = float3(0.96, 0.98, 1.03);
    float3 warmTint = float3(1.03, 1.01, 0.97);

    color = lerp(color, color * coldTint, shadowMask * warmthAmt * 0.45);
    color = lerp(color, color * warmTint, (1.0 - shadowMask) * warmthAmt * 0.20);

    return color;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

float4 main(VSOutput input) : SV_TARGET
{
    float2 uv = input.uv;

    // 1. Sample inputs.  fx:0 is sRGB display space.
    float4 skyOut = InColor.SampleLevel(InSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);
    float shadow = sampleShadowPCF(uv, depth);
    float3 srgb = skyOut.rgb;

    // 2. Raytrace shadow (depth > 0 only).  Modest darkening with a cold
    //    tint, so a fully-shadowed frame can't nuke the scene.
    float shadowMask = 0.0;
    if (depth > 1e-6)
    {
        shadowMask = 1.0 - shadow;
        float3 shadowColor = float3(0.60, 0.64, 0.74);
        srgb = lerp(srgb, srgb * shadowColor, shadowMask * 0.30);
    }

    // 3. Cold/warm split (sRGB space).
    srgb = colorSplitSrgb(srgb, shadowMask, saturate(O_WARMTH));

    // 4. Linearise for physical ops.
    float3 color = srgbToLinear(srgb);

    // 5. Exposure.
    color *= saturate(O_EXPOSURE);

    // 6. Contrast S-curve (linear space, before re-encoding).
    float contrast = saturate(O_CONTRAST);
    color = lerp(color, (color - 0.5) * (1.0 + contrast * 0.7) + 0.5, contrast);

    // 7. Saturation.
    float sat = saturate(O_SATURATION);
    float lum = dot(color, float3(0.299, 0.587, 0.114));
    color = lerp(float3(lum, lum, lum), color, sat);

    // 8. Vignette.
    float vig = saturate(O_VIGNETTE);
    float2 vigUV = (uv - 0.5) * 2.0;
    float vigMask = 1.0 - dot(vigUV, vigUV) * vig * 0.4;
    color *= clamp(vigMask, 0.0, 1.0);

    // 9. Shadow lift.
    float lift = saturate(O_SHADOW_LIFT);
    color = max(color, lift * color + lift * 0.015);

    // 10. linear -> sRGB.
    color = linearToSrgb(color);

    return float4(color, 1.0);
}
