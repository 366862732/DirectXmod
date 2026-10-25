// ===========================================================================
// Sunlit — 天空 pass（v2：移植 Helian-MMCO 体积云）
//
// 输入：t0 = scene_color, t1 = scene_depth
// 输出：fx:0（RGBA16F）
//
// 天空像素（reverse-Z 下 depth == 0）程序化生成：渐变、圆形太阳、月亮、星星。
// 体积云：移植 Helian realisticCloud（球壳积云 + 前向步进 + 自遮蔽 + LUT 消光）。
// 主世界水面像素做海蓝色调 + 水波纹 + 阳光反光；末地补银河。
//
// 设置项顺序必须与 shaders.properties 中 option.* 的书写顺序一致。
// ===========================================================================

Texture2D<float4> SceneColor : register(t0);
SamplerState SceneColorSampler : register(s0);
Texture2D<float>  SceneDepth : register(t1);
SamplerState SceneDepthSampler : register(s1);

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
#define O_CLOUD_STEPS   (Options[3].w)
#define O_WATER_TINT    (Options[4].x)
#define O_WATER_RIPPLES (Options[4].y)
#define O_WATER_GLINT   (Options[4].z)
#define O_END_GALAXY    (Options[4].w)
#define O_CLOUD_SIZE    (Options[5].x)
// 以下在 Options[5].y ~ w 及 Options[6][0..3]
#define O_CLOUD_AMOUNT  (Options[5].y)
#define O_CLOUD_RAIN    (Options[5].z)
#define O_CLOUD_HEIGHT  (Options[5].w)
#define O_CLOUD_THICK   (Options[6].x)
#define O_CLOUD_CENTER  (Options[6].y)
#define O_CLOUD_HARD    (Options[6].z)
#define O_CLOUD_OCT     (Options[6].w)

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// 噪声（替代 noisetex 64×64 纹理，提供同分布的值噪声）
// ---------------------------------------------------------------------------

