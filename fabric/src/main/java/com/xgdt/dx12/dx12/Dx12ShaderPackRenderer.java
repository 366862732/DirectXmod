package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.shaderpack.ShaderPack;
import com.xgdt.dx12.shaderpack.ShaderPackManager;
import com.xgdt.dx12.shaderpack.ShaderPackManifest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HLSL 光影包渲染器：把 {@link ShaderPackManifest} 描述的 pass 链录进当前命令列表。
 *
 * <p>不经过原版 GLSL → SPIR-V → SPIRV-Cross 链：光影包的 HLSL 直接交给
 * native 的 {@code D3DCompile}（vs_5_1 / ps_5_1，入口固定 {@code main}），
 * 管线描述缓冲由本类按 {@link Dx12Native#dx12CreateGraphicsPipeline} 的
 * 布局自行打包。
 *
 * <p>约定（写光影包时必须遵守）：
 * <ul>
 *   <li>顶点着色器入口 {@code main}，可选；缺省用内置全屏三角形 VS。</li>
 *   <li>片元着色器入口 {@code main}，返回 {@code SV_TARGET}。</li>
 *   <li>输入纹理按 manifest 的 {@code inputs} 顺序对应 {@code register(t0..tN-1)}，
 *       静态采样器对应 {@code register(s0..sN-1)}（线性过滤 + CLAMP）。</li>
 *   <li>内置 uniform 常量缓冲固定在 {@code register(b0)}，见 {@link #UNIFORM_HEADER}。</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class Dx12ShaderPackRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-shaderpack");

    /** 每个 pass 的 uniform 槽位字节数（CBV 偏移需 256 对齐）。 */
    private static final int UNIFORM_SLOT = 512;
    /** 在飞帧数（与 3-frame flight 同步）：每帧一份槽位，避免 CPU 回头覆写 GPU 仍在读的数据。 */
    private static final int UNIFORM_FRAMES = 3;
    /** 每帧最多缓存的 pass 数（超出部分复用最后一槽）。 */
    private static final int UNIFORM_PASSES = 8;
    private static final int UNIFORM_RING = UNIFORM_FRAMES * UNIFORM_PASSES;
    /** 实际写入的 uniform 字节数（368 = 23 个 float4，见 {@link #UNIFORM_HEADER}）。 */
    private static final int UNIFORM_DATA_BYTES = 368;
    private static final int MAX_OPTIONS = 24;

    /**
     * 自定义 usage 位 32 = UAV（{@code GpuTexture} 的官方位只到 16）。
     * native 侧 {@code textureFlags()} 据此加上 {@code ALLOW_UNORDERED_ACCESS}，
     * 光追 pass 的 {@code RWTexture2D} 输出必须带这一位。
     */
    private static final int USAGE_UNORDERED_ACCESS = 32;

    /**
     * 自动注入到每个光影着色器头部的 uniform 声明。
     *
     * <p>偏移：Viewport 0 / Time 16 / CameraPosition 32 / SunDirection 48 /
     * PassParams 64 / Sky 80 / SkyColor 96 / SunsetColor 112 / Misc 128 /
     * InvViewProj 144 / ViewRotation 208 / Options 272。
     *
     * <p>{@code Options[0..5]} 按 {@code shaders.properties} 中 {@code option.*}
     * 的书写顺序填入（第 n 个设置 = {@code Options[n / 4][n % 4]}，最多 24 个）。
     */
    public static final String UNIFORM_HEADER = """
        cbuffer ShaderPackUniforms : register(b0)
        {
            float4 Viewport;        // xy = 宽高, zw = 1/宽高
            float4 Time;            // x = 秒, y = 帧序号, z = 帧间隔秒, w = 世界时间秒
            float4 CameraPosition;  // xyz = 相机坐标, w = 1
            float4 SunDirection;    // xyz = 太阳方向（世界空间单位向量）, w = 太阳高度
            float4 PassParams;      // 来自 pipeline.json 的 params[0..3]
            float4 Sky;             // x = 太阳角(rad), y = 星光亮度, z = 晴空亮度, w = 天空类型 0/1/2
            float4 SkyColor;        // rgb = 原版天空颜色, a = 末地闪光强度
            float4 SunsetColor;     // rgb = 日出日落颜色, a = 1
            float4 Misc;            // x = 视距, y = 云层高度, z = 月亮角(rad), w = 星空角(rad)
            float4x4 InvViewProj;   // inverse(projection * viewRotation)：相机相对世界的反投影
            float4x4 ViewRotation;  // 世界 -> 相机（仅旋转）
            float4 Options[6];      // shaders.properties 的 option.* 值（最多 24 个）
        };
        """;

    /**
     * 内置全屏三角形顶点着色器（无顶点缓冲）。
     *
     * <p>D3D 的 NDC y=-1 在屏幕**底部**，而经典全屏三角形写法假定 y=-1 在顶部
     * （Vulkan 习惯）。这里把 uv.y 取反，保证屏幕左上角采样 (0,0)、右下角 (1,1)，
     * 与原版渲染目标的 row 0 = 屏幕顶部一致，避免整幅画面上下颠倒。
     */
    private static final String FULLSCREEN_VS = """
        struct VSOutput
        {
            float4 position : SV_POSITION;
            float2 uv : TEXCOORD0;
        };

        VSOutput main(uint vertexId : SV_VertexID)
        {
            VSOutput output;
            float2 corner = float2((vertexId << 1) & 2, vertexId & 2);
            output.position = float4(corner * 2.0 - 1.0, 0.0, 1.0);
            output.uv = float2(corner.x, 1.0 - corner.y);
            return output;
        }
        """;

    /** 链尾隐式拷贝 pass：最后一个 pass 未写主目标时把结果贴回去。 */
    private static final String COPY_PS = """
        Texture2D<float4> InColor : register(t0);
        SamplerState InSampler : register(s0);

        float4 main(float4 position : SV_POSITION, float2 uv : TEXCOORD0) : SV_TARGET
        {
            return InColor.Sample(InSampler, uv);
        }
        """;

    /**
     * ITRP 风格离线渲染的累积 pass：把本帧与历史累积按 1/n 混合（乒乓 fx:6/fx:7）。
     * {@code PassParams.x = 1/n}，{@code PassParams.y = 1} 表示首帧（直接覆盖，
     * 避免读到未初始化的历史缓冲）。
     */
    private static final String ACCUM_PS = """
        Texture2D<float4> InColor : register(t0);
        Texture2D<float4> InAccum : register(t1);
        SamplerState InSampler : register(s0);
        SamplerState AccumSampler : register(s1);

        float4 main(float4 position : SV_POSITION, float2 uv : TEXCOORD0) : SV_TARGET
        {
            float4 scene = InColor.SampleLevel(InSampler, uv, 0.0);
            float weight = PassParams.x;
            float reset = PassParams.y;
            float4 accum = InAccum.SampleLevel(AccumSampler, uv, 0.0);
            float4 result = reset > 0.5 ? scene : lerp(accum, scene, weight);
            return float4(result.rgb, 1.0);
        }
        """;

    private static final Dx12ShaderPackRenderer INSTANCE = new Dx12ShaderPackRenderer();

    /** (packId, passName, outputFormat) -> native pipeline handle。 */
    private final Map<String, Long> pipelineCache = new HashMap<>();
    /** 主目标 / 深度纹理句柄 -> SRV 视图（RenderTarget 重建后句柄变化，自动失效）。 */
    private final Map<Long, Dx12GpuTextureView> sceneViews = new HashMap<>();
    private final List<Intermediate> intermediates = new ArrayList<>();

    private Dx12GpuBuffer uniformBuffer;
    private GpuBufferSlice.MappedView uniformView;
    private ByteBuffer uniformMapped;
    private String loadedPackId = "";
    private long frameIndex;
    private long lastNanos;
    private float deltaSeconds;
    private boolean failureLogged;
    private int rtStatsFrames;

    private Dx12ShaderPackRenderer() {
    }

    public static Dx12ShaderPackRenderer getInstance() {
        return INSTANCE;
    }

    /** 每帧调用：在世界渲染完成、GUI 绘制之前执行光影 pass 链。 */
    public void renderFrame() {
        ShaderPackManager manager = ShaderPackManager.getInstance();
        ShaderPack pack = manager.activeIfEnabled();
        if (pack == null) {
            OfflineRender.abort();
            if (Dx12RtScene.enabled) {
                Dx12RtScene.enabled = false;
                Dx12RtScene.clear();
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        GpuTexture colorTexture = target.getColorTexture();
        GpuTexture depthTexture = target.getDepthTexture();
        if (colorTexture == null || !(colorTexture instanceof Dx12GpuTexture color)
            || depthTexture == null || !(depthTexture instanceof Dx12GpuTexture depth)) {
            return;
        }
        Dx12CommandEncoderBackend encoder = Dx12Device.sharedEncoder();
        if (encoder == null) {
            return;
        }
        long ctx = encoder.nativeHandle();

        int width = color.getWidth(0);
        int height = color.getHeight(0);
        if (width <= 0 || height <= 0) {
            return;
        }

        try {
            if (!pack.id().equals(this.loadedPackId)) {
                invalidate();
                this.loadedPackId = pack.id();
            }
            execute(ctx, pack, color, depth, width, height, minecraft);
            this.failureLogged = false;
        } catch (Throwable t) {
            if (!this.failureLogged) {
                this.failureLogged = true;
                LOGGER.error("[shaderpack] 执行光影失败，本帧跳过", t);
            }
        }
    }

    private void execute(long ctx, ShaderPack pack, Dx12GpuTexture color, Dx12GpuTexture depth,
                         int width, int height, Minecraft minecraft) {
        ShaderPackManifest manifest = pack.manifest();
        List<ShaderPackManifest.Pass> passes = new ArrayList<>();
        for (ShaderPackManifest.Pass pass : manifest.passes()) {
            if (pass.enabled()) {
                passes.add(pass);
            }
        }
        if (passes.isEmpty()) {
            return;
        }

        long now = System.nanoTime();
        if (this.lastNanos != 0L) {
            this.deltaSeconds = (now - this.lastNanos) / 1_000_000_000.0f;
        }
        this.lastNanos = now;
        this.frameIndex++;

        // P68：包内存在 raytrace pass 时才开启世界几何捕获，否则零开销。
        boolean hasRaytrace = passes.stream().anyMatch(ShaderPackManifest.Pass::isRaytrace);
        updateRaytracing(hasRaytrace, minecraft);

        // 本帧只会用到主目标与深度两张纹理：先清掉上一帧残留的旧句柄视图，
        // 再分别建视图。注意不能像早期实现那样在 view() 里“关掉除当前句柄外
        // 的所有视图”——那会在取深度视图时把刚建好的颜色视图关掉。
        retainViews(color.handle(), depth.handle());
        Dx12GpuTextureView colorView = view(color);
        Dx12GpuTextureView depthView = view(depth);
        int sceneFormat = color.getFormat().ordinal();
        int intermediateFormat = GpuFormat.RGBA16_FLOAT.ordinal();

        // 预热中间纹理，避免 pass 之间尺寸不一致。
        for (int i = 0; i < manifest.intermediateCount(); i++) {
            intermediate(i, width, height);
        }

        // 首次执行时 ensureUniformBuffer 尚未被 writeUniforms 触发，
        // 这里必须先建好，否则 handle() 会 NPE。
        ensureUniformBuffer();
        long uniformHandle = this.uniformBuffer.handle();

        String lastOutput = "scene_color";
        int passIndex = 0;

        // 光追 pass 之前必须先把本帧的 TLAS 建好（BuildRaytracingAccelerationStructure
        // 不能在 render pass 内录制，所以在第一个 BeginRenderPass 之前调用）。
        if (hasRaytrace) {
            Dx12Native.dx12RtBuildScene(ctx);
            if (++this.rtStatsFrames >= 120) {
                this.rtStatsFrames = 0;
                LOGGER.info("[shaderpack] RT stats: {} | {}", Dx12Native.dx12RtStats(),
                    Dx12RtScene.debugSummary());
            }
        }

        for (ShaderPackManifest.Pass pass : passes) {
            if (pass.isRaytrace()) {
                passIndex = dispatchRaytracePass(ctx, pack, pass, depthView, width, height,
                    minecraft, passIndex);
                lastOutput = pass.output();
                continue;
            }
            boolean sceneColor = pass.writesSceneColor();
            Dx12TextureRef output = sceneColor
                ? new Dx12TextureRef(color, colorView, sceneFormat)
                : intermediateRef(pass.outputTargetIndex(), width, height);

            List<Dx12TextureRef> inputs = new ArrayList<>(pass.inputs().size());
            for (String input : pass.inputs()) {
                inputs.add(resolveInput(input, color, colorView, depth, depthView));
            }

            long pipeline = pipelineFor(pack, pass, output.formatOrdinal(), inputs.size());
            if (pipeline == 0L) {
                return;
            }

            // 每个 pass 独占一个槽位：CBV 地址被烘焙进命令列表，若同帧所有 pass
            // 共用一个槽位，CPU 在录制期就会把前一个 pass 的 params 覆写掉。
            int uniformOffset = writeUniforms(pack, width, height, minecraft,
                pass.params(), passIndex);

            Dx12Native.dx12BeginRenderPass(ctx, new long[]{output.texture().handle()},
                new int[]{0}, new byte[]{0}, null, 0L, 0, (byte) 0, 0.0,
                0, 0, width, height);
            // 必须 finally 收尾：中途抛异常会让 native 侧 render pass 一直处于
            // open 状态，下一帧原版 createRenderPass 会直接崩“already open”。
            try {
                Dx12Native.dx12SetPipeline(ctx, pipeline, false);
                pushDescriptors(ctx, inputs, uniformHandle, uniformOffset);
                Dx12Native.dx12Draw(ctx, 3, 1, 0, 0);
            } finally {
                Dx12Native.dx12EndRenderPass(ctx);
            }
            lastOutput = pass.output();
            passIndex++;
        }

        if (!"scene_color".equals(lastOutput)) {
            // 链尾把最后一个中间结果贴回主目标。
            Dx12TextureRef source = resolveInput(lastOutput, color, colorView, depth, depthView);
            long pipeline = copyPipeline(sceneFormat);
            if (pipeline != 0L) {
                Dx12Native.dx12BeginRenderPass(ctx, new long[]{color.handle()}, new int[]{0},
                    new byte[]{0}, null, 0L, 0, (byte) 0, 0.0, 0, 0, width, height);
                int copyOffset = writeUniforms(pack, width, height, minecraft, null,
                    passIndex);
                try {
                    Dx12Native.dx12SetPipeline(ctx, pipeline, false);
                    pushDescriptors(ctx, List.of(source), uniformHandle, copyOffset);
                    Dx12Native.dx12Draw(ctx, 3, 1, 0, 0);
                } finally {
                    Dx12Native.dx12EndRenderPass(ctx);
                }
            }
        }

        // ITRP 风格离线渲染：把本帧混入累积缓冲并展示累积结果（截图也在这一步抓取）。
        if (OfflineRender.framePrepared()) {
            offlineAccumulate(ctx, pack, color, colorView, sceneFormat, width, height,
                minecraft, uniformHandle, passIndex);
        }
    }

    /**
     * 离线渲染的累积步骤：scene_color 与历史累积按 1/n 乒乓混合（fx:6/fx:7），
     * 再把累积结果贴回主目标显示——主目标里因此始终是收敛中的最终画面，
     * {@link OfflineRender#onFrameAccumulated()} 在最后一帧直接对它截图。
     */
    private void offlineAccumulate(long ctx, ShaderPack pack, Dx12GpuTexture color,
                                   Dx12GpuTextureView colorView, int sceneFormat, int width, int height,
                                   Minecraft minecraft, long uniformHandle, int passIndex) {
        int n = OfflineRender.sampleIndex();
        int srcIndex = (n & 1) == 0 ? 6 : 7;
        int dstIndex = (n & 1) == 0 ? 7 : 6;
        Dx12TextureRef accumDst = intermediateRef(dstIndex, width, height);
        Dx12TextureRef accumSrc = intermediateRef(srcIndex, width, height);

        long pipeline = accumPipeline();
        if (pipeline == 0L) {
            OfflineRender.cancel("累积管线创建失败");
            return;
        }
        float weight = 1.0f / (n + 1);
        float reset = n == 0 ? 1.0f : 0.0f;
        int accumOffset = writeUniforms(pack, width, height, minecraft,
            new float[]{weight, reset, 0.0f, 0.0f}, passIndex + 1);
        Dx12Native.dx12BeginRenderPass(ctx, new long[]{accumDst.texture().handle()},
            new int[]{0}, new byte[]{0}, null, 0L, 0, (byte) 0, 0.0, 0, 0, width, height);
        try {
            Dx12Native.dx12SetPipeline(ctx, pipeline, false);
            pushDescriptors(ctx, List.of(
                new Dx12TextureRef(color, colorView, sceneFormat),
                accumSrc), uniformHandle, accumOffset);
            Dx12Native.dx12Draw(ctx, 3, 1, 0, 0);
        } finally {
            Dx12Native.dx12EndRenderPass(ctx);
        }

        long copy = copyPipeline(sceneFormat);
        if (copy == 0L) {
            OfflineRender.cancel("展示管线创建失败");
            return;
        }
        int copyOffset = writeUniforms(pack, width, height, minecraft, null, passIndex + 2);
        Dx12Native.dx12BeginRenderPass(ctx, new long[]{color.handle()}, new int[]{0},
            new byte[]{0}, null, 0L, 0, (byte) 0, 0.0, 0, 0, width, height);
        try {
            Dx12Native.dx12SetPipeline(ctx, copy, false);
            pushDescriptors(ctx, List.of(accumDst), uniformHandle, copyOffset);
            Dx12Native.dx12Draw(ctx, 3, 1, 0, 0);
        } finally {
            Dx12Native.dx12EndRenderPass(ctx);
        }
        OfflineRender.onFrameAccumulated();
    }

    /** 离线渲染的累积管线（RGBA16F 输出；输入 t0=scene_color、t1=历史累积）。 */
    private long accumPipeline() {
        String key = "__offline_accum__";
        Long cached = this.pipelineCache.get(key);
        if (cached != null) {
            return cached;
        }
        long handle = createPipeline(FULLSCREEN_VS, ACCUM_PS, GpuFormat.RGBA16_FLOAT.ordinal(), 2);
        this.pipelineCache.put(key, handle);
        return handle;
    }

    /**
     * 光影包是否含光追 pass 发生变化时，开关世界几何捕获。
     *
     * <p>从关到开时已建好的区块不会重新上传，所以让原版丢弃并重编译全部 section；
     * 该调用推迟到下一个客户端 tick，避免在渲染中途重建 ViewArea。
     */
    private void updateRaytracing(boolean raytrace, Minecraft minecraft) {
        if (raytrace == Dx12RtScene.enabled) {
            return;
        }
        Dx12RtScene.enabled = raytrace;
        Dx12RtScene.clear();
        LOGGER.info("[shaderpack] 光追世界几何 {}", raytrace ? "开启（重建全部区块几何）" : "关闭");
        if (!raytrace || minecraft.level == null) {
            return;
        }
        minecraft.execute(() -> {
            if (Dx12RtScene.enabled && minecraft.level != null) {
                LOGGER.info("[shaderpack] 光追开启：重建全部区块几何（invalidateCompiledGeometry）");
                minecraft.levelRenderer.invalidateCompiledGeometry(minecraft.level,
                    minecraft.options, minecraft.gameRenderer.mainCamera(),
                    minecraft.getBlockColors());
            }
        });
    }

    /** 录一次 DispatchRays：深度重建世界位置 → 太阳阴影可见性写入 fx:N。 */
    private int dispatchRaytracePass(long ctx, ShaderPack pack, ShaderPackManifest.Pass pass,
                                     Dx12GpuTextureView depthView, int width, int height,
                                     Minecraft minecraft, int passIndex) {
        if (pass.outputTargetIndex() < 0) {
            LOGGER.warn("[shaderpack] raytrace pass '{}' 的 output 必须是 fx:N（当前 {}）",
                pass.name(), pass.output());
            return passIndex;
        }
        Dx12TextureRef output = intermediateRef(pass.outputTargetIndex(), width, height);
        int uniformOffset = writeUniforms(pack, width, height, minecraft, pass.params(), passIndex);
        boolean ok = Dx12Native.dx12RtDispatch(ctx, depthView.handle(), output.texture().handle(),
            this.uniformBuffer.handle(), uniformOffset, width, height);
        if (!ok) {
            // 光追没跑（DXR 不可用 / 尚无 TLAS）：把输出清成“全受光”，
            // 否则 final 会采样到上一帧甚至未初始化的内容。
            Dx12Native.dx12ClearColorTexture(ctx, output.texture().handle(), 1.0f, 1.0f, 1.0f, 1.0f);
            if (Dx12Native.LOG_VERBOSE) {
                LOGGER.warn("[shaderpack] raytrace pass '{}' 未执行（DXR 不可用或场景为空）",
                    pass.name());
            }
        }
        return passIndex + 1;
    }

    // ------------------------------------------------------------------
    // 输入 / 输出
    // ------------------------------------------------------------------

    private Dx12TextureRef resolveInput(String name, Dx12GpuTexture color,
                                        Dx12GpuTextureView colorView, Dx12GpuTexture depth,
                                        Dx12GpuTextureView depthView) {
        if ("scene_color".equals(name)) {
            return new Dx12TextureRef(color, colorView, color.getFormat().ordinal());
        }
        if ("scene_depth".equals(name)) {
            return new Dx12TextureRef(depth, depthView, depth.getFormat().ordinal());
        }
        if (name.startsWith("fx:")) {
            try {
                int index = Integer.parseInt(name.substring(3).trim());
                return intermediateRef(index, color.getWidth(0), color.getHeight(0));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的输入目标: " + name);
            }
        }
        throw new IllegalArgumentException("未知的输入目标: " + name
            + "（可用：scene_color / scene_depth / fx:N）");
    }

    private Dx12TextureRef intermediateRef(int index, int width, int height) {
        Intermediate intermediate = this.intermediate(index, width, height);
        return new Dx12TextureRef(intermediate.texture, intermediate.view,
            intermediate.texture.getFormat().ordinal());
    }

    private Intermediate intermediate(int index, int width, int height) {
        while (this.intermediates.size() <= index) {
            this.intermediates.add(null);
        }
        Intermediate current = this.intermediates.get(index);
        if (current != null && current.width == width && current.height == height) {
            return current;
        }
        if (current != null) {
            current.close();
        }
        Dx12GpuTexture texture = new Dx12GpuTexture(
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | USAGE_UNORDERED_ACCESS,
            "shaderpack fx:" + index, GpuFormat.RGBA16_FLOAT, width, height, 1, 1);
        Dx12GpuTextureView view = new Dx12GpuTextureView(texture, 0, 1);
        Intermediate created = new Intermediate(texture, view, width, height);
        this.intermediates.set(index, created);
        return created;
    }

    private Dx12GpuTextureView view(Dx12GpuTexture texture) {
        long handle = texture.handle();
        Dx12GpuTextureView existing = this.sceneViews.get(handle);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }
        if (existing != null) {
            this.sceneViews.remove(handle);
        }
        Dx12GpuTextureView created = new Dx12GpuTextureView(texture, 0, texture.getMipLevels());
        this.sceneViews.put(handle, created);
        return created;
    }

    /** 关闭除给定句柄之外的全部缓存视图（RenderTarget 重建后旧句柄已失效）。 */
    private void retainViews(long keepA, long keepB) {
        var iterator = this.sceneViews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Dx12GpuTextureView> entry = iterator.next();
            if (entry.getKey() != keepA && entry.getKey() != keepB) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    // ------------------------------------------------------------------
    // uniform
    // ------------------------------------------------------------------

    private int writeUniforms(ShaderPack pack, int width, int height, Minecraft minecraft,
                              float[] passParams, int passIndex) {
        ensureUniformBuffer();
        int frameSlot = (int) (this.frameIndex % UNIFORM_FRAMES) * UNIFORM_PASSES;
        int slot = frameSlot + Math.min(Math.max(passIndex, 0), UNIFORM_PASSES - 1);
        int base = slot * UNIFORM_SLOT;
        ByteBuffer buffer = this.uniformMapped;
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        GameRenderState renderState = minecraft.gameRenderer == null ? null
            : minecraft.gameRenderer.gameRenderState();
        LevelRenderState levelState = renderState == null ? null : renderState.levelRenderState;
        CameraRenderState camera = levelState == null ? null : levelState.cameraRenderState;
        SkyRenderState sky = levelState == null ? null : levelState.skyRenderState;

        float cameraX = 0.0f, cameraY = 0.0f, cameraZ = 0.0f;
        if (camera != null && camera.pos != null) {
            cameraX = (float) camera.pos.x;
            cameraY = (float) camera.pos.y;
            cameraZ = (float) camera.pos.z;
        }

        buffer.putFloat(base, width);
        buffer.putFloat(base + 4, height);
        buffer.putFloat(base + 8, 1.0f / Math.max(1, width));
        buffer.putFloat(base + 12, 1.0f / Math.max(1, height));

        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        buffer.putFloat(base + 16, OfflineRender.shaderTime());
        buffer.putFloat(base + 20, (float) this.frameIndex);
        buffer.putFloat(base + 24, this.deltaSeconds);
        buffer.putFloat(base + 28, gameTime / 20.0f);

        buffer.putFloat(base + 32, cameraX);
        buffer.putFloat(base + 36, cameraY);
        buffer.putFloat(base + 40, cameraZ);
        buffer.putFloat(base + 44, 1.0f);

        // 太阳方向：与原版 SkyRenderer 一致（Axis.YP(-90) 后绕 X 轴旋转 sunAngle）。
        float sunAngle = sky == null ? 0.0f : sky.sunAngle;
        float sunX = -(float) Math.sin(sunAngle);
        float sunY = (float) Math.cos(sunAngle);
        buffer.putFloat(base + 48, sunX);
        buffer.putFloat(base + 52, sunY);
        buffer.putFloat(base + 56, 0.0f);
        buffer.putFloat(base + 60, sunY);

        putFloats(buffer, base + 64, passParams);

        // Sky / SkyColor / SunsetColor
        buffer.putFloat(base + 80, sunAngle);
        buffer.putFloat(base + 84, sky == null ? 0.0f : sky.starBrightness);
        buffer.putFloat(base + 88, sky == null ? 1.0f : sky.rainBrightness);
        buffer.putFloat(base + 92, sky == null ? 0.0f : sky.skybox.ordinal());
        putArgb(buffer, base + 96, sky == null ? 0 : sky.skyColor,
            sky == null ? 0.0f : sky.endFlashIntensity);
        putArgb(buffer, base + 112, sky == null ? 0 : sky.sunriseAndSunsetColor, 1.0f);

        // Misc：视距 / 云层高度 / 月亮角 / 星空角
        buffer.putFloat(base + 128, camera == null ? 256.0f : camera.depthFar);
        float cloudHeight = levelState == null ? 0.0f : levelState.cloudHeight;
        buffer.putFloat(base + 132, cloudHeight > 1.0f ? cloudHeight : 192.0f);
        buffer.putFloat(base + 136, sky == null ? 0.0f : sky.moonAngle);
        buffer.putFloat(base + 140, sky == null ? 0.0f : sky.starAngle);

        // InvViewProj / ViewRotation（JOML 与 HLSL 默认列主序一致，直接按存储序写入）
        float[] matrix = new float[16];
        Matrix4f invViewProj = new Matrix4f();
        if (camera != null) {
            invViewProj.set(camera.projectionMatrix).mul(camera.viewRotationMatrix).invert();
        }
        invViewProj.get(matrix);
        for (int i = 0; i < 16; i++) {
            buffer.putFloat(base + 144 + i * 4, matrix[i]);
        }
        if (camera != null) {
            camera.viewRotationMatrix.get(matrix);
        } else {
            new Matrix4f().get(matrix);
        }
        for (int i = 0; i < 16; i++) {
            buffer.putFloat(base + 208 + i * 4, matrix[i]);
        }

        float[] options = ShaderPackManager.getInstance().settingValues(MAX_OPTIONS);
        for (int i = 0; i < MAX_OPTIONS; i++) {
            buffer.putFloat(base + 272 + i * 4, options[i]);
        }
        return base;
    }

    private static void putFloats(ByteBuffer buffer, int offset, float[] values) {
        for (int i = 0; i < 4; i++) {
            buffer.putFloat(offset + i * 4, values != null && i < values.length ? values[i] : 0.0f);
        }
    }

    /** ARGB32 -> rgb(0..1) + 自定义 alpha。 */
    private static void putArgb(ByteBuffer buffer, int offset, int argb, float alpha) {
        buffer.putFloat(offset, ((argb >> 16) & 0xFF) / 255.0f);
        buffer.putFloat(offset + 4, ((argb >> 8) & 0xFF) / 255.0f);
        buffer.putFloat(offset + 8, (argb & 0xFF) / 255.0f);
        buffer.putFloat(offset + 12, alpha);
    }

    private void ensureUniformBuffer() {
        if (this.uniformBuffer != null && !this.uniformBuffer.isClosed()) {
            return;
        }
        this.uniformBuffer = new Dx12GpuBuffer(GpuBuffer.USAGE_MAP_WRITE,
            (long) UNIFORM_SLOT * UNIFORM_RING);
        // 持有 MappedView（而不是只取 data()）：close 时才触发 dx12UnmapBuffer，
        // 避免每次重建都泄漏一个 map 引用。
        this.uniformView = this.uniformBuffer.map(0L, (long) UNIFORM_SLOT * UNIFORM_RING,
            false, true);
        this.uniformMapped = this.uniformView.data();
        this.uniformMapped.order(ByteOrder.LITTLE_ENDIAN);
    }

    // ------------------------------------------------------------------
    // pipeline
    // ------------------------------------------------------------------

    private long pipelineFor(ShaderPack pack, ShaderPackManifest.Pass pass, int format,
                             int inputCount) {
        String key = pack.id() + "|" + pass.name() + "|" + format + "|" + inputCount;
        Long cached = this.pipelineCache.get(key);
        if (cached != null) {
            return cached;
        }
        String fragmentSource;
        String vertexSource;
        try {
            fragmentSource = pack.readText(pass.fragment());
            if (fragmentSource == null) {
                throw new IllegalArgumentException("找不到片元着色器: " + pass.fragment());
            }
            if (pass.vertex() != null) {
                vertexSource = pack.readText(pass.vertex());
                if (vertexSource == null) {
                    throw new IllegalArgumentException("找不到顶点着色器: " + pass.vertex());
                }
            } else {
                vertexSource = FULLSCREEN_VS;
            }
        } catch (Exception e) {
            LOGGER.error("[shaderpack] {}", e.getMessage());
            this.pipelineCache.put(key, 0L);
            return 0L;
        }
        long handle = createPipeline(vertexSource, fragmentSource, format, inputCount);
        this.pipelineCache.put(key, handle);
        return handle;
    }

    private long copyPipeline(int format) {
        String key = "__copy__|" + format;
        Long cached = this.pipelineCache.get(key);
        if (cached != null) {
            return cached;
        }
        long handle = createPipeline(FULLSCREEN_VS, COPY_PS, format, 1);
        this.pipelineCache.put(key, handle);
        return handle;
    }

    /**
     * 打包 {@link Dx12Native#dx12CreateGraphicsPipeline} 的 desc：
     * SRV t0..tN-1 在前（type=3：线性 + CLAMP 静态采样器），CBV b0 最后。
     */
    private static long createPipeline(String vertexSource, String fragmentSource, int format,
                                       int inputCount) {
        byte[] vs = (UNIFORM_HEADER + vertexSource).getBytes(StandardCharsets.UTF_8);
        byte[] ps = (UNIFORM_HEADER + fragmentSource).getBytes(StandardCharsets.UTF_8);

        int entryCount = inputCount + 1;
        int size = 4 + vs.length + 4 + ps.length;
        size += 4 + 12;             // colorCount=1 + 一个颜色目标
        size += 1;                  // hasDepth=0
        size += 4 + 1 + 4;          // topology / cull / polygonMode
        size += 4;                  // inputElementCount=0
        size += 4;                  // semanticCount=0
        size += 4 + entryCount * 2; // entryCount + 每条 (type, reg)

        ByteBuffer desc = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
        desc.putInt(vs.length);
        desc.put(vs);
        desc.putInt(ps.length);
        desc.put(ps);

        desc.putInt(1);
        desc.putInt(format);
        desc.put((byte) 0x0F);   // RGBA 写掩码
        desc.put((byte) 0);      // blendEnabled
        for (int i = 0; i < 6; i++) {
            desc.put((byte) 0);  // blend 参数
        }

        desc.put((byte) 0);      // hasDepth
        desc.putInt(4);          // PrimitiveTopology.TRIANGLES
        desc.put((byte) 0);      // cull = false（全屏三角形）
        desc.putInt(0);          // PolygonMode.FILL

        desc.putInt(0);          // inputElementCount
        desc.putInt(0);          // semanticCount

        desc.putInt(entryCount);
        for (int i = 0; i < inputCount; i++) {
            desc.put((byte) 3);  // SRV + 线性/CLAMP 静态采样器
            desc.put((byte) i);  // t{i} / s{i}
        }
        desc.put((byte) 0);      // CBV
        desc.put((byte) 0);      // b0

        desc.flip();
        return Dx12Native.dx12CreateGraphicsPipeline(desc);
    }

    private static void pushDescriptors(long ctx, List<Dx12TextureRef> inputs,
                                        long uniformHandle, int uniformOffset) {
        int count = inputs.size();
        int[] types = new int[count + 1];
        long[] buffers = new long[count + 1];
        long[] offsets = new long[count + 1];
        long[] lengths = new long[count + 1];
        int[] texelFormats = new int[count + 1];
        long[] views = new long[count + 1];
        for (int i = 0; i < count; i++) {
            types[i] = 1;
            views[i] = inputs.get(i).view().handle();
        }
        types[count] = 0;
        buffers[count] = uniformHandle;
        offsets[count] = uniformOffset;
        lengths[count] = UNIFORM_DATA_BYTES;
        if (!Dx12Native.dx12PushDescriptors(ctx, types, buffers, offsets, lengths,
            texelFormats, views)) {
            throw new IllegalStateException("dx12PushDescriptors 失败");
        }
    }

    /** 释放全部缓存（切换光影包 / 资源重载时调用）。 */
    public void invalidate() {
        OfflineRender.abort();
        for (Long handle : this.pipelineCache.values()) {
            if (handle != null && handle != 0L) {
                Dx12Native.dx12DestroyPipeline(handle);
            }
        }
        this.pipelineCache.clear();
        for (Map.Entry<Long, Dx12GpuTextureView> entry : this.sceneViews.entrySet()) {
            entry.getValue().close();
        }
        this.sceneViews.clear();
        for (Intermediate intermediate : this.intermediates) {
            if (intermediate != null) {
                intermediate.close();
            }
        }
        this.intermediates.clear();
        if (this.uniformView != null) {
            try {
                this.uniformView.close();
            } catch (Throwable ignored) {
                // 资源可能已随设备销毁。
            }
            this.uniformView = null;
            this.uniformMapped = null;
        }
        if (this.uniformBuffer != null) {
            try {
                this.uniformBuffer.close();
            } catch (Throwable ignored) {
                // 资源可能已随设备销毁。
            }
            this.uniformBuffer = null;
        }
        this.lastNanos = 0L;
        this.loadedPackId = "";
    }

    private record Dx12TextureRef(Dx12GpuTexture texture, Dx12GpuTextureView view,
                                  int formatOrdinal) {
    }

    private record Intermediate(Dx12GpuTexture texture, Dx12GpuTextureView view,
                                int width, int height) {
        private void close() {
            this.view.close();
            this.texture.close();
        }
    }
}
