// ===========================================================================
// Foxy DX12 — Sky Pass (HLSL, SM 5.1, single-file self-contained)
//
// Inputs:
//   t0 = scene_color  (sRGB, vanilla world already rendered)
//   t1 = scene_depth  (reverse-Z, 0 = sky/far, > 0 = geometry)
//
// Output:
//   fx:0 (RGBA16F) — sky gradient, sun disk, moon, star field,
//                    volumetric clouds, aerial fog, rain streaks,
//                    water tint on water surfaces.
//
// The cbuffer ShaderPackUniforms is auto-injected by the framework; do NOT
// redeclare it. Options mapping (24 options, n -> Options[n/4][n%4]):
//   [0] BRIGHTNESS, SATURATION, CONTRAST, VIGNETTE
//   [1] EXPOSURE, TONEMAPPER, AGX_LOOK, BLOOM_STRENGTH
//   [2] SUN_INTENSITY, MOON_INTENSITY, NIGHT_BRIGHTNESS, PURKINJE
//   [3] CLOUD_COVERAGE, CLOUD_SPEED, CLOUD_STEPS, CLOUD_SIZE
//   [4] CLOUD_HEIGHT, CLOUD_THICK, CLOUD_DENSITY, HAZE
//   [5] WARMTH, SHADOW_LIFT, WATER_TINT, RAIN
// ===========================================================================

Texture2D<float4> SceneColor : register(t0);
SamplerState SceneColorSampler : register(s0);
Texture2D<float>  SceneDepth : register(t1);
SamplerState SceneDepthSampler : register(s1);

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

// ---------------------------------------------------------------------------
// Constants (Foxy flavor)
// ---------------------------------------------------------------------------
#define FOXY_PI            3.141592653589793
#define FOXY_CLOUD_MAXDIST 3000.0

// ---------------------------------------------------------------------------
// Struct matching the built-in fullscreen VS (SV_POSITION + TEXCOORD0)
// ---------------------------------------------------------------------------
struct VSOut
{
    float4 position : SV_POSITION;
    float2 uv : TEXCOORD0;
};

// ---------------------------------------------------------------------------
// Integer lattice hash / value noise (stable at large world coords)
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
    uint h = uint(p.x) * 374761393u
           + uint(p.y) * 668265263u
           + uint(p.z) * 2127819773u
           +  1442695041u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0xFFFFFFu) * (1.0 / 16777215.0);
}

float ivnoise2(float2 p)
{
    int2 ip = floor(p).xy;
    float2 f = p - float2(ip);
    float a = ihash12(ip);
    float b = ihash12(ip + int2(1, 0));
    float c = ihash12(ip + int2(0, 1));
    float d = ihash12(ip + int2(1, 1));
    float2 u = f * f * (3.0 - 2.0 * f);
    return lerp(lerp(a, b, u.x), lerp(c, d, u.x), u.y);
}

float ivnoise3(float3 p)
{
    int3 ip = floor(p).xyz;
    float3 f = p - float3(ip);
    float3 u = f * f * (3.0 - 2.0 * f);
    return lerp(
        lerp(lerp(ihash13(ip),                 ihash13(ip + int3(1,0,0)), u.x),
             lerp(ihash13(ip + int3(0,1,0)),   ihash13(ip + int3(1,1,0)), u.x), u.y),
        lerp(lerp(ihash13(ip + int3(0,0,1)),   ihash13(ip + int3(1,0,1)), u.x),
             lerp(ihash13(ip + int3(0,1,1)),   ihash13(ip + int3(1,1,1)), u.x), u.y),
        u.z);
}

float fbm4(float2 p)
{
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; ++i)
    {
        v += amp * ivnoise2(p);
        p = p * 2.1 + 13.7;
        amp *= 0.5;
    }
    return v;
}

// ---------------------------------------------------------------------------
// Star field (spherical equidistant grid, two layers for parallax)
// ---------------------------------------------------------------------------
float starHash2(float2 p)
{
    return ihash12(int2(p * 91.7 + 17.3));
}

