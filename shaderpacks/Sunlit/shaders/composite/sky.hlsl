// ===========================================================================
// Sunlit — 天空 pass
//
// 输入：t0 = scene_color, t1 = scene_depth
// 输出：fx:0（RGBA16F）
//
// 天空像素（reverse-Z 下 depth == 0）程序化生成：渐变、圆形太阳、月亮、星星、大片层云。
// 主世界水面像素做海蓝色调 + 水波纹 + 阳光反光；末地补银河。其余地形像素原样透传。
//
// 设置项顺序必须与 shaders.properties 中 option.* 的书写顺序一致。
// ===========================================================================

Texture2D<float4> SceneColor : register(t0);
Texture2D<float>  SceneDepth : register(t1);
SamplerState SceneColorSampler : register(s0);
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

struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// 噪声
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

float fbm(float2 p, int octaves)
{
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
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

// 三维值噪声：体积云需要在云层厚度里取密度
float hash13(float3 p3)
{
    p3 = frac(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return frac((p3.x + p3.y) * p3.z);
}

float vnoise3(float3 p)
{
    float3 i = floor(p);
    float3 f = frac(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + float3(1.0, 0.0, 0.0));
    float n010 = hash13(i + float3(0.0, 1.0, 0.0));
    float n110 = hash13(i + float3(1.0, 1.0, 0.0));
    float n001 = hash13(i + float3(0.0, 0.0, 1.0));
    float n101 = hash13(i + float3(1.0, 0.0, 1.0));
    float n011 = hash13(i + float3(0.0, 1.0, 1.0));
    float n111 = hash13(i + float3(1.0, 1.0, 1.0));
    float nx00 = lerp(n000, n100, f.x);
    float nx10 = lerp(n010, n110, f.x);
    float nx01 = lerp(n001, n101, f.x);
    float nx11 = lerp(n011, n111, f.x);
    return lerp(lerp(nx00, nx10, f.y), lerp(nx01, nx11, f.y), f.z);
}

float fbm3(float3 p, int octaves)
{
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 5; i++)
    {
        if (i >= octaves) break;
        sum += vnoise3(p) * amp;
        norm += amp;
        p = p * 2.03 + float3(11.7, 5.3, 8.9);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// ---------------------------------------------------------------------------
// 星空（球面等距格点）
// ---------------------------------------------------------------------------

float3 starField(float3 dir)
{
    float2 sph = float2(atan2(dir.z, dir.x), asin(clamp(dir.y, -1.0, 1.0)));
    float2 p = sph * 150.0;
    float2 cell = floor(p);
    float2 f = frac(p) - 0.5;
    float2 j = hash22(cell) - 0.5;
    float dist = length(f - j * 0.8);
    float h = hash12(cell + 7.77);
    float h2 = hash12(cell + 3.13);
    float star = (1.0 - smoothstep(0.0, 0.105, dist)) * step(0.80, h);
    star = pow(star, 0.85);
    float twinkle = 0.70 + 0.30 * sin(Time.x * (1.5 + h2 * 3.0) + h * 60.0);
    float3 tint = lerp(float3(0.70, 0.80, 1.00), float3(1.00, 0.87, 0.70), h2);
    return tint * star * twinkle;
}

// ---------------------------------------------------------------------------
// 末地银河：移植 Helian-MMCO（Sundial Lite）的 endStars()
// 原出处 shadertoy stBcW1 "Stars and galaxy"（CC0）。
//
// 球面等距网格：每层把 (极角, 方位角) 切成大小不一的格子，
// 每格用哈希决定一颗星的位置 / 亮度，指数衰减形成锐利星点；
// 极角越小（越靠近赤道平面）可通过 gate 的星星越多，于是连成一条银河带。
// ---------------------------------------------------------------------------

float2 galaxyHash2(float2 p)
{
    p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
    return frac(sin(p) * 43758.5453123);
}

// 把 p 折回格心附近（p 原地更新为格内坐标），返回格子编号
float2 galaxyMod2(inout float2 p, float2 size)
{
    float2 c = floor(p / size + 0.5);
    p = p - c * size;
    return c;
}

float endGalaxy(float3 rd, float amount)
{
    // rd.xzy 的球面坐标 = 以世界 Y 轴为极轴：(极角, 方位角)
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

// ---------------------------------------------------------------------------
// 体积云：在云层 slab 内步进光线，边走边累积消光与散射
//
// 只有天空像素会进入这里（地形像素在主函数已提前返回），所以步进不会穿过实体。
// 三层结构（远 -> 近合成）：高层卷云 / 主层积云 / 低层碎云，
// 每层高度、尺度、风速都不同，形成上下层次。
// 密度场用「域扭曲 + billow」把噪声拧成卷曲的团块，而不是一张平铺的板。
// ---------------------------------------------------------------------------

struct CloudLayer
{
    float base;      // 层底高度
    float top;       // 层顶高度
    float hScale;    // 水平采样频率
    float vScale;    // 垂直采样频率
    float amount;    // 覆盖量（越大云越连续）
    float edgeLoss;  // 云底/云顶的收边强度
    float density;   // 消光倍率
    float wind;      // 漂移速度倍率
    float billow;    // 卷曲（billow）权重
};

// 连续密度场（参考 Helian-MMCO 的 realistic cloud 做法）：
// 多层 fbm 得到 0..1 的连续密度，而不是用阈值切成硬块；
// 再用「amount - 垂直收边」控制覆盖与上下轮廓，云底云顶自然收拢。
float cloudDensity(float3 pos, CloudLayer L, int octaves)
{
    float h = (pos.y - L.base) / max(L.top - L.base, 1.0);
    if (h <= 0.0 || h >= 1.0)
    {
        return 0.0;
    }

    // 0 = 层中央，1 = 层底/层顶：两端密度衰减
    float edge = abs(h * 2.0 - 1.0);
    float shapeLoss = L.edgeLoss * edge * edge;

    float3 p = float3(pos.x * L.hScale, pos.y * L.vScale, pos.z * L.hScale);
    p.xz += float2(Time.x * 0.0075, Time.x * 0.0025) * L.wind * max(O_CLOUD_SPEED, 0.0);

    // 大尺度域扭曲：把云团拧出卷曲走向。位移控制在一格以内，
    // 之前给到 2 格以上会把所有细节糊成一团漩涡。y 方向也加一点位移，
    // 打断“水平拉丝”，让团块更像立体的云朵而不是一层条纹。
    float3 w = float3(vnoise3(p * 0.55 + float3(3.7, 8.1, 5.3)),
                      vnoise3(p * 0.55 + float3(11.3, 1.9, 17.7)),
                      vnoise3(p * 0.55 + float3(19.3, 4.7, 13.9))) - 0.5;
    p += w * 0.55;

    // 多层 fbm：每层频率 x2.6、幅度 x0.5，堆出菜花状的团块细节
    float n = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    float3 q = p;
    for (int i = 0; i < 6; i++)
    {
        if (i >= octaves) { break; }
        n += vnoise3(q) * amp;
        norm += amp;
        q = q * 2.6 + float3(9.1, 5.7, 13.3);
        amp *= 0.5;
    }
    n /= max(norm, 1e-4);

    // billow：单层噪声做绝对值折叠，得到圆鼓鼓的团块与卷边
    // （若用 fbm，值集中在 0.5 附近，折叠后几乎整片都是 1，云会糊成实心墙）
    float billow = 1.0 - abs(vnoise3(p * 2.6 + 7.7) * 2.0 - 1.0);
    float d = lerp(n, billow, L.billow);

    float dens = d + L.amount - 1.0 - shapeLoss;
    return saturate(dens) * L.density;
}

// 单层步进：返回预乘颜色与不透明度
float4 marchLayer(float3 dir, float3 sunDir, CloudLayer L, int steps, int octaves,
                  float jitter, float maxDist, float3 haze)
{
    float4 result = float4(0.0, 0.0, 0.0, 0.0);

    // 同时支持从下往上看和从云上往下看：只取两个交点之间的区间
    float ta = (L.base - CameraPosition.y) / dir.y;
    float tb = (L.top - CameraPosition.y) / dir.y;
    float t0 = max(min(ta, tb), 0.0);
    float t1 = min(max(ta, tb), maxDist);
    if (t1 - t0 < 1.0) { return result; }

    int n = max(steps, 4);
    float dt = (t1 - t0) / (float)n;

    float cosTheta = dot(dir, sunDir);
    float phase = 0.35 + 0.65 * pow(saturate(cosTheta * 0.5 + 0.5), 3.0);
    float dayAmt = saturate(sunDir.y * 3.0 + 0.22);
    float sunsetAmt = pow(saturate(1.0 - abs(sunDir.y) * 3.0), 1.3);
    float3 sunsetCloud = SunsetColor.rgb * 1.15 + 0.06;

    float transmittance = 1.0;
    float3 scatter = float3(0.0, 0.0, 0.0);

    for (int i = 0; i < 32; i++)
    {
        if (i >= n || transmittance < 0.02)
        {
            break;
        }

        float t = t0 + dt * ((float)i + jitter);
        // 远处云自然变薄：避免在 maxDist 处一刀切出硬边
        float farFade = 1.0 - smoothstep(maxDist * 0.60, maxDist, t);
        if (farFade <= 0.001) { continue; }

        float3 pos = CameraPosition + dir * t;
        float density = cloudDensity(pos, L, octaves) * farFade;
        if (density > 0.002)
        {
            // 朝太阳 3 步（步长递减），估算云内自遮蔽：这是团块立体感的来源
            float shadow = 0.0;
            float w = 1.0;
            for (int j = 1; j <= 3; j++)
            {
                shadow += cloudDensity(pos + sunDir * ((float)j * 46.0), L, 2) * w;
                w *= 0.62;
            }
            float lightTrans = exp(-shadow * 1.05);

            // 云底暗、云顶亮：这是形成上下层次的关键（Helian 用 skylight 高度项）
            float hRel = saturate((pos.y - L.base) / max(L.top - L.base, 1.0));
            float skyH = 0.28 + 0.72 * pow(hRel, 1.6);

            // 天空散射环境光打底 + 太阳直射：厚云芯也不会黑成烟块。
            // 参考图是逆光层云：亮部偏暖奶油色，暗部偏冷灰紫，整体对比不高。
            float3 ambient = lerp(float3(0.075, 0.085, 0.135),
                                  float3(0.50, 0.56, 0.68), dayAmt);
            float3 sunTint = lerp(float3(1.00, 0.85, 0.66),
                                  float3(1.02, 0.98, 0.93),
                                  saturate(sunDir.y * 2.0 + 0.30));
            float3 direct = sunTint * lerp(0.18, 1.15, dayAmt);
            float3 lit = ambient * skyH * 1.15
                       + direct * saturate(lightTrans * phase * 1.10) * (0.55 + 0.45 * skyH);
            lit = lerp(lit, sunsetCloud, sunsetAmt * 0.50);
            lit = min(lit, float3(1.00, 0.97, 0.92));

            // 消光系数调高：让每个像素只看到云的“外表面”，
            // 而不是把整层云的明暗平均成一片灰，立体感来自表面明暗而不是体积平均。
            float alpha = 1.0 - exp(-density * dt * 0.075);
            scatter += lit * alpha * transmittance;
            transmittance *= 1.0 - alpha;
        }
    }

    float alpha = saturate(1.0 - transmittance);
    // 空气透视：远处云融进地平线霞光，而不是变成一条硬边黑带
    float falloff = exp(-max(t0 - 800.0, 0.0) * 0.00028);
    scatter = scatter * falloff + haze * alpha * (1.0 - falloff);
    alpha *= clamp(falloff * 1.35, 0.0, 1.0);

    result.rgb = scatter;
    result.a = alpha;
    return result;
}

float4 volumetricClouds(float3 dir, float3 sunDir, float2 uv, float3 haze)
{
    float4 result = float4(0.0, 0.0, 0.0, 0.0);
    if (abs(dir.y) < 1e-4)
    {
        return result;
    }

    float base = max(Misc.y, 96.0);
    // 覆盖量（Helian 的 amount）：0.62 左右云量接近参考图的大片层积云
    float amount = lerp(0.52, 0.72, saturate(O_CLOUD_COVER));
    float maxDist = 4300.0;

    // 单一连续云体：整块 530 格厚的体积里连续变化的密度场。
    // 之前用三层独立 slab 叠加，飞到高空能明显看出是两片平板；
    // 合成一个体积后，上下表面都由噪声决定，才是真正的立体云。
    // 垂直层次靠 3D 噪声本身（vScale）产生，而不是靠分层。
    CloudLayer cloud;
    cloud.base = base - 110.0;
    cloud.top = base + 420.0;
    cloud.hScale = 0.00100;
    cloud.vScale = 0.0034;
    cloud.amount = amount;
    cloud.edgeLoss = 1.00;
    cloud.density = 1.0;
    cloud.wind = 1.0;
    cloud.billow = 0.55;

    int steps = min((int)clamp(O_CLOUD_STEPS, 6.0, 48.0) + 4, 32);

    // 用屏幕位置抖动起始偏移，把有限步数带来的条带打散（不含时间，避免闪烁）
    float jitter = frac(sin(dot(uv * Viewport.xy, float2(12.9898, 78.233))) * 43758.5453);

    float4 c = marchLayer(dir, sunDir, cloud, steps, 5, jitter, maxDist, haze);

    // 地平线处不是把云切掉，而是让 rgb 与 alpha 同步淡出，
    // 这样云底与天空之间不会出现一条硬缝。
    float fade = smoothstep(0.0, 0.014, abs(dir.y));
    result.rgb = c.rgb * fade;
    result.a = c.a * fade;
    return result;
}

// ---------------------------------------------------------------------------
// 水面着色（只在主世界对“朝上、偏蓝的平坦表面”生效）
//
// 框架没有水掩码，只能从深度反推世界坐标 + 屏幕空间法线来猜：
//   1) 表面法线接近竖直朝上——水面永远水平；
//   2) 颜色明显偏蓝——排除沙、草、土；雪 / 冰被距离衰减压住；
//   3) 远处不做位移，避免像素拉伸伪影。
// 效果 = 多层滚动噪声做屏幕空间扰动（波纹）+ 海蓝色调 + 阳光反光。
// 不做 Helian-MMCO 那种深色水：颜色向海蓝收敛，夜里压暗但保持蓝调可见。
// ---------------------------------------------------------------------------

float3 shadeWater(float4 scene, float2 uv, float2 ndc, float depth, float3 sunDir,
                  float day, float sunsetAmt)
{
    float4 wpH = mul(InvViewProj, float4(ndc, depth, 1.0));
    if (wpH.w <= 1e-6)
    {
        return scene.rgb;
    }
    float3 rel = wpH.xyz / wpH.w;               // 相机相对世界坐标
    float3 worldPos = CameraPosition.xyz + rel;

    // 屏幕空间几何法线：只有近水平朝上的表面才当作水面
    float3 nrm = cross(ddx(worldPos), ddy(worldPos));
    float ny = abs(nrm.y) / max(length(nrm), 1e-9);
    float flatness = smoothstep(0.985, 0.999, ny);

    // 蓝色判定：水体的蓝来自吸收与天空反射，红通道明显更弱
    float3 c = scene.rgb;
    float blueness = smoothstep(0.035, 0.16, c.b - c.r);

    float dist = length(rel);
    float distFade = 1.0 - smoothstep(90.0, 420.0, dist);
    float mask = flatness * blueness * (0.30 + 0.70 * distFade);
    if (mask <= 0.004)
    {
        return c;
    }

    // ---- 波纹：三层不同尺度 / 方向的滚动噪声 ----
    float t = Time.x;
    float2 wp = worldPos.xz;
    float n1 = vnoise(wp * 0.24 + float2(t * 0.055, t * 0.041));
    float n2 = vnoise(wp * 0.55 - float2(t * 0.083, t * 0.061));
    float n3 = vnoise(wp * 1.30 + float2(-t * 0.110, t * 0.140));

    float2 ripple = float2(n1 - 0.5, n2 - 0.5) * 1.7
                  + float2(n3 - 0.5, n1 + n2 - 1.0) * 0.7;

    float strength = saturate(O_WATER_RIPPLES) * mask * (0.35 + 0.65 * distFade);
    float2 offset = ripple * strength * 0.0055;
    float3 col = SceneColor.SampleLevel(SceneColorSampler, uv + offset, 0).rgb;

    // ---- 海蓝染色：白天明亮偏青，夜里压暗但保持蓝调 ----
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    float3 seaBlue = lerp(float3(0.030, 0.085, 0.145), float3(0.105, 0.330, 0.520), day);
    seaBlue = lerp(seaBlue, SunsetColor.rgb * 0.42, sunsetAmt * 0.35);
    float3 target = seaBlue * (0.55 + 1.10 * lum);
    col = lerp(col, target, saturate(O_WATER_TINT) * mask * 0.90);

    // ---- 阳光反光：尖峰噪声 + 朝向太阳的一侧更亮 ----
    float glint = smoothstep(0.68, 0.88, n3) * saturate(O_WATER_GLINT) * mask;
    float sunUp = smoothstep(-0.05, 0.22, sunDir.y);
    float facing = saturate(dot(normalize(rel), sunDir) * 0.5 + 0.65);
    float3 glintCol = lerp(float3(1.00, 0.95, 0.82), SunsetColor.rgb * 1.25, sunsetAmt);
    col += glintCol * glint * sunUp * facing * 0.55;

    return col;
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
    {
        // 无天空维度（地狱）：完全不改画面
        return scene;
    }

    float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float3 sunDir = normalize(SunDirection.xyz);
    float sunY = sunDir.y;
    float day = saturate(sunY * 2.2 + 0.35);
    float sunsetAmt = pow(saturate(1.0 - abs(sunY) * 3.0), 1.4);

    // reverse-Z：远平面 = 0 = 天空；其余像素在主世界尝试水面处理
    if (depth > 1e-6)
    {
        if (skybox == 1 && max(O_WATER_TINT, O_WATER_RIPPLES) > 0.002)
        {
            return float4(shadeWater(scene, uv, ndc, depth, sunDir, day, sunsetAmt), 1.0);
        }
        return scene;
    }

    float4 nearH = mul(InvViewProj, float4(ndc, 1.0, 1.0));
    float3 dir = normalize(nearH.xyz);
    float up = saturate(dir.y);

    float3 color;

    if (skybox == 1)
    {
        // ---------------- 主世界 ----------------
        float3 skyTint = max(SkyColor.rgb, float3(0.02, 0.03, 0.06));

        // 天顶偏深蓝、地平线偏灰白：拉开层次，避免整片天空发白。
        float3 dayZenith = lerp(float3(0.07, 0.18, 0.46), skyTint * 0.52, 0.45);
        float3 dayHorizon = lerp(float3(0.52, 0.66, 0.86), skyTint * 1.02, 0.35);
        float3 zenith = lerp(float3(0.010, 0.014, 0.036), dayZenith, day);
        float3 horizonCol = lerp(float3(0.026, 0.034, 0.072), dayHorizon, day);
        float3 warmHorizon = SunsetColor.rgb * 0.92 + float3(0.02, 0.012, 0.0);

        float h = pow(saturate(1.0 - up), 3.0);
        color = lerp(zenith, horizonCol, h);
        color = lerp(color, warmHorizon, sunsetAmt * h * 0.62);

        // 太阳方向的暖色辉光（收敛到太阳附近，避免把整片天空洗白）
        float cosSun = saturate(dot(dir, sunDir));
        float glow = pow(cosSun, 8.0) * 0.10 + pow(cosSun, 56.0) * 0.24;
        color += lerp(float3(1.00, 0.94, 0.82), SunsetColor.rgb * 1.3, sunsetAmt)
               * glow * (0.30 + 0.70 * day);

        // 圆形太阳（在云之前绘制，让云能遮住它）
        float inner = cos(0.0125 * max(O_SUN_SIZE, 0.1));
        float outer = cos(0.0210 * max(O_SUN_SIZE, 0.1));
        float disc = smoothstep(outer, inner, cosSun);
        float above = smoothstep(-0.08, 0.02, sunY);
        float3 sunCol = lerp(float3(1.00, 0.96, 0.88), SunsetColor.rgb * 1.35, sunsetAmt);
        color += sunCol * disc * above * 2.4 * max(O_SUN_INTENSITY, 0.0);
        color += sunCol * pow(cosSun, 220.0) * 0.35 * above * max(O_SUN_INTENSITY, 0.0);

        // 夜间星空
        float night = 1.0 - day;
        float starVis = night * (0.25 + 0.75 * Sky.y) * (0.35 + 0.65 * Sky.z);
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
        float moonDisc = smoothstep(cos(0.0165), cos(0.0095), moonCos);
        float moonGlow = pow(saturate(moonCos), 900.0) * 0.25;
        color = lerp(color, float3(0.86, 0.90, 1.00) * 1.15, moonDisc * night);
        color += float3(0.55, 0.62, 0.85) * moonGlow * night;

        // 体积云：scatter 已是预乘颜色，按消光混合
        float4 clouds = volumetricClouds(dir, sunDir, uv, horizonCol);
        color = color * (1.0 - clouds.a) + clouds.rgb;

        // 地平线雾霭
        color += horizonCol * h * 0.12 * saturate(O_HAZE);

        // 视锥里没有几何体的“真空区”（depth == 0）：地平线以上画程序化天空；
        // 地平线以下（高空俯视世界边缘时的远方虚空）保留原版雾色，避免刷出一条
        // 假地平线。用一条窄过渡带衔接，不产生硬边。
        float voidMix = smoothstep(-0.045, -0.006, dir.y);
        color = lerp(scene.rgb, color, voidMix);
    }
    else
    {
        // ---------------- 末地（照 Helian-MMCO：深色底 + 银河 + 末地闪光） ----------------
        float3 endBase = lerp(float3(0.006, 0.005, 0.014), float3(0.022, 0.010, 0.044), up);
        color = endBase;

        float2 sp = float2(atan2(dir.z, dir.x) * 1.6, dir.y * 2.4);
        float neb = fbm(sp * 2.0 + float2(Time.x * 0.004, Time.x * 0.001), 4);
        color += float3(0.20, 0.05, 0.34) * pow(neb, 2.4) * 0.35;

        // 银河（endStars 移植）：末地天空的主体特征
        color += endGalaxy(dir, 0.7) * float3(0.82, 0.88, 1.00) * max(O_END_GALAXY, 0.0);

        // 疏星做底：压低强度，避免和银河抢视线
        color += starField(dir) * 0.28;

        float flash = saturate(SkyColor.a);
        color += float3(0.34, 0.14, 0.55) * flash * (0.25 + 0.75 * up) * 0.9;
    }

    return float4(color, 1.0);
}