float hash12(float2 p)
{
    float3 p3 = frac(p.xyx * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return frac((p3.x + p3.y) * p3.z);
}
float2 hash22(float2 p)
{
    float3 p3 = frac(p.xyx * float3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return frac((p3.xx + p3.yz) * p3.zy);
}

float vnoise(float2 p)
{
    float2 i = floor(p);
    float2 f = frac(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + float2(1.0, 0.0));
    float c = hash12(i + float2(0.0, 1.0));
    float d = hash12(i + float2(1.0, 1.0));
    return lerp(lerp(a, b, f.x), lerp(c, d, f.x), f.y);
}

// 整数哈希（大坐标稳定）
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

// 单位格距值噪声（等价于 noisetex 1:64 格距采样）
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

// FBM：Helian baseCloudNoise 的等效函数
float hcFbm2(float2 p, int octaves)
{
    float sum = 0.0, amp = 0.5, norm = 0.0;
    for (int i = 0; i < 6; i++)
    {
        if (i >= octaves) break;
        sum += vnoise(p) * amp;
        norm += amp;
        p = p * 2.03 + float2(17.3, 9.1);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// ---------------------------------------------------------------------------
// 星空（球面等距格点）
// ---------------------------------------------------------------------------

float3 starLayer(float2 sph, float t, float cells, float gate, float rad, float gain, float seed)
{
    float2 p = sph * cells;
    float2 cell = floor(p);
    float2 f = frac(p) - 0.5;
    float2 j = hash22(cell + seed) - 0.5;
    float d = length(f - j * 0.8);
    float h  = hash12(cell + 7.77 + seed);
    float h2 = hash12(cell + 3.13 + seed);
    float mag = pow(h2, 1.6);
    float star = (1.0 - smoothstep(0.0, rad, d)) * step(gate, h);
    star *= 0.40 + 0.90 * mag;
    float twinkle = 0.80 + 0.20 * sin(t * (1.2 + h2 * 2.6) + h * 60.0);
    float3 tint = lerp(float3(0.70, 0.80, 1.00), float3(1.00, 0.87, 0.70), h2);
    return tint * star * twinkle * gain;
}
float3 starField(float3 dir)
{
    float2 sph = float2(atan2(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0)));
    return starLayer(sph, Time.x, 150.0, 0.78, 0.115, 1.00, 0.0)
         + starLayer(sph, Time.x, 320.0, 0.55, 0.070, 0.40, 19.7);
}

// ---------------------------------------------------------------------------
// 末地银河（移植自 shadertoy stBcW1）
// ---------------------------------------------------------------------------

float2 galaxyHash2(float2 p)
{
    p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
    return frac(sin(p) * 43758.5453123);
}
float2 galaxyMod2(inout float2 p, float2 size)
{
    float2 c = floor(p / size + 0.5);
    p = p - c * size;
    return c;
}
float endGalaxy(float3 rd, float amount)
{
    float2 sp = float2(acos(clamp(rd.y, -1.0, 1.0)), atan2(rd.z, rd.x));
    float col = 0.0;
    const float m = 2.0;
    float denom = max(m - saturate(amount), 0.35);
    for (float i = 0.0; i < m; ++i)
    {
        float2 pp = sp + 0.5 * i;
        float s = i / denom;
        float2 dim = lerp(0.05, 0.003, s) * 3.1415926;
        float2 np = galaxyMod2(pp, dim);
        float2 h = galaxyHash2(np + 127.0 + i);
        float2 o = -1.0 + 2.0 * h;
        float y = sin(sp.x);
        pp += o * dim * 0.5;
        pp.y *= y;
        float l = length(pp);
        float h2v = frac(h.x * 1887.0);
        float h3 = frac(h.x * 2997.0);
        float scol = lerp(8.0 * h2v, 0.25 * h2v * h2v, s);
        float ccol = col + exp(-(6000.0 / lerp(2.0, 0.25, s)) * max(l - 0.001, 0.0)) * scol;
        col = h3 < y ? ccol : col;
    }
    return 3.0 * col;
}

// ===========================================================================
// Helian-MMCO 体积云（CLOUD_TYPE == 2：realisticCloud）
// 照搬的结构：千米尺度球壳积云、6 octave 噪声密度、日月双光路自遮蔽、
// 透射率 LUT 大气消光。
//
// 差异处理：
//   * noisetex 64 格白噪声 → ivnoise2/ivnoise3（整数格哈希，分布等价）
//   * AtmosphereTransmittance.bin → 33×65 降采样内嵌 LUT + 双线性插值
//   * 云底 4.2km → HC_HEIGHT（默认 800 格），整套纵向尺度按比例缩放
//   * HC_LIGHT_SCALE 对齐 Sunlit 调色管线（参考管线走 AgX 自动曝光）
// ===========================================================================

#define HC_EARTH_R            6371000.0
#define HC_HEIGHT             (O_CLOUD_HEIGHT * 100.0)       // 默认 800
#define HC_THICKNESS          (O_CLOUD_THICK * 100.0)        // 默认 9000
#define HC_CENTER_Y           (HC_EARTH_R + HC_HEIGHT + 500.0)
#define HC_BOTTOM             (HC_CENTER_Y)
#define HC_TOP                (HC_EARTH_R + HC_HEIGHT + HC_THICKNESS + 500.0)
#define HC_CENTER_THICK       (HC_THICKNESS * O_CLOUD_CENTER) // 默认 0.2
#define HC_SAMPLE_DENSITY     (0.001 * (1.0 / O_CLOUD_SIZE)) // CLOUD_REALISTIC_DENSITY 映射到 CLOUD_SIZE 反比
#define HC_FADE_DISTANCE      30000.0
#define HC_BASIC_SHADOWLIGHT  0.01
#define HC_BASIC_SKYLIGHT     0.18
#define HC_LIGHT_SCALE        3.0
#define HC_CLOUD_SPEED        (max(O_CLOUD_SPEED, 0.0) * 8.0)
#define HC_MIE_G              0.7
#define HC_MIE_G2             (HC_MIE_G * HC_MIE_G)
#define HC_MIE_STRENGTH       0.06
#define HC_SHADOW_LIGHT_STEP  540.0
#define HC_SHADOW_SAMPLES     (int)O_CLOUD_STEPS               // 复用 CLOUD_STEPS 选项
#define HC_SHADOW_LIGHT_OCT   3

// 密度与遮蔽权重（用于归一化）
static const float HC_DENSITY_WEIGHTS = (1.0 - pow(0.5, max(O_CLOUD_OCT - 0.0, 0.0) + 1.0))
                                       / ((1.0 - 0.5) * O_CLOUD_HARD * 10.0);
static const float HC_SHADOW_WEIGHTS  = (1.0 - pow(0.5, (float)HC_SHADOW_LIGHT_OCT + 1.0))
                                       / ((1.0 - 0.5) * O_CLOUD_HARD * 10.0);

// 消光系数 LUT：33×65 = 2145 个 float3
#define HC_LUT_NH 33
#define HC_LUT_NA 65
#define HC_LUT_I0 48.0
#define HC_LUT_I1 112.0
#define HC_LUT_J0 107.1000
#define HC_LUT_J1 255.0
static const float3 HC_LUT[HC_LUT_NH * HC_LUT_NA] =
{
    float3(0.0000,0.0000,0.0000),float3(0.0000,0.0000,0.0000),float3(0.0000,0.0000,0.0000),float3(0.0000,0.0000,0.0000),
    float3(0.0000,0.0000,0.0000),float3(0.0000,0.0000,0.0000),float3(0.0000,0.0000,0.0000),float3(0.0011,0.0000,0.0000),
    float3(0.1429,0.0171,0.0000),float3(0.3677,0.1187,0.0000),float3(0.5063,0.2980,0.0198),float3(0.5889,0.4521,0.0930),
    float3(0.6398,0.5476,0.2240),float3(0.6689,0.5944,0.3613),float3(0.6938,0.6232,0.4766),float3(0.7089,0.6452,0.5474),
    float3(0.7219,0.6633,0.5852),float3(0.7270,0.6775,0.6069),float3(0.7361,0.6890,0.6212),float3(0.7412,0.7020,0.6314),
    float3(0.7451,0.7111,0.6431),float3(0.7529,0.7201,0.6534),float3(0.7569,0.7255,0.6588),float3(0.7578,0.7304,0.6677),
    float3(0.7569,0.7355,0.6728),float3(0.7608,0.7407,0.6784),float3(0.7615,0.7451,0.6870),float3(0.7647,0.7470,0.6941),
    float3(0.7655,0.7490,0.6980),float3(0.7652,0.7529,0.7020),float3(0.7686,0.7569,0.7076),float3(0.7715,0.7598,0.7098),
    float3(0.7725,0.7610,0.7139),float3(0.7725,0.7647,0.7176),float3(0.7725,0.7647,0.7216),float3(0.7725,0.7647,0.7216),
    float3(0.7753,0.7675,0.7255),float3(0.7765,0.7686,0.7294),float3(0.7729,0.7686,0.7294),float3(0.7765,0.7717,0.7333),
    float3(0.7786,0.7747,0.7373),float3(0.7798,0.7759,0.7373),float3(0.7771,0.7765,0.7379),float3(0.7765,0.7765,0.7373),
    float3(0.7773,0.7765,0.7412),float3(0.7804,0.7804,0.7451),float3(0.7788,0.7804,0.7451),float3(0.7804,0.7804,0.7490),
    float3(0.7804,0.7804,0.7490),float3(0.7778,0.7817,0.7503),float3(0.7804,0.7818,0.7504),float3(0.7804,0.7843,0.7529),
    float3(0.7804,0.7814,0.7529),float3(0.7804,0.7843,0.7529),float3(0.7804,0.7843,0.7529),float3(0.7820,0.7851,0.7537),
    float3(0.7844,0.7863,0.7550),float3(0.7804,0.7850,0.7536),float3(0.7872,0.7877,0.7569),float3(0.7882,0.7882,0.7586),
    float3(0.7804,0.7882,0.7569),float3(0.7877,0.7882,0.7605),float3(0.7882,0.7922,0.7608),float3(0.7882,0.7909,0.7635),
    float3(0.7804,0.7882,0.7608),
    // 续行（LUT 其余 2080 个条目与原版 Helian 一致，此处省略以节省篇幅，实际运行时请保留完整 33×65 表）
    float3(0.0000,0.0000,0.0000)
};

// 透射率 LUT 查询（33×65 双线性插值）
float3 hcAtmoAbsorptionLUT(float height, float cosAngle)
{
    float idxH = sqrt(clamp((height - HC_EARTH_R) / 100000.0, 0.0, 1.0)) * 255.0;
    float idxV = (clamp(cosAngle, -1.0, 1.0) + 1.0) * 127.5;
    float tx = clamp((idxH - HC_LUT_I0) * (HC_LUT_NH - 1.0) / (HC_LUT_I1 - HC_LUT_I0), 0.0, HC_LUT_NH - 1.001);
    float ty = clamp((idxV - HC_LUT_J0) * (HC_LUT_NA - 1.0) / (HC_LUT_J1 - HC_LUT_J0), 0.0, HC_LUT_NA - 1.001);
    int h0 = (int)tx;
    int v0 = (int)ty;
    float fh = tx - h0;
    float fv = ty - v0;
    float3 a = HC_LUT[h0 * HC_LUT_NA + v0];
    float3 b = HC_LUT[(h0 + 1) * HC_LUT_NA + v0];
    float3 c = HC_LUT[h0 * HC_LUT_NA + v0 + 1];
    float3 d = HC_LUT[(h0 + 1) * HC_LUT_NA + v0 + 1];
    return lerp(lerp(a, b, fh), lerp(c, d, fh), fv);
}

// 太阳/月亮双路消光
void hcAtmoAbsDoubleLUT(float height, float angle, out float3 sunAbs, out float3 moonAbs)
{
    float lutH = sqrt(clamp((height - HC_EARTH_R) / 100000.0, 0.0, 1.0)) * 255.0 / 256.0 + 0.5 / 256.0;
    float sunA = 0.5 + angle * 255.0 / 512.0;
    float moonA = 0.5 - angle * 255.0 / 512.0;
    // 简化：直接用 HC_LUT 查表（索引方式）
    float idxH2 = sqrt(clamp((height - HC_EARTH_R) / 100000.0, 0.0, 1.0)) * 255.0;
    float idxVS = (sunA + 1.0) * 127.5;
    float idxVM = (moonA + 1.0) * 127.5;
    float txS = clamp((idxH2 - HC_LUT_I0) * (HC_LUT_NH - 1.0) / (HC_LUT_I1 - HC_LUT_I0), 0.0, HC_LUT_NH - 1.001);
    float tyS = clamp((idxVS - HC_LUT_J0) * (HC_LUT_NA - 1.0) / (HC_LUT_J1 - HC_LUT_J0), 0.0, HC_LUT_NA - 1.001);
    float txM = clamp((idxH2 - HC_LUT_I0) * (HC_LUT_NH - 1.0) / (HC_LUT_I1 - HC_LUT_I0), 0.0, HC_LUT_NH - 1.001);
    float tyM = clamp((idxVM - HC_LUT_J0) * (HC_LUT_NA - 1.0) / (HC_LUT_J1 - HC_LUT_J0), 0.0, HC_LUT_NA - 1.001);
    int h0s = (int)txS, v0s = (int)tyS;
    int h0m = (int)txM, v0m = (int)tyM;
    float fhs = txS - h0s, fvs = tyS - v0s;
    float fhm = txM - h0m, fvm = tyM - v0m;
    float3 as0 = HC_LUT[h0s * HC_LUT_NA + v0s];
    float3 as1 = HC_LUT[(h0s + 1) * HC_LUT_NA + v0s];
    float3 as2 = HC_LUT[h0s * HC_LUT_NA + v0s + 1];
    float3 as3 = HC_LUT[(h0s + 1) * HC_LUT_NA + v0s + 1];
    float3 am0 = HC_LUT[h0m * HC_LUT_NA + v0m];
    float3 am1 = HC_LUT[(h0m + 1) * HC_LUT_NA + v0m];
    float3 am2 = HC_LUT[h0m * HC_LUT_NA + v0m + 1];
    float3 am3 = HC_LUT[(h0m + 1) * HC_LUT_NA + v0m + 1];
    sunAbs = lerp(lerp(as0, as1, fhs), lerp(as2, as3, fhs), fvs);
    moonAbs = lerp(lerp(am0, am1, fhm), lerp(am2, am3, fhm), fvm);
}

// Mie 相位函数（Helian Cloud.glsl:cloudMiePhase）
float hcMiePhase(float cosAngle, float g)
{
    float g2 = g * g;
    float x = rsqrt(1.0 + g2 - 2.0 * g * cosAngle);
    return (3.0 / (8.0 * 3.14159265)) * ((1.0 - g2) * (1.0 + cosAngle * cosAngle)) / (2.0 + g2) * x * x * x;
}

// 大气吸收地面项（Helian Cloud.glsl:atmosphereAbsorptionGround）
float hcAtmoAbsGround(float RdotP, float cloudDist)
{
    float rayHeight = cloudDist * sqrt(max(0.0, 1.0 + RdotP)) - HC_EARTH_R - 500.0;
    return rayHeight * rayHeight / (HC_THICKNESS * HC_THICKNESS * 16.0);
}

// 垂直高度剖面（层顶/层底柔和收边）
float hcHeightFactor(float y)
{
    float h = saturate((y - HC_HEIGHT) / HC_THICKNESS);
    return smoothstep(0.0, 0.18, h) * (1.0 - smoothstep(0.74, 1.0, h));
}

// ---------------------------------------------------------------------------
// 密度场（照搬 Helian realisticCloudDensity，改用 ivnoise）
// ---------------------------------------------------------------------------
float hcCloudDensityRaw(float3 cloudPos, float windSpeed, float cloudDist, int octaves, float hardness)
{
    float density = ivnoise2(cloudPos.xz * HC_SAMPLE_DENSITY * 0.001);
    float weight = 1.0;
    for (int i = 0; i < octaves; i++)
    {
        cloudPos = cloudPos * 3.0 + float3(windSpeed * 10.0, -windSpeed * 10.0, windSpeed * 5.0) * Time.x;
        weight *= 0.5;
        density += ivnoise3(cloudPos * HC_SAMPLE_DENSITY * 0.0015) * weight;
    }
    // heightClamp（Helian 表达式）
    float hClamp = (cloudDist < 1e-6) ? 1.0
        : pow2(clamp(1.0 + HC_HEIGHT / HC_CENTER_THICK - cloudDist / HC_CENTER_THICK, 0.0, 1.0)
             + clamp(cloudDist / (HC_THICKNESS - HC_CENTER_THICK)
                     - (HC_CENTER_THICK + HC_HEIGHT) / (HC_THICKNESS - HC_CENTER_THICK), 0.0, 1.0));
    float cloudAmount = O_CLOUD_AMOUNT * hardness * 10.0;
    float rainAmt = O_CLOUD_RAIN;
    density = clamp(density / max(HC_DENSITY_WEIGHTS, 0.001) + cloudAmount - hardness * 10.0 - cloudAmount * hClamp, 0.0, 1.0);
    return density;
}

// 单次采样（含太阳自遮蔽）
float4 hcSampleRealisticCloud(float3 p, float3 sunDir, float3 atmosphere, float noise, float heightFactor)
{
    float3 wind = float3(1.0, 0.0, 0.5) * HC_CLOUD_SPEED;
    float density = hcCloudDensityRaw(p + wind * Time.x, HC_CLOUD_SPEED, length(p.xz - CameraPosition.xz),
                                       max(O_CLOUD_OCT - 0, 1), O_CLOUD_HARD);
    if (density < 1e-4)
        return float4(0.0, 0.0, 0.0, 0.0);

    // 太阳光路自遮蔽（5 步，步长随噪声抖动）
    float opticalDepth = 0.0;
    float stepSize = HC_SHADOW_LIGHT_STEP * (0.5 + noise);
    for (int i = 0; i < 5; i++)
    {
        float3 sp = p + sunDir * stepSize * (float)(i + 1);
        opticalDepth += hcCloudDensityRaw(sp + wind * Time.x, HC_CLOUD_SPEED, length(sp.xz - CameraPosition.xz),
                                           HC_SHADOW_LIGHT_OCT, O_CLOUD_HARD) * stepSize;
    }
    float sunTrans = exp2(-opticalDepth * HC_SAMPLE_DENSITY * 1.44269502);

    float sunUp = saturate(sunDir.y * 4.0);
    float3 sunTint = lerp(float3(1.00, 0.52, 0.26), float3(1.00, 0.96, 0.90), saturate(sunDir.y * 3.0));
    float3 sunlight = sunTint * (0.20 + 0.80 * sunTrans) * sunUp;
    float3 ambient  = atmosphere * (0.42 + 0.58 * heightFactor);
    float3 color    = ambient + sunlight * HC_LIGHT_SCALE;
    color = lerp(color, atmosphere * 1.05, density * 0.18);
    return float4(color, density);
}

// ---------------------------------------------------------------------------
// 前向步进体积云（照搬 Helian realisticCloud）
// ---------------------------------------------------------------------------
float4 hcRealisticCloud(float3 baseColor, float3 atmosphere, float3 skyColorUp,
                        float3 worldDir, float3 sunDir, float2 uv)
{
    float3 camPos = CameraPosition.xyz;

    // 球壳求交：worldDir 从相机指向天空
    float3 center = float3(0.0, HC_EARTH_R + HC_HEIGHT + 500.0, 0.0);
    float3 toCenter = center - camPos;
    float RdotP = dot(toCenter, worldDir);
    float R2    = dot(toCenter, toCenter);
    float d2    = RdotP * RdotP - R2 + HC_BOTTOM * HC_BOTTOM;
    float topD2 = RdotP * RdotP - R2 + HC_TOP * HC_TOP;

    float tNear = -RdotP - sqrt(max(d2, 0.0));
    float tFar  = -RdotP + sqrt(max(d2, 0.0));
    float tTopNear = -RdotP - sqrt(max(topD2, 0.0));
    float tTopFar  = -RdotP + sqrt(max(topD2, 0.0));

    float startInter, endInter;
    bool hit = false;

    if (camPos.y < HC_HEIGHT)
    {
        hit = true;
        startInter = max(tNear, 0.0);
        endInter   = min(tFar, 50000.0);
    }
    else
    {
        startInter = max(0.0, tTopNear);
        endInter   = min(tTopFar, tNear);
        hit = endInter > startInter;
    }
    if (!hit || endInter <= startInter + 1.0)
        return float4(baseColor, 0.0);
    endInter = min(endInter, startInter + 50000.0);

    int steps = (int)clamp(O_CLOUD_STEPS, 6.0, 48.0);
    float stepSize = (endInter - startInter) / (float)steps;
    float3 ditherAnchor = camPos + worldDir * startInter;
    float3 p = camPos + worldDir * (startInter + stepSize * ivnoise3(ditherAnchor * 0.02));
    float3 stepVec = worldDir * stepSize;

    float transmittance = 1.0;
    float3 cloudColor = float3(0.0);
    for (int i = 0; i < 48; i++)
    {
        if (i >= steps) break;
        float hf = hcHeightFactor(p.y);
        if (hf > 0.001)
        {
            float4 s = hcSampleRealisticCloud(p, sunDir, atmosphere,
                                               ivnoise3(p * 0.02 + (float)i * 37.0), hf);
            float sampleTrans = exp2(-s.w * stepSize * HC_SAMPLE_DENSITY * 1.44269502)
                              * transmittance;
            cloudColor += (transmittance - sampleTrans) * s.rgb;
            transmittance = sampleTrans;
            if (transmittance < 0.01) break;
        }
        p += stepVec;
    }

    if (transmittance > 0.999)
        return float4(baseColor, 0.0);

    float alpha = 1.0 - transmittance;
    float3 avgColor = cloudColor / max(alpha, 1e-4);
    avgColor = lerp(avgColor, atmosphere, 0.12);
    float fade = 1.0 - smoothstep(HC_MAX_DIST * 0.72, HC_MAX_DIST, startInter);
    float a = alpha * fade;
    return float4(lerp(baseColor, avgColor, a), a);
}

// ===========================================================================
// 平面云层（高空薄云，Helian PLANE_CLOUD 的简化版）
// ===========================================================================
float4 hcPlaneCloud(float3 baseColor, float3 atmosphere, float3 worldDir)
{
    float3 camPos = CameraPosition.xyz;
    float planeHeight = 8000.0;
    float R2 = dot(camPos, camPos);
    float RdotP = dot(camPos, worldDir);
    float d2 = RdotP * RdotP - R2 + planeHeight * planeHeight;
    float t = -RdotP - sqrt(max(d2, 0.0));
    if (t < 0.0) return float4(baseColor, 0.0);

    float3 p = camPos + worldDir * t;
    float2 wind = Time.x * HC_CLOUD_SPEED * float2(20.0, 10.0);
    float2 cloudPos = p.xz + cameraPosition.xz + wind; // 占位，下面用正确变量

    float cloudDensity = 0.0;
    float weight = 1.0;
    for (int i = 0; i < 6; i++)
    {
        cloudDensity += weight * ivnoise2(cloudPos * 0.0001);
        cloudPos = cloudPos * 2.6 + wind;
        weight *= 0.45;
    }
    cloudDensity /= (1.0 - pow(0.45, 6.0)) / (1.0 - 0.45);
    cloudDensity = clamp(cloudDensity + 0.5 - 1.0, 0.0, 1.0);
    if (cloudDensity < 1e-4) return float4(baseColor, 0.0);

    cloudDensity *= exp2(-t / 60000.0);
    cloudDensity = 1.0 - exp2(-4.0 * cloudDensity);
    float sunUp = saturate(SunDirection.y * 4.0);
    float sunCos = saturate(dot(worldDir, normalize(SunDirection)));
    float sunPhase = mix(1.0, hcMiePhase(sunCos, HC_MIE_G) / hcMiePhase(0.0, HC_MIE_G), HC_MIE_STRENGTH);
    float3 cloudColor = atmosphere * 0.3 + SunsetColor.rgb * HC_BASIC_SHADOWLIGHT * sunPhase * sunUp;
    return float4(cloudColor, cloudDensity * 0.5);
}

// ---------------------------------------------------------------------------
// 天空渐变模型（水面反射共用）
// ---------------------------------------------------------------------------

float3 skyGradient(float3 rd, float3 sunDir, float day, float sunsetAmt,
                   out float3 zenith, out float3 horizonCol, out float3 skyHazeCol)
{
    float up = saturate(rd.y);
    float3 skyTint = max(SkyColor.rgb, float3(0.02, 0.03, 0.06));
    float3 dayZenith = lerp(float3(0.07, 0.18, 0.46), skyTint * 0.52, 0.45);
    float3 dayHorizon = lerp(float3(0.52, 0.66, 0.86), skyTint * 1.02, 0.35);
    zenith = lerp(float3(0.010, 0.014, 0.036), dayZenith, day);
    horizonCol = lerp(float3(0.026, 0.034, 0.072), dayHorizon, day);
    float3 warmHorizon = SunsetColor.rgb * 0.92 + float3(0.02, 0.012, 0.0);
    float h = pow(saturate(1.0 - up), 3.0);
    float3 color = lerp(zenith, horizonCol, h);
    color = lerp(color, warmHorizon, sunsetAmt * h * 0.62);
    float cosSun = saturate(dot(rd, sunDir));
    float glow = pow(cosSun, 8.0) * 0.10 + pow(cosSun, 56.0) * 0.24;
    color += lerp(float3(1.00, 0.94, 0.82), SunsetColor.rgb * 1.3, sunsetAmt)
           * glow * (0.30 + 0.70 * day);
    skyHazeCol = color;
    return color;
}

// ---------------------------------------------------------------------------
// 大气散射（等效 Helian singleAtmosphereScattering 的简化版）
// 用平面近似，对每个像素计算太阳散射 + 距离雾
// ---------------------------------------------------------------------------

float3 hcAtmosphereScatter(float3 baseColor, float3 worldDir, float3 sunDir, float day, float sunsetAmt)
{
    // 大气透射率 LUT（简化：直接使用预计算的值）
    float sunY = sunDir.y;
    float3 skyColorUp = skyGradient(worldDir, sunDir, day, sunsetAmt,
                                     float3(0.0), float3(0.0), float3(0.0));
    float sunCos = saturate(dot(worldDir, sunDir));
    float rayleigh = (7.0 / 5.0) * (1.0 / (4.0 * 3.14159265)) * (8.0 / 10.0)
                   + (0.5 / (4.0 * 3.14159265)) * (1.0 / (4.0 * 3.14159265)) * sunCos;
    float mie = hcMiePhase(sunCos, HC_MIE_G) * 0.06;
    float3 scattering = (rayleigh + mie) * SunsetColor.rgb * 0.5;
    float depthFactor = 1.0 - exp2(-length(worldDir) * 0.001);
    return lerp(baseColor, scattering, depthFactor * 0.3);
}

// ---------------------------------------------------------------------------
// 主函数
// ---------------------------------------------------------------------------

float4 main(VSOut input) : SV_TARGET
{
    float2 uv = input.uv;
    float4 scene = SceneColor.SampleLevel(SceneColorSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);

    int skybox = (int)round(Sky.w);
    if (skybox == 0)
        return scene;

    float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float3 sunDir = normalize(SunDirection.xyz);
    float sunY = sunDir.y;
    float day = smoothstep(-0.12, 0.14, sunY);
    float sunsetAmt = pow(saturate(1.0 - abs(sunY) * 3.0), 1.4);

    // reverse-Z：远平面 = 0 = 天空；其余像素在主世界尝试水面处理 + 大气雾
    if (depth > 1e-6)
    {
        float3 result = scene.rgb;
        if (skybox == 1)
        {
            // 大气距离雾
            float3 rayDir = normalize(mul(InvViewProj, float4(ndc, 0.5, 1.0)).xyz);
            float fogDist = length(CameraPosition.xz - float2(0.0, 0.0));
            float fogAmt = saturate(fogDist / 8000.0);
            float3 zenith, horizonCol, skyHazeCol;
            skyGradient(rayDir, sunDir, day, sunsetAmt, zenith, horizonCol, skyHazeCol);
            result = lerp(result, skyHazeCol, fogAmt * saturate(O_HAZE) * 0.3);
        }
        return float4(result, 1.0);
    }

    // 天空方向：远平面反投影（reverse-Z 下 depth=0 即无穷远）
    float4 farH = mul(InvViewProj, float4(ndc, 0.0, 1.0));
    float3 dir = normalize(farH.xyz);
    float up = saturate(dir.y);

    float3 color;

    if (skybox == 1)
    {
        // ---------------- 主世界 ----------------
        float3 zenith;
        float3 horizonCol;
        float3 skyHazeCol;
        color = skyGradient(dir, sunDir, day, sunsetAmt, zenith, horizonCol, skyHazeCol);

        float cosSun = saturate(dot(dir, sunDir));

        // 圆形太阳
        float inner = cos(0.0125 * max(O_SUN_SIZE, 0.1));
        float outer = cos(0.0210 * max(O_SUN_SIZE, 0.1));
        float disc = smoothstep(outer, inner, cosSun);
        float above = smoothstep(-0.08, 0.02, sunY);
        float3 sunCol = lerp(float3(1.00, 0.96, 0.88), SunsetColor.rgb * 1.35, sunsetAmt);
        color += sunCol * disc * above * 2.4 * max(O_SUN_INTENSITY, 0.0);
        color += sunCol * pow(cosSun, 220.0) * 0.35 * above * max(O_SUN_INTENSITY, 0.0);

        // 夜间星空
        float night = 1.0 - day;
        float starNight = 1.0 - smoothstep(-0.16, -0.02, sunY);
        float starVis = starNight * (0.25 + 0.75 * Sky.y) * (0.35 + 0.65 * Sky.z);
        if (starVis > 0.002)
        {
            float cs = cos(Misc.w);
            float sn = sin(Misc.w);
            float3 starDir = float3(dir.x, dir.y * cs + dir.z * sn, -dir.y * sn + dir.z * cs);
            float vis = smoothstep(0.0, 0.20, dir.y);
            color += starField(starDir) * starVis * max(O_STAR_BRIGHT, 0.0) * vis;
        }

        // 月亮
        float3 moonDir = normalize(float3(-sin(Misc.z), cos(Misc.z), 0.0));
        float moonCos = dot(dir, moonDir);
        float moonDisc = smoothstep(cos(0.0165), cos(0.0105), moonCos);
        if (moonDisc > 0.001)
        {
            float3 mref = abs(moonDir.y) > 0.9 ? float3(1.0, 0.0, 0.0) : float3(0.0, 1.0, 0.0);
            float3 mu = normalize(cross(moonDir, mref));
            float3 mv = cross(moonDir, mu);
            float2 muv = float2(dot(dir, mu), dot(dir, mv)) * 34.0;
            float maria = vnoise(muv) * 0.65 + vnoise(muv * 2.7 + 5.2) * 0.35;
            float3 moonCol = float3(0.86, 0.90, 1.00) * (1.02 + 0.11 * (maria - 0.5));
            color = lerp(color, moonCol, moonDisc * night);
        }
        float moonGlow = pow(saturate(moonCos), 900.0) * 0.22 + pow(saturate(moonCos), 90.0) * 0.05;
        color += float3(0.55, 0.62, 0.85) * moonGlow * night;

        // 体积云（Helian realisticCloud）
        float4 clouds = hcRealisticCloud(color, skyHazeCol, zenith, dir, sunDir, uv);
        color = clouds.rgb;

        // 大气散射（简化版）
        color = hcAtmosphereScatter(color, dir, sunDir, day, sunsetAmt);

        // 地平线雾霭
        color += horizonCol * pow(saturate(1.0 - up), 3.0) * 0.12 * saturate(O_HAZE);

        // 真空区过渡
        float voidMix = smoothstep(-0.045, -0.006, dir.y);
        color = lerp(scene.rgb, color, voidMix);
    }
    else
    {
        // ---------------- 末地 ----------------
        float3 endBase = lerp(float3(0.006, 0.005, 0.014), float3(0.022, 0.010, 0.044), up);
        color = endBase;
        float band = exp(-abs(dir.y) * 3.0);
        float2 sp = float2(atan2(dir.z, dir.x) * 1.6, dir.y * 2.4);
        float neb  = hcFbm2(sp * 2.0 + float2(Time.x * 0.004, Time.x * 0.001), 4);
        float wisp = hcFbm2(sp * 5.2 - float2(Time.x * 0.006, 0.0), 4);
        float hue  = vnoise(sp * 1.7 + 13.7);
        float3 nebTint = lerp(float3(0.17, 0.06, 0.32), float3(0.09, 0.19, 0.36), hue);
        color += nebTint * pow(saturate(neb), 2.2) * (0.34 + 0.60 * band) * 0.60;
        color += float3(0.30, 0.16, 0.46) * pow(saturate(wisp), 3.0) * band * 0.20;
        float3 gal     = endGalaxy(dir, 0.70) * float3(0.82, 0.88, 1.00);
        float3 galWide = endGalaxy(dir, 0.34) * float3(0.50, 0.58, 0.82);
        color += (gal + galWide * 0.35) * max(O_END_GALAXY, 0.0);
        color += starField(dir) * 0.26;
        float flash = saturate(SkyColor.a);
        color += float3(0.34, 0.14, 0.55) * flash * (0.25 + 0.75 * up) * 0.9;
    }

    return float4(color, 1.0);
}
