// ===========================================================================
// Foxy DX12 — Final Pass (HLSL, SM 5.1, single-file self-contained)
//
// Inputs:
//   t0 = fx:3 (RGBA16F) — motion-blurred composite (fx:0 → SSR fx:2 → blur fx:3)
//   t1 = fx:1 (RGBA16F) — ray-traced shadow visibility (1 = lit, 0 = shadow)
//   t2 = scene_depth (reverse-Z, 0 = sky/far, > 0 = geometry)
//
// Output:
//   scene_color — final image after all post-processing:
//     PCF shadow → sRGB→linear → exposure → bloom (single-tap approx) →
//     tone map (None/ACES/AGX/Filmic/Reinhard/ACES2/Hable) →
//     saturation → warm/cool split → shadow lift → night lift →
//     Purkinje → vignette → dither.
//
// cbuffer ShaderPackUniforms is auto-injected; do NOT redeclare.
// Option mapping (24 options, n -> Options[n/4][n%4]):
//   [0] BRIGHTNESS, SATURATION, CONTRAST, VIGNETTE
//   [1] EXPOSURE, TONEMAPPER, AGX_LOOK, BLOOM_STRENGTH
//   [2] SUN_INTENSITY, MOON_INTENSITY, NIGHT_BRIGHTNESS, PURKINJE
//   [3] CLOUD_COVERAGE, CLOUD_SPEED, CLOUD_STEPS, CLOUD_SIZE
//   [4] CLOUD_HEIGHT, CLOUD_THICK, CLOUD_DENSITY, HAZE
//   [5] WARMTH, SHADOW_LIFT, WATER_TINT, RAIN
// ===========================================================================

Texture2D<float4> SkyOut  : register(t0);
SamplerState SkySampler  : register(s0);
Texture2D<float4> Shadow  : register(t1);
SamplerState ShadowSampler : register(s1);
Texture2D<float>  SceneDepth : register(t2);
SamplerState DepthSampler  : register(s2);

#define O_BRIGHTNESS     (Options[0].x)
#define O_SATURATION     (Options[0].y)
#define O_CONTRAST       (Options[0].z)
#define O_VIGNETTE       (Options[0].w)
#define O_EXPOSURE       (Options[1].x)
#define O_TONEMAPPER     (Options[1].y)
#define O_AGX_LOOK       (Options[1].z)
#define O_BLOOM_STRENGTH (Options[1].w)
#define O_SUN_INTENSITY  (Options[2].x)
#define O_MOON_INTENSITY (Options[2].y)
#define O_NIGHT_BRIGHT   (Options[2].z)
#define O_PURKINJE       (Options[2].w)
#define O_CLOUD_COVER    (Options[3].x)
#define O_CLOUD_SPEED    (Options[3].y)
#define O_CLOUD_STEPS    (Options[3].z)
#define O_CLOUD_SIZE     (Options[3].w)
#define O_CLOUD_HEIGHT   (Options[4].x)
#define O_CLOUD_THICK    (Options[4].y)
#define O_CLOUD_DENSITY  (Options[4].z)
#define O_HAZE           (Options[4].w)
#define O_WARMTH         (Options[5].x)
#define O_SHADOW_LIFT    (Options[5].y)
#define O_WATER_TINT     (Options[5].z)
#define O_RAIN           (Options[5].w)

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// sRGB ↔ linear
// ---------------------------------------------------------------------------
float3 srgbToLinear(float3 c) { return pow(max(c, 0.0), 2.2); }
float3 linearToSrgb(float3 c) { return pow(saturate(c), 1.0 / 2.2); }

// ---------------------------------------------------------------------------
// Tone mappers
// ---------------------------------------------------------------------------
float3 tmapNone(float3 x)      { return saturate(x); }

float3 tmapACES(float3 x)
{
    const float3 A = float3(2.51, 2.43, 2.45);
    const float3 B = float3(0.03, 0.027, 0.025);
    const float3 C = float3(2.43, 2.40, 2.39);
    const float3 D = float3(0.59, 0.55, 0.50);
    const float3 E = float3(0.14, 0.125, 0.108);
    return saturate((x * (A * x + B)) / (x * (C * x + D) + E));
}