float3 starLayer(float3 dir, float scale, float threshold, float brightness)
{
    float3 d = normalize(dir);
    // Project to cube face (simplest: pick dominant axis)
    float3 ad = abs(d);
    float2 uv;
    if (ad.y >= ad.x && ad.y >= ad.z) { uv = d.zx; uv.y = -uv.y; }
    else if (ad.x >= ad.z)            { uv = d.yz; uv.y = -uv.y; }
    else                               { uv = d.xy; }

    float2 cell = floor(uv * scale + 0.5);
    float2 frc = frac(uv * scale + 0.5);
    float h = starHash2(cell);
    if (h < threshold) return float3(0.0, 0.0, 0.0);
    float2 c = float2(starHash2(cell + 57.1), starHash2(cell + 113.7)) - 0.5;
    float dist = length(frc - c);
    float star = smoothstep(0.18, 0.02, dist);
    float tw = 0.75 + 0.25 * sin(Time.x * 3.1 + h * 40.0);
    float3 col = float3(0.85, 0.92, 1.10) * star * tw * brightness * (h - threshold) / (1.0 - threshold);
    return col;
}

float3 starField(float3 dir, float nightAmount)
{
    if (nightAmount < 0.001) return float3(0.0, 0.0, 0.0);
    float3 c = float3(0.0, 0.0, 0.0);
    c += starLayer(dir, 220.0, 0.18, 0.35 * nightAmount);
    c += starLayer(dir,  90.0, 0.22, 0.80 * nightAmount);
    return c;
}

// ---------------------------------------------------------------------------
// Moon disk + craters (procedural)
// ---------------------------------------------------------------------------
float3 moonDisk(float3 dir, float3 sunDir, float night)
{
    float3 moonDir = -sunDir; // rough antipode
    float m = dot(dir, normalize(moonDir));
    float r = 0.045 * O_MOON_INTENSITY;
    float disk = smoothstep(0.0, r * 0.35, m) * smoothstep(1.0, 1.0 - r * 0.15, m);
    if (disk < 0.001) return float3(0.0, 0.0, 0.0);

    // Crater texture (2-octave FBM on projected coords)
    float2 muv = dir.xz / (dir.y + 0.2);
    float c1 = ivnoise2(muv * 8.0 + 3.1);
    float c2 = ivnoise2(muv * 16.0 + 7.7);
    float craters = 1.0 - saturate(c1 * 0.5 + c2 * 0.3) * 0.55;

    float3 moonCol = float3(0.92, 0.94, 1.05) * craters;
    moonCol *= disk * O_MOON_INTENSITY * night;
    return moonCol;
}

// ---------------------------------------------------------------------------
// Sun disk (with corona)
// ---------------------------------------------------------------------------
float3 sunDisk(float3 dir, float3 sunDir, float day)
{
    if (day < 0.001) return float3(0.0, 0.0, 0.0);
    float d = dot(normalize(dir), sunDir);
    float discR = 0.018;
    float disc = smoothstep(discR, discR * 0.55, abs(d - 1.0));
    float corona = pow(saturate(d), 24.0) * 0.35;
    float3 col = float3(1.30, 1.15, 0.80) * (disc + corona) * O_SUN_INTENSITY * day;
    return col;
}

