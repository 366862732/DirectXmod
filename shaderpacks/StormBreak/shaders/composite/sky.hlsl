// ===========================================================================
// StormBreak — 天空 pass（暴风雨前夕）
//
// 输入：t0 = scene_color, t1 = scene_depth
// 输出：fx:0（RGBA16F）
//
// 天空像素（reverse-Z 下 depth == 0）程序化生成：
//   1. 暗灰色暴风雨云层（移植体积云引擎，参数调暗）
//   2. 不规则光缝（大尺度 FBM 在云层中撕开对角裂口）
//   3. 丁达尔光束（光缝沿太阳方向投射的锥形体积光）
//   4. 光缝处的暖金色阳光（穿透云层的耀眼高光）
// 地面/建筑像素：云影斑驳光斑 + 大气雾
//
// 设置项顺序必须与 shaders.properties 中 option.* 的书写顺序一致。
// ===========================================================================

Texture2D<float4> SceneColor : register(t0);
Texture2D<float>  SceneDepth : register(t1);
SamplerState SceneColorSampler : register(s0);
SamplerState SceneDepthSampler : register(s1);

// [Options[0]]
#define O_BRIGHTNESS    (Options[0].x)
#define O_SATURATION    (Options[0].y)
#define O_CONTRAST      (Options[0].z)
#define O_VIGNETTE      (Options[0].w)
// [Options[1]]
#define O_CLOUD_COVER   (Options[1].x)
#define O_CLOUD_SPEED   (Options[1].y)
#define O_LIGHT_BEAM    (Options[1].z)
#define O_BEAM_SPREAD   (Options[1].w)
// [Options[2]]
#define O_SHADOW_STORM  (Options[2].x)
#define O_SUN_INTENSITY (Options[2].y)
#define O_HAZE          (Options[2].z)
#define O_EXPOSURE      (Options[2].w)
// [Options[3]]
#define O_WARMTH        (Options[3].x)
#define O_SHADOW_LIFT   (Options[3].y)
#define O_SHARPEN       (Options[3].z)
#define O_CLOUD_STEPS   (Options[3].w)
// [Options[4]]
#define O_CLOUD_SIZE    (Options[4].x)
#define O_CLOUD_AMOUNT  (Options[4].y)
#define O_CLOUD_RAIN    (Options[4].z)
#define O_CLOUD_HEIGHT  (Options[4].w)
#define O_CLOUD_THICK   (Options[5].x)

struct VSOutput
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// 噪声（整数格点哈希，大坐标稳定）
// ---------------------------------------------------------------------------

float ihash12(int2 p)
{
    uint h = uint(p.x) * 374761393u + uint(p.y) * 668265263u + 1442695041u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0xFFFFFFu) * (1.0 / 16777215.0);
}

float ihash13(int3 p)
{
    uint h = uint(p.x) * 374761393u + uint(p.y) * 668265263u + uint(p.z) * 1274126177u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0xFFFFFFu) * (1.0 / 16777215.0);
}

float ivnoise2(float2 p)
{
    float2 i = floor(p);
    float2 f = frac(p);
    f = f * f * (3.0 - 2.0 * f);
    int2 ii = int2(i);
    float a = ihash12(ii);
    float b = ihash12(ii + int2(1, 0));
    float c = ihash12(ii + int2(0, 1));
    float d = ihash12(ii + int2(1, 1));
    return lerp(lerp(a, b, f.x), lerp(c, d, f.x), f.y);
}

float ivnoise3(float3 p)
{
    float3 i = floor(p);
    float3 f = frac(p);
    f = f * f * (3.0 - 2.0 * f);
    int3 ii = int3(i);
    float n000 = ihash13(ii);
    float n100 = ihash13(ii + int3(1, 0, 0));
    float n010 = ihash13(ii + int3(0, 1, 0));
    float n110 = ihash13(ii + int3(1, 1, 0));
    float n001 = ihash13(ii + int3(0, 0, 1));
    float n101 = ihash13(ii + int3(1, 0, 1));
    float n011 = ihash13(ii + int3(0, 1, 1));
    float n111 = ihash13(ii + int3(1, 1, 1));
    float nx00 = lerp(n000, n100, f.x);
    float nx10 = lerp(n010, n110, f.x);
    float nx01 = lerp(n001, n101, f.x);
    float nx11 = lerp(n011, n111, f.x);
    return lerp(lerp(nx00, nx10, f.y), lerp(nx01, nx11, f.y), f.z);
}

float fbm4(float2 p)
{
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++)
    {
        sum += ivnoise2(p) * amp;
        p = p * 2.03 + float2(17.3, 9.1);
        amp *= 0.5;
    }
    return sum / 0.9375;
}

