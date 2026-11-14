// ===========================================================================
// Foxy DX12 — Screen Space Reflections (HLSL, SM 5.1, single-file self-contained)
//
// Inputs:
//   t0 = fx:0 (RGBA16F) — sky / clouds / fog composite (already lit world)
//   t1 = scene_depth    — reverse-Z (0 = sky/far, > 0 = geometry)
//
// Output:
//   fx:2 (RGBA16F) — the input color plus a screen-space reflection term.
//
// Params (pipeline.json):
//   x = reflection strength   (0..1)
//   y = max ray distance      (blocks)
//   z = hit thickness / bias  (blocks)
//   w = march steps           (4..64)
//
// Normals are reconstructed from depth derivatives; the ray is marched in
// camera-relative world space (InvViewProj output) and re-projected with the
// frame's ViewProj. cbuffer ShaderPackUniforms is auto-injected.
// ===========================================================================

Texture2D<float4> Source : register(t0);
SamplerState SourceSampler : register(s0);
Texture2D<float> SceneDepth : register(t1);
SamplerState DepthSampler : register(s1);

#define SSR_STRENGTH  (PassParams.x)
#define SSR_MAXDIST   (PassParams.y)
#define SSR_THICKNESS (PassParams.z)
#define SSR_STEPS     (PassParams.w)

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// (uv, depth) -> camera-relative world position (camera at the origin)
float3 relFromUv(float2 uv, float depth)
{
    float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float4 rel = mul(InvViewProj, float4(ndc, depth, 1.0));
    return rel.xyz / max(abs(rel.w), 1e-4);
}

// camera-relative world -> uv
float2 uvFromRel(float3 rel)
{
    float4 clip = mul(ViewProj, float4(rel, 1.0));
    float w = (abs(clip.w) < 1e-6) ? 1e-6 : clip.w;
    float2 ndc = clip.xy / w;
    return float2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
}

float4 main(VSOut input) : SV_TARGET
{
    float2 uv = input.uv;
    float3 color = Source.SampleLevel(SourceSampler, uv, 0).rgb;
    float depth = SceneDepth.SampleLevel(DepthSampler, uv, 0.0);

    float strength = saturate(SSR_STRENGTH);
    int steps = (int)clamp(SSR_STEPS, 4.0, 64.0);
    if (strength < 0.005 || depth <= 1e-6 || SSR_MAXDIST < 0.5)
        return float4(color, 1.0);

    // --- reconstruct position + normal (depth-space derivatives) ---
    float2 texel = Viewport.zw;
    float2 uvR = uv + float2(texel.x, 0.0);
    float2 uvD = uv + float2(0.0, texel.y);
    float3 pC = relFromUv(uv, depth);
    float3 pR = relFromUv(uvR, SceneDepth.SampleLevel(DepthSampler, uvR, 0.0));
    float3 pD = relFromUv(uvD, SceneDepth.SampleLevel(DepthSampler, uvD, 0.0));

    float3 n = normalize(cross(pR - pC, pD - pC));
    float3 V = normalize(pC);                 // camera sits at the origin
    if (dot(n, -V) < 0.0) n = -n;             // always face the camera

    float3 R = reflect(V, n);
    if (dot(R, n) < 0.02)
        return float4(color, 1.0);            // reflection points into the surface

    // --- march the reflected ray through the depth buffer ---
    float stepLen = SSR_MAXDIST / (float)steps;
    float tolerance = max(SSR_THICKNESS, stepLen * 1.5);
    float3 p = pC + n * (SSR_THICKNESS + 0.05) + R * stepLen;
    float2 hitUv = uv;
    bool hit = false;

    for (int i = 0; i < steps; ++i)
    {
        float2 suv = uvFromRel(p);
        if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0)
            break;
        float sd = SceneDepth.SampleLevel(DepthSampler, suv, 0.0);
        if (sd > 1e-6)
        {
            float3 sp = relFromUv(suv, sd);
            float rayDist = length(p);
            float sceneDist = length(sp);
            // hit = the ray passed behind the depth surface, but not too far
            if (rayDist > sceneDist && (rayDist - sceneDist) < tolerance)
            {
                hit = true;
                hitUv = suv;
                break;
            }
        }
        p += R * stepLen;
    }

    if (hit)
    {
        float2 edge = min(hitUv, float2(1.0, 1.0) - hitUv);
        float edgeFade = smoothstep(0.0, 0.06, min(edge.x, edge.y));
        float distFade = saturate(1.0 - length(p - pC) / SSR_MAXDIST);
        float fresnel = 0.04 + 0.96 * pow(1.0 - saturate(dot(n, -V)), 5.0);
        float3 refl = Source.SampleLevel(SourceSampler, hitUv, 0).rgb;
        color += refl * (edgeFade * distFade * fresnel * strength);
    }

    return float4(color, 1.0);
}
