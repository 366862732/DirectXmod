package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.config.Dx12Config;
import com.xgdt.dx12.lod.LodRegionCache;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远视距 LOD：用一张连续的、格宽随距离逐级放大的高度场网格把原版视距之外的地形补出来。
 *
 * <p>几何不是「每格一个独立平面」，而是一张<b>共享顶点</b>的规则网格：
 * 坐标轴以相机为中心，每一级把格宽翻倍、走若干格再翻倍，于是近处密（8 方块以下）、
 * 远处疏（上千方块），中间没有任何环形拼接缝，也不会出现「一格一堵假墙」。
 *
 * <p>每个顶点的 y 取自真实高度图（已加载区块用 {@link Heightmap.Types#WORLD_SURFACE}，
 * 之外用 {@link LodRegionCache} 后台读存档 region 文件），颜色取该列表层方块的
 * {@link MapColor}，再按局部坡度做一次方向光明暗烘焙进顶点色，远景因此有起伏与体积感，
 * 而不是整片纯色。
 *
 * <p>深度沿用本帧世界深度（反向 Z，{@code GREATER_THAN_OR_EQUAL} + LOAD），原版地形
 * 天然遮住 LOD；LOD 自身也写深度，因此后接的 DLSS 深度捕获与光影链都能看到它。
 *
 * <p>投影矩阵按原版公式重建（相同 fov/aspect/近平面、更远的远平面），只改深度映射，
 * 屏幕空间位置与原版地形严格一致，接缝不会错位。
 */
@Environment(EnvType.CLIENT)
public final class Dx12LodRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-lod");

    private static final Dx12LodRenderer INSTANCE = new Dx12LodRenderer();

    /** 顶点步长：position(float3, 12B) + color(RGBA8, 4B)。 */
    private static final int VERTEX_STRIDE = 16;
    /** 顶点格式 ordinal：46 = RGB32_FLOAT，6 = RGBA8_UNORM。 */
    private static final int FORMAT_POSITION = 46;
    private static final int FORMAT_COLOR = 6;

    /** native 侧 kBufferUsageUav：DEFAULT 堆 + ALLOW_UNORDERED_ACCESS，compute 可写。 */
    private static final int BUFFER_USAGE_UAV = 1024;
    /** compute 线程组大小，必须与 HLSL 的 {@code [numthreads]} 一致。 */
    private static final int COMPUTE_GROUP_SIZE = 64;
    /** 建网根常量个数：N / 海平面 / 雪线 / 保留。 */
    private static final int COMPUTE_CONST_COUNT = 4;
    /** 角点输入布局：int height + int rgb。 */
    private static final int CORNER_STRIDE = 8;

    /** uniform 槽位大小（CBV 偏移需 256 对齐）与在飞帧数。 */
    private static final int UNIFORM_SLOT = 256;
    private static final int UNIFORM_RING = 3;
    /** 实际写入字节数：float4x4 + CameraPos + FogParams + FogColor + LightTint + Params。 */
    private static final int UNIFORM_DATA_BYTES = 144;

    /** 几何环：与命令列表的帧在飞数一致，避免覆写 GPU 仍在读取的顶点缓冲。 */
    private static final int GEOMETRY_SLOTS = 3;

    /** 最远 LOD 半径（方块）。同时是 lod_distance 滑块的最大值对应的半径。 */
    private static final int MAX_RADIUS_BLOCKS = 16384;
    /** 无位移时的兜底重建间隔（帧）：让后台陆续读到的地形逐步补齐。 */
    private static final int REBUILD_INTERVAL = 40;

    /** 采样失败：高 32 位是 {@link Integer#MIN_VALUE}。 */
    private static final long UNKNOWN_SAMPLE = (long) Integer.MIN_VALUE << 32;
    private static final int UNKNOWN_HEIGHT = Integer.MIN_VALUE;
    /** 颜色未知时的兜底灰。 */
    private static final int FALLBACK_COLOR = 0x8A8A8A;

    /** 太阳方向（已近似单位化），用于把坡度烘成顶点亮度。 */
    private static final float SUN_X = 0.35f;
    private static final float SUN_Y = 0.82f;
    private static final float SUN_Z = -0.45f;
    /** 环境光下限：平地按 0.93 亮度，最陡的背光坡降到 0.6。 */
    private static final float AMBIENT = 0.6f;
    private static final float DIFFUSE = 0.4f;

    /**
     * 远景降噪：格宽超过 {@link #SMOOTH_START} 方块后，把角点高度往邻域均值拉，
     * 混合比例随格宽线性增长到 {@link #SMOOTH_MAX}。
     *
     * <p>远处一格的宽度可能上百方块，单个角点若是山峰，整格就会被拉成一根巨锥
     * （截图里的紫白色尖刺就是这么来的）。按格宽做多尺度降噪后，远景只剩宏观起伏，
     * 与「Voxy 远景只剩山形」的观感一致。
     */
    private static final float SMOOTH_START = 40.0f;
    private static final float SMOOTH_SPAN = 260.0f;
    private static final float SMOOTH_MAX = 0.8f;
    /** 平滑后仍允许偏离邻域均值的幅度：{@code max(MIN, 格宽 * SCALE)}。 */
    private static final float SMOOTH_LIMIT_MIN = 24.0f;
    private static final float SMOOTH_LIMIT_SCALE = 1.5f;

    /** 世界亮度下限：深夜远景仍能辨认，不会全黑。 */
    private static final float MIN_BRIGHTNESS = 0.18f;
    /** 天空光颜色（夜里偏蓝）只做染色，不作为整体亮度系数。 */
    private static final float SKY_TINT_STRENGTH = 0.45f;

    /**
     * 网格格宽上限（方块），按距离分段放宽。
     *
     * <p>没有上限时格宽会一路翻倍，到 16384 方块半径时一格顶几百个方块，整片丘陵都被
     * 三角面插值抹成一块平板——这正是远景「跟真区块完全不像」的主因。现在近处封顶
     * {@link #FINE_STEP_BLOCKS}、中距封顶 {@link #COARSE_STEP_BLOCKS}，只有雾已经把
     * 地形完全吃掉的距离之外才继续翻倍。
     */
    private static final int FINE_STEP_BLOCKS = 32;
    private static final int COARSE_STEP_BLOCKS = 64;
    private static final int FINE_DETAIL_RADIUS = 3072;
    private static final int COARSE_DETAIL_RADIUS = 6144;

    /** 分层配色：陡坡露岩、水边显沙、高处积雪，避免整片纯绿/纯蓝。 */
    private static final float ROCK_SLOPE = 0.85f;
    private static final int ROCK_COLOR = 0x8C8C8C;
    private static final int SAND_COLOR = 0xE2D6A8;
    private static final int SNOW_COLOR = 0xE8EEF4;
    /** 水体（water / flowing_water）的 MapColor。 */
    private static final int WATER_COLOR = 0x4040FF;
    /** 远景水体用偏暗的青蓝：MapColor 的纯蓝铺满一整片湖会像塑料。 */
    private static final int WATER_DEEP_COLOR = 0x2C5480;
    /** 积雪起始高度（相对海平面）。 */
    private static final int SNOW_ABOVE_SEA = 75;

    private static final String UNIFORM_HEADER =
        "cbuffer LodUniforms : register(b0)\n"
        + "{\n"
        + "    float4x4 ViewProj;   // 世界(相机相关) -> clip\n"
        + "    float4 CameraPos;    // xyz = 相机世界坐标\n"
        + "    float4 FogParams;    // x = 雾起点, y = 雾终点, z = 世界亮度\n"
        + "    float4 FogColor;     // rgb = 雾色\n"
        + "    float4 LightTint;    // rgb = 天空光染色（夜里偏蓝）\n"
        + "    float4 Params;       // x = 海平面高度\n"
        + "};\n";

    private static final String VERTEX_SHADER =
        "struct VSInput\n"
        + "{\n"
        + "    float3 position : POSITION;\n"
        + "    float4 color    : COLOR;\n"
        + "};\n"
        + "struct VSOutput\n"
        + "{\n"
        + "    float4 position : SV_POSITION;\n"
        + "    float4 color    : COLOR;\n"
        + "    float3 world    : TEXCOORD0;\n"
        + "};\n"
        + "VSOutput main(VSInput input)\n"
        + "{\n"
        + "    VSOutput output;\n"
        + "    float3 relative = input.position - CameraPos.xyz;\n"
        + "    output.position = mul(ViewProj, float4(relative, 1.0));\n"
        + "    output.color = input.color;\n"
        + "    output.world = relative;\n"
        + "    return output;\n"
        + "}\n";

    private static final String PIXEL_SHADER =
        "struct VSOutput\n"
        + "{\n"
        + "    float4 position : SV_POSITION;\n"
        + "    float4 color    : COLOR;\n"
        + "    float3 world    : TEXCOORD0;\n"
        + "};\n"
        + "float hash21(float2 p)\n"
        + "{\n"
        + "    p = frac(p * float2(0.1031, 0.1030));\n"
        + "    p += dot(p, p.yx + 33.33);\n"
        + "    return frac((p.x + p.y) * p.x);\n"
        + "}\n"
        + "float4 main(VSOutput input) : SV_TARGET\n"
        + "{\n"
        + "    float dist = length(input.world);\n"
        + "    float fog = saturate((dist - FogParams.x) / max(FogParams.y - FogParams.x, 1.0));\n"
        + "    float3 world = input.world + CameraPos.xyz;\n"
        + "    float3 rgb = input.color.rgb;\n"
        + "\n"
        + "    // 屏幕空间细节：先算出「一个像素跨多少方块」，噪声格宽按它取。这样不管远处格\n"
        + "    // 宽是 16 还是 512 方块，地面上始终有大约 2 像素一块的碎块感，不会糊成一片纯色。\n"
        + "    float px = max(max(length(ddx(world.xz)), length(ddy(world.xz))), 0.02);\n"
        + "    float cell = max(px * 2.0, 0.8);\n"
        + "    float nA = hash21(floor(world.xz / cell)) - 0.5;\n"
        + "    float nB = hash21(floor(world.xz / (cell * 4.0)) + 17.0) - 0.5;\n"
        + "    float nC = hash21(floor(world.yz / (cell * 2.0)) + 41.0) - 0.5;\n"
        + "    rgb *= 1.0 + nA * 0.26 + nB * 0.16 + nC * 0.10;\n"
        + "\n"
        + "    // 高度明暗：海平面以上越高越亮，低地略压暗，远景因此有体积而不是一片平色\n"
        + "    float above = world.y - Params.x;\n"
        + "    rgb *= saturate(0.84 + max(above, 0.0) * 0.0022);\n"
        + "\n"
        + "    // 顶点色 alpha = 0 是水体标记：换成同尺度的细碎波光，不用大波长正弦\n"
        + "    // （原来的波纹波长 20 多方块，远景看就是一团模糊的漩涡）\n"
        + "    float water = 1.0 - saturate(input.color.a * 2.0);\n"
        + "    if (water > 0.01)\n"
        + "    {\n"
        + "        float2 p = floor(world.xz / max(px * 2.0, 1.0));\n"
        + "        float ripple = hash21(p) - 0.5;\n"
        + "        float ripple2 = hash21(p * 0.5 + 13.0) - 0.5;\n"
        + "        float3 surface = rgb * (1.15 + ripple * 0.14 + ripple2 * 0.10)\n"
        + "            + float3(0.012, 0.045, 0.07);\n"
        + "        rgb = lerp(rgb, surface, water);\n"
        + "    }\n"
        + "\n"
        + "    rgb *= FogParams.z * LightTint.rgb;\n"
        + "    return float4(lerp(rgb, FogColor.rgb, fog), 1.0);\n"
        + "}\n";

    /**
     * 顶点生成的 compute 着色器：把 CPU 采样好的角点高度/地表色（{@code t0}）与
     * 坐标轴（{@code t1}）在 GPU 上展开成最终顶点（{@code u0}）。
     *
     * <p>这里逐条对应 Java 侧原来的三步 CPU 逻辑（降噪 / 分层配色 / 方向光烘焙），
     * 只是把 n² 次迭代从主线程搬到 compute 单元：立方体级网格（n=610）一帧要算
     * 三十多万个顶点，CPU 上要几十毫秒，GPU 上不到一毫秒，重建不再掉帧。
     */
    private static final String COMPUTE_SHADER =
        "cbuffer Build : register(b0)\n"
        + "{\n"
        + "    uint N;          // 网格边长（角点数 = N * N）\n"
        + "    int SeaLevel;\n"
        + "    int SnowLine;\n"
        + "    uint Pad;\n"
        + "};\n"
        + "struct Corner\n"
        + "{\n"
        + "    int height;      // 未知用 -2147483648\n"
        + "    int rgb;         // 0xRRGGBB，-1 表示未采样到颜色\n"
        + "};\n"
        + "StructuredBuffer<Corner> Corners : register(t0);\n"
        + "StructuredBuffer<int> Axis : register(t1);\n"
        + "RWStructuredBuffer<uint4> OutVerts : register(u0);\n"
        + "static const int UNKNOWN = (int)0x80000000;\n"
        + "int RawH(uint i, uint j) { return Corners[j * N + i].height; }\n"
        + "int CellW(uint i, uint j)\n"
        + "{\n"
        + "    int w = 0;\n"
        + "    if (i > 0) { w = max(w, Axis[i] - Axis[i - 1]); }\n"
        + "    if (i + 1 < N) { w = max(w, Axis[i + 1] - Axis[i]); }\n"
        + "    if (j > 0) { w = max(w, Axis[j] - Axis[j - 1]); }\n"
        + "    if (j + 1 < N) { w = max(w, Axis[j + 1] - Axis[j]); }\n"
        + "    return w;\n"
        + "}\n"
        + "// 按格宽把角点高度往四邻域均值拉（远处一格的宽度上百方块，单个山峰会被\n"
        + "// 拉成一根巨锥）。格宽小于 40 时完全不动，保证和原版地形贴合。\n"
        + "int SmoothH(uint i, uint j)\n"
        + "{\n"
        + "    int h = RawH(i, j);\n"
        + "    if (h == UNKNOWN) { return UNKNOWN; }\n"
        + "    int cell = CellW(i, j);\n"
        + "    if (cell < 40) { return h; }\n"
        + "    int sum = 0;\n"
        + "    int count = 0;\n"
        + "    if (i > 0) { int v = RawH(i - 1, j); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (i + 1 < N) { int v = RawH(i + 1, j); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (j > 0) { int v = RawH(i, j - 1); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (j + 1 < N) { int v = RawH(i, j + 1); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (count == 0) { return h; }\n"
        + "    float average = (float)sum / (float)count;\n"
        + "    float blend = min(0.8, (cell - 40.0) / 260.0);\n"
        + "    float mixed = h + (average - h) * blend;\n"
        + "    float limit = max(24.0, (float)cell * 1.5);\n"
        + "    mixed = clamp(mixed, average - limit, average + limit);\n"
        + "    return (int)round(mixed);\n"
        + "}\n"
        + "void Gradient(uint i, uint j, out float dhx, out float dhz)\n"
        + "{\n"
        + "    int center = SmoothH(i, j);\n"
        + "    dhx = 0.0;\n"
        + "    dhz = 0.0;\n"
        + "    if (i > 0 && i + 1 < N)\n"
        + "    {\n"
        + "        int left = SmoothH(i - 1, j);\n"
        + "        int right = SmoothH(i + 1, j);\n"
        + "        if (left != UNKNOWN && right != UNKNOWN)\n"
        + "        {\n"
        + "            dhx = (float)(right - left) / (float)(Axis[i + 1] - Axis[i - 1]);\n"
        + "        }\n"
        + "    }\n"
        + "    else if (i + 1 < N)\n"
        + "    {\n"
        + "        int right = SmoothH(i + 1, j);\n"
        + "        if (right != UNKNOWN)\n"
        + "        {\n"
        + "            dhx = (float)(right - center) / (float)(Axis[i + 1] - Axis[i]);\n"
        + "        }\n"
        + "    }\n"
        + "    else if (i > 0)\n"
        + "    {\n"
        + "        int left = SmoothH(i - 1, j);\n"
        + "        if (left != UNKNOWN)\n"
        + "        {\n"
        + "            dhx = (float)(center - left) / (float)(Axis[i] - Axis[i - 1]);\n"
        + "        }\n"
        + "    }\n"
        + "    if (j > 0 && j + 1 < N)\n"
        + "    {\n"
        + "        int down = SmoothH(i, j - 1);\n"
        + "        int up = SmoothH(i, j + 1);\n"
        + "        if (down != UNKNOWN && up != UNKNOWN)\n"
        + "        {\n"
        + "            dhz = (float)(up - down) / (float)(Axis[j + 1] - Axis[j - 1]);\n"
        + "        }\n"
        + "    }\n"
        + "    else if (j + 1 < N)\n"
        + "    {\n"
        + "        int up = SmoothH(i, j + 1);\n"
        + "        if (up != UNKNOWN)\n"
        + "        {\n"
        + "            dhz = (float)(up - center) / (float)(Axis[j + 1] - Axis[j]);\n"
        + "        }\n"
        + "    }\n"
        + "    else if (j > 0)\n"
        + "    {\n"
        + "        int down = SmoothH(i, j - 1);\n"
        + "        if (down != UNKNOWN)\n"
        + "        {\n"
        + "            dhz = (float)(center - down) / (float)(Axis[j] - Axis[j - 1]);\n"
        + "        }\n"
        + "    }\n"
        + "}\n"
        + "int ClampByte(int v) { return v < 0 ? 0 : min(v, 255); }\n"
        + "int MixColor(int from, int to, float t)\n"
        + "{\n"
        + "    if (t <= 0.0) { return from; }\n"
        + "    if (t >= 1.0) { return to; }\n"
        + "    int fr = (from >> 16) & 0xFF;\n"
        + "    int fg = (from >> 8) & 0xFF;\n"
        + "    int fb = from & 0xFF;\n"
        + "    int r = ClampByte((int)round(fr + (float)(((to >> 16) & 0xFF) - fr) * t));\n"
        + "    int g = ClampByte((int)round(fg + (float)(((to >> 8) & 0xFF) - fg) * t));\n"
        + "    int b = ClampByte((int)round(fb + (float)((to & 0xFF) - fb) * t));\n"
        + "    return (r << 16) | (g << 8) | b;\n"
        + "}\n"
        + "int ShadeColor(int rgb, float shade)\n"
        + "{\n"
        + "    int r = (int)((float)((rgb >> 16) & 0xFF) * shade);\n"
        + "    int g = (int)((float)((rgb >> 8) & 0xFF) * shade);\n"
        + "    int b = (int)((float)(rgb & 0xFF) * shade);\n"
        + "    return (ClampByte(r) << 16) | (ClampByte(g) << 8) | ClampByte(b);\n"
        + "}\n"
        + "[numthreads(64, 1, 1)]\n"
        + "void main(uint3 tid : SV_DispatchThreadID)\n"
        + "{\n"
        + "    uint idx = tid.x;\n"
        + "    if (idx >= N * N) { return; }\n"
        + "    uint i = idx % N;\n"
        + "    uint j = idx / N;\n"
        + "    int height = SmoothH(i, j);\n"
        + "    if (height == UNKNOWN) { return; }\n"
        + "    int rgb = Corners[idx].rgb;\n"
        + "    if (rgb < 0) { rgb = 0x8A8A8A; }\n"
        + "    float dhx;\n"
        + "    float dhz;\n"
        + "    Gradient(i, j, dhx, dhz);\n"
        + "    float slope = sqrt(dhx * dhx + dhz * dhz);\n"
        + "    int alpha;\n"
        + "    if ((rgb & 0xFFFFFF) == 0x4040FF)\n"
        + "    {\n"
        + "        rgb = 0x2C5480;\n"
        + "        alpha = 0;\n"
        + "    }\n"
        + "    else\n"
        + "    {\n"
        + "        rgb = MixColor(rgb, 0x8C8C8C, min(0.85, max(0.0, (slope - 0.85) * 0.9)));\n"
        + "        if (slope < 0.45 && height <= SeaLevel + 4 && height >= SeaLevel - 4)\n"
        + "        {\n"
        + "            rgb = MixColor(rgb, 0xE2D6A8, 0.35);\n"
        + "        }\n"
        + "        rgb = MixColor(rgb, 0xE8EEF4,\n"
        + "            min(0.9, max(0.0, (float)(height - SnowLine) / 40.0)));\n"
        + "        alpha = 255;\n"
        + "    }\n"
        + "    float nx = -dhx;\n"
        + "    float ny = 1.0;\n"
        + "    float nz = -dhz;\n"
        + "    float inverse = 1.0 / sqrt(nx * nx + ny * ny + nz * nz);\n"
        + "    float lambert = max((nx * 0.35 + ny * 0.82 + nz * -0.45) * inverse, 0.0);\n"
        + "    int shaded = ShadeColor(rgb, 0.6 + 0.4 * lambert);\n"
        + "    uint packed = (uint)(shaded & 0xFF)\n"
        + "        | ((uint)((shaded >> 8) & 0xFF) << 8)\n"
        + "        | ((uint)((shaded >> 16) & 0xFF) << 16)\n"
        + "        | ((uint)alpha << 24);\n"
        + "    OutVerts[idx] = uint4(asuint((float)Axis[i]),\n"
        + "        asuint((float)height + 1.0), asuint((float)Axis[j]), packed);\n"
        + "}\n";

    private final Slot[] slots = new Slot[GEOMETRY_SLOTS];
    private final Map<Integer, Long> pipelines = new HashMap<>();

    private int rebuildIndex;
    private Slot geometry;
    private int framesSinceBuild;

    /** 上次建网时的网格参数，任一变化都要重建。 */
    private int builtBase = -1;
    private int builtStepsPerLevel = -1;
    private int builtRadius = -1;
    private int builtCellX = Integer.MIN_VALUE;
    private int builtCellZ = Integer.MIN_VALUE;
    private int builtCachedChunks = -1;

    /** 建网时复用的采样缓冲，避免每秒分配上万个 int。 */
    private int[] heightScratch = new int[0];
    private int[] colorScratch = new int[0];
    private int[] smoothScratch = new int[0];
    /** 单个角点的高度梯度暂存（x, z），避免每次建网分配。 */
    private final float[] gradientScratch = new float[2];

    private Dx12GpuBuffer uniformBuffer;
    private GpuBufferSlice.MappedView uniformView;
    private ByteBuffer uniformMapped;
    private int uniformSlotIndex;

    private boolean failureLogged;

    /** 首次真正进入 LOD 渲染时打一条日志，便于确认该功能在正式整合包里确实在跑。 */
    private boolean activeLogged;

    private long lastDiagnosticNanos;

    /** 角点输入（int height + int rgb）：UPLOAD 堆，作为 compute 的 t0 只读。 */
    private Dx12GpuBuffer cornerBuffer;
    private GpuBufferSlice.MappedView cornerView;
    private ByteBuffer cornerData;
    private int cornerCapacity;

    /** 坐标轴（int 方块偏移）：UPLOAD 堆，作为 compute 的 t1 只读。 */
    private Dx12GpuBuffer axisBuffer;
    private GpuBufferSlice.MappedView axisView;
    private ByteBuffer axisData;
    private int axisCapacity;

    private long computePipeline;
    /** compute 建网任一环节失败后永久回退 CPU 路径，不再重试。 */
    private boolean computeFailed;

    private Dx12LodRenderer() {
    }

    public static Dx12LodRenderer getInstance() {
        return INSTANCE;
    }

    public void renderFrame() {
        if (!Dx12Config.getInstance().isLodActive() || !Dx12Device.isInitialized()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.gameRenderer == null) {
            return;
        }
        // 绑定当前存档的 region 目录（只比路径，不做 IO）
        LodRegionCache.getInstance().prepare(level);
        Dx12CommandEncoderBackend encoder = Dx12Device.sharedEncoder();
        if (encoder == null) {
            return;
        }
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        GpuTexture colorTexture = target.getColorTexture();
        GpuTexture depthTexture = target.getDepthTexture();
        if (!(colorTexture instanceof Dx12GpuTexture color)
            || !(depthTexture instanceof Dx12GpuTexture depth)) {
            return;
        }
        int width = color.getWidth(0);
        int height = color.getHeight(0);
        if (width <= 0 || height <= 0) {
            return;
        }
        GameRenderState renderState = minecraft.gameRenderer.gameRenderState();
        LevelRenderState levelState = renderState == null ? null : renderState.levelRenderState;
        CameraRenderState camera = levelState == null ? null : levelState.cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }
        if (!this.activeLogged) {
            this.activeLogged = true;
            LOGGER.info("[lod] 远视距 LOD 已启用：distance={} quality={}",
                Dx12Config.getInstance().getLodDistance(),
                Dx12Config.getInstance().getLodQuality());
        }
        try {
            execute(encoder.nativeHandle(), minecraft, level, color, depth, width, height,
                camera, renderState, levelState);
            this.failureLogged = false;
        } catch (Throwable t) {
            if (!this.failureLogged) {
                this.failureLogged = true;
                LOGGER.error("[lod] 执行远视距 LOD 失败，本帧跳过", t);
            }
        }
    }

    private void execute(long ctx, Minecraft minecraft, ClientLevel level, Dx12GpuTexture color,
                         Dx12GpuTexture depth, int width, int height, CameraRenderState camera,
                         GameRenderState renderState, LevelRenderState levelState) {
        Dx12Config config = Dx12Config.getInstance();
        // quality 只决定近处格宽与每级步数（网格密度），远处始终按几何级数放大
        int quality = Mth.clamp(config.getLodQuality(), 0, 2);
        int base = quality == 0 ? 16 : quality == 1 ? 8 : 4;
        int stepsPerLevel = quality == 0 ? 16 : quality == 1 ? 24 : 32;
        int radiusBlocks = Mth.clamp(config.getLodDistance() * 16, 256, MAX_RADIUS_BLOCKS);

        int cameraBlockX = Mth.floor(camera.pos.x);
        int cameraBlockZ = Mth.floor(camera.pos.z);
        // 网格中心吸附到 64 方块的整数倍：既保证各级格线始终对齐，也让走动时
        // 不会每隔几格就整张重建（重建一次要重新采样几万个角点）。
        int snap = Math.max(base, 64);
        int centerX = Math.floorDiv(cameraBlockX, snap) * snap;
        int centerZ = Math.floorDiv(cameraBlockZ, snap) * snap;

        int renderDistance = Math.max(2, minecraft.options.renderDistance().get());
        float innerRadius = (renderDistance - 1) * 16.0f;

        this.framesSinceBuild++;
        // 后台陆续读到新区块时才需要定时重建；站着不动且没有新数据就不重建，
        // 免得每 40 帧白采样一次几万个角点造成卡顿
        int cachedChunks = LodRegionCache.getInstance().cachedCount();
        boolean rebuild = this.geometry == null
            || base != this.builtBase
            || stepsPerLevel != this.builtStepsPerLevel
            || radiusBlocks != this.builtRadius
            || centerX != this.builtCellX
            || centerZ != this.builtCellZ
            || (this.framesSinceBuild > REBUILD_INTERVAL && cachedChunks != this.builtCachedChunks);
        if (rebuild) {
            rebuildGeometry(ctx, level, centerX, centerZ, radiusBlocks, base, stepsPerLevel,
                innerRadius, cameraBlockX, cameraBlockZ);
            this.framesSinceBuild = 0;
            this.builtBase = base;
            this.builtStepsPerLevel = stepsPerLevel;
            this.builtRadius = radiusBlocks;
            this.builtCellX = centerX;
            this.builtCellZ = centerZ;
            this.builtCachedChunks = cachedChunks;
        }

        Slot slot = this.geometry;
        if (slot == null || slot.indexCount <= 0) {
            return;
        }

        long pipeline = pipelineFor(color.getFormat().ordinal());
        if (pipeline == 0L) {
            return;
        }

        // 投影必须和原版世界共用同一套深度映射。
        //
        // 原版 {@code Projection.getMatrix} 把 zNear/zFar 交换后才喂给 setPerspective，
        // 也就是反向 Z：离得越近深度越大，d 处的深度约为 zNear / d。之前这里写的是普通
        // 透视（近平面 0.05、远平面几千），远景顶点深度全程接近 1，而原版地形只有 0 附近，
        // {@code GREATER_THAN_OR_EQUAL} 永远成立 —— 远景于是盖住所有近处地形，完全没有遮挡。
        //
        // 这里直接把深度改成 {@code depth = k / d}（第三行清成 m22 = 0、m32 = k，w 仍等于
        // 视距）：深度随距离严格单调，所以 LOD 自己之间也有正确的前后关系，远处的深度也
        // 不会退化成 0 而整片 z-fighting。k 按原版在 LOD 起始圈上的深度取，接缝处两边
        // 深度相等，近处方块地形依旧能正常遮住后面的 LOD。
        float far = Math.max(camera.depthFar, radiusBlocks * 1.6f);
        float zNear = 0.05f;
        float seamDistance = Math.max(innerRadius, zNear * 4.0f);
        float depthScale = zNear * Mth.clamp((camera.depthFar - seamDistance) / camera.depthFar,
            0.2f, 1.0f);
        Matrix4f projection = new Matrix4f().setPerspective(
            fovY(camera.projectionMatrix), aspect(camera.projectionMatrix), zNear, far, true);
        projection.m22(0.0f);
        projection.m32(depthScale);
        float[] viewProj = new float[16];
        Dx12ShaderPackRenderer.applyCameraBob(projection, camera, renderState)
            .mul(camera.viewRotationMatrix)
            .get(viewProj);

        // 雾：从原版视距外开始，外缘完全融进天空色，远景因此没有一条硬切边
        float fogStart = Math.max(innerRadius, radiusBlocks * 0.55f);
        float fogEnd = Math.max(fogStart + 1.0f, radiusBlocks * 0.95f);
        int uniformOffset = writeUniforms(viewProj, camera, renderState, levelState, fogStart, fogEnd,
            centerX, centerZ, level.getSeaLevel());

        Dx12Native.dx12BeginRenderPass(ctx, new long[]{color.handle()}, new int[]{0},
            new byte[]{0}, null, depth.handle(), 0, (byte) 0, 0.0, 0, 0, width, height);
        try {
            Dx12Native.dx12SetPipeline(ctx, pipeline, true);
            Dx12Native.dx12SetVertexBuffer(ctx, 0, slot.vertexBuffer.handle(), 0L, VERTEX_STRIDE);
            Dx12Native.dx12SetIndexBuffer(ctx, slot.indexBuffer.handle(), 1);
            pushUniform(ctx, uniformOffset);
            Dx12Native.dx12DrawIndexed(ctx, slot.indexCount, 1, 0, 0, 0);
        } finally {
            Dx12Native.dx12EndRenderPass(ctx);
        }
    }

    // ------------------------------------------------------------------
    // 几何
    // ------------------------------------------------------------------

    /**
     * 建一张以相机为中心的高度场网格。
     *
     * <p>坐标轴从 0 向两侧生长：每级以 {@code base} 为格宽走 {@code stepsPerLevel} 格，
     * 然后格宽翻倍，直到覆盖 {@code radius}。因此同一张网格里近处格宽只有几个方块、
     * 远处上千方块，且整张网格共享顶点，不存在拼接缝。
     */
    private void rebuildGeometry(long ctx, ClientLevel level, int centerX, int centerZ,
                                 int radiusBlocks, int base, int stepsPerLevel, float innerRadius,
                                 int cameraBlockX, int cameraBlockZ) {
        long startNanos = System.nanoTime();
        int[] axis = buildAxis(radiusBlocks, base, stepsPerLevel);
        int n = axis.length;
        int vertexCount = n * n;
        int cellCount = (n - 1) * (n - 1);
        // GPU 建网开关：一旦失败就永久回退 CPU（每帧重试只会刷日志）
        boolean gpu = !this.computeFailed && ensureCompute(n, axis);

        Slot slot = this.slots[this.rebuildIndex % GEOMETRY_SLOTS];
        if (slot == null) {
            slot = new Slot();
            this.slots[this.rebuildIndex % GEOMETRY_SLOTS] = slot;
        }
        this.rebuildIndex++;
        slot.ensure(vertexCount, cellCount * 6, gpu);

        int[] heights = this.heightScratch;
        int[] colors = this.colorScratch;
        if (heights.length < vertexCount) {
            heights = new int[vertexCount];
            colors = new int[vertexCount];
            this.heightScratch = heights;
            this.colorScratch = colors;
        }

        // 1) 采样每个角点的真实高度与地表方块颜色
        int minBuildY = level.getMinY();
        int maxBuildY = level.getMaxY();
        int known = 0;
        for (int j = 0; j < n; j++) {
            int z = centerZ + axis[j];
            for (int i = 0; i < n; i++) {
                int x = centerX + axis[i];
                int index = j * n + i;
                long sample = sampleCorner(level, x, z);
                int height = (int) (sample >> 32);
                if (height < minBuildY || height > maxBuildY) {
                    // 未探索到 / 高度图损坏：当作未知，绝不写进顶点
                    heights[index] = UNKNOWN_HEIGHT;
                    colors[index] = -1;
                    continue;
                }
                heights[index] = height;
                colors[index] = (int) sample;
                known++;
            }
        }

        int seaLevel = level.getSeaLevel();
        int snowLine = seaLevel + SNOW_ABOVE_SEA;

        // 1b) 顶点：GPU 可用时把降噪 / 分层配色 / 方向光烘焙整段交给 compute，
        // 主线程只负责采样与索引；compute 失败则本帧立刻回退 CPU，之后永久走 CPU。
        boolean gpuOk = false;
        if (gpu) {
            String failure = dispatchVertexBuild(ctx, slot, heights, colors, vertexCount, n,
                seaLevel, snowLine);
            if (failure == null) {
                gpuOk = true;
            } else {
                LOGGER.error("[lod] GPU 建网失败，永久回退 CPU 路径：{}", failure);
                this.computeFailed = true;
                // 顶点缓冲换回 CPU 可映射的 UPLOAD 资源
                slot.ensure(vertexCount, cellCount * 6, false);
            }
        }
        if (!gpuOk) {
            smoothHeights(axis, heights, n, vertexCount);
            writeVerticesCpu(axis, heights, colors, n, slot.vertexData, seaLevel, snowLine);
        }

        // 2) 索引：整块落在原版视距内的跳过，四个角都有效才连成两个三角形
        ByteBuffer indices = slot.indexData;
        int indexCount = 0;
        int innerSquared = (int) Math.min(innerRadius * innerRadius, Integer.MAX_VALUE / 4);
        int emitted = 0;
        for (int j = 0; j < n - 1; j++) {
            int z0 = centerZ + axis[j];
            int z1 = centerZ + axis[j + 1];
            int dz = Math.max(Math.abs(z0 - cameraBlockZ), Math.abs(z1 - cameraBlockZ));
            for (int i = 0; i < n - 1; i++) {
                int a = j * n + i;
                int b = a + 1;
                int d = a + n;
                int c = d + 1;
                if (heights[a] == UNKNOWN_HEIGHT || heights[b] == UNKNOWN_HEIGHT
                    || heights[c] == UNKNOWN_HEIGHT || heights[d] == UNKNOWN_HEIGHT) {
                    continue;
                }
                int x0 = centerX + axis[i];
                int x1 = centerX + axis[i + 1];
                int dx = Math.max(Math.abs(x0 - cameraBlockX), Math.abs(x1 - cameraBlockX));
                if (dx * dx + dz * dz < innerSquared) {
                    // 原版已经画过这块地形，不重复覆盖
                    continue;
                }
                putIndex(indices, indexCount, a);
                putIndex(indices, indexCount + 1, b);
                putIndex(indices, indexCount + 2, c);
                putIndex(indices, indexCount + 3, a);
                putIndex(indices, indexCount + 4, c);
                putIndex(indices, indexCount + 5, d);
                indexCount += 6;
                emitted++;
            }
        }

        slot.indexCount = indexCount;
        this.geometry = slot;

        long now = System.nanoTime();
        if (now - this.lastDiagnosticNanos > 5_000_000_000L) {
            this.lastDiagnosticNanos = now;
            LOGGER.info("[lod] 网格={}x{} 格宽={} {} 已采样={} 地形格={} 顶点={} 索引={} 缓存区块={} 耗时={}ms",
                n - 1, n - 1, base, gpuOk ? "GPU" : "CPU", known, emitted, vertexCount, indexCount,
                LodRegionCache.getInstance().cachedCount(),
                (System.nanoTime() - startNanos) / 1_000_000L);
        }
    }

    /**
     * 远景降噪：格宽达到上百方块时，单个角点的山峰会被拉成一根巨锥，因此把角点高度
     * 往四邻域均值按格宽混合，并硬性限制偏离幅度。近处（格宽 &lt; {@link #SMOOTH_START}）
     * 完全不动，保证和原版地形贴合。
     *
     * <p>与 {@link #COMPUTE_SHADER} 中的 {@code SmoothH} 等价，仅在 GPU 建网不可用时使用。
     */
    private void smoothHeights(int[] axis, int[] heights, int n, int vertexCount) {
        int[] smoothed = this.smoothScratch;
        if (smoothed.length < vertexCount) {
            smoothed = new int[vertexCount];
            this.smoothScratch = smoothed;
        }
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int index = j * n + i;
                int height = heights[index];
                if (height == UNKNOWN_HEIGHT) {
                    smoothed[index] = UNKNOWN_HEIGHT;
                    continue;
                }
                int cell = cellWidth(axis, i, j);
                if (cell < SMOOTH_START) {
                    smoothed[index] = height;
                    continue;
                }
                int sum = 0;
                int count = 0;
                if (i > 0 && heights[index - 1] != UNKNOWN_HEIGHT) {
                    sum += heights[index - 1];
                    count++;
                }
                if (i + 1 < n && heights[index + 1] != UNKNOWN_HEIGHT) {
                    sum += heights[index + 1];
                    count++;
                }
                if (j > 0 && heights[index - n] != UNKNOWN_HEIGHT) {
                    sum += heights[index - n];
                    count++;
                }
                if (j + 1 < n && heights[index + n] != UNKNOWN_HEIGHT) {
                    sum += heights[index + n];
                    count++;
                }
                if (count == 0) {
                    smoothed[index] = height;
                    continue;
                }
                float average = sum / (float) count;
                float blend = Math.min(SMOOTH_MAX, (cell - SMOOTH_START) / SMOOTH_SPAN);
                float mixed = height + (average - height) * blend;
                float limit = Math.max(SMOOTH_LIMIT_MIN, cell * SMOOTH_LIMIT_SCALE);
                if (mixed > average + limit) {
                    mixed = average + limit;
                } else if (mixed < average - limit) {
                    mixed = average - limit;
                }
                smoothed[index] = Math.round(mixed);
            }
        }
        System.arraycopy(smoothed, 0, heights, 0, vertexCount);
    }

    /**
     * CPU 写顶点：真实高度 + 真实地表色，再按坡度/高度分层（岩、沙、雪、水）并烘焙方向光。
     * 与 {@link #COMPUTE_SHADER} 的 {@code main} 等价，仅在 GPU 建网不可用时使用。
     */
    private void writeVerticesCpu(int[] axis, int[] heights, int[] colors, int n,
                                  ByteBuffer vertices, int seaLevel, int snowLine) {
        float[] gradient = this.gradientScratch;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int index = j * n + i;
                int height = heights[index];
                if (height == UNKNOWN_HEIGHT) {
                    // 未探索到的角点：不写顶点，也不会有任何索引引用它
                    continue;
                }
                int rgb = colors[index];
                if (rgb < 0) {
                    rgb = FALLBACK_COLOR;
                }
                gradient(axis, heights, n, i, j, gradient);
                float dhx = gradient[0];
                float dhz = gradient[1];
                float slope = (float) Math.sqrt(dhx * dhx + dhz * dhz);
                int alpha;
                if ((rgb & 0xFFFFFF) == WATER_COLOR) {
                    // 水体：换成偏暗的青蓝，并把 alpha 打成 0 交给像素着色器加水波
                    rgb = WATER_DEEP_COLOR;
                    alpha = 0;
                } else {
                    // 陡坡露岩：坡度越大越接近石头色，悬崖不再是「被拉长的草地」
                    rgb = mixColor(rgb, ROCK_COLOR,
                        Math.min(0.85f, Math.max(0.0f, (slope - ROCK_SLOPE) * 0.9f)));
                    // 海平面附近的低平地带带一点沙色，海岸线因此看得出来
                    if (slope < 0.45f && height <= seaLevel + 4 && height >= seaLevel - 4) {
                        rgb = mixColor(rgb, SAND_COLOR, 0.35f);
                    }
                    // 高处积雪
                    rgb = mixColor(rgb, SNOW_COLOR,
                        Math.min(0.9f, Math.max(0.0f, (height - snowLine) / 40.0f)));
                    alpha = 0xFF;
                }
                float shade = shadeFromGradient(dhx, dhz);
                // 顶点坐标是相对网格中心的局部坐标（见 writeUniforms 里的 CameraPos），
                // 这样大世界坐标不会因为 float 精度产生抖动
                putVertex(vertices, index, axis[i], height + 1.0f, axis[j],
                    shadeColor(rgb, shade), alpha);
            }
        }
    }

    // ------------------------------------------------------------------
    // GPU 建网
    // ------------------------------------------------------------------

    /**
     * 准备 compute 资源：管线、坐标轴（t1）与角点（t0）。
     *
     * <p>两个输入都是 UPLOAD 堆上的可映射缓冲（SRV 允许），只有输出顶点缓冲必须落在
     * DEFAULT 堆并带 ALLOW_UNORDERED_ACCESS —— 因此顶点缓冲一律不可 map。
     */
    private boolean ensureCompute(int n, int[] axis) {
        try {
            if (this.computePipeline == 0L) {
                long handle = Dx12Native.dx12CreateComputePipeline(
                    COMPUTE_SHADER.getBytes(StandardCharsets.UTF_8), 2, 1, COMPUTE_CONST_COUNT);
                if (handle == 0L) {
                    LOGGER.error("[lod] compute 管线创建失败，回退 CPU 建网");
                    this.computeFailed = true;
                    return false;
                }
                this.computePipeline = handle;
            }

            if (this.axisBuffer == null || this.axisBuffer.isClosed()
                || this.axisCapacity < n) {
                closeAxis();
                long bytes = (long) n * 4L;
                this.axisBuffer = new Dx12GpuBuffer(GpuBuffer.USAGE_MAP_WRITE, bytes);
                this.axisView = this.axisBuffer.map(0L, bytes, false, true);
                this.axisData = this.axisView.data();
                this.axisData.order(ByteOrder.LITTLE_ENDIAN);
                this.axisCapacity = n;
            }
            for (int i = 0; i < n; i++) {
                this.axisData.putInt(i * 4, axis[i]);
            }

            long vertexCount = (long) n * n;
            if (this.cornerBuffer == null || this.cornerBuffer.isClosed()
                || this.cornerCapacity < vertexCount) {
                closeCorners();
                long bytes = vertexCount * CORNER_STRIDE;
                this.cornerBuffer = new Dx12GpuBuffer(GpuBuffer.USAGE_MAP_WRITE, bytes);
                this.cornerView = this.cornerBuffer.map(0L, bytes, false, true);
                this.cornerData = this.cornerView.data();
                this.cornerData.order(ByteOrder.LITTLE_ENDIAN);
                this.cornerCapacity = (int) vertexCount;
            }
            return true;
        } catch (Throwable t) {
            LOGGER.error("[lod] GPU 建网资源准备失败，回退 CPU 建网", t);
            this.computeFailed = true;
            return false;
        }
    }

    /**
     * 把采样好的角点交给 compute 展开成顶点。
     *
     * @return {@code null} 表示成功，否则为失败原因（调用方据此永久回退 CPU）
     */
    private String dispatchVertexBuild(long ctx, Slot slot, int[] heights, int[] colors,
                                       int vertexCount, int n, int seaLevel, int snowLine) {
        ByteBuffer corners = this.cornerData;
        for (int k = 0; k < vertexCount; k++) {
            int offset = k * CORNER_STRIDE;
            corners.putInt(offset, heights[k]);
            corners.putInt(offset + 4, colors[k]);
        }
        int groups = (vertexCount + COMPUTE_GROUP_SIZE - 1) / COMPUTE_GROUP_SIZE;
        return Dx12Native.dx12DispatchCompute(ctx, this.computePipeline,
            new int[]{n, seaLevel, snowLine, 0},
            new long[]{this.cornerBuffer.handle(), this.axisBuffer.handle()},
            new int[]{0, 0},
            new int[]{vertexCount, n},
            new int[]{CORNER_STRIDE, 4},
            new long[]{slot.vertexBuffer.handle()},
            new int[]{0},
            new int[]{vertexCount},
            new int[]{VERTEX_STRIDE},
            groups, 1, 1);
    }

    private void closeAxis() {
        if (this.axisView != null) {
            try {
                this.axisView.close();
            } catch (Throwable ignored) {
            }
            this.axisView = null;
        }
        this.axisData = null;
        if (this.axisBuffer != null) {
            try {
                this.axisBuffer.close();
            } catch (Throwable ignored) {
            }
            this.axisBuffer = null;
        }
        this.axisCapacity = 0;
    }

    private void closeCorners() {
        if (this.cornerView != null) {
            try {
                this.cornerView.close();
            } catch (Throwable ignored) {
            }
            this.cornerView = null;
        }
        this.cornerData = null;
        if (this.cornerBuffer != null) {
            try {
                this.cornerBuffer.close();
            } catch (Throwable ignored) {
            }
            this.cornerBuffer = null;
        }
        this.cornerCapacity = 0;
    }

    /** 角点 (i,j) 周边最大格宽（方块），用来决定降噪强度。 */
    private static int cellWidth(int[] axis, int i, int j) {
        int width = 0;
        if (i > 0) {
            width = Math.max(width, axis[i] - axis[i - 1]);
        }
        if (i + 1 < axis.length) {
            width = Math.max(width, axis[i + 1] - axis[i]);
        }
        if (j > 0) {
            width = Math.max(width, axis[j] - axis[j - 1]);
        }
        if (j + 1 < axis.length) {
            width = Math.max(width, axis[j + 1] - axis[j]);
        }
        return width;
    }

    /**
     * 生成以 0 为中心、对称的坐标轴：每级走 {@code stepsPerLevel} 格后格宽翻倍，
     * 直到覆盖 {@code maxRadius}。
     */
    private static int[] buildAxis(int maxRadius, int base, int stepsPerLevel) {
        int[] forward = new int[256];
        int count = 1;
        forward[0] = 0;
        int coord = 0;
        int stepSize = base;
        int stepsInLevel = 0;
        while (coord < maxRadius) {
            coord += stepSize;
            if (count == forward.length) {
                forward = Arrays.copyOf(forward, count * 2);
            }
            forward[count++] = coord;
            if (++stepsInLevel >= stepsPerLevel) {
                stepsInLevel = 0;
                // 格宽翻倍，但按距离封顶：不封顶时远景一格能顶几百方块，
                // 整片丘陵都被三角面插值抹成一块平板
                stepSize = Math.min(stepSize << 1, stepCap(coord));
            }
        }
        int[] axis = new int[count * 2 - 1];
        for (int i = 0; i < count; i++) {
            axis[count - 1 - i] = -forward[i];
            axis[count - 1 + i] = forward[i];
        }
        return axis;
    }

    /** 该距离上允许的最大格宽（方块）。 */
    private static int stepCap(int distance) {
        if (distance <= FINE_DETAIL_RADIUS) {
            return FINE_STEP_BLOCKS;
        }
        if (distance <= COARSE_DETAIL_RADIUS) {
            return COARSE_STEP_BLOCKS;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 取一个角点的高度与地表颜色（打包成 long：高 32 位高度，低 32 位 RGB）。
     *
     * <p>已加载的区块优先用客户端里的高度图，保证和原版地形严丝合缝；之外回退到
     * {@link LodRegionCache} 后台读盘的结果。
     */
    private static long sampleCorner(ClientLevel level, int x, int z) {
        LevelChunk chunk = level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk != null) {
            int height;
            try {
                height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            } catch (Throwable t) {
                height = UNKNOWN_HEIGHT;
            }
            if (height != UNKNOWN_HEIGHT) {
                int rgb = LodRegionCache.getInstance().colorAt(x, z);
                if (rgb < 0) {
                    rgb = blockColorAt(level, x, height, z);
                }
                return ((long) height << 32) | (rgb & 0xFFFFFFFFL);
            }
        }
        long sample = LodRegionCache.getInstance().sample(x, z);
        return sample == LodRegionCache.NO_SAMPLE ? UNKNOWN_SAMPLE : sample;
    }

    private static int blockColorAt(ClientLevel level, int x, int y, int z) {
        try {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            MapColor color = state.getMapColor(level, pos);
            int rgb = color == null ? 0 : color.col;
            return rgb == 0 ? FALLBACK_COLOR : rgb;
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * 用相邻角点的高差估计角点 (i,j) 的高度梯度（方块 / 方块），结果写进 {@code out}。
     * 邻居未知时按「同高」处理，边缘点退化为单侧差分。
     */
    private static void gradient(int[] axis, int[] heights, int n, int i, int j, float[] out) {
        int center = heights[j * n + i];
        float dhx = 0.0f;
        float dhz = 0.0f;
        if (i > 0 && i + 1 < n) {
            int left = heights[j * n + i - 1];
            int right = heights[j * n + i + 1];
            if (left != UNKNOWN_HEIGHT && right != UNKNOWN_HEIGHT) {
                dhx = (right - left) / (float) (axis[i + 1] - axis[i - 1]);
            }
        } else if (i + 1 < n) {
            int right = heights[j * n + i + 1];
            if (right != UNKNOWN_HEIGHT) {
                dhx = (right - center) / (float) (axis[i + 1] - axis[i]);
            }
        } else if (i > 0) {
            int left = heights[j * n + i - 1];
            if (left != UNKNOWN_HEIGHT) {
                dhx = (center - left) / (float) (axis[i] - axis[i - 1]);
            }
        }
        if (j > 0 && j + 1 < n) {
            int down = heights[(j - 1) * n + i];
            int up = heights[(j + 1) * n + i];
            if (down != UNKNOWN_HEIGHT && up != UNKNOWN_HEIGHT) {
                dhz = (up - down) / (float) (axis[j + 1] - axis[j - 1]);
            }
        } else if (j + 1 < n) {
            int up = heights[(j + 1) * n + i];
            if (up != UNKNOWN_HEIGHT) {
                dhz = (up - center) / (float) (axis[j + 1] - axis[j]);
            }
        } else if (j > 0) {
            int down = heights[(j - 1) * n + i];
            if (down != UNKNOWN_HEIGHT) {
                dhz = (center - down) / (float) (axis[j] - axis[j - 1]);
            }
        }
        out[0] = dhx;
        out[1] = dhz;
    }

    /**
     * 由高度梯度算法线，和固定太阳方向点乘得到亮度系数。
     * 平地约 0.93，最陡的背光坡降到 {@link #AMBIENT}。
     */
    private static float shadeFromGradient(float dhx, float dhz) {
        float nx = -dhx;
        float ny = 1.0f;
        float nz = -dhz;
        float inverse = 1.0f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        float lambert = (nx * SUN_X + ny * SUN_Y + nz * SUN_Z) * inverse;
        if (lambert < 0.0f) {
            lambert = 0.0f;
        }
        return AMBIENT + DIFFUSE * lambert;
    }

    /** 按比例 {@code t} 混合两个 RGB 颜色。 */
    private static int mixColor(int from, int to, float t) {
        if (t <= 0.0f) {
            return from;
        }
        if (t >= 1.0f) {
            return to;
        }
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int r = clampByte(Math.round(fr + (((to >> 16) & 0xFF) - fr) * t));
        int g = clampByte(Math.round(fg + (((to >> 8) & 0xFF) - fg) * t));
        int b = clampByte(Math.round(fb + ((to & 0xFF) - fb) * t));
        return r << 16 | g << 8 | b;
    }

    private static int shadeColor(int rgb, float shade) {
        int r = (int) (((rgb >> 16) & 0xFF) * shade);
        int g = (int) (((rgb >> 8) & 0xFF) * shade);
        int b = (int) ((rgb & 0xFF) * shade);
        return clampByte(r) << 16 | clampByte(g) << 8 | clampByte(b);
    }

    private static int clampByte(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    /**
     * 写一个顶点。alpha 是给像素着色器的材质标记：255 = 普通陆地，0 = 水体。
     */
    private static void putVertex(ByteBuffer buffer, int vertexIndex, float x, float y, float z,
                                  int rgb, int alpha) {
        int offset = vertexIndex * VERTEX_STRIDE;
        buffer.putFloat(offset, x);
        buffer.putFloat(offset + 4, y);
        buffer.putFloat(offset + 8, z);
        // RGBA8_UNORM 在显存里按 R,G,B,A 排列，小端 int 写入即 b0=R
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        buffer.putInt(offset + 12, r | (g << 8) | (b << 16) | ((alpha & 0xFF) << 24));
    }

    private static void putIndex(ByteBuffer buffer, int indexOffset, int value) {
        buffer.putInt(indexOffset * 4, value);
    }

    // ------------------------------------------------------------------
    // uniform / pipeline
    // ------------------------------------------------------------------

    private int writeUniforms(float[] viewProj, CameraRenderState camera, GameRenderState gameState,
                              LevelRenderState levelState, float fogStart, float fogEnd,
                              int centerX, int centerZ, int seaLevel) {
        ensureUniformBuffer();
        int offset = (this.uniformSlotIndex++ % UNIFORM_RING) * UNIFORM_SLOT;
        ByteBuffer buffer = this.uniformMapped;
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < 16; i++) {
            buffer.putFloat(offset + i * 4, viewProj[i]);
        }
        // 顶点用的是相对网格中心的局部坐标（避免大世界坐标转换成 float 后的抖动），
        // 相机的相对位置也要跟着减去同一个中心
        buffer.putFloat(offset + 64, (float) (camera.pos.x - centerX));
        buffer.putFloat(offset + 68, (float) camera.pos.y);
        buffer.putFloat(offset + 72, (float) (camera.pos.z - centerZ));
        buffer.putFloat(offset + 76, 1.0f);

        buffer.putFloat(offset + 80, fogStart);
        buffer.putFloat(offset + 84, fogEnd);
        buffer.putFloat(offset + 88, worldBrightness(gameState, levelState));
        buffer.putFloat(offset + 92, 0.0f);

        SkyRenderState sky = levelState == null ? null : levelState.skyRenderState;
        int skyColor = sky == null ? 0x8090C0 : sky.skyColor;
        buffer.putFloat(offset + 96, ((skyColor >> 16) & 0xFF) / 255.0f);
        buffer.putFloat(offset + 100, ((skyColor >> 8) & 0xFF) / 255.0f);
        buffer.putFloat(offset + 104, (skyColor & 0xFF) / 255.0f);
        buffer.putFloat(offset + 108, 1.0f);

        // 天空光染色：白天白、夜里偏蓝，只调色不动整体亮度
        float tintR = 1.0f;
        float tintG = 1.0f;
        float tintB = 1.0f;
        LightmapRenderState lightmap = gameState == null ? null : gameState.lightmapRenderState;
        if (lightmap != null && lightmap.skyLightColor != null) {
            Vector3fc color = lightmap.skyLightColor;
            tintR = 1.0f + (color.x() - 1.0f) * SKY_TINT_STRENGTH;
            tintG = 1.0f + (color.y() - 1.0f) * SKY_TINT_STRENGTH;
            tintB = 1.0f + (color.z() - 1.0f) * SKY_TINT_STRENGTH;
        }
        buffer.putFloat(offset + 112, tintR);
        buffer.putFloat(offset + 116, tintG);
        buffer.putFloat(offset + 120, tintB);
        buffer.putFloat(offset + 124, 1.0f);

        // Params.x = 海平面：像素着色器据此做高度明暗
        buffer.putFloat(offset + 128, (float) seaLevel);
        buffer.putFloat(offset + 132, 0.0f);
        buffer.putFloat(offset + 136, 0.0f);
        buffer.putFloat(offset + 140, 0.0f);
        return offset;
    }

    /**
     * 世界亮度：取原版 lightmap 的 {@code skyFactor}（白天 1.0、深夜 0.24），
     * 因此远景的黑夜程度和近处地形一致；取不到时退回按天空色亮度估算。
     */
    private static float worldBrightness(GameRenderState gameState, LevelRenderState levelState) {
        LightmapRenderState lightmap = gameState == null ? null : gameState.lightmapRenderState;
        if (lightmap != null && lightmap.skyFactor > 0.0f) {
            return Math.min(1.0f, Math.max(MIN_BRIGHTNESS, lightmap.skyFactor));
        }
        SkyRenderState sky = levelState == null ? null : levelState.skyRenderState;
        int skyColor = sky == null ? 0x8090C0 : sky.skyColor;
        float r = ((skyColor >> 16) & 0xFF) / 255.0f;
        float g = ((skyColor >> 8) & 0xFF) / 255.0f;
        float b = (skyColor & 0xFF) / 255.0f;
        float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;
        return Math.min(1.0f, Math.max(MIN_BRIGHTNESS, luma));
    }

    private void ensureUniformBuffer() {
        if (this.uniformBuffer != null && !this.uniformBuffer.isClosed()) {
            return;
        }
        this.uniformBuffer = new Dx12GpuBuffer(GpuBuffer.USAGE_MAP_WRITE,
            (long) UNIFORM_SLOT * UNIFORM_RING);
        this.uniformView = this.uniformBuffer.map(0L, (long) UNIFORM_SLOT * UNIFORM_RING,
            false, true);
        this.uniformMapped = this.uniformView.data();
        this.uniformMapped.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void pushUniform(long ctx, int offset) {
        if (!Dx12Native.dx12PushDescriptors(ctx, new int[]{0},
            new long[]{this.uniformBuffer.handle()}, new long[]{offset},
            new long[]{UNIFORM_DATA_BYTES}, new int[]{0}, new long[]{0L})) {
            throw new IllegalStateException("dx12PushDescriptors 失败");
        }
    }

    private long pipelineFor(int colorFormat) {
        Long cached = this.pipelines.get(colorFormat);
        if (cached != null) {
            return cached;
        }
        long handle = createPipeline(colorFormat);
        this.pipelines.put(colorFormat, handle);
        if (handle == 0L) {
            LOGGER.error("[lod] LOD 管线创建失败（颜色格式 ordinal={}）", colorFormat);
        }
        return handle;
    }

    private static long createPipeline(int colorFormat) {
        byte[] vs = (UNIFORM_HEADER + VERTEX_SHADER).getBytes(StandardCharsets.UTF_8);
        byte[] ps = (UNIFORM_HEADER + PIXEL_SHADER).getBytes(StandardCharsets.UTF_8);
        String[] semantics = {"POSITION", "COLOR"};

        int size = 4 + vs.length + 4 + ps.length;
        size += 4 + 12;          // colorCount + 一个颜色目标
        size += 1 + 6;           // hasDepth + 深度状态
        size += 4 + 1 + 4;       // topology / cull / polygonMode
        size += 4 + 24 * 2;      // inputElementCount + 两个元素
        size += 4;               // semanticCount
        for (String semantic : semantics) {
            size += 4 + semantic.length();
        }
        size += 4 + 2;           // entryCount + 一条 CBV b0

        ByteBuffer desc = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
        desc.putInt(vs.length);
        desc.put(vs);
        desc.putInt(ps.length);
        desc.put(ps);

        desc.putInt(1);
        desc.putInt(colorFormat);
        desc.put((byte) 0x0F);   // 写 RGBA
        desc.put((byte) 0);      // 不混合
        for (int i = 0; i < 6; i++) {
            desc.put((byte) 0);
        }

        desc.put((byte) 1);      // hasDepth
        desc.putInt(51);         // GpuFormat.D32_FLOAT
        desc.put((byte) 1);      // 写深度
        desc.put((byte) 5);      // CompareOp.GREATER_THAN_OR_EQUAL（反向 Z）

        desc.putInt(4);          // PrimitiveTopology.TRIANGLES
        desc.put((byte) 0);      // 不剔除：网格方块两侧都可见
        desc.putInt(0);          // PolygonMode.FILL

        desc.putInt(2);          // inputElementCount
        // location, binding, format, offset, stride, stepRate
        desc.putInt(0);
        desc.putInt(0);
        desc.putInt(FORMAT_POSITION);
        desc.putInt(0);
        desc.putInt(VERTEX_STRIDE);
        desc.putInt(0);
        desc.putInt(1);
        desc.putInt(0);
        desc.putInt(FORMAT_COLOR);
        desc.putInt(12);
        desc.putInt(VERTEX_STRIDE);
        desc.putInt(0);

        desc.putInt(semantics.length);
        for (String semantic : semantics) {
            byte[] bytes = semantic.getBytes(StandardCharsets.UTF_8);
            desc.putInt(bytes.length);
            desc.put(bytes);
        }

        desc.putInt(1);          // entryCount
        desc.put((byte) 0);      // CBV
        desc.put((byte) 0);      // register b0

        desc.flip();
        return Dx12Native.dx12CreateGraphicsPipeline(desc);
    }

    private static float fovY(Matrix4f projection) {
        float m11 = projection.m11();
        return Math.abs(m11) < 1.0e-6f ? (float) (Math.PI / 2.0) : 2.0f * (float) Math.atan(1.0 / m11);
    }

    private static float aspect(Matrix4f projection) {
        float m00 = projection.m00();
        float m11 = projection.m11();
        if (Math.abs(m00) < 1.0e-6f || Math.abs(m11) < 1.0e-6f) {
            return 1.0f;
        }
        return m11 / m00;
    }

    /** 顶点/索引缓冲槽位：{@code ensure} 只在容量不足时重建缓冲。 */
    private static final class Slot {
        private Dx12GpuBuffer vertexBuffer;
        private Dx12GpuBuffer indexBuffer;
        private GpuBufferSlice.MappedView vertexView;
        private GpuBufferSlice.MappedView indexView;
        private ByteBuffer vertexData;
        private ByteBuffer indexData;
        /** 容量按“顶点数/索引数”计，字节数由 VERTEX_STRIDE 换算。 */
        private int vertexCapacity;
        private int indexCapacity;
        private int indexCount;
        /** 顶点缓冲是否由 compute（UAV）写入：DEFAULT 堆，不可 map。 */
        private boolean vertexUav;

        void ensure(int vertexCount, int indexCount, boolean gpu) {
            // 缓冲类型必须和本帧的建网方式一致，否则 GPU→CPU 回退时容量已够、
            // 会被误判为“无需重建”，vertexData 仍是 null。
            if (vertexBuffer == null || vertexBuffer.isClosed()
                || this.vertexCapacity < vertexCount || this.vertexUav != gpu) {
                closeVertex();
                long bytes = (long) vertexCount * VERTEX_STRIDE;
                if (gpu) {
                    // compute 只能写 DEFAULT 堆，而 DEFAULT 堆不可 map
                    this.vertexBuffer = new Dx12GpuBuffer(
                        BUFFER_USAGE_UAV | GpuBuffer.USAGE_VERTEX, bytes);
                    this.vertexView = null;
                    this.vertexData = null;
                    this.vertexUav = true;
                } else {
                    this.vertexBuffer = new Dx12GpuBuffer(
                        GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_VERTEX, bytes);
                    this.vertexView = this.vertexBuffer.map(0L, bytes, false, true);
                    this.vertexData = this.vertexView.data();
                    this.vertexData.order(ByteOrder.LITTLE_ENDIAN);
                    this.vertexUav = false;
                }
                this.vertexCapacity = vertexCount;
            }
            if (indexBuffer == null || indexBuffer.isClosed() || this.indexCapacity < indexCount) {
                closeIndex();
                long bytes = (long) indexCount * 4L;
                this.indexBuffer = new Dx12GpuBuffer(
                    GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDEX, bytes);
                this.indexView = this.indexBuffer.map(0L, bytes, false, true);
                this.indexData = this.indexView.data();
                this.indexData.order(ByteOrder.LITTLE_ENDIAN);
                this.indexCapacity = indexCount;
            }
        }

        private void closeVertex() {
            if (this.vertexView != null) {
                try {
                    this.vertexView.close();
                } catch (Throwable ignored) {
                }
                this.vertexView = null;
            }
            this.vertexData = null;
            if (this.vertexBuffer != null) {
                try {
                    this.vertexBuffer.close();
                } catch (Throwable ignored) {
                }
                this.vertexBuffer = null;
            }
            this.vertexCapacity = 0;
            this.vertexUav = false;
        }

        private void closeIndex() {
            if (this.indexView != null) {
                try {
                    this.indexView.close();
                } catch (Throwable ignored) {
                }
                this.indexView = null;
            }
            this.indexData = null;
            if (this.indexBuffer != null) {
                try {
                    this.indexBuffer.close();
                } catch (Throwable ignored) {
                }
                this.indexBuffer = null;
            }
            this.indexCapacity = 0;
        }
    }
}