// ---------------------------------------------------------------------------
// 大气雾（空气透视）
// ---------------------------------------------------------------------------

static const float3 HC_ASF_BETA = float3(7.5e-5, 1.45e-4, 4.25e-4);

float3 hcAirFog(float3 color, float2 ndc, float depth,
                float3 sunDir, float day, float sunsetAmt)
{
    float strength = saturate(O_HAZE) * 3.0;
    if (strength <= 0.003) return color;

    float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));
    float3 relPos = rel4.xyz / max(abs(rel4.w), 1e-4);
    float dist = length(relPos);
    if (dist < 0.5) return color;
    float3 worldDir = relPos / dist;

    strength *= clamp(300.0 / max(Misc.x, 96.0), 0.75, 1.75);

    float weather = saturate(1.0 - Sky.z);
    float skyLight = lerp(0.22, 1.0, day);

    float opt = dist * strength * (1.0 + 2.5 * weather * weather);
    float3 absorb = exp(-opt * HC_ASF_BETA);

    // 散射色：暴风雨前的冷灰蓝
    float3 scatter = lerp(float3(0.28, 0.32, 0.40), float3(0.45, 0.50, 0.58), skyLight);
    scatter *= skyLight;

    // 太阳方向暖光（米氏前向散射近似）
    float cosSunDir = dot(worldDir, sunDir);
    float sunTint = lerp(float3(1.0, 0.96, 0.90), SunsetColor.rgb * 1.35, sunsetAmt);
    float phase = pow(saturate(cosSunDir), 8.0) * 0.55;
    float sunUp = smoothstep(-0.10, 0.06, sunDir.y);
    scatter += sunTint * phase * sunUp * skyLight;

    return scatter * (1.0 - absorb) + color * absorb;
}

// ---------------------------------------------------------------------------
// 体积云引擎（移植自 Sunlit，参数调为暴风雨模式）
// ---------------------------------------------------------------------------

#define HC_EARTH_R            6371000.0
#define HC_HEIGHT             (O_CLOUD_HEIGHT * 100.0)
#define HC_CLOUD_SCALE_DOWN   (4200.0 / HC_HEIGHT)
#define HC_THICKNESS          (O_CLOUD_THICK * 100.0)
#define HC_CLOUD_TOP_Y        (HC_HEIGHT + HC_THICKNESS)
#define HC_SAMPLE_DENSITY     (0.02 * HC_CLOUD_SCALE_DOWN)
#define HC_CLOUD_SPEED        (max(O_CLOUD_SPEED, 0.0) * 8.0 / HC_CLOUD_SCALE_DOWN)
#define HC_MAX_DIST           14000.0

float3 hcCloudWind()
{
    return Time.x * HC_CLOUD_SPEED * float3(10.0, 0.0, 5.0);
}

float hcHeightFactor(float y)
{
    float h = saturate((y - HC_HEIGHT) / HC_THICKNESS);
    return smoothstep(0.0, 0.18, h) * (1.0 - smoothstep(0.74, 1.0, h));
}

float hcCloudDensity(float3 p, float3 wind, float heightFactor)
{
    float sizeScale = rcp(clamp(O_CLOUD_SIZE, 0.2, 1.0));
    float3 q = p + wind;
    q.y -= HC_HEIGHT;

    float d = ivnoise2(q.xz * (0.00030 * sizeScale));
    float d3 = ivnoise3(q * (0.00025 * sizeScale));
    d = d * 0.82 + d3 * 0.18;

    // 暴风雨云：阈值更低（更厚）、密度增益更大
    float threshold = lerp(0.55, 0.22, saturate(O_CLOUD_COVER));
    float density = saturate((d - threshold) * 3.8);
    return density * heightFactor;
}

float hcSampleDither(float3 worldPos, int i)
{
    return ivnoise3(worldPos * 0.02 + (float)i * 37.0);
}

float hcStartDither(float3 worldPos)
{
    return ivnoise3(worldPos * 0.02);
}

float4 hcSampleRealisticCloud(float3 p, float3 sunDir, float3 atmosphere,
                              float noise, float heightFactor)
{
    float3 wind = hcCloudWind();
    float density = hcCloudDensity(p, wind, heightFactor);
    if (density < 1e-4)
    {
        return float4(0.0, 0.0, 0.0, 0.0);
    }

    float opticalDepth = 0.0;
    float stepSize = 90.0 * (0.5 + noise);
    for (int i = 0; i < 4; i++)
    {
        float t = stepSize * (float)(i + 1);
        float3 sp = p + sunDir * t;
        opticalDepth += hcCloudDensity(sp, wind, hcHeightFactor(sp.y)) * stepSize;
    }
    float sunTrans = exp2(-opticalDepth * HC_SAMPLE_DENSITY * 1.44269502);

    float sunUp = saturate(sunDir.y * 4.0);
    float3 sunTint = lerp(float3(0.85, 0.72, 0.50), float3(1.00, 0.88, 0.65),
                          saturate(sunDir.y * 3.0));
    float3 sunlight = sunTint * (0.08 + 0.12 * sunTrans) * sunUp;

    float3 ambient = atmosphere * (0.30 + 0.35 * heightFactor);
    float3 color = ambient + sunlight * 0.85;

    color = lerp(color, atmosphere * 0.85, density * 0.25);
    return float4(color, density);
}

