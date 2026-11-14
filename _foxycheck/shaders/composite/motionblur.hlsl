// ===========================================================================
// Foxy DX12 — Motion Blur (HLSL, SM 5.1, single-file self-contained)
//
// Inputs:
//   t0 = fx:2 (RGBA16F)      — fully composited frame (after SSR)
//   t1 = history_color       — previous frame's final image (RGBA16F)
//   t2 = scene_depth         — reverse-Z (0 = sky/far, > 0 = geometry)
//
// Output:
//   fx:3 (RGBA16F) — camera motion blur (reprojected velocity) + light
//                    temporal accumulation with clamped history.
//
// Params (pipeline.json):
//   x = blur strength multiplier
//   y = max blur length in pixels
//   z = history blend weight (0 = pure current frame)
//   w = reserved
//
// Reprojection: ViewProj / PrevViewProj / PrevCameraPosition are supplied by
// the framework every frame. cbuffer ShaderPackUniforms is auto-injected.
// ===========================================================================

Texture2D<float4> Source : register(t0);
SamplerState SourceSampler : register(s0);
Texture2D<float4> History : register(t1);
SamplerState HistorySampler : register(s1);
Texture2D<float> SceneDepth : register(t2);
SamplerState DepthSampler : register(s2);

#define MB_STRENGTH      (PassParams.x)
#define MB_MAX_PIXELS    (PassParams.y)
#define MB_HISTORY_BLEND (PassParams.z)

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

float3 relFromUv(float2 uv, float depth)
{
    float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float4 rel = mul(InvViewProj, float4(ndc, depth, 1.0));
    return rel.xyz / max(abs(rel.w), 1e-4);
}

float4 main(VSOut input) : SV_TARGET
{
    float2 uv = clamp(input.uv, float2(0.0, 0.0), float2(1.0, 1.0));
    float3 color = Source.SampleLevel(SourceSampler, uv, 0).rgb;
    float depth = SceneDepth.SampleLevel(DepthSampler, uv, 0.0);

    // --- reproject this pixel into the previous frame ---
    float2 prevUv = uv;
    bool moved = false;
    if (depth > 1e-6 && MB_STRENGTH > 0.001)
    {
        float3 rel = relFromUv(uv, depth);
        float3 prevRel = rel + CameraPosition.xyz - PrevCameraPosition.xyz;
        float4 prevClip = mul(PrevViewProj, float4(prevRel, 1.0));
        if (abs(prevClip.w) > 1e-5)
        {
            prevUv = float2(prevClip.x / prevClip.w * 0.5 + 0.5,
                            0.5 - prevClip.y / prevClip.w * 0.5);
            moved = true;
        }
    }

    // --- velocity in uv space, clamped to a pixel budget ---
    float2 vel = moved ? (uv - prevUv) * MB_STRENGTH : float2(0.0, 0.0);
    float maxLenUv = max(MB_MAX_PIXELS, 1.0) * Viewport.w;   // Viewport.w = 1/height
    float len = length(vel);
    if (len > maxLenUv)
        vel *= maxLenUv / len;

    // --- gather along the velocity vector (camera motion blur) ---
    if (length(vel) > Viewport.w * 0.5)
    {
        const int taps = 8;
        float3 acc = float3(0.0, 0.0, 0.0);
        for (int i = 0; i < taps; ++i)
        {
            float t = (float)i / (float)(taps - 1);
            float2 suv = clamp(uv - vel * t, float2(0.0, 0.0), float2(1.0, 1.0));
            acc += Source.SampleLevel(SourceSampler, suv, 0).rgb;
        }
        color = acc / (float)taps;
    }

    // --- temporal blend with the reprojected history (clamped to avoid smear) ---
    if (moved && MB_HISTORY_BLEND > 0.0013)
    {
        float3 hist = History.SampleLevel(HistorySampler,
            clamp(prevUv, float2(0.0, 0.0), float2(1.0, 1.0)), 0).rgb;
        float3 current = Source.SampleLevel(SourceSampler, uv, 0).rgb;
        hist = clamp(hist, current * 0.5, current * 2.0 + 0.25);
        color = lerp(color, hist, saturate(MB_HISTORY_BLEND));
    }

    return float4(color, 1.0);
}
