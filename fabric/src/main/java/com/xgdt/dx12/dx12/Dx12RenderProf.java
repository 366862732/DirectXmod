package com.xgdt.dx12.dx12;

/**
 * P50：渲染线程分段计时。
 *
 * 目的：定位 frameInterval 中除 dx12 后端（P3 已覆盖）之外的 CPU 开销分布——
 * 是 MC 的渲染状态提取（extract）、命令录制（render）、世界渲染（renderLevel）、
 * GUI，还是 Present/提交。
 *
 * 各阶段由 mixin 在目标方法 HEAD/RETURN 处调用 begin/end 累加，每 INTERVAL 帧
 * 输出一次均值。开关沿用 DX12_PROF=1（或 DX12_LOG_VERBOSE=1）；关闭时每个埋点
 * 只是一次 static final boolean 判断，运行期无额外开销。
 *
 * 输出示例：
 *   [dx12-java] P50 render n=300 frameInterval=4.05ms frame=3.98ms extract=1.10ms
 *     render=2.40ms[world=2.10 gui=0.25]ms blit=0.05ms present=0.16ms
 *
 * 注：frame − (extract + render + blit + present) 即 update/executePendingTasks/
 * frameLimiter 等其余开销（未单独埋点，用残差观察）。
 */
public final class Dx12RenderProf {
    public static final boolean ENABLED = Dx12Native.PROF;

    private static final int INTERVAL = 300;

    /** Minecraft.renderFrame 全程（含 extract/render/present）。 */
    public static final int FRAME = 0;
    /** GameRenderer.extract：渲染状态提取。 */
    public static final int EXTRACT = 1;
    /** GameRenderer.render：本帧命令录制。 */
    public static final int RENDER = 2;
    /** GameRenderer.renderLevel：世界渲染（RENDER 的子集）。 */
    public static final int WORLD = 3;
    /** GuiRenderer.render（RENDER 的子集）。 */
    public static final int GUI = 4;
    /** Dx12GpuSurface.blitFromTexture：等 back buffer + blit 录制。 */
    public static final int BLIT = 5;
    /** Dx12GpuSurface.present：swapchain Present。 */
    public static final int PRESENT = 6;

    // ---- P51：renderLevel（WORLD）内部细分，定位世界渲染 2~3.6ms 的构成 ----
    /** LevelRenderer.submitFeatures：实体/方块实体/Gizmo 提交。 */
    public static final int SUBMIT_FEATURES = 7;
    /** LevelRenderer.prepareChunkRenders：遍历可见 section × layer 生成绘制列表。 */
    public static final int PREPARE_CHUNKS = 8;
    /** LevelRenderer.compileSections：分发区块重建任务。 */
    public static final int COMPILE_SECTIONS = 9;
    /** SectionRenderDispatcher.uploadTerrainBuffersToGpu：地形顶点/索引上传。 */
    public static final int UPLOAD_TERRAIN = 10;
    /** ChunkSectionsToRender.renderGroup：实际绘制 pass（含 multidraw 批量）。 */
    public static final int RENDER_GROUP = 11;
    /** SectionOcclusionGraph.update：可见性/遮挡图更新。 */
    public static final int OCCLUSION = 12;

    // ---- P52：world 中尚未归因的 ~1.1ms 继续细分 ----
    /** FeatureRenderDispatcher.prepareFrame：实体/方块实体提交排序与准备。 */
    public static final int PREPARE_FEATURES = 13;
    /** FrameGraphBuilder.execute：整帧所有 pass 体的执行（含 renderGroup）。 */
    public static final int EXECUTE_FRAMEGRAPH = 14;
    /** PreparedFrame.executeSolid：实体/方块实体实际绘制。 */
    public static final int EXEC_FEATURES = 15;

    private static final int N = 16;

    private static final long[] acc = new long[N];
    private static final long[] mark = new long[N];
    private static long lastFrameStartNs;
    private static long accIntervalNs;
    private static int frames;
    private static boolean samplerStarted;

    private Dx12RenderProf() {}

