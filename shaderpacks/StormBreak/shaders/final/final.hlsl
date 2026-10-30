// ===========================================================================
// StormBreak — 最终调色 pass（DEBUG 透传模式）
//
// 输入：t0 = fx:0, t1 = fx:1, t2 = scene_depth
// 输出：scene_color
//
// DEBUG: 暂时只透传 t0（sky pass 的输出），验证管线是否正确。
// 确认管线 OK 后恢复完整调色逻辑。
// ===========================================================================

Texture2D<float4> InColor : register(t0);
SamplerState InSampler : register(s0);
Texture2D<float4> ShadowTex : register(t1);
SamplerState ShadowSampler : register(s1);
Texture2D<float> SceneDepth : register(t2);
SamplerState SceneDepthSampler : register(s2);

struct VSOutput
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

float4 main(VSOutput input) : SV_TARGET
{
    float2 uv = input.uv;
    // 纯透传：直接把 sky pass 的输出（fx:0）写回 scene_color
    return InColor.SampleLevel(InSampler, uv, 0.0);
}