float4 hcRealisticCloud(float3 baseColor, float3 atmosphere, float3 skyColorUp,
                        float3 worldDir, float3 sunDir, float2 uv)
{
    float3 camPos = CameraPosition.xyz;
    float bottom = HC_HEIGHT;
    float top = HC_CLOUD_TOP_Y;

    float tStart = 0.0;
    float tEnd = 0.0;
    if (abs(worldDir.y) < 1e-4)
    {
        if (camPos.y < bottom || camPos.y > top)
        {
            return float4(baseColor, 0.0);
        }
        tStart = 0.0;
        tEnd = HC_MAX_DIST;
    }
    else
    {
        float t0 = (bottom - camPos.y) / worldDir.y;
        float t1 = (top - camPos.y) / worldDir.y;
        tStart = max(min(t0, t1), 0.0);
        tEnd = min(max(t0, t1), HC_MAX_DIST);
    }
    if (tEnd <= tStart + 1.0)
    {
        return float4(baseColor, 0.0);
    }

    int steps = (int)clamp(O_CLOUD_STEPS, 6.0, 32.0);
    float stepSize = (tEnd - tStart) / (float)steps;
    float3 ditherAnchor = camPos + worldDir * tStart;
    float3 p = camPos + worldDir * (tStart + stepSize * hcStartDither(ditherAnchor));
    float3 stepVec = worldDir * stepSize;

    float transmittance = 1.0;
    float3 cloudColor = float3(0.0, 0.0, 0.0);
    for (int i = 0; i < 32; i++)
    {
        if (i >= steps) break;
        float hf = hcHeightFactor(p.y);
        if (hf > 0.001)
        {
            float4 s = hcSampleRealisticCloud(p, sunDir, atmosphere,
                                              hcSampleDither(p, i), hf);
            float sampleTrans = exp2(-s.w * stepSize * HC_SAMPLE_DENSITY * 1.44269502)
                              * transmittance;
            cloudColor += (transmittance - sampleTrans) * s.rgb;
            transmittance = sampleTrans;
            if (transmittance < 0.01) break;
        }
        p += stepVec;
    }

    if (transmittance > 0.999)
    {
        return float4(baseColor, 0.0);
    }

    float alpha = 1.0 - transmittance;
    float3 avgColor = cloudColor / max(alpha, 1e-4);
    avgColor = lerp(avgColor, atmosphere, 0.10);
    float fade = 1.0 - smoothstep(HC_MAX_DIST * 0.72, HC_MAX_DIST, tStart);
    float a = alpha * fade;

    return float4(lerp(baseColor, avgColor, a), a);
}

// ---------------------------------------------------------------------------
// 不规则光缝（God Ray Gap）
//
// 大尺度 FBM 在世界 XZ 平面撕开对角裂口，裂口处云层通透。
// 采样点用 相机位置 + 视线方向 × 固定距离（世界锚定，不随视角晃动）。
// ---------------------------------------------------------------------------

float godRayGap(float3 samplePoint, float3 sunDir, float3 camPos)
{
    float2 gapCoord = samplePoint.xz * 0.0004 + samplePoint.y * 0.00008;

    float mainGap = fbm4(gapCoord);
    float ragged = ivnoise2(samplePoint.xz * 0.003) * 0.3;
    float gap = mainGap + ragged;

    float gapWidth = lerp(0.15, 0.60, saturate(O_BEAM_SPREAD));
    float center = 0.5 + sin(gapCoord.x * 1.8) * 0.12;
    float dist = abs(gap - center);
    float openness = 1.0 - smoothstep(gapWidth * 0.3, gapWidth, dist);

    float sunBoost = saturate(dot(normalize(samplePoint - camPos), sunDir) * 0.5 + 0.5);
    openness *= lerp(0.6, 1.0, sunBoost);

    openness *= 1.0 - saturate(O_CLOUD_COVER) * 0.5;

    return openness;
}