// ---------------------------------------------------------------------------
// Sky gradient (zenith / horizon / sun haze)
// ---------------------------------------------------------------------------
float3 skyGradient(float3 dir, float3 sunDir, float day, float sunsetAmt)
{
    float up = saturate(dir.y);

    // Day palette: warm zenith → cool horizon
    float3 dayZenith  = float3(0.36, 0.56, 0.92);
    float3 dayHorizon = float3(0.72, 0.84, 0.95);

    // Night palette: deep blue-black
    float3 nightZenith  = float3(0.012, 0.020, 0.055);
    float3 nightHorizon = float3(0.045, 0.065, 0.120);

    // Sunset palette: warm orange-pink
    float3 sunZenith  = float3(0.55, 0.35, 0.45);
    float3 sunHorizon = float3(1.05, 0.62, 0.35);

    float3 zenith  = lerp(nightZenith,  dayZenith,  day);
    float3 horizon = lerp(nightHorizon, dayHorizon, day);

    zenith  = lerp(zenith,  sunZenith,  sunsetAmt);
    horizon = lerp(horizon, sunHorizon, sunsetAmt);

    // Haze toward horizon (lower sky is more hazy)
    float hazeF = pow(1.0 - up, 3.0) * O_HAZE;
    float3 hazeCol = lerp(horizon, float3(0.80, 0.78, 0.72), 0.55);
    float3 col = lerp(zenith, horizon, up);
    col = lerp(col, hazeCol, hazeF);

    // Sun-side warm tint (Mie forward scattering approximation)
    float cosSun = saturate(dot(normalize(dir), sunDir));
    float mie = pow(cosSun, 14.0) * day * 0.85;
    col += float3(1.20, 0.85, 0.45) * mie * O_SUN_INTENSITY * 0.6;

    return col;
}

// ---------------------------------------------------------------------------
// Volumetric clouds (2D FBM + 3D noise mix, plane intersection march)
// ---------------------------------------------------------------------------
float3 cloudWind()
{
    float t = Time.x * O_CLOUD_SPEED * 0.0006;
    return float3(sin(t * 0.13) * 0.7 + t * 0.4, 0.0, cos(t * 0.17) * 0.5 + t * 0.3);
}

float cloudHeightFactor(float y)
{
    float bot = O_CLOUD_HEIGHT * 100.0;
    float top = (O_CLOUD_HEIGHT + O_CLOUD_THICK) * 100.0;
    float f = 1.0 - saturate((y - bot) / max(top - bot, 1.0));
    return max(f, 0.0);
}

float cloudDensityAt(float3 p, float3 wind, float hf)
{
    float sizeScale = rcp(clamp(O_CLOUD_SIZE, 0.15, 1.0));
    float3 q = p + wind;
    q.y -= O_CLOUD_HEIGHT * 100.0;
    float d2 = ivnoise2(q.xz * (0.00028 * sizeScale));
    float d3 = ivnoise3(q * (0.00022 * sizeScale));
    float d = d2 * 0.78 + d3 * 0.22;
    float threshold = lerp(0.52, 0.20, saturate(O_CLOUD_COVER));
    float density = saturate((d - threshold) * 3.6) * O_CLOUD_DENSITY;
    return density * hf;
}

float3 sampleCloudColor(float3 p, float3 sunDir, float day, float sunsetAmt, float hf)
{
    // Sun-lit side warm / shadowed side cool
    float sunFac = saturate(dot(normalize(p - CameraPosition.xyz), sunDir) * 0.5 + 0.5);
    float3 lit = lerp(float3(0.70, 0.74, 0.82), float3(1.15, 1.05, 0.85), day) * (0.7 + 0.6 * sunFac);
    lit = lerp(lit, float3(1.05, 0.72, 0.50), sunsetAmt * 0.6);
    // Night: darker, blue tint
    lit = lerp(lit, float3(0.10, 0.12, 0.18) * O_NIGHT_BRIGHT * 4.0, 1.0 - day);
    return lit;
}

float4 marchClouds(float3 camPos, float3 worldDir, float3 sunDir,
                   float day, float sunsetAmt, int steps, float2 dither)
{
    float bottom = O_CLOUD_HEIGHT * 100.0;
    float top = (O_CLOUD_HEIGHT + O_CLOUD_THICK) * 100.0;
    float t0 = (bottom - camPos.y) / worldDir.y;
    float t1 = (top - camPos.y) / worldDir.y;
    float tStart = max(min(t0, t1), 0.0);
    float tEnd = min(max(t0, t1), FOXY_CLOUD_MAXDIST);
    if (tStart >= tEnd || worldDir.y <= 0.0 || O_CLOUD_COVER < 0.005)
        return float4(0.0, 0.0, 0.0, 0.0);

    float stepLen = (tEnd - tStart) / max(steps, 1);
    float3 wind = cloudWind();
    float dith = dither.x * stepLen * 0.5;

    float transmittance = 1.0;
    float3 accum = float3(0.0, 0.0, 0.0);
    float3 p = camPos + worldDir * (tStart + dith);
    for (int i = 0; i < steps; ++i)
    {
        float hf = cloudHeightFactor(p.y);
        float dens = cloudDensityAt(p, wind, hf);
        float optical = dens * stepLen * 0.9;
        float sampleTrans = exp(-optical) * transmittance;
        float3 col = sampleCloudColor(p, sunDir, day, sunsetAmt, hf);
        accum += (transmittance - sampleTrans) * col;
        transmittance = sampleTrans;
        if (transmittance < 0.02) break;
        p += worldDir * stepLen;
    }
    return float4(accum, 1.0 - transmittance);
}