    public static void begin(int stage) {
        if (ENABLED) {
            mark[stage] = System.nanoTime();
        }
    }

    public static void end(int stage) {
        if (ENABLED) {
            acc[stage] += System.nanoTime() - mark[stage];
        }
    }

    /** 帧首：累计相邻帧起始的时间差（即真实 frameInterval）。 */
    public static void frameStart() {
        if (!ENABLED) {
            return;
        }
        if (!samplerStarted) {
            samplerStarted = true;
            Dx12StackSampler.attach();
            Dx12StackSampler.start();
        }
        long now = System.nanoTime();
        if (lastFrameStartNs != 0L) {
            accIntervalNs += now - lastFrameStartNs;
        }
        lastFrameStartNs = now;
    }

    /** 帧尾：每 INTERVAL 帧输出一次均值并清零。 */
    public static void frameEnd() {
        if (!ENABLED) {
            return;
        }
        if (++frames % INTERVAL != 0) {
            return;
        }
        double inv = 1.0 / INTERVAL;
        System.err.println("[dx12-java] P50 render n=" + INTERVAL
            + " frameInterval=" + ms(accIntervalNs * inv) + "ms"
            + " frame=" + ms(acc[FRAME] * inv) + "ms"
            + " extract=" + ms(acc[EXTRACT] * inv) + "ms"
            + " render=" + ms(acc[RENDER] * inv) + "ms"
            + "[world=" + ms(acc[WORLD] * inv) + " gui=" + ms(acc[GUI] * inv) + "]ms"
            + " blit=" + ms(acc[BLIT] * inv) + "ms"
            + " present=" + ms(acc[PRESENT] * inv) + "ms");
        System.err.println("[dx12-java] P51 world n=" + INTERVAL
            + " submitFeatures=" + ms(acc[SUBMIT_FEATURES] * inv) + "ms"
            + " prepareChunks=" + ms(acc[PREPARE_CHUNKS] * inv) + "ms"
            + " renderGroup=" + ms(acc[RENDER_GROUP] * inv) + "ms"
            + " compileSections=" + ms(acc[COMPILE_SECTIONS] * inv) + "ms"
            + " uploadTerrain=" + ms(acc[UPLOAD_TERRAIN] * inv) + "ms"
            + " occlusion=" + ms(acc[OCCLUSION] * inv) + "ms");
        System.err.println("[dx12-java] P52 world2 n=" + INTERVAL
            + " prepareFeatures=" + ms(acc[PREPARE_FEATURES] * inv) + "ms"
            + " execFrameGraph=" + ms(acc[EXECUTE_FRAMEGRAPH] * inv) + "ms"
            + " [execFeaturesSolid=" + ms(acc[EXEC_FEATURES] * inv) + "ms]"
            + " " + gcStats());
        System.err.println(Dx12StackSampler.report(10));
        System.err.flush();
        java.util.Arrays.fill(acc, 0L);
        accIntervalNs = 0L;
    }

    private static String ms(double ns) {
        return String.format(java.util.Locale.ROOT, "%.2f", ns / 1_000_000.0);
    }

    /** P52：本区间内 GC 次数/耗时增量 + 当前堆占用，用于判断是否有分配风暴。 */
    private static long gcCountBase = -1L;
    private static long gcTimeBase = -1L;

    private static String gcStats() {
        long count = 0L;
        long time = 0L;
        for (java.lang.management.GarbageCollectorMXBean gc
                : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = gc.getCollectionCount();
            long t = gc.getCollectionTime();
            if (c > 0L) {
                count += c;
            }
            if (t > 0L) {
                time += t;
            }
        }
        if (gcCountBase < 0L) {
            gcCountBase = count;
            gcTimeBase = time;
        }
        long dc = count - gcCountBase;
        long dt = time - gcTimeBase;
        gcCountBase = count;
        gcTimeBase = time;
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
        return String.format(java.util.Locale.ROOT,
            "gc(per%dF)=%d/%.1fms heap=%dMB", INTERVAL, dc, (double) dt, usedMb);
    }
}