// AGX approximation (used by Foxy)
float3 tmapAGX(float3 x, float look)
{
    float a = lerp(0.75, 1.10, saturate(look / 4.0));
    float b = lerp(0.10, 0.045, saturate(look / 4.0));
    float3 c = x / (x + a);
    c = c * (1.0 - c) * 4.0;
    c = lerp(c, c * c * (3.0 - 2.0 * c), b * saturate(look / 4.0));
    return saturate(c);
}

float3 tmapFilmic(float3 x)
{
    const float a = 2.51, b = 0.02, c = 2.43, d = 0.59, e = 0.14;
    return saturate(x * (a * x + b) / (x * (c * x + d) + e));
}

float3 tmapReinhard(float3 x)
{
    return x / (1.0 + x);
}

float3 tmapACES2(float3 x)
{
    float3 t = tmapAGX(x, max(O_AGX_LOOK, 1.0));
    t = t * t * (3.0 - 2.0 * t);
    return saturate(t);
}

float3 tmapHable(float3 x)
{
    const float inputMax = 8.0;
    x /= inputMax;
    x = max(x, 0.0);
    float a = 0.266, b = 0.5, c = 0.999, d = 0.13, e = 0.20, f = 0.01;
    x = ((x * (a * x + c * b) + d * e) / (x * (a * x + b) + d * f)) - e / f;
    return saturate(x);
}

float3 toneMap(float3 x, float mode, float look)
{
    if      (mode < 0.5) return tmapNone(x);
    else if (mode < 1.5) return tmapACES(x);
    else if (mode < 2.5) return tmapAGX(x, look);
    else if (mode < 3.5) return tmapFilmic(x);
    else if (mode < 4.5) return tmapReinhard(x);
    else if (mode < 5.5) return tmapACES2(x);
    else                 return tmapHable(x);
}

// ---------------------------------------------------------------------------
// PCF shadow (8-tap ring, radius grows with distance)
// ---------------------------------------------------------------------------
float pcfsShadow(float2 uv, float radiusPx)
{
    static const float2 kOffsets[8] =
    {
        float2( 0.00,  1.00), float2( 0.71,  0.71),
        float2( 1.00,  0.00), float2( 0.71, -0.71),
        float2( 0.00, -1.00), float2(-0.71, -0.71),
        float2(-1.00,  0.00), float2(-0.71,  0.71)
    };
    float ang = frac(sin(dot(uv * Viewport.xy, float2(12.9898, 78.233))) * 43758.5453) * 6.2831853;
    float cs = cos(ang); float sn = sin(ang);
    float2 px = Viewport.zw * radiusPx;
    float sum = 0.0;
    for (int i = 0; i < 8; ++i)
    {
        float2 o = kOffsets[i];
        float2 r = float2(o.x * cs - o.y * sn, o.x * sn + o.y * cs) * px;
        sum += saturate(Shadow.SampleLevel(ShadowSampler, uv + r, 0.0).r);
    }
    return sum * 0.125;
}

// ---------------------------------------------------------------------------
// Bloom (single-tap downsampled approximation — good enough for a port)
// ---------------------------------------------------------------------------
float3 bloomSample(float2 uv, float strength)
{
    if (strength < 0.005) return float3(0.0, 0.0, 0.0);
    // 5-tap cross
    float2 px = Viewport.zw * 4.0;
    float3 acc = SkyOut.SampleLevel(SkySampler, uv, 0).rgb * 2.0;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2( px.x, 0), 0).rgb;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2(-px.x, 0), 0).rgb;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2(0,  px.y), 0).rgb;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2(0, -px.y), 0).rgb;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2( px.x, px.y), 0).rgb * 0.5;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2(-px.x, px.y), 0).rgb * 0.5;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2( px.x,-px.y), 0).rgb * 0.5;
    acc += SkyOut.SampleLevel(SkySampler, uv + float2(-px.x,-px.y), 0).rgb * 0.5;
    float thr = 0.45;
    float3 b = max(acc - thr, 0.0);
    return b * (strength * 0.012);
}