// ---------------------------------------------------------------------------
// Aerial fog (aerial perspective) — cool blue-grey
// ---------------------------------------------------------------------------
float3 airFog(float3 color, float2 ndc, float depth, float3 sunDir,
              float day, float sunsetAmt, float weather)
{
    if (depth <= 1e-5 || O_HAZE < 0.001) return color;
    float4 rel4 = mul(InvViewProj, float4(ndc, depth, 1.0));
    float dist = length(rel4.xyz / max(abs(rel4.w), 1e-4));

    float opt = dist * (O_HAZE * 0.00085 + 0.00006) * (1.0 + 2.5 * weather * weather);
    float3 absorb = exp(-opt);

    float3 scatter = lerp(float3(0.34, 0.42, 0.58),
                          float3(0.58, 0.66, 0.78), day);
    scatter = lerp(scatter, float3(0.92, 0.62, 0.45), sunsetAmt * 0.6);

    // Mie forward scattering toward sun (warm)
    float cosSunDir = dot(normalize(rel4.xyz), sunDir);
    float phase = pow(saturate(cosSunDir), 8.0) * 0.5;
    scatter += float3(1.20, 0.92, 0.55) * phase * day * O_SUN_INTENSITY * 0.25;

    return scatter * (1.0 - absorb) + color * absorb;
}

// ---------------------------------------------------------------------------
// Rain (procedural streaks, tilted by wind)
// ---------------------------------------------------------------------------
float3 rainStreaks(float2 uv, float3 worldDir, float3 wind, float time)
{
    if (O_RAIN < 0.005) return float3(0.0, 0.0, 0.0);
    // World-anchored so it doesn't jitter with camera
    float2 rc = uv * 64.0;
    rc.x += wind.x * 4.0;
    rc.y += wind.z * 2.0;
    float t = time * 0.5;
    float y = fmod(rc.y + t, 8.0);
    float streak = smoothstep(7.0, 6.0, y) * smoothstep(0.0, 1.0, y);
    float h = ihash12(int2(rc));
    float a = saturate(h - 0.85) * 8.0; // sparse streaks
    float3 col = float3(0.75, 0.80, 0.92) * streak * a * O_RAIN * 0.55;
    return col;
}

// ---------------------------------------------------------------------------
// Water surface tint (applied on water-depth pixels, depth > 0.5 in vanilla)
// ---------------------------------------------------------------------------
float3 waterTint(float3 sceneCol, float3 sunDir, float day)
{
    if (O_WATER_TINT < 0.005) return sceneCol;
    // Water: blue tint with sun glint
    float3 waterCol = float3(0.18, 0.38, 0.55);
    float glint = pow(saturate(dot(normalize(float3(sunDir.x, 0.3, sunDir.z)),
                                   float3(0, 1, 0))), 12.0) * day;
    float3 col = lerp(sceneCol, waterCol, 0.55 * saturate(O_WATER_TINT));
    col += float3(1.20, 1.05, 0.70) * glint * 0.45 * O_WATER_TINT;
    return col;
}

// ---------------------------------------------------------------------------
// Purkinje (night blue shift) — applied on sky
// ---------------------------------------------------------------------------
float3 purkinjeShift(float3 c, float night)
{
    if (O_PURKINJE < 0.005 || night < 0.005) return c;
    float3 shifted = c * float3(0.82, 0.94, 1.18) * saturate(O_PURKINJE);
    return lerp(c, shifted, night * saturate(O_PURKINJE));
}