// ---------------------------------------------------------------------------
// 丁达尔光束（Volumetric God Rays）
//
// 相机在云层中沿视线方向 march，采样光缝通透度 × 锥形衰减。
// 采样点 = camPos + viewDir × stepDist（世界锚定）。
// ---------------------------------------------------------------------------

float3 godRayBeam(float3 viewDir, float3 sunDir, float3 camPos)
{
    float sunUp = smoothstep(-0.05, 0.15, sunDir.y);
    if (sunUp < 0.01) return float3(0.0);

    float beamAmt = saturate(O_LIGHT_BEAM);
    if (beamAmt <= 0.001) return float3(0.0);

    // 沿视线在云层中 march 5 步，采样光缝
    float totalGap = 0.0;
    float stepDist = 2000.0;
    for (int i = 0; i < 5; i++)
    {
        float3 probe = camPos + viewDir * (stepDist * (float)(i + 1));
        totalGap += godRayGap(probe, sunDir, camPos);
    }
    totalGap *= 0.2;  // 平均

    if (totalGap < 0.02) return float3(0.0);

    // 锥形衰减：视线与太阳方向夹角越小，光束越亮
    float cosBeam = saturate(dot(viewDir, sunDir));
    float beamProfile = pow(cosBeam, lerp(8.0, 2.0, saturate(O_BEAM_SPREAD * 0.4)));

    // 距离衰减（越远离光源越弱）
    float distFade = exp(-stepDist * 0.00012);

    float3 beamColor = float3(1.15, 0.92, 0.55);
    float intensity = totalGap * beamProfile * distFade * beamAmt * 2.5 * sunUp;

    return beamColor * intensity;
}

// ---------------------------------------------------------------------------
// 天空渐变（暴风雨模式）
// ---------------------------------------------------------------------------

float3 stormSkyGradient(float3 rd, float3 sunDir, float day, float sunsetAmt,
                       out float3 zenith, out float3 horizonCol)
{
    float up = saturate(rd.y);

    float3 skyTint = max(SkyColor.rgb, float3(0.02, 0.03, 0.06));
    float3 dayZenith = lerp(float3(0.025, 0.035, 0.070), skyTint * 0.35, 0.40);
    float3 dayHorizon = lerp(float3(0.18, 0.22, 0.30), skyTint * 0.70, 0.30);

    zenith = lerp(float3(0.008, 0.010, 0.022), dayZenith, day);
    horizonCol = lerp(float3(0.020, 0.025, 0.045), dayHorizon, day);

    float h = pow(saturate(1.0 - up), 3.0);
    float3 color = lerp(zenith, horizonCol, h);

    float cosSun = saturate(dot(rd, sunDir));
    float glow = pow(cosSun, 12.0) * 0.06 + pow(cosSun, 64.0) * 0.12;
    float3 sunGlow = lerp(float3(0.80, 0.65, 0.40), SunsetColor.rgb * 0.9, sunsetAmt);
    color += sunGlow * glow * day;

    return color;
}

// ---------------------------------------------------------------------------
// 云影斑驳光斑（Screen-Space Dapple）
// ---------------------------------------------------------------------------

float cloudDapple(float3 worldPos, float3 sunDir)
{
    float shadowShift = sunDir.x * 800.0 - sunDir.z * 600.0;
    float2 dappleCoord = worldPos.xz * 0.0008 + shadowShift;

    float big   = fbm4(dappleCoord);
    float mid   = ivnoise2(dappleCoord * 3.1 + 5.2);
    float fine  = ivnoise2(dappleCoord * 8.7 + 13.1);

    float dapple = big * 0.55 + mid * 0.30 + fine * 0.15;

    float threshold = 0.42;
    float mask = 1.0 - smoothstep(threshold - 0.06, threshold + 0.04, dapple);

    float gap = godRayGap(worldPos, sunDir, CameraPosition.xyz);
    mask *= 1.0 - gap * 0.7;

    return mask;
}

// ---------------------------------------------------------------------------
// 主函数
// ---------------------------------------------------------------------------

float4 main(VSOutput input) : SV_TARGET
{
    float2 uv = input.uv;
    float4 scene = SceneColor.SampleLevel(SceneColorSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);

    int skybox = (int)round(Sky.w);
    if (skybox == 0)
    {
        return scene;
    }

    // ====================================================================
    // DEBUG: 纯色测试 — 红色=天空，蓝色=地面
    // 如果画面变色（天空变红/地面变蓝），管线 OK
    // 如果画面没变（原版），管线有问题
    // ====================================================================
    if (depth <= 1e-6)
    {
        return float4(0.9, 0.1, 0.1, 1.0);  // 天空 → 红色
    }
    else
    {
        return float4(0.1, 0.1, 0.9, 1.0);  // 地面 → 蓝色
    }
}