// ---------------------------------------------------------------------------
// Warmth / cool split (Foxy style: cool shadows, warm highlights)
// ---------------------------------------------------------------------------
float3 warmCoolSplit(float3 c, float shadowMask, float warmth)
{
    float3 cold = float3(0.88, 0.94, 1.12);
    float3 warm = float3(1.10, 1.02, 0.86);
    c = lerp(c, c * cold, shadowMask * warmth * 0.6);
    c = lerp(c, c * warm, (1.0 - shadowMask) * warmth * 0.35);
    return c;
}

// ---------------------------------------------------------------------------
// Dither (4x4 BlueNoise-style table)
// ---------------------------------------------------------------------------
float dither4x4(float2 uv)
{
    int2 i = int2(uv * Viewport.xy) & 3;
    static const float dither[16] =
    {
        0.00, 8.00, 2.00, 10.00,
        12.00,4.00, 14.00,6.00,
        3.00, 11.00,1.00, 9.00,
        15.00,7.00, 5.00, 13.00
    };
    return dither[(i.y & 3) * 4 + (i.x & 3)] / 16.0 - 0.5;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
float4 main(VSOut input) : SV_TARGET
{
    float2 uv = input.uv;

    // Read sky pass output (in linear HDR via fx:0 RGBA16F)
    float3 c = SkyOut.SampleLevel(SkySampler, uv, 0).rgb;
    float depth = SceneDepth.SampleLevel(DepthSampler, uv, 0.0);

    // === PCF shadow from ray-traced shadow map ===
    float sunUp = saturate(SunDirection.y * 6.0);
    float vis = 1.0;
    float shadowStrength = saturate(PassParams.x); // = 0.85 from pipeline.json
    if (shadowStrength > 0.001 && sunUp > 0.001 && depth > 1e-6)
    {
        float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
        float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));
        float dist = length(rel4.xyz / max(rel4.w, 1e-4));
        float radius = lerp(0.7, 4.5, saturate(dist / 260.0));
        vis = pcfsShadow(uv, radius);
    }
    float3 shadowTint = lerp(float3(0.52, 0.58, 0.74), float3(1.0, 1.0, 1.0), vis);
    c *= lerp(float3(1.0, 1.0, 1.0), shadowTint, shadowStrength * sunUp);

    // === sRGB → linear ===
    float3 lin = srgbToLinear(c);

    // === Exposure ===
    lin *= max(O_EXPOSURE, 0.05) * max(O_BRIGHTNESS, 0.05);

    // === Bloom (add back) ===
    float3 bloom = bloomSample(uv, O_BLOOM_STRENGTH);
    lin += bloom;

    // === Warm/cool split ===
    lin = warmCoolSplit(lin, 1.0 - vis, O_WARMTH);

    // === Tone map ===
    lin = toneMap(lin, O_TONEMAPPER, O_AGX_LOOK);

    // === Saturation ===
    float luma = dot(lin, float3(0.2126, 0.7152, 0.0722));
    lin = lerp(float3(luma, luma, luma), lin, saturate(O_SATURATION));

    // === Soft S-curve (contrast) ===
    float3 sCurve = lin * lin * (3.0 - 2.0 * lin);
    lin = lerp(lin, sCurve, saturate(O_CONTRAST) * 0.55);

    // === Shadow lift (prevents dead black) ===
    lin += O_SHADOW_LIFT;

    // === Night brightness (lifts night scene slightly) ===
    float night = saturate(1.0 - smoothstep(-0.12, 0.22, SunDirection.y));
    lin += night * O_NIGHT_BRIGHT * 0.35;

    // === Purkinje (night blue shift) ===
    if (O_PURKINJE > 0.005 && night > 0.005)
    {
        float3 shifted = lin * float3(0.82, 0.94, 1.18) * saturate(O_PURKINJE);
        lin = lerp(lin, shifted, night * saturate(O_PURKINJE));
    }

    // === Vignette ===
    float2 d = uv - 0.5;
    lin *= 1.0 - saturate(O_VIGNETTE) * dot(d, d) * 1.5;

    // === Final sRGB + dither ===
    lin = linearToSrgb(lin);
    lin += dither4x4(uv) / 255.0;

    return float4(saturate(lin), 1.0);
}