// ---------------------------------------------------------------------------
// Black hole (gravitational lensing + accretion disk + photon ring)
//
// Direction comes from this pass' params (pipeline.json):
//   x = enabled / intensity, y = azimuth(deg), z = elevation(deg), w = scale
// The accretion-disk particle set is produced by the compute pass before this
// one and read back through a read-only structured buffer (t2 = SSBO SRV).
// ---------------------------------------------------------------------------
struct BhParticle
{
    float4 pos;    // x = radius 0..1, y = phase(rad), z = height, w = size
    float4 color;  // rgb = temperature color, a = brightness
};

StructuredBuffer<BhParticle> BlackHoleParticles : register(t2);

#define O_BLACKHOLE  (PassParams.x)
#define O_BH_AZIMUTH (PassParams.y)
#define O_BH_ELEV    (PassParams.z)
#define O_BH_SIZE    (PassParams.w)

float3 blackHoleDir()
{
    float az = radians(O_BH_AZIMUTH);
    float el = radians(O_BH_ELEV);
    return normalize(float3(cos(el) * sin(az), sin(el), cos(el) * cos(az)));
}

// Returns (rgb = emission, a = occlusion). Caller does
//   c = c * (1 - a) + rgb;
float4 blackHole(float3 dir, float night, float2 uv)
{
    if (O_BLACKHOLE < 0.005)
        return float4(0.0, 0.0, 0.0, 0.0);

    float3 bhDir = blackHoleDir();
    float cosA = saturate(dot(dir, bhDir));
    float ang = acos(cosA);
    float rSh = 0.052 * max(O_BH_SIZE, 0.25);   // shadow radius (radians)
    float b = ang / rSh;
    if (b > 14.0)
        return float4(0.0, 0.0, 0.0, 0.0);

    // Tangential basis around the hole.
    float3 up = (abs(bhDir.y) > 0.95) ? float3(1.0, 0.0, 0.0) : float3(0.0, 1.0, 0.0);
    float3 u = normalize(cross(up, bhDir));
    float3 v = cross(bhDir, u);

    // Lensing: push the sampled direction outward near the shadow.
    float3 tangent = normalize(dir - bhDir * cosA + 1e-5);
    float3 sampleDir = normalize(dir + tangent * (1.1 / max(b, 0.35)) * rSh);

    // Accretion disk: tilted plane through the hole.
    float3 nDisk = normalize(v * 0.94 + bhDir * 0.34);
    float hgt = dot(sampleDir, nDisk);
    float diskR = acos(saturate(dot(sampleDir, bhDir))) / rSh;

    float radial = smoothstep(1.05, 1.55, diskR) * smoothstep(6.5, 4.2, diskR);
    float thick = exp(-pow(abs(hgt) * (26.0 / max(diskR, 1.0)), 2.0));
    float diskMask = radial * thick;

    float phi = atan2(dot(sampleDir, v), dot(sampleDir, u));
    float2 nc = float2(cos(phi), sin(phi)) * 2.3 + diskR * 0.55;
    float turb = 0.6 + 0.4 * fbm4(nc + Time.x * 0.05);
    float doppler = 1.0 + 0.85 * saturate(dot(tangent, u));   // approaching side

    float heat = saturate((6.5 - diskR) / 5.0);
    float3 diskCol = lerp(float3(1.0, 0.40, 0.10), float3(1.0, 0.94, 0.82),
                          heat * heat);
    float diskI = diskMask * turb * doppler * 1.6;

    // Photon ring hugging the shadow edge.
    float ring = exp(-pow((diskR - 1.30) * 14.0, 2.0)) * 1.4;

    // SSBO sparkle: sample a few of the compute-written disk particles.
    float3 sparkle = float3(0.0, 0.0, 0.0);
    uint particleCount = 0;
    uint particleStride = 0;
    BlackHoleParticles.GetDimensions(particleCount, particleStride);
    if (particleCount > 0 && diskMask > 0.01)
    {
        float2 sp = uv * Viewport.xy;
        [unroll]
        for (int k = 0; k < 4; ++k)
        {
            uint idx = (uint)(ihash12(int2(sp + (float)k * 37.0)) * (float)particleCount);
            idx = idx % particleCount;
            BhParticle p = BlackHoleParticles[idx];
            float2 off = float2(cos(p.pos.y), sin(p.pos.y)) * (p.pos.x * rSh * 5.0);
            float3 pd = normalize(bhDir + u * off.x + v * off.y * 0.35
                                  + bhDir * p.pos.z * 0.01);
            float da = acos(saturate(dot(dir, pd)));
            float glow = exp(-pow(da / max(0.0016 * p.pos.w, 1e-4), 2.0));
            sparkle += p.color.rgb * (p.color.a * glow);
        }
    }

    float3 emission = diskCol * diskI + float3(1.0, 0.85, 0.62) * ring
                    + sparkle * diskMask * 2.0;
    float visibility = lerp(0.35, 1.0, night) * O_BLACKHOLE;
    float shadow = smoothstep(1.02, 0.86, diskR);
    return float4(emission * visibility, shadow * O_BLACKHOLE);
}

