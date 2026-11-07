package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.config.Dx12Config;
import com.xgdt.dx12.lod.LodRegionCache;
import com.xgdt.dx12.lod.LodVoxelCache;
import com.xgdt.dx12.lod.LodVoxelMesher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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
import net.minecraft.tags.BlockTags;
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
    /** 建网根常量个数：N / 顶点容量 / 海平面 / 雪线 / Pass / 内圈半径 / 相机相对网格中心 x,z。 */
    private static final int COMPUTE_CONST_COUNT = 12;
    /** 角点输入布局：int height + int rgb。 */
    private static final int CORNER_STRIDE = 8;
    /**
     * quad 模式的顶点缓冲硬上限（顶点数，约 101 MB）。
     *
     * <p>每格 8 个顶点（压平顶面 4 个 + 两条竖直墙各补 2 个），必须有上限兜底：
     * 超过时 compute 会丢弃后续格子，而不是把缓冲写爆。
     *
     * <p>坐标轴环数由这个上限反推（见 {@link #AXIS_RINGS}），所以网格规模恒定为
     * 上限能装下的最大值：{@code (2*441)² = 777,924} 格 × 8 = 6,223,392 顶点，
     * 留约 1.2% 余量给等比段的取整误差。
     */
    private static final int MAX_QUAD_VERTS = 6_300_000;

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

    /**
     * 远景降噪：格宽超过 {@link #SMOOTH_START} 方块后，把角点高度往邻域均值拉，
     * 混合比例随格宽线性增长到 {@link #SMOOTH_MAX}。
     *
     * <p>远处一格的宽度可能上百方块，单个角点若是山峰，整格就会被拉成一根巨锥
     * （截图里的紫白色尖刺就是这么来的）。按格宽做多尺度降噪后，远景只剩宏观起伏，
     * 与「Voxy 远景只剩山形」的观感一致。
     */
    private static final float SMOOTH_START = 64.0f;
    private static final float SMOOTH_SPAN = 320.0f;
    private static final float SMOOTH_MAX = 0.6f;
    /** 平滑后仍允许偏离邻域均值的幅度：{@code max(MIN, 格宽 * SCALE)}。 */
    private static final float SMOOTH_LIMIT_MIN = 24.0f;
    private static final float SMOOTH_LIMIT_SCALE = 2.0f;

    /** 孤立尖刺判据：比四周最高点还高出这么多格才算「孤立」，见 {@link #removeIsolatedSpikes}。 */
    private static final int PEAK_TOLERANCE = 3;

    /** 世界亮度下限：深夜远景仍能辨认，不会全黑。取值过小时整个远景会沉进雾色的蓝。 */
    private static final float MIN_BRIGHTNESS = 0.45f;
    /** 天空光颜色（夜里偏蓝）只做染色，不作为整体亮度系数。 */
    private static final float SKY_TINT_STRENGTH = 0.15f;

    /**
     * 原版视距圈之外、保持最小格宽的那一段长度（方块）。
     *
     * <p>这一段是玩家最先盯着的远景，也是 LOD 和原版地形的接缝，必须用最细的格宽
     * 才不露馅；再往外就交给等比放大的段。
     */
    private static final int NEAR_DETAIL_BAND = 192;

    /**
     * 坐标轴单侧环数，由顶点上限反推：{@code (2*rings)² * 8 <= MAX_QUAD_VERTS}。
     *
     * <p>留两环余量，避免等比段的取整让环数多出几环、刚好越过顶点上限。
     */
    private static final int AXIS_RINGS =
        (int) Math.floor(Math.sqrt(MAX_QUAD_VERTS / 8.0) / 2.0) - 2;

    /** 分层配色：陡坡露岩、水边显沙、高处积雪，避免整片纯绿/纯蓝。 */
    private static final float ROCK_SLOPE = 1.5f;
    private static final float ROCK_SPAN = 1.7f;
    private static final float ROCK_MAX = 0.45f;
    private static final int ROCK_COLOR = 0x8C8C8C;
    private static final int SAND_COLOR = 0xE2D6A8;
    private static final int SNOW_COLOR = 0xE8EEF4;
    /** 远景水体的配色：MapColor.WATER 的纯蓝铺满整片湖太假，换成偏青的深水色。 */
    private static final int WATER_DEEP_COLOR = 0x2F6FC4;
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
        + "// 三维哈希：远景细节噪声必须用三维坐标，见下面 px 那段注释。\n"
        + "float hash31(float3 p)\n"
        + "{\n"
        + "    p = frac(p * 0.1031);\n"
        + "    p += dot(p, p.yzx + 33.33);\n"
        + "    return frac((p.x + p.y) * p.z);\n"
        + "}\n"
        + "float4 main(VSOutput input) : SV_TARGET\n"
        + "{\n"
        + "    // input.world 已经在顶点着色器里减掉了相机位置，就是相机相对坐标\n"
        + "    float3 rel = input.world;\n"
        + "    float3 world = rel + CameraPos.xyz;\n"
        + "    float dist = length(rel);\n"
        + "    float fog = saturate((dist - FogParams.x) / max(FogParams.y - FogParams.x, 1.0));\n"
        + "    float3 top = input.color.rgb;\n"
        + "\n"
        + "    // 面法线：屏幕空间导数重建。顶面把 y 取绝对值强制朝上（反向 Z 下叉积符号\n"
        + "    // 会随三角形绕序翻转，直接拿它算光会一亮一暗）；墙面只保留水平分量，并对\n"
        + "    // x/z 取绝对值，保证同一条墙的两半不会一明一暗。\n"
        + "    float3 face = cross(ddx(rel), ddy(rel));\n"
        + "    float faceLen = length(face);\n"
        + "    float3 normal = faceLen > 1e-8 ? face / faceLen : float3(0.0, 1.0, 0.0);\n"
        + "    if (abs(normal.y) < 0.5)\n"
        + "    {\n"
        + "        normal = float3(abs(normal.x), 0.0, abs(normal.z));\n"
        + "        normal = length(normal) > 1e-6 ? normalize(normal) : float3(0.0, 1.0, 0.0);\n"
        + "    }\n"
        + "    else\n"
        + "    {\n"
        + "        normal.y = abs(normal.y);\n"
        + "        normal = normalize(normal);\n"
        + "    }\n"
        + "\n"
        + "    // 顶面就是地表本色。以前还按「绿色占比」往泥土色拉一部分，结果每级台阶的\n"
        + "    // 边缘都刷成一条棕带，远景整片是竖条纹。草坡就该是草色，露岩交给坡度分层。\n"
        + "    float3 rgb = top;\n"
        + "\n"
        + "    // 方块面明暗：原版是顶面 1.0、南北墙 0.8、东西墙 0.6。LOD 的墙全是竖直面，\n"
        + "    // 按法线主轴挑一个固定系数，否则竖直墙和顶面的亮度只差几个百分点，压平的\n"
        + "    // 顶面和侧壁会糊成一整片，完全看不出方块台阶。\n"
        + "    float vertical = 1.0 - saturate(abs(normal.y));\n"
        + "    float faceShade = 1.0 - vertical * (abs(normal.x) > abs(normal.z) ? 0.40 : 0.20);\n"
        + "    // 方向光：光源固定在西侧上方，顶面最亮、陡面暗，接近原版的方块面明暗\n"
        + "    float lambert = saturate(dot(normal, normalize(float3(0.42, 0.80, -0.42))) * 0.5 + 0.5);\n"
        + "    rgb *= (0.88 + 0.20 * lambert) * faceShade;\n"
        + "\n"
        + "    // 逐方块颗粒：按 1 方块网格取三维哈希。靠近时才有、像素跨度大到看不见\n"
        + "    // 单一方块时淡出，否则远处会出现整屏闪烁的噪点。\n"
        + "    float px = max(max(length(ddx(world.xz)), length(ddy(world.xz))), 0.02);\n"
        + "    float fine = saturate(1.0 - px * 0.55);\n"
        + "    float n1 = hash31(floor(world)) - 0.5;\n"
        + "    float n2 = hash31(floor(world * 0.25) + 19.0) - 0.5;\n"
        + "    float n3 = hash31(floor(world * 0.0625) + 71.0) - 0.5;\n"
        + "    rgb *= 1.0 + n1 * 0.08 * fine + n2 * 0.06 + n3 * 0.05;\n"
        + "\n"
        + "    // 大尺度色块抖动：只抖亮度、不往某个色相偏\n"
        + "    rgb *= 1.0 + (hash31(floor(world * 0.125) + 5.0) - 0.5) * 0.08;\n"
        + "\n"
        + "    // 高度明暗：高处略亮、低地略暗，远景才有体积而不是一张平色\n"
        + "    rgb *= 1.0 + saturate((world.y - Params.x) * 0.012) * 0.10;\n"
        + "\n"
        + "    // 顶点色 alpha = 0 是水体标记。水面是平的，不该跟着地表颗粒噪点抖，\n"
        + "    // 只用大尺度波光把水体本色提亮一点。墙面不会命中这里（alpha 由更高格决定，\n"
        + "    // 通常仍是 255），竖直水墙极少见。\n"
        + "    float water = 1.0 - saturate(input.color.a * 2.0);\n"
        + "    if (water > 0.01)\n"
        + "    {\n"
        + "        float ripple = hash31(floor(world * 0.0625) + 13.0) - 0.5;\n"
        + "        float ripple2 = hash31(floor(world * 0.015625) + 41.0) - 0.5;\n"
        + "        float3 surface = top * (1.02 + ripple * 0.10 + ripple2 * 0.12);\n"
        + "        rgb = lerp(rgb, surface, water);\n"
        + "    }\n"
        + "\n"
        + "    rgb *= FogParams.z * LightTint.rgb;\n"
        + "    return float4(lerp(rgb, FogColor.rgb, fog), 1.0);\n"
        + "}\n";

    /**
     * 顶点生成的 compute 着色器：<b>一个线程 = 一个格子</b>。
     *
     * <p>每个有效格子用 {@code InterlockedAdd} 在顶点缓冲里抢 8 个槽位，写出
     * <b>一个压平的顶面 + 两条竖直墙面</b>：顶面 4 个顶点在 {@code h00} 处压平，
     * +X / +Z 两个方向的墙面各复用顶面边界上的 2 个顶点、再补 2 个邻居高度上的
     * 顶点。每条内部边界只由低坐标一侧的格子负责，因此网格保持水密，不会重复画
     * 同一面墙。
     *
     * <p>抢到的槽位总数写进计数器，第二个 dispatch（{@code Pass == 1}，单线程）
     * 把它翻译成 ExecuteIndirect 的参数 {@code {indexCount, 1, 0, 0}} 并顺手把计数器
     * 清零。主线程完全不需要回读，也不需要预先知道这帧有多少个可见格子，画多少个
     * 由 GPU 自己决定。
     */
    private static final String COMPUTE_SHADER =
        "cbuffer Build : register(b0)\n"
        + "{\n"
        + "    uint N;            // 每边角点数（格子数 = (N-1)^2）\n"
        + "    uint MaxVerts;     // 顶点缓冲容量（顶点数）\n"
        + "    int SeaLevel;\n"
        + "    int SnowLine;\n"
        + "    uint Mode;         // 0 = 展开 quad, 1 = 收尾写 args, 2 = 清计数\n"
        + "    float InnerRadius; // 原版视距圈半径（方块）\n"
        + "    int CamRelX;       // 相机相对网格中心的方块偏移\n"
        + "    int CamRelZ;\n"
        + "    // 体素 LOD 实际覆盖的矩形（相对网格中心）。区块是 16 方块对齐的，覆盖区\n"
        + "    // 通常不以相机为中心，用单个半径只能取最保守的并集，会在某些方向多叠十几格。\n"
        + "    // 这里直接给矩形，整格都落在矩形里才跳过；跨边界的格子照画，接缝最多重叠一格。\n"
        + "    int VoxelMinX;\n"
        + "    int VoxelMaxX;\n"
        + "    int VoxelMinZ;\n"
        + "    int VoxelMaxZ;\n"
        + "};\n"
        + "struct Corner\n"
        + "{\n"
        + "    int height;      // 未知用 -2147483648\n"
        + "    int rgb;         // 0xRRGGBB，-1 表示未采样到颜色\n"
        + "};\n"
        + "StructuredBuffer<Corner> Corners : register(t0);\n"
        + "StructuredBuffer<int> Axis : register(t1);\n"
        + "RWStructuredBuffer<uint4> OutVerts : register(u0);\n"
        + "RWStructuredBuffer<uint> Counter : register(u1);\n"
        + "RWStructuredBuffer<uint> Args : register(u2);\n"
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
        + "// 拉成一根巨锥）。格宽小于 64 时完全不动，保证和原版地形贴合。\n"
        + "int SmoothH(uint i, uint j)\n"
        + "{\n"
        + "    int h = RawH(i, j);\n"
        + "    if (h == UNKNOWN) { return UNKNOWN; }\n"
        + "    int cell = CellW(i, j);\n"
        + "    if (cell < 64) { return h; }\n"
        + "    int sum = 0;\n"
        + "    int count = 0;\n"
        + "    if (i > 0) { int v = RawH(i - 1, j); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (i + 1 < N) { int v = RawH(i + 1, j); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (j > 0) { int v = RawH(i, j - 1); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (j + 1 < N) { int v = RawH(i, j + 1); if (v != UNKNOWN) { sum += v; count++; } }\n"
        + "    if (count == 0) { return h; }\n"
        + "    float average = (float)sum / (float)count;\n"
        + "    float blend = min(0.6, (cell - 64.0) / 320.0);\n"
        + "    float mixed = h + (average - h) * blend;\n"
        + "    float limit = max(24.0, (float)cell * 2.0);\n"
        + "    mixed = clamp(mixed, average - limit, average + limit);\n"
        + "    return (int)round(mixed);\n"
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
        + "// 单个角点的最终颜色：分层配色（露岩 / 沙滩 / 积雪 / 水体）全部按这个角点自己的\n"
        + "// 高度算，不再是整格一个平均色。一格上百方块时四角各给一个颜色，插值出来的地面\n"
        + "// 会跟着地形变，而不是糊成一块纯色。\n"
        + "int ShadeCorner(int rgb, int height, float slope, out int alpha)\n"
        + "{\n"
        + "    // rgb < 0 是「未采样到颜色」的哨兵值。必须先处理它：-1 的补码里 bit 24\n"
        + "    // 也是 1，直接查水体标记会把整片未知区域误判成水面。\n"
        + "    if (rgb < 0) { rgb = 0x8A8A8A; }\n"
        + "    // 水体标记（bit 24）在解包地表方块时就按方块名打好了，不靠颜色猜：\n"
        + "    // MapColor.WATER 的色值随版本变过，按颜色相等判断会把整片水面漏掉。\n"
        + "    else if ((rgb & 0x01000000) != 0)\n"
        + "    {\n"
        + "        alpha = 0;\n"
        + "        return 0x2F6FC4;\n"
        + "    }\n"
        + "    rgb = MixColor(rgb, 0x8C8C8C, smoothstep(1.5, 3.2, slope) * 0.45);\n"
        + "    if (slope < 0.45 && height <= SeaLevel + 4 && height >= SeaLevel - 4)\n"
        + "    {\n"
        + "        rgb = MixColor(rgb, 0xE2D6A8, 0.35);\n"
        + "    }\n"
        + "    rgb = MixColor(rgb, 0xE8EEF4,\n"
        + "        min(0.9, max(0.0, (float)(height - SnowLine) / 40.0)));\n"
        + "    alpha = 255;\n"
        + "    return rgb;\n"
        + "}\n"
        + "// 顶点格式是 R8G8B8A8_UNORM（日志 ord=6 -> dxgi_fmt=28），R 在最低字节。\n"
        + "// rgb 是 0xRRGGBB，所以红必须写到最低 8 位，蓝写到第 16..23 位，和 CPU 侧 putVertex 一致。\n"
        + "uint PackColor(int rgb, int alpha)\n"
        + "{\n"
        + "    return (uint)((rgb >> 16) & 0xFF)\n"
        + "        | ((uint)((rgb >> 8) & 0xFF) << 8)\n"
        + "        | ((uint)(rgb & 0xFF) << 16)\n"
        + "        | ((uint)alpha << 24);\n"
        + "}\n"
        + "uint4 Vertex(int x, int z, int height, uint packed)\n"
        + "{\n"
        + "    // height 是「首个空气方块的 y」，也就是地表顶面所在的层，直接用\n"
        + "    return uint4(asuint((float)x), asuint((float)height),\n"
        + "        asuint((float)z), packed);\n"
        + "}\n"
        + "[numthreads(64, 1, 1)]\n"
        + "void main(uint3 tid : SV_DispatchThreadID)\n"
        + "{\n"
        + "    // Mode 2：先把计数器清零（首帧缓冲内容是未定义的，不能直接 InterlockedAdd）\n"
        + "    if (Mode == 2)\n"
        + "    {\n"
        + "        if (tid.x == 0) { Counter[0] = 0; }\n"
        + "        return;\n"
        + "    }\n"
        + "    // Mode 1：把计数翻译成 ExecuteIndirect 的参数（索引绘制），顺手清回 0\n"
        + "    if (Mode == 1)\n"
        + "    {\n"
        + "        if (tid.x == 0)\n"
        + "        {\n"
        + "            uint cells = Counter[0] / 8;\n"
        + "            uint maxCells = MaxVerts / 8;\n"
        + "            if (cells > maxCells) { cells = maxCells; }\n"
        + "            Args[0] = cells * 18;  // 每格 18 个索引：顶面 6 + 两面墙各 6\n"
        + "            Args[1] = 1;\n"
        + "            Args[2] = 0;\n"
        + "            Args[3] = 0;\n"
        + "            Args[4] = 0;\n"
        + "            Counter[0] = 0;\n"
        + "        }\n"
        + "        return;\n"
        + "    }\n"
        + "    uint span = N - 1;\n"
        + "    uint cell = tid.x;\n"
        + "    if (cell >= span * span) { return; }\n"
        + "    uint i = cell % span;\n"
        + "    uint j = cell / span;\n"
        + "    // 四个角的高度必须全部已知，否则这一格留给 CPU 兜底/不画\n"
        + "    int h00 = SmoothH(i, j);\n"
        + "    int h10 = SmoothH(i + 1, j);\n"
        + "    int h01 = SmoothH(i, j + 1);\n"
        + "    int h11 = SmoothH(i + 1, j + 1);\n"
        + "    if (h00 == UNKNOWN || h10 == UNKNOWN || h01 == UNKNOWN || h11 == UNKNOWN) { return; }\n"
        + "    int x0 = Axis[i];\n"
        + "    int x1 = Axis[i + 1];\n"
        + "    int z0 = Axis[j];\n"
        + "    int z1 = Axis[j + 1];\n"
        + "    // 内圈（原版视距圈 / 体素 LOD 的方形覆盖区）里已经画过了，跳过。\n"
        + "    // 用「方形 + 最远角」：完全落在方形内的格子才跳过，跨在边界上的格子照画，\n"
        + "    // 于是接缝最多重叠一格（1 方块），不会因为用圆形/最近点判定而空出一整圈。\n"
        + "    int fx = max(abs(CamRelX - x0), abs(CamRelX - x1));\n"
        + "    int fz = max(abs(CamRelZ - z0), abs(CamRelZ - z1));\n"
        + "    if (max(fx, fz) < (int)InnerRadius) { return; }\n"
        + "    // 体素覆盖区（按实际区块矩形，可能不对称）：整格都在矩形里才跳过。\n"
        + "    // 跨在边界上的格子必须照画，否则体素/高度场之间会空出一条缝。\n"
        + "    if (min(x0, x1) >= VoxelMinX && max(x0, x1) <= VoxelMaxX\n"
        + "        && min(z0, z1) >= VoxelMinZ && max(z0, z1) <= VoxelMaxZ) { return; }\n"
        + "    // 抢 8 个顶点槽位（顶面 4 个 + 两条墙各复用 2 个顶面顶点、再补 2 个）；\n"
        + "    // 越界就把这一格丢掉（远处优先，保证缓冲不会写爆）。\n"
        + "    uint slot;\n"
        + "    InterlockedAdd(Counter[0], 8, slot);\n"
        + "    if (slot + 8 > MaxVerts) { return; }\n"
        + "    // 坡度逐格算（四点差分），颜色逐角点算：整格涂一个平均色等于把地表纹理糊\n"
        + "    // 成一团，近景一格就是一方块时四角同色还看不出来，远景一格上百方块就成了\n"
        + "    // 一块巨大的纯色补丁 —— 「太糊」主要就出在这里。\n"
        + "    float dhx = (float)(h10 + h11 - h00 - h01) * 0.5 / (float)max(1, x1 - x0);\n"
        + "    float dhz = (float)(h01 + h11 - h00 - h10) * 0.5 / (float)max(1, z1 - z0);\n"
        + "    float slope = sqrt(dhx * dhx + dhz * dhz);\n"
        + "    int a00, a10, a11, a01;\n"
        + "    int c00 = ShadeCorner(Corners[j * N + i].rgb, h00, slope, a00);\n"
        + "    int c10 = ShadeCorner(Corners[j * N + i + 1].rgb, h10, slope, a10);\n"
        + "    int c01 = ShadeCorner(Corners[(j + 1) * N + i].rgb, h01, slope, a01);\n"
        + "    int c11 = ShadeCorner(Corners[(j + 1) * N + i + 1].rgb, h11, slope, a11);\n"
        + "    // 顶面在 h00 处压平：整格是一个真正的方块平面，和邻居的高差由竖直墙补上。\n"
        + "    // 最外圈没有邻居，墙高退回 h00，墙自然退化成零面积三角形。\n"
        + "    int wxh = (i + 1 < span) ? h10 : h00;\n"
        + "    int wzh = (j + 1 < span) ? h01 : h00;\n"
        + "    // 墙颜色取更高的、暴露在外的那一格：邻居更高用邻居的顶面色，否则用本格。\n"
        + "    int wxa = (h10 > h00) ? a10 : a00;\n"
        + "    int wxc = (h10 > h00) ? c10 : c00;\n"
        + "    int wza = (h01 > h00) ? a01 : a00;\n"
        + "    int wzc = (h01 > h00) ? c01 : c00;\n"
        + "    // 8 个顶点：0..3 顶面（都在 h00），4/5 是 +X 墙顶（h10），6/7 是 +Z 墙顶（h01）。\n"
        + "    // 墙面底部复用 0..3 里的边界顶点，因此 8 个顶点就够画三个 quad。\n"
        + "    OutVerts[slot + 0] = Vertex(x0, z0, h00, PackColor(c00, a00));\n"
        + "    OutVerts[slot + 1] = Vertex(x1, z0, h00, PackColor(c10, a10));\n"
        + "    OutVerts[slot + 2] = Vertex(x1, z1, h00, PackColor(c11, a11));\n"
        + "    OutVerts[slot + 3] = Vertex(x0, z1, h00, PackColor(c01, a01));\n"
        + "    OutVerts[slot + 4] = Vertex(x1, z0, wxh, PackColor(wxc, wxa));\n"
        + "    OutVerts[slot + 5] = Vertex(x1, z1, wxh, PackColor(wxc, wxa));\n"
        + "    OutVerts[slot + 6] = Vertex(x0, z1, wzh, PackColor(wzc, wza));\n"
        + "    OutVerts[slot + 7] = Vertex(x1, z1, wzh, PackColor(wzc, wza));\n"
        + "}\n";

    private final Slot[] slots = new Slot[GEOMETRY_SLOTS];
    private final Map<Integer, Long> pipelines = new HashMap<>();

    // ------------------------------------------------------------------
    // 3D 体素 LOD
    // ------------------------------------------------------------------

    /**
     * 体素 LOD 覆盖的半径（方块）。这一圈内不再用高度场，而是画真实方块面，
     * 于是房屋有屋顶和墙、树有树干树冠、悬崖是真正的竖直岩壁。
     *
     * <p>再往外仍然交给高度场：那个距离上一个体素只占几个像素，画真方块纯粹浪费。
     */
    private static final int VOXEL_RADIUS_BLOCKS = 448;
    /**
     * 保持 1 方块精度的半径。再往外按 2 方块降采样，面数掉到四分之一左右。
     * 房屋、围墙这类几格宽的结构只有在这一圈里才认得出来。
     */
    private static final int VOXEL_FULL_DETAIL_RADIUS = 288;
    /** 体素顶点缓冲的硬上限（约 72 MB），按「由近到远」填，放不下的远处区块直接跳过。 */
    private static final int VOXEL_MAX_VERTICES = 4_500_000;
    /** 有新区块建好时，最多每隔这么多帧重拼一次顶点缓冲（相机跨区块时不受此限）。 */
    private static final int VOXEL_REBUILD_INTERVAL = 16;
    /**
     * 体素环中心吸附的区块数（4 区块 = 64 方块）。相机在同一格内走动时环集合和
     * 高度场剔除矩形都不变，避免每跨 16 方块就重新采样整张远景。
     */
    private static final int VOXEL_CENTER_SNAP_CHUNKS = 4;
    /**
     * 体素实际覆盖半径的量化步长（方块）。覆盖会随区块补齐一圈圈长大，直接把
     * 它喂给坐标轴会让整张远景反复重采样；量化后只有跨过边界才换一次坐标轴。
     */
    private static final int VOXEL_DETAIL_QUANTUM = 128;

    /** 体素顶点缓冲（CPU 映射写入）与共享静态索引。 */
    private Dx12GpuBuffer voxelVertexBuffer;
    private GpuBufferSlice.MappedView voxelVertexView;
    private ByteBuffer voxelVertexData;
    private int voxelVertexCapacity;
    private Dx12GpuBuffer voxelIndexBuffer;
    private GpuBufferSlice.MappedView voxelIndexView;
    private int voxelIndexCapacity;
    private int voxelFaceCount;
    /** 上次重拼时的完成计数与半径：任一变化才需要重拼。 */
    private int voxelBuiltCompleted = -1;
    private int voxelBuiltRadius = -1;
    private int voxelBuiltChunkX = Integer.MIN_VALUE;
    private int voxelBuiltChunkZ = Integer.MIN_VALUE;
    private int voxelFrames;
    /** 体素连续铺满的半径（方块）；0 表示没有可用的体素几何。高度场从它外缘接上。 */
    private float voxelCoverRadius;
    /**
     * 当前正在显示的连续完整环数。新区块还没建好时覆盖会暂时变小，这时保留旧几何，
     * 只等后台补齐后再换 —— 否则远景会随着每一圈的补齐/失效反复重建、疯狂闪烁。
     */
    private int voxelDrawnOuterRing;
    /** 当前这份体素几何是按多大的目标半径拼出来的；配置缩小到比它小时要立刻放弃旧几何。 */
    private int voxelDrawnRadius;
    /** 上次拼几何时的缓存代次；换世界/维度/锚点后必须整份重拼。 */
    private int voxelBuiltGeneration;
    /** 当前正在显示的体素几何中心（方块坐标，64 对齐）。高度场中心也跟着它。 */
    private int voxelDrawnCenterX;
    private int voxelDrawnCenterZ;
    /**
     * 体素几何实际覆盖的世界方块矩形（右/下边界为开区间）。区块按 16 对齐、且覆盖
     * 是否完整按环判定，所以这个矩形通常不以相机为中心；高度场按这个矩形精确跳过，
     * 而不是用一个偏保守的半径在某些方向多叠十几格。{@code min > max} 表示没有几何。
     */
    private int voxelMinBlockX = 1;
    private int voxelMaxBlockX = 0;
    private int voxelMinBlockZ = 1;
    private int voxelMaxBlockZ = 0;
    /** 重拼时的区块网格暂存，避免每帧分配。 */
    private LodVoxelMesher.Mesh[] voxelMeshScratch = new LodVoxelMesher.Mesh[0];

    private int rebuildIndex;
    private Slot geometry;
    /**
     * 共享的静态索引模式：第 k 格用 {@code 8k+{...}} 拼顶面和两面墙。
     * 所有 quad 槽位的顶点布局都是「每格 8 个顶点（顶面 + 两面墙）」，
     * 因此一份索引足够所有槽位复用。
     */
    private Dx12GpuBuffer quadIndexBuffer;
    private GpuBufferSlice.MappedView quadIndexView;
    private int quadIndexCells;
    private int framesSinceBuild;

    /** 上次建网时的网格参数，任一变化都要重建。 */
    private int builtBase = -1;
    private int builtRadius = -1;
    /** 上次建网时的原版视距半径：坐标轴的细格段和它绑定，视距一改就得重建。 */
    private float builtVanillaInner = -1.0f;
    /** 上次建网时坐标轴细格段的起点（体素外缘或原版视距圈）。 */
    private float builtFineStart = -1.0f;
    private int builtCellX = Integer.MIN_VALUE;
    private int builtCellZ = Integer.MIN_VALUE;
    private int builtCachedChunks = -1;
    /** 上次建网时体素覆盖矩形（相对网格中心），变化也要重建高度场剔除区。 */
    private int builtVoxelMinX = 1;
    private int builtVoxelMaxX = 0;
    private int builtVoxelMinZ = 1;
    private int builtVoxelMaxZ = 0;

    /** 建网时复用的采样缓冲，避免每秒分配上万个 int。 */
    private int[] heightScratch = new int[0];
    private int[] colorScratch = new int[0];
    private int[] spikeScratch = new int[0];
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
        // quality 只决定近处格宽（网格密度），远处按坐标轴分段放宽
        int quality = Mth.clamp(config.getLodQuality(), 0, 2);
        int base = quality == 0 ? 16 : quality == 1 ? 8 : 4;
        int radiusBlocks = Mth.clamp(config.getLodDistance() * 16, 256, MAX_RADIUS_BLOCKS);

        int cameraBlockX = Mth.floor(camera.pos.x);
        int cameraBlockZ = Mth.floor(camera.pos.z);
        // 相机所在的 64 方块格。高度场中心最终不一定用它：体素几何还停在旧中心
        // 时（覆盖只增不减），高度场也跟着停在旧中心，避免相机每跨 64 方块就
        // 白白重采样一次整张远景。
        int snap = Math.max(base, 64);
        int snapCenterX = Math.floorDiv(cameraBlockX, snap) * snap;
        int snapCenterZ = Math.floorDiv(cameraBlockZ, snap) * snap;

        int renderDistance = Math.max(2, minecraft.options.renderDistance().get());
        float vanillaInner = (renderDistance - 1) * 16.0f;
        // 体素 LOD 占住原版视距外最近的一圈，高度场从它外缘开始。
        int voxelRadius = Math.min(VOXEL_RADIUS_BLOCKS, radiusBlocks);
        if (voxelRadius <= vanillaInner) {
            // LOD 整个落在原版视距圈里，体素没有存在的意义
            voxelRadius = 0;
        }
        updateVoxelMesh(cameraBlockX, cameraBlockZ, voxelRadius, vanillaInner);
        // 高度场中心：有体素几何时跟着体素几何的中心走。体素覆盖只增不减，
        // 所以相机在覆盖矩形内走动时中心不动，远景不会因为跨 64 方块而重采样；
        // 等体素几何真正换中心（或关闭）时再一起换。
        // 但视距很短时不能让中心落后太远，否则前方的远景会被截掉，所以加一个
        // 与视距成比例的滞回上限。
        int centerX;
        int centerZ;
        int maxLag = Math.max(64, radiusBlocks / 8);
        if (this.voxelFaceCount > 0
            && Math.abs(cameraBlockX - this.voxelDrawnCenterX) <= maxLag
            && Math.abs(cameraBlockZ - this.voxelDrawnCenterZ) <= maxLag) {
            centerX = this.voxelDrawnCenterX;
            centerZ = this.voxelDrawnCenterZ;
        } else {
            centerX = snapCenterX;
            centerZ = snapCenterZ;
        }
        // 坐标轴细格段的起点。体素顶点预算是硬上限，实际只能铺到某个半径（常见
        // 200~300 方块），所以细格段要跟着「实际覆盖」走，接缝处才是 1 方块细格。
        // 但覆盖会随区块补齐一圈圈长大，直接跟着变会让整张远景反复重采样。这里把
        // 覆盖量化到 128 方块：只有跨过量化边界才换坐标轴，细格段再往前放一个量化
        // 步长，保证接缝永远落在细格段内部。
        int coverage = (int) this.voxelCoverRadius;
        int fineStart;
        if (coverage > 0) {
            int quantum = VOXEL_DETAIL_QUANTUM;
            int quantized = (coverage + quantum - 1) / quantum * quantum;
            fineStart = Math.max(0, quantized - quantum);
        } else {
            // 体素还没铺开（或多人游戏）：细格段从原版视距圈外开始
            fineStart = Math.max(0, (int) vanillaInner - 16);
        }
        // 高度场要跳过两块区域：原版视距圈（方形）和体素实际覆盖的区块矩形。
        // 矩形可能不以相机为中心，所以不能只用一个半径 —— 那只能取并集，某些方向
        // 会多叠十几格粗地形，把刚画好的体素面盖住。
        int voxelMinX = this.voxelMaxBlockX > this.voxelMinBlockX
            ? this.voxelMinBlockX - centerX : 1;
        int voxelMaxX = this.voxelMaxBlockX > this.voxelMinBlockX
            ? this.voxelMaxBlockX - centerX : 0;
        int voxelMinZ = this.voxelMaxBlockZ > this.voxelMinBlockZ
            ? this.voxelMinBlockZ - centerZ : 1;
        int voxelMaxZ = this.voxelMaxBlockZ > this.voxelMinBlockZ
            ? this.voxelMaxBlockZ - centerZ : 0;

        this.framesSinceBuild++;
        // 后台陆续读到新区块时才需要定时重建；站着不动且没有新数据就不重建，
        // 免得每 40 帧白采样一次几万个角点造成卡顿
        int cachedChunks = LodRegionCache.getInstance().cachedCount();
        boolean rebuild = this.geometry == null
            || base != this.builtBase
            || radiusBlocks != this.builtRadius
            || vanillaInner != this.builtVanillaInner
            || fineStart != this.builtFineStart
            || centerX != this.builtCellX
            || centerZ != this.builtCellZ
            || voxelMinX != this.builtVoxelMinX
            || voxelMaxX != this.builtVoxelMaxX
            || voxelMinZ != this.builtVoxelMinZ
            || voxelMaxZ != this.builtVoxelMaxZ
            || (this.framesSinceBuild > REBUILD_INTERVAL && cachedChunks != this.builtCachedChunks);
        if (rebuild) {
            rebuildGeometry(ctx, level, centerX, centerZ, radiusBlocks, base,
                fineStart, vanillaInner, voxelMinX, voxelMaxX, voxelMinZ, voxelMaxZ,
                cameraBlockX, cameraBlockZ);
            this.framesSinceBuild = 0;
            this.builtBase = base;
            this.builtRadius = radiusBlocks;
            this.builtVanillaInner = vanillaInner;
            this.builtFineStart = fineStart;
            this.builtCellX = centerX;
            this.builtCellZ = centerZ;
            this.builtVoxelMinX = voxelMinX;
            this.builtVoxelMaxX = voxelMaxX;
            this.builtVoxelMinZ = voxelMinZ;
            this.builtVoxelMaxZ = voxelMaxZ;
            this.builtCachedChunks = cachedChunks;
        }

        Slot slot = this.geometry;
        boolean hasHeightfield = slot != null
            && (slot.quadMode ? this.quadIndexBuffer != null : slot.indexCount > 0);

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
        // 接缝只跟原版视距绑定：体素覆盖会一圈圈长大，不能让它影响深度映射。
        float seamDistance = Math.max(vanillaInner, zNear * 4.0f);
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

        // 雾：从原版视距外开始，外缘完全融进天空色，远景因此没有一条硬切边。
        // 体素覆盖只是决定「哪里开始有细节」，雾的起点仍跟着稳定量走，避免忽浓忽淡。
        float fogStart = Math.max(vanillaInner, radiusBlocks * 0.55f);
        float fogEnd = Math.max(fogStart + 1.0f, radiusBlocks * 0.95f);
        int uniformOffset = writeUniforms(viewProj, camera, renderState, levelState, fogStart, fogEnd,
            centerX, centerZ, level.getSeaLevel());

        Dx12Native.dx12BeginRenderPass(ctx, new long[]{color.handle()}, new int[]{0},
            new byte[]{0}, null, depth.handle(), 0, (byte) 0, 0.0, 0, 0, width, height);
        try {
            Dx12Native.dx12SetPipeline(ctx, pipeline, true);
            // 体素 LOD 先画：顶点坐标是绝对世界坐标，所以参考中心传 0，和高度场
            // （相对网格中心）用的 uniform 不是同一个槽位。
            if (this.voxelFaceCount > 0 && this.voxelVertexBuffer != null
                && this.voxelIndexBuffer != null) {
                LodVoxelCache voxelCache = LodVoxelCache.getInstance();
                int voxelUniform = writeUniforms(viewProj, camera, renderState, levelState,
                    fogStart, fogEnd, voxelCache.anchorX(), voxelCache.anchorZ(), level.getSeaLevel());
                Dx12Native.dx12SetVertexBuffer(ctx, 0, this.voxelVertexBuffer.handle(), 0L, VERTEX_STRIDE);
                Dx12Native.dx12SetIndexBuffer(ctx, this.voxelIndexBuffer.handle(), 1);
                pushUniform(ctx, voxelUniform);
                Dx12Native.dx12DrawIndexed(ctx, this.voxelFaceCount * 6, 1, 0, 0, 0);
            }
            if (hasHeightfield) {
                Dx12Native.dx12SetVertexBuffer(ctx, 0, slot.vertexBuffer.handle(), 0L, VERTEX_STRIDE);
                pushUniform(ctx, uniformOffset);
                if (slot.quadMode) {
                    // 索引数由 compute 写进参数缓冲：主线程不回读，GPU 说画多少就画多少。
                    Dx12Native.dx12SetIndexBuffer(ctx, this.quadIndexBuffer.handle(), 1);
                    Dx12Native.dx12DrawIndexedIndirect(ctx, slot.argsBuffer.handle(), 0L, 1);
                } else {
                    Dx12Native.dx12SetIndexBuffer(ctx, slot.indexBuffer.handle(), 1);
                    Dx12Native.dx12DrawIndexed(ctx, slot.indexCount, 1, 0, 0, 0);
                }
            }
        } finally {
            Dx12Native.dx12EndRenderPass(ctx);
        }
    }

    // ------------------------------------------------------------------
    // 几何
    // ------------------------------------------------------------------

    /**
     * 重拼体素 LOD 的顶点缓冲。
     *
     * <p>区块网格在后台线程上建好并缓存，这里只做「按距离由近到远拼接」。还没建好的
     * 区块先跳过，等后台补齐后再拼一次，所以地形是一圈一圈长出来的，不会卡住一帧。
     *
     * <p>顶点是「世界坐标 − 锚点」，缓存不随玩家移动失效；但可见区块集合会变，所以
     * 相机跨过一个区块就立即重拼一次。后台陆续建好新区块时则按
     * {@link #VOXEL_REBUILD_INTERVAL} 帧节流，避免每帧复制几十 MB。
     */
    private void updateVoxelMesh(int cameraBlockX, int cameraBlockZ, int voxelRadius, float vanillaInner) {
        this.voxelFrames++;
        LodVoxelCache cache = LodVoxelCache.getInstance();
        cache.ensureAnchor(cameraBlockX, cameraBlockZ);
        // 换世界/维度、或锚点漂移超过阈值时，缓存代次会变：旧顶点是相对旧锚点/
        // 旧存档的，必须整份丢掉，否则会整片错位。
        int generation = cache.generation();
        if (generation != this.voxelBuiltGeneration) {
            clearVoxelCover();
            this.voxelBuiltGeneration = generation;
            this.voxelBuiltCompleted = -1;
            this.voxelBuiltRadius = -1;
            this.voxelBuiltChunkX = Integer.MIN_VALUE;
            this.voxelBuiltChunkZ = Integer.MIN_VALUE;
        }
        int completed = cache.completedCount();
        // 环中心吸附到 64 方块网格：相机在同一格内走动时几何不动，高度场的剔除
        // 矩形也保持不变。rect 和高度场中心都用 64 对齐，接缝因此始终落在网格线上。
        int camChunkX = Math.floorDiv(cameraBlockX >> 4, VOXEL_CENTER_SNAP_CHUNKS)
            * VOXEL_CENTER_SNAP_CHUNKS;
        int camChunkZ = Math.floorDiv(cameraBlockZ >> 4, VOXEL_CENTER_SNAP_CHUNKS)
            * VOXEL_CENTER_SNAP_CHUNKS;
        boolean moved = camChunkX != this.voxelBuiltChunkX || camChunkZ != this.voxelBuiltChunkZ;
        boolean stale = completed != this.voxelBuiltCompleted || voxelRadius != this.voxelBuiltRadius;
        if (!moved && !stale) {
            return;
        }
        // 相机跨区块必须立刻重拼，否则前方会露出一条 16 方块宽的天空缝；
        // 只是后台又建好几块的话，攒够一批再拼，省掉几十 MB 的重复拷贝。
        if (!moved && this.voxelBuiltCompleted >= 0
            && this.voxelFrames < VOXEL_REBUILD_INTERVAL) {
            return;
        }
        this.voxelFrames = 0;
        this.voxelBuiltCompleted = completed;
        this.voxelBuiltRadius = voxelRadius;
        this.voxelBuiltChunkX = camChunkX;
        this.voxelBuiltChunkZ = camChunkZ;
        if (moved) {
            // 走动后新生成的区块可能刚写进 region 文件，给之前读不到的区块一次机会
            cache.retryMissing();
        }

        if (voxelRadius <= 0) {
            clearVoxelCover();
            return;
        }
        // 多人游戏没有 region 文件，体素路径直接关掉，别把几千个区块都塞进 missing
        if (!LodRegionCache.getInstance().isAvailable()) {
            clearVoxelCover();
            return;
        }
        int chunkRadius = Math.max(1, voxelRadius >> 4);
        int maxFaces = VOXEL_MAX_VERTICES / 4;
        LodVoxelMesher.Mesh[] selected = this.voxelMeshScratch;
        int count = 0;
        int totalFaces = 0;
        // 已经「完整铺满」的环数。只把连续的完整环画出来：某一环里还有区块没建好，
        // 就从这一环起停画，高度场从上一环的外缘接上 —— 否则会露出半圈天空。
        int outerRing = 0;
        boolean drawing = true;
        // 按 Chebyshev 距离一圈一圈铺：近处的区块先占顶点预算，超限时丢的是远处。
        // 只铺到 chunkRadius - 1，保证体素覆盖半径约为 voxelRadius。
        for (int d = 0; d < chunkRadius; d++) {
            int ringCount = count;
            int ringFaces = totalFaces;
            boolean ringComplete = true;
            boolean budgetHit = false;
            for (int dz = -d; dz <= d; dz++) {
                for (int dx = -d; dx <= d; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != d) {
                        continue;
                    }
                    // 整块都落在原版视距圈内：原版已经画过这块地形，重复画只是浪费
                    if (d * 16 + 16 <= vanillaInner) {
                        continue;
                    }
                    int shift = d * 16 <= VOXEL_FULL_DETAIL_RADIUS ? 0 : 1;
                    // 即使已经停止绘制也要继续 get()：否则外圈永远不会排队建网，
                    // 体素就再也长不回去了。
                    LodVoxelMesher.Mesh mesh = cache.get(camChunkX + dx, camChunkZ + dz, shift);
                    if (mesh == null || mesh == LodVoxelMesher.Mesh.UNAVAILABLE) {
                        ringComplete = false;
                        continue;
                    }
                    if (!drawing || mesh.faces() == 0) {
                        continue;
                    }
                    if (totalFaces + mesh.faces() > maxFaces) {
                        budgetHit = true;
                        continue;
                    }
                    if (count == selected.length) {
                        int grown = Math.max(64, Math.min(selected.length * 2, 4096));
                        selected = Arrays.copyOf(selected, grown);
                        this.voxelMeshScratch = selected;
                    }
                    selected[count++] = mesh;
                    totalFaces += mesh.faces();
                }
            }
            if (drawing) {
                if (!ringComplete || budgetHit) {
                    // 本环不完整：整环回滚，只保留前面完整的环
                    count = ringCount;
                    totalFaces = ringFaces;
                    drawing = false;
                } else {
                    outerRing = d + 1;
                }
            }
        }
        // 覆盖只增不减：相机跨区块时，新进来的那一环往往还没建好，如果这时把
        // outerRing 缩回 0，远景就会在「有体素 / 没体素」之间反复切换。只要上一份
        // 几何还盖着相机，就先保留它，等后台补齐后再一次性换过去。
        boolean haveCover = this.voxelFaceCount > 0 && this.voxelCoverRadius > 0.0f;
        if (haveCover && voxelRadius >= this.voxelDrawnRadius
            && outerRing < this.voxelDrawnOuterRing
            && this.voxelRectContains(cameraBlockX, cameraBlockZ)) {
            return;
        }
        this.voxelDrawnOuterRing = outerRing;
        this.voxelDrawnRadius = voxelRadius;
        this.voxelCoverRadius = outerRing > 0 ? outerRing * 16.0f : 0.0f;
        if (totalFaces == 0 || outerRing == 0) {
            // 没有实际几何就退回高度场：否则高度场会以为这一圈已被体素盖住而空出大洞
            clearVoxelCover();
            return;
        }
        try {
            ensureVoxelBuffers(totalFaces * 4, totalFaces * 6);
        } catch (Throwable t) {
            LOGGER.error("[lod] 体素顶点缓冲创建失败，本帧跳过体素 LOD", t);
            clearVoxelCover();
            return;
        }
        if (this.voxelVertexData == null) {
            clearVoxelCover();
            return;
        }
        int vertex = 0;
        for (int k = 0; k < count; k++) {
            LodVoxelMesher.Mesh mesh = selected[k];
            this.voxelVertexData.position(vertex * VERTEX_STRIDE);
            this.voxelVertexData.put(mesh.vertices(), 0, mesh.vertices().length);
            vertex += mesh.faces() * 4;
        }
        this.voxelFaceCount = totalFaces;
        this.voxelDrawnCenterX = camChunkX * 16;
        this.voxelDrawnCenterZ = camChunkZ * 16;
        // 只把「连续完整环」算进覆盖矩形：高度场会精确跳过这块矩形，多算一格就会
        // 露出没有几何的天空，少算一格只是多叠一格粗地形。
        int minChunkX = camChunkX - (outerRing - 1);
        int minChunkZ = camChunkZ - (outerRing - 1);
        this.voxelMinBlockX = minChunkX * 16;
        this.voxelMaxBlockX = (camChunkX + outerRing) * 16;
        this.voxelMinBlockZ = minChunkZ * 16;
        this.voxelMaxBlockZ = (camChunkZ + outerRing) * 16;
    }

    /** 相机是否还落在上一份体素几何覆盖的区块矩形内。 */
    private boolean voxelRectContains(int blockX, int blockZ) {
        return this.voxelMaxBlockX > this.voxelMinBlockX
            && this.voxelMaxBlockZ > this.voxelMinBlockZ
            && blockX >= this.voxelMinBlockX && blockX < this.voxelMaxBlockX
            && blockZ >= this.voxelMinBlockZ && blockZ < this.voxelMaxBlockZ;
    }

    /** 关掉体素几何，并让高度场重新覆盖整圈（否则会留下一个没有几何的空洞）。 */
    private void clearVoxelCover() {
        this.voxelFaceCount = 0;
        this.voxelCoverRadius = 0.0f;
        this.voxelDrawnOuterRing = 0;
        this.voxelDrawnRadius = 0;
        this.voxelDrawnCenterX = 0;
        this.voxelDrawnCenterZ = 0;
        this.voxelMinBlockX = 1;
        this.voxelMaxBlockX = 0;
        this.voxelMinBlockZ = 1;
        this.voxelMaxBlockZ = 0;
    }

    /** 体素顶点缓冲（CPU 映射）与 4 顶点/面的共享静态索引。 */
    private void ensureVoxelBuffers(int vertexCapacity, int indexCapacity) {
        if (this.voxelVertexBuffer == null || this.voxelVertexBuffer.isClosed()
            || this.voxelVertexCapacity < vertexCapacity) {
            int capacity = Math.min(VOXEL_MAX_VERTICES,
                Math.max(vertexCapacity, Math.max(this.voxelVertexCapacity * 2, 65536)));
            closeVoxelVertex();
            long bytes = (long) capacity * VERTEX_STRIDE;
            Dx12GpuBuffer buffer = new Dx12GpuBuffer(
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_VERTEX, bytes);
            GpuBufferSlice.MappedView view = buffer.map(0L, bytes, false, true);
            this.voxelVertexBuffer = buffer;
            this.voxelVertexView = view;
            this.voxelVertexData = view.data();
            this.voxelVertexData.order(ByteOrder.LITTLE_ENDIAN);
            this.voxelVertexCapacity = capacity;
        }
        if (this.voxelIndexBuffer == null || this.voxelIndexBuffer.isClosed()
            || this.voxelIndexCapacity < indexCapacity) {
            int capacity = Math.min(VOXEL_MAX_VERTICES / 4 * 6,
                Math.max(indexCapacity, Math.max(this.voxelIndexCapacity * 2, 65536 * 6)));
            closeVoxelIndex();
            long bytes = (long) capacity * 4L;
            Dx12GpuBuffer buffer = new Dx12GpuBuffer(
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDEX, bytes);
            GpuBufferSlice.MappedView view = buffer.map(0L, bytes, false, true);
            IntBuffer indices = view.data().asIntBuffer();
            int quads = capacity / 6;
            for (int k = 0; k < quads; k++) {
                int v = k * 4;
                int o = k * 6;
                indices.put(o, v);
                indices.put(o + 1, v + 1);
                indices.put(o + 2, v + 2);
                indices.put(o + 3, v);
                indices.put(o + 4, v + 2);
                indices.put(o + 5, v + 3);
            }
            this.voxelIndexBuffer = buffer;
            this.voxelIndexView = view;
            this.voxelIndexCapacity = capacity;
        }
    }

    private void closeVoxelVertex() {
        if (this.voxelVertexView != null) {
            try {
                this.voxelVertexView.close();
            } catch (Throwable ignored) {
            }
            this.voxelVertexView = null;
        }
        this.voxelVertexData = null;
        if (this.voxelVertexBuffer != null) {
            try {
                this.voxelVertexBuffer.close();
            } catch (Throwable ignored) {
            }
            this.voxelVertexBuffer = null;
        }
        this.voxelVertexCapacity = 0;
    }

    private void closeVoxelIndex() {
        if (this.voxelIndexView != null) {
            try {
                this.voxelIndexView.close();
            } catch (Throwable ignored) {
            }
            this.voxelIndexView = null;
        }
        if (this.voxelIndexBuffer != null) {
            try {
                this.voxelIndexBuffer.close();
            } catch (Throwable ignored) {
            }
            this.voxelIndexBuffer = null;
        }
        this.voxelIndexCapacity = 0;
    }

    /**
     * 建一张以相机为中心的高度场网格。
     *
     * <p>坐标轴从 0 向两侧生长：视距圈内用粗格宽（反正会被剔除），圈外先细后按固定
     * 张角等比放大（见 {@link #buildAxis}）。整张网格共享顶点，不存在拼接缝。
     */
    private void rebuildGeometry(long ctx, ClientLevel level, int centerX, int centerZ,
                                 int radiusBlocks, int base, float fineStart,
                                 float vanillaInner, int voxelMinX, int voxelMaxX,
                                 int voxelMinZ, int voxelMaxZ,
                                 int cameraBlockX, int cameraBlockZ) {
        long startNanos = System.nanoTime();
        int[] axis = buildAxis(radiusBlocks, base, fineStart);
        int n = axis.length;
        int vertexCount = n * n;
        int cellCount = (n - 1) * (n - 1);
        // GPU 建网开关：一旦失败就永久回退 CPU（每帧重试只会刷日志）
        boolean gpu = !this.computeFailed && ensureCompute(n, axis);
        // quad 模式每格 8 个顶点（顶面 + 两条墙）+ 共享静态索引，容量按硬上限截断
        // （compute 会丢弃放不下的格子）
        int quadVerts = Math.min(cellCount * 8, MAX_QUAD_VERTS);

        Slot slot = this.slots[this.rebuildIndex % GEOMETRY_SLOTS];
        if (slot == null) {
            slot = new Slot();
            this.slots[this.rebuildIndex % GEOMETRY_SLOTS] = slot;
        }
        this.rebuildIndex++;
        slot.ensure(gpu ? quadVerts : vertexCount, gpu ? 0 : cellCount * 6, gpu);
        if (gpu) {
            // 索引模式与地形无关，所有槽位共用一份；建不出来的话这一帧就退回 CPU
            ensureQuadIndex(quadVerts / 8);
            if (this.quadIndexBuffer == null) {
                gpu = false;
                slot.ensure(vertexCount, cellCount * 6, false);
            }
        }

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

        // 1a) 先削掉孤立的单点尖刺/凹坑，否则树干会在远景里变成一根根锥子
        removeIsolatedSpikes(heights, n, vertexCount);

        int seaLevel = level.getSeaLevel();
        int snowLine = seaLevel + SNOW_ABOVE_SEA;

        // 1b) 顶点：GPU 可用时把降噪 / 分层配色 / 方向光烘焙整段交给 compute，
        // 主线程只负责采样与索引；compute 失败则本帧立刻回退 CPU，之后永久走 CPU。
        boolean gpuOk = false;
        if (gpu) {
            String failure = dispatchVertexBuild(ctx, slot, heights, colors, vertexCount, n,
                seaLevel, snowLine, quadVerts, vanillaInner,
                cameraBlockX - centerX, cameraBlockZ - centerZ,
                voxelMinX, voxelMaxX, voxelMinZ, voxelMaxZ);
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

        // 2) 索引：quad 模式的索引是共享的静态模式（GPU 写顶点、GPU 给绘制参数），
        //    CPU 路径才连三角形，且整块落在原版视距内的跳过、四个角都有效才画。
        int indexCount = 0;
        int emitted = 0;
        if (!gpuOk) {
            ByteBuffer indices = slot.indexData;
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
                    // 与 compute 着色器保持一致：原版视距方形 + 体素覆盖矩形，
                    // 两块都用「最远角」判定，跨边界的格子照画，接缝最多重叠一格。
                    if (Math.max(dx, dz) < vanillaInner) {
                        // 原版地形已经画过
                        continue;
                    }
                    int rx0 = axis[i];
                    int rx1 = axis[i + 1];
                    int rz0 = axis[j];
                    int rz1 = axis[j + 1];
                    if (rx0 >= voxelMinX && rx1 <= voxelMaxX
                        && rz0 >= voxelMinZ && rz1 <= voxelMaxZ) {
                        // 体素 LOD 已经画过
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
        }

        slot.indexCount = indexCount;
        this.geometry = slot;

        long now = System.nanoTime();
        if (now - this.lastDiagnosticNanos > 5_000_000_000L) {
            this.lastDiagnosticNanos = now;
            // GPU 路径的可见 quad 数由 compute 阶段的 InterlockedAdd 决定，CPU 侧拿不到，
            // 这里打的是容量（quadVerts / 8），不要再显示成 0。
            int seamCell = 0;
            for (int k = 1; k < n; k++) {
                if (axis[k] >= fineStart) {
                    seamCell = axis[k] - axis[k - 1];
                    break;
                }
            }
            LOGGER.info("[lod] 网格={}x{} 接缝格宽={} 远端格宽={} {} 已采样={} 地形格={} 顶点容量={} 索引={} 缓存区块={} 体素面={} 体素半径={} 耗时={}ms",
                n - 1, n - 1, seamCell, axis[n - 1] - axis[n - 2], gpuOk ? "GPU-block" : "CPU", known,
                gpuOk ? quadVerts / 8 : emitted,
                gpuOk ? quadVerts : vertexCount, gpuOk ? quadVerts / 8 * 18 : indexCount,
                LodRegionCache.getInstance().cachedCount(), this.voxelFaceCount, (int) this.voxelCoverRadius,
                (System.nanoTime() - startNanos) / 1_000_000L);
        }
    }

    /**
     * 削掉「孤立」的单点尖刺与凹坑。
     *
     * <p>高度取的是 {@code MOTION_BLOCKING_NO_LEAVES}：它跳过树叶，但树干本身挡光，
     * 所以树干那一列仍比周围地面高出四到七格。这一列一旦正好落在采样点上，远景里
     * 就是一根一格宽的锥子，再被侧面的泥土配色一刷，整片森林变成满地深棕色尖刺。
     *
     * <p>判据是「八个邻居全都比它低（或全都比它高）」：真实的地形台阶、山脊、悬崖
     * 至少在某一侧有同高的邻居，因此不会被误削；而树干是孤立的一根，四周必然是地面。
     */
    private void removeIsolatedSpikes(int[] heights, int n, int vertexCount) {
        int[] src = this.spikeScratch;
        if (src.length < vertexCount) {
            src = new int[vertexCount];
            this.spikeScratch = src;
        }
        System.arraycopy(heights, 0, src, 0, vertexCount);
        for (int j = 1; j < n - 1; j++) {
            for (int i = 1; i < n - 1; i++) {
                int index = j * n + i;
                int height = src[index];
                if (height == UNKNOWN_HEIGHT) {
                    continue;
                }
                int maxNeighbor = Integer.MIN_VALUE;
                int minNeighbor = Integer.MAX_VALUE;
                boolean allKnown = true;
                for (int dj = -1; dj <= 1 && allKnown; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        if (di == 0 && dj == 0) {
                            continue;
                        }
                        int nh = src[index + dj * n + di];
                        if (nh == UNKNOWN_HEIGHT) {
                            allKnown = false;
                            break;
                        }
                        if (nh > maxNeighbor) {
                            maxNeighbor = nh;
                        }
                        if (nh < minNeighbor) {
                            minNeighbor = nh;
                        }
                    }
                }
                if (!allKnown) {
                    continue;
                }
                if (height > maxNeighbor + PEAK_TOLERANCE) {
                    heights[index] = maxNeighbor;
                } else if (height < minNeighbor - PEAK_TOLERANCE) {
                    heights[index] = minNeighbor;
                }
            }
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
     *
     * <p><b>注意：CPU 兜底仍然是一张平滑高度场</b>（每个角点一个共享顶点，不做压平顶面
     * 和竖直墙）。GPU compute 建网失败时才会走到这里，优先保证「能画出来、不崩」；
     * Voxy 风格的方块面（压平顶面 + 竖直墙）只在 {@link #COMPUTE_SHADER} 路径里实现。
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
                gradient(axis, heights, n, i, j, gradient);
                float dhx = gradient[0];
                float dhz = gradient[1];
                float slope = (float) Math.sqrt(dhx * dhx + dhz * dhz);
                int alpha;
                if (LodRegionCache.isWater(rgb)) {
                    // 水体：换成偏暗的青蓝，并把 alpha 打成 0 交给像素着色器加水波
                    rgb = WATER_DEEP_COLOR;
                    alpha = 0;
                } else {
                    if (rgb < 0) {
                        rgb = FALLBACK_COLOR;
                    }
                    // 陡坡露岩：坡度越大越接近石头色，悬崖不再是「被拉长的草地」。
                    // 用平滑过渡而不是线性斜坡，相邻格不会一灰一绿地跳变（那就是竖条纹）。
                    float rock = Mth.clamp((slope - ROCK_SLOPE) / ROCK_SPAN, 0.0f, 1.0f);
                    rgb = mixColor(rgb, ROCK_COLOR,
                        rock * rock * (3.0f - 2.0f * rock) * ROCK_MAX);
                    // 海平面附近的低平地带带一点沙色，海岸线因此看得出来
                    if (slope < 0.45f && height <= seaLevel + 4 && height >= seaLevel - 4) {
                        rgb = mixColor(rgb, SAND_COLOR, 0.35f);
                    }
                    // 高处积雪
                    rgb = mixColor(rgb, SNOW_COLOR,
                        Math.min(0.9f, Math.max(0.0f, (height - snowLine) / 40.0f)));
                    alpha = 0xFF;
                }
                // 顶点坐标是相对网格中心的局部坐标（见 writeUniforms 里的 CameraPos），
                // 这样大世界坐标不会因为 float 精度产生抖动。方向光改由像素着色器按
                // 逐面法线计算（见 PIXEL_SHADER），这里只保留地表本色。
                putVertex(vertices, index, axis[i], height, axis[j], rgb, alpha);
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
                long handle;
                try {
                    handle = Dx12Native.dx12CreateComputePipeline(
                        COMPUTE_SHADER.getBytes(StandardCharsets.UTF_8), 2, 3, COMPUTE_CONST_COUNT);
                } catch (Throwable t) {
                    LOGGER.error("[lod] compute 管线创建失败，回退 CPU 建网：{}", t.getMessage());
                    this.computeFailed = true;
                    return false;
                }
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
     * 保证共享静态索引模式够 {@code cells} 格用：第 k 格写
     * {@code 8k+{0,1,2, 0,2,3, 1,4,5, 1,5,2, 3,2,7, 3,7,6}}（顶面 + 两面墙）。
     *
     * <p>整份模式与具体地形无关，所以只分配一次、所有槽位共用；容量不够时才重建。
     */
    private void ensureQuadIndex(int cells) {
        if (cells <= this.quadIndexCells && this.quadIndexBuffer != null
            && !this.quadIndexBuffer.isClosed()) {
            return;
        }
        int capacity = Math.max(cells, Math.max(this.quadIndexCells * 2, 1024));
        long bytes = (long) capacity * 18L * 4L;
        try {
            if (this.quadIndexView != null) {
                try {
                    this.quadIndexView.close();
                } catch (Throwable ignored) {
                }
                this.quadIndexView = null;
            }
            if (this.quadIndexBuffer != null) {
                try {
                    this.quadIndexBuffer.close();
                } catch (Throwable ignored) {
                }
                this.quadIndexBuffer = null;
            }
            this.quadIndexCells = 0;
            Dx12GpuBuffer buffer = new Dx12GpuBuffer(
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDEX, bytes);
            GpuBufferSlice.MappedView view = buffer.map(0L, bytes, false, true);
            IntBuffer indices = view.data().asIntBuffer();
            for (int k = 0; k < capacity; k++) {
                int v = k * 8;
                int o = k * 18;
                // 顶面：0,1,2 / 0,2,3
                indices.put(o, v);
                indices.put(o + 1, v + 1);
                indices.put(o + 2, v + 2);
                indices.put(o + 3, v);
                indices.put(o + 4, v + 2);
                indices.put(o + 5, v + 3);
                // +X 墙：1,4,5 / 1,5,2
                indices.put(o + 6, v + 1);
                indices.put(o + 7, v + 4);
                indices.put(o + 8, v + 5);
                indices.put(o + 9, v + 1);
                indices.put(o + 10, v + 5);
                indices.put(o + 11, v + 2);
                // +Z 墙：3,2,7 / 3,7,6
                indices.put(o + 12, v + 3);
                indices.put(o + 13, v + 2);
                indices.put(o + 14, v + 7);
                indices.put(o + 15, v + 3);
                indices.put(o + 16, v + 7);
                indices.put(o + 17, v + 6);
            }
            this.quadIndexBuffer = buffer;
            this.quadIndexView = view;
            this.quadIndexCells = capacity;
        } catch (Throwable t) {
            LOGGER.error("[lod] 共享静态索引缓冲创建失败，回退 CPU 建网", t);
            this.computeFailed = true;
        }
    }

    /**
     * 把采样好的角点交给 compute 展开成顶点。
     *
     * <p>分三个 pass：先把计数器清零（缓冲首帧内容未定义）、再让每个格子抢 8 个槽位
     * 写一个压平顶面 + 两条竖直墙、最后把总顶点数翻译成 {@code ExecuteIndirect} 的参数。
     * 主线程全程不回读。
     *
     * @return {@code null} 表示成功，否则为失败原因（调用方据此永久回退 CPU）
     */
    private String dispatchVertexBuild(long ctx, Slot slot, int[] heights, int[] colors,
                                       int vertexCount, int n, int seaLevel, int snowLine,
                                       int maxVerts, float vanillaInner, int camRelX, int camRelZ,
                                       int voxelMinX, int voxelMaxX, int voxelMinZ, int voxelMaxZ) {
        ByteBuffer corners = this.cornerData;
        for (int k = 0; k < vertexCount; k++) {
            int offset = k * CORNER_STRIDE;
            corners.putInt(offset, heights[k]);
            corners.putInt(offset + 4, colors[k]);
        }
        int span = n - 1;
        int cellCount = span * span;
        long[] uavs = {
            slot.vertexBuffer.handle(),
            slot.counterBuffer.handle(),
            slot.argsBuffer.handle()
        };
        int[] uavOffsets = {0, 0, 0};
        // Args 是 DRAW_INDEXED 参数：indexCount / instanceCount / startIndex / baseVertex / startInstance
        int[] uavCounts = {maxVerts, 1, 5};
        int[] uavStrides = {VERTEX_STRIDE, 4, 4};
        int innerBits = Float.floatToIntBits(vanillaInner);

        // Pass 2 = 清计数（1 组）→ Pass 0 = 展开 quad（每格一线程）→ Pass 1 = 写 args（1 组）
        int[][] passes = {
            {2, 1},
            {0, (cellCount + COMPUTE_GROUP_SIZE - 1) / COMPUTE_GROUP_SIZE},
            {1, 1}
        };
        for (int[] pass : passes) {
            String failure = Dx12Native.dx12DispatchCompute(ctx, this.computePipeline,
                new int[]{n, maxVerts, seaLevel, snowLine, pass[0], innerBits, camRelX, camRelZ,
                    voxelMinX, voxelMaxX, voxelMinZ, voxelMaxZ},
                new long[]{this.cornerBuffer.handle(), this.axisBuffer.handle()},
                new int[]{0, 0},
                new int[]{vertexCount, n},
                new int[]{CORNER_STRIDE, 4},
                uavs, uavOffsets, uavCounts, uavStrides,
                pass[1], 1, 1);
            if (failure != null) {
                return failure;
            }
        }
        return null;
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
     * 生成以 0 为中心、对称的坐标轴。整张网格因此是一张连续的高度场，没有拼接缝。
     *
     * <p>分三段：
     * <ol>
     *   <li><b>原版视距圈以内</b>：这些格子会被建网阶段整片按距离剔除，永远画不出来，
     *       所以只用大格宽铺过去，不占环数预算。以前这一段也铺 1 方块，白吃掉近一半
     *       的坐标轴，远景只能退到 256 方块一格。</li>
     *   <li><b>视距圈外 {@link #NEAR_DETAIL_BAND} 方块</b>：保持最小格宽，保证 LOD
     *       起始处和原版地形贴合、接缝不露。</li>
     *   <li><b>再往外</b>：格宽按固定比率等比放大。等比级数的格宽与距离成正比，
     *       也就是<b>固定张角</b> —— 远处的台阶在屏幕上始终是差不多大小，不会出现
     *       4096 方块处一格 256 方块、张角 3.6° 的巨型平台。</li>
     * </ol>
     *
     * <p>等比段的增长率由「剩余环数」反解，总是把 {@link #AXIS_RINGS} 用满，
     * 所以无论视距调到多少，远景细节都是同样的屏幕密度。
     */
    private static int[] buildAxis(int maxRadius, int base, float fineStart) {
        int[] forward = new int[512];
        int count = 1;
        forward[0] = 0;
        int coord = 0;

        int nearWidth = Math.max(1, base / 4);
        // 第一段：细格段起点以内，粗格宽铺过去（这些格子大多被原版地形/体素盖住，
        // 只占坐标轴）。必须夹到 maxRadius，否则视距很小时会冲过网格边界。
        int fineStartCoord = Math.min(maxRadius, Math.max(0, (int) fineStart));
        int coarseWidth = Math.max(32, nearWidth * 8);
        while (coord < fineStartCoord) {
            coord = Math.min(coord + coarseWidth, fineStartCoord);
            if (count == forward.length) {
                forward = Arrays.copyOf(forward, count * 2);
            }
            forward[count++] = coord;
        }
        // 第二段：细格段保持最细格宽，正好覆盖体素外缘，接缝处两边格宽一致
        int fineEnd = Math.min(maxRadius, coord + NEAR_DETAIL_BAND);
        while (coord < fineEnd) {
            coord = Math.min(coord + nearWidth, fineEnd);
            if (count == forward.length) {
                forward = Arrays.copyOf(forward, count * 2);
            }
            forward[count++] = coord;
        }
        // 第三段：等比放大到最远半径，格宽与距离成正比（固定张角）
        int remaining = AXIS_RINGS - (count - 1);
        double growth = solveGrowth(maxRadius - coord, nearWidth, remaining);
        double widthAcc = nearWidth;
        while (coord < maxRadius) {
            widthAcc *= growth;
            int step = Math.max(1, (int) Math.round(widthAcc));
            coord = Math.min(coord + step, maxRadius);
            if (count == forward.length) {
                forward = Arrays.copyOf(forward, count * 2);
            }
            forward[count++] = coord;
        }

        int[] axis = new int[count * 2 - 1];
        for (int i = 0; i < count; i++) {
            axis[count - 1 - i] = -forward[i];
            axis[count - 1 + i] = forward[i];
        }
        return axis;
    }

    /**
     * 解出等比增长率 {@code r}，使从 {@code startWidth} 起连续乘 {@code rings} 次、
     * 每环推进当前格宽，累计正好走到 {@code span}：
     * {@code startWidth * (r^rings - 1) / (r - 1) = span}。
     *
     * <p>用二分求数值解（{@code r} 在 1.0~1.5 之间，60 次足够收敛到 1e-12）。
     * 环数不足以铺完时直接给一个足够大的比率兜底，保证循环一定终止。
     */
    private static double solveGrowth(double span, double startWidth, int rings) {
        if (span <= 0.0) {
            return 1.0;
        }
        if (rings <= 0 || startWidth <= 0.0) {
            return 1.5;
        }
        double lo = 1.0 + 1e-9;
        double hi = 1.5;
        for (int it = 0; it < 60; it++) {
            double mid = (lo + hi) * 0.5;
            double sum = mid <= 1.0 + 1e-9
                ? startWidth * rings
                : startWidth * (Math.pow(mid, rings) - 1.0) / (mid - 1.0);
            if (sum > span) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return (lo + hi) * 0.5;
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
            int height = surfaceHeight(chunk, x & 15, z & 15);
            if (height != UNKNOWN_HEIGHT) {
                // 树干挡光，会被 MOTION_BLOCKING_NO_LEAVES 算进地表，往下跳过原木
                height = stripLogs(chunk, x, height, z, level.getMinY());
                int rgb = LodRegionCache.getInstance().colorAt(x, z);
                if (rgb < 0) {
                    // 上色取最高层（树冠/雪/水），和磁盘侧 colorHeightmap 保持一致
                    int colorHeight = heightOf(chunk, Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                    rgb = blockColorAt(level, x, colorHeight == UNKNOWN_HEIGHT ? height : colorHeight, z);
                }
                return ((long) height << 32) | (rgb & 0xFFFFFFFFL);
            }
        }
        long sample = LodRegionCache.getInstance().sample(x, z);
        return sample == LodRegionCache.NO_SAMPLE ? UNKNOWN_SAMPLE : sample;
    }

    /**
     * 取一列的地表高度，<b>避开树叶与雪层</b>。
     *
     * <p>原来用 {@code WORLD_SURFACE}（最高非空气方块），树冠会把高度抬高十几格：森林在
     * 远景里变成一根根尖刺，颜色还是树叶的深绿，和周围草地拼在一起就是一片噪点。
     * {@code MOTION_BLOCKING_NO_LEAVES} 跳过树叶、仍把水面算作地表，正合适。
     */
    private static int surfaceHeight(LevelChunk chunk, int x, int z) {
        int height = heightOf(chunk, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (height == UNKNOWN_HEIGHT) {
            height = heightOf(chunk, Heightmap.Types.WORLD_SURFACE, x, z);
        }
        return height;
    }

    /**
     * 树干挡光，会被 {@code MOTION_BLOCKING_NO_LEAVES} 算成地表：树干那一列比周围
     * 地面高出十几格，在 LOD 里就是一根整格宽的红棕色柱子（丛林、深色橡木这类
     * 2×2 粗干尤其明显）。这里从高度图往下跳过连续的原木，取真正的地面高度。
     *
     * <p>和磁盘侧 {@code LodRegionCache.stripLogColumns} 是同一套规则，两条来源
     * 拼在一起才不会在已加载区块的边界上出现高度跳变。
     */
    private static int stripLogs(LevelChunk chunk, int x, int height, int z, int minY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y = height - 1;
        for (int guard = 0; guard < 32 && y > minY; guard++) {
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).is(BlockTags.LOGS)) {
                break;
            }
            y--;
        }
        return y + 1;
    }

    private static int heightOf(LevelChunk chunk, Heightmap.Types type, int x, int z) {
        try {
            return chunk.getHeight(type, x, z);
        } catch (Throwable t) {
            return UNKNOWN_HEIGHT;
        }
    }

    /**
     * 取某一列真正的地表方块颜色。
     *
     * <p>传进来的 {@code y} 是高度图里的「首个空气方块的 y」（{@code WORLD_SURFACE} 那种），
     * 直接拿它查方块只会查到空气：{@code MapColor.NONE} 的色值是 0，于是整片已加载区块的
     * 近景都被刷成灰色兜底色。这里从 y 往下探，跳过空气，取第一个真正有颜色的方块。
     */
    private static int blockColorAt(ClientLevel level, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        try {
            int minY = level.getMinY();
            for (int probe = y; probe >= minY && probe > y - 32; probe--) {
                pos.set(x, probe, z);
                BlockState state = level.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) {
                    // 水体按流体状态判定（不靠颜色猜），带标记返回给着色器
                    return LodRegionCache.FLAG_WATER | (MapColor.WATER.col & 0xFFFFFF);
                }
                if (state.isAir()) {
                    continue;
                }
                MapColor color = state.getMapColor(level, pos);
                int rgb = color == null ? 0 : color.col;
                if (rgb == 0) {
                    continue;
                }
                return LodRegionCache.adjustMapColor(rgb);
            }
        } catch (Throwable t) {
            return -1;
        }
        return FALLBACK_COLOR;
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
        /** 顶点是否按「每格 8 个顶点（顶面 + 两面墙）」的 quad 方式生成，索引走共享静态模式 + ExecuteIndirect 绘制。 */
        private boolean quadMode;
        /** ExecuteIndirect 参数：{vertexCount, instanceCount, startVertex, startInstance}。 */
        private Dx12GpuBuffer argsBuffer;
        /** compute 抢槽计数器（Pass 2 清零 / Pass 0 累加 / Pass 1 读回写 args）。 */
        private Dx12GpuBuffer counterBuffer;

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
            this.quadMode = gpu;
            if (gpu) {
                // quad 模式：参数与计数器都是 compute 的 UAV，各给 256B（对齐友好）
                if (this.argsBuffer == null || this.argsBuffer.isClosed()) {
                    this.argsBuffer = new Dx12GpuBuffer(BUFFER_USAGE_UAV, 256L);
                }
                if (this.counterBuffer == null || this.counterBuffer.isClosed()) {
                    this.counterBuffer = new Dx12GpuBuffer(BUFFER_USAGE_UAV, 256L);
                }
                // quad 模式每格 8 个顶点（顶面 + 两条墙），索引由全局共享的静态模式提供，
                // 槽位自己不留索引
                if (this.indexBuffer != null) {
                    closeIndex();
                }
                this.indexCount = 0;
                return;
            }
            closeQuadSupport();
            if (indexCount > 0
                && (indexBuffer == null || indexBuffer.isClosed() || this.indexCapacity < indexCount)) {
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
            this.quadMode = false;
        }

        private void closeQuadSupport() {
            if (this.argsBuffer != null) {
                try {
                    this.argsBuffer.close();
                } catch (Throwable ignored) {
                }
                this.argsBuffer = null;
            }
            if (this.counterBuffer != null) {
                try {
                    this.counterBuffer.close();
                } catch (Throwable ignored) {
                }
                this.counterBuffer = null;
            }
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
