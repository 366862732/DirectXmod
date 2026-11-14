// ===========================================================================
// Foxy DX12 — Black Hole Particle Compute Pass (HLSL, SM 5.1)
//
// pipeline.json entry:
//   { "name": "blackhole_particles", "type": "compute",
//     "fragment": "shaders/compute/blackhole_particles.hlsl",
//     "buffers": [ { "name": "blackhole", "stride": 32, "count": 512,
//                    "access": "uav" } ],
//     "groups": [8, 1, 1],
//     "params": [1.0, 0.0, 0.0, 0.0] }
//
// The compute pipeline binds `params` as four 32-bit root constants at b0
// (NOT the injected ShaderPackUniforms cbuffer). Declaring a 4-float cbuffer
// at b0 reads them back.
//
// Writes the accretion-disk particle set the sky pass samples as sparkle
// (StructuredBuffer<BhParticle>, stride 32 = two float4).
// ===========================================================================

cbuffer ComputeParams : register(b0)
{
    float4 Params;   // x = intensity, yzw reserved
};

struct BhParticle
{
    float4 pos;    // x = radius 0..1, y = phase(rad), z = height, w = size
    float4 color;  // rgb = temperature color, a = brightness
};

RWStructuredBuffer<BhParticle> Particles : register(u0);

float hash11(uint i)
{
    uint h = i * 747796405u + 2891336453u;
    h = ((h >> ((h >> 28u) + 4u)) ^ h) * 277803737u;
    h = (h >> 22u) ^ h;
    return float(h & 0xFFFFFFu) * (1.0 / 16777215.0);
}

[numthreads(64, 1, 1)]
void main(uint3 tid : SV_DispatchThreadID)
{
    uint index = tid.x;
    uint count = 0;
    uint stride = 0;
    Particles.GetDimensions(count, stride);
    if (index >= count)
        return;

    float rnd0 = hash11(index * 3u + 1u);
    float rnd1 = hash11(index * 3u + 2u);
    float rnd2 = hash11(index * 3u + 3u);

    // Annulus with a soft inner edge; radius is normalized against the disk.
    float radius = saturate(0.18 + rnd0 * 0.82 + (rnd1 - 0.5) * 0.05);
    float phase = rnd1 * 6.2831853;
    float height = (rnd2 - 0.5) * 2.0 * (1.0 - radius * 0.6);
    float size = 0.4 + rnd2 * 1.6;

    // Inner disk is hotter (blue-white) and brighter than the outer rim.
    float heat = saturate(1.0 - radius);
    float3 color = lerp(float3(1.0, 0.38, 0.08), float3(1.0, 0.92, 0.78),
                        heat * heat);
    float brightness = (0.35 + heat * 1.25) * max(Params.x, 0.0);

    Particles[index].pos = float4(radius, phase, height, size);
    Particles[index].color = float4(color, brightness);
}