// ---------------------------------------------------------------------------
// Main entry
// ---------------------------------------------------------------------------
float4 main(VSOut input) : SV_TARGET
{
    float2 uv = input.uv;
    float4 scene = SceneColor.SampleLevel(SceneColorSampler, uv, 0);
    float depth = SceneDepth.SampleLevel(SceneDepthSampler, uv, 0);

    int skybox = (int)round(Sky.w);
    if (skybox == 0) return scene; // No sky (Nether)

    float2 ndc = float2(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0);
    float3 sunDir = normalize(SunDirection.xyz);
    float sunUp = smoothstep(-0.12, 0.14, sunDir.y);
    float sunsetAmt = pow(saturate(1.0 - abs(sunDir.y) * 3.0), 1.4);
    float weather = clamp(Sky.z, 0.0, 1.0);

    float3 camPos = CameraPosition.xyz;
    float4 rel = mul(InvViewProj, float4(ndc, 0.0, 1.0));
    float3 dir = normalize(rel.xyz / max(abs(rel.w), 1e-4));

    if (depth > 1e-6)
    {
        // Non-sky pixel: apply water tint (heuristic: blue-ish + depth) + aerial fog
        float3 c = scene.rgb;
        // Rough water heuristic: bluish pixels (vanilla water) get tint
        float blueBias = saturate((c.b - c.r) * 1.2) * saturate(O_WATER_TINT);
        if (blueBias > 0.02)
            c = waterTint(c, sunDir, sunUp);
        c = airFog(c, ndc, depth, sunDir, sunUp, sunsetAmt, weather);
        return float4(c, 1.0);
    }

    // Sky pixel
    float day = sunUp;
    float night = 1.0 - day;

    // Sky gradient
    float3 c = skyGradient(dir, sunDir, day, sunsetAmt);

    // Sun disk
    c += sunDisk(dir, sunDir, day);

    // Moon (only visible at night)
    c += moonDisk(dir, sunDir, night);

    // Stars (only at night)
    c += starField(dir, night);

    // Black hole (occludes sky/stars, adds lensed accretion disk)
    float4 bh = blackHole(dir, night, uv);
    c = c * (1.0 - bh.a) + bh.rgb;

    // Volumetric clouds
    int steps = (int)saturate(O_CLOUD_STEPS);
    float2 dith = float2(ihash12(int2(uv * Viewport.xy)), ihash12(int2(uv * Viewport.xy + 17)));
    float4 cl = marchClouds(camPos, dir, sunDir, day, sunsetAmt, steps, dith);
    c = c * cl.a + cl.rgb * (1.0 - cl.a);

    // Rain streaks (world-anchored, tilted by wind)
    float3 wind = cloudWind();
    c += rainStreaks(uv, dir, wind, Time.x * O_CLOUD_SPEED * 0.5);

    // Night Purkinje shift
    c = purkinjeShift(c, night);

    return float4(c, 1.0);
}
