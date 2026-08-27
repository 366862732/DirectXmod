package com.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D3D12 implementation of the vanilla {@link GpuDeviceBackend} factory.
 *
 * P2 scope: every resource-creating method (texture / buffer / sampler /
 * texture view) creates real D3D12 resources through dx12_mc.dll. The render
 * pipeline methods (surface / command encoder / pipeline / query pool) are
 * stubbed and arrive in P3+.
 */
@Environment(EnvType.CLIENT)
public class Dx12Device implements GpuDeviceBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    /** P15 诊断：标记 DX12 后端是否已成功初始化。 */
    private static volatile boolean initialized = false;
    public static boolean isInitialized() { return initialized; }

    private final DeviceInfo deviceInfo;
    private long timestampCtx;

    // 官方 VulkanDevice 构造时创建一次共享 CommandEncoder，createCommandEncoder()
    // 每次返回同一实例（blit 与 submit 用同一 encoder，整帧命令一次提交）。
    // 之前每次 new 一个独立 CommandContext：绘制指令记在 A、帧末 submit 却提交
    // 新建的空 ctx B → GPU 每帧执行空命令列表 → backbuffer 呈现未初始化垃圾
    // （疯狂闪烁）。改为懒加载共享单例复现官方语义。
    private @Nullable Dx12CommandEncoderBackend sharedCommandEncoder;

    // P4: pipeline + shader caches（镜像官方 VulkanDevice）
    private final Map<RenderPipeline, Dx12CompiledRenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<ShaderCompilationKey, Dx12IntermediaryShaderModule> shaderCache = new HashMap<>();
    @Nullable
    private Dx12ShaderCompiler glslCompiler;
    /** 默认 ShaderSource（createDevice 传入的官方 shader 管理器）；precompile/getOrCompilePipeline 用。 */
    @Nullable
    private final ShaderSource defaultShaderSource;

    public Dx12Device(@Nullable ShaderSource defaultShaderSource) {
        this.defaultShaderSource = defaultShaderSource;
        String probe = Dx12Native.dx12CreateDevice();
        LOGGER.info("[dx12] Device probe + resource self-test: {}", probe);
        long frequency = Dx12Native.dx12GetTimestampFrequency();
        if (frequency == 0) {
            LOGGER.warn("[dx12] GetTimestampFrequency returned 0; timestampPeriod=1.0");
        }
        this.deviceInfo = buildDeviceInfo(parseAdapterName(probe), frequency);
        initialized = true;
    }

    // -----------------------------------------------------------------------
    // P2: real D3D12 resources
    // -----------------------------------------------------------------------

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV,
        FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new Dx12GpuSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage,
        GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new Dx12GpuTexture(usage, label == null ? "" : label.get(), format, width, height,
            depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage,
        GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new Dx12GpuTexture(usage, label == null ? "" : label, format, width, height,
            depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return this.createTextureView(texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new Dx12GpuTextureView(texture, baseMipLevel, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        return new Dx12GpuBuffer(usage, size);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage,
        ByteBuffer data) {
        // Mirror of the official VulkanDevice.createBuffer(data): allocate a buffer
        // with COPY_DST and upload the data through the command encoder.
        GpuBuffer buffer = this.createBuffer(label, usage | GpuBuffer.USAGE_COPY_DST, data.remaining());
        Dx12CommandEncoderBackend encoder = new Dx12CommandEncoderBackend();
        try {
            encoder.writeToBuffer(buffer.slice(), data);
            encoder.submit();
        } finally {
            encoder.close();
        }
        return buffer;
    }

    // -----------------------------------------------------------------------
    // P3+: stubbed until the render layer lands
    // -----------------------------------------------------------------------

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        if (hwnd == 0) {
            throw new IllegalStateException("glfwGetWin32Window returned null for window " + windowHandle);
        }
        return new Dx12GpuSurface(hwnd);
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        // 共享单例：全帧 blit/绘制/submit 落同一 CommandContext（官方语义）。
        Dx12CommandEncoderBackend encoder = this.sharedCommandEncoder;
        if (encoder == null) {
            encoder = new Dx12CommandEncoderBackend(this);
            this.sharedCommandEncoder = encoder;
        }
        return encoder;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline,
        @Nullable ShaderSource shaderSource) {
        ShaderSource source = shaderSource == null ? this.defaultShaderSource : shaderSource;
        return this.pipelineCache.computeIfAbsent(pipeline, ignored -> this.compilePipeline(pipeline, source));
    }

    /**
     * 取（或编译）管线，镜像官方 {@code VulkanDevice.getOrCompilePipeline}。
     * 渲染 pass setPipeline 时按需编译，使用 createDevice 传入的默认 shader 源。
     */
    public Dx12CompiledRenderPipeline getOrCompilePipeline(RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline,
            ignored -> this.compilePipeline(pipeline, this.defaultShaderSource));
    }

    @Override
    public void clearPipelineCache() {
        this.pipelineCache.values().forEach(Dx12CompiledRenderPipeline::close);
        this.pipelineCache.clear();
        this.shaderCache.values().forEach(Dx12IntermediaryShaderModule::close);
        this.shaderCache.clear();
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int size) {
        return new Dx12GpuQueryPool(size);
    }

    @Override
    public long getTimestampNow() {
        // The native call needs a command context; keep one lazily for timestamps.
        if (this.timestampCtx == 0) {
            this.timestampCtx = Dx12Native.dx12CreateCommandEncoder();
            if (this.timestampCtx == 0) {
                throw new IllegalStateException("dx12CreateCommandEncoder returned a null handle");
            }
        }
        return Dx12Native.dx12GetTimestampNow(this.timestampCtx);
    }

    @Override
    public List<String> getLastDebugMessages() {
        return List.of();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    @Override
    public void close() {
        // P12b 诊断：看门狗线程——若 close() 5 秒未完成，dump 全部线程栈到
        // %TEMP%\dx12-java.log（System.err 在 Worker shutdown 后不再写入
        // debug.log，卡死时只有独立文件能看到渲染线程卡在哪一行）。
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
            StringBuilder sb = new StringBuilder("[dx12-java] WATCHDOG: device.close() hung >5s\n");
            for (Map.Entry<Thread, StackTraceElement[]> en : Thread.getAllStackTraces().entrySet()) {
                Thread t = en.getKey();
                sb.append("Thread ").append(t.getName()).append(" [").append(t.getState()).append("]\n");
                for (StackTraceElement el : en.getValue()) {
                    sb.append("    at ").append(el).append('\n');
                }
            }
            appendJavaLog(sb.toString());
        }, "dx12-close-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        appendJavaLog("device.close: begin");
        // 镜像官方 VulkanDevice.close()：先销毁共享 encoder 再清管线缓存。
        // 注意：encoder.close() 对共享 encoder 不调用 dx12DestroyCommandEncoder
        // （避免 CubeMap.render() 内部 close 时意外销毁 ctx），此处显式销毁。
        Dx12CommandEncoderBackend encoder = this.sharedCommandEncoder;
        if (encoder != null) {
            encoder.close();
            Dx12Native.dx12DestroyCommandEncoder(encoder.nativeHandle());
            this.sharedCommandEncoder = null;
        }
        appendJavaLog("device.close: after sharedEncoder.close");
        this.clearPipelineCache();
        appendJavaLog("device.close: after clearPipelineCache");
        Dx12ShaderCompiler compiler = this.glslCompiler;
        if (compiler != null) {
            compiler.close();
            this.glslCompiler = null;
        }
        appendJavaLog("device.close: done");
        watchdog.interrupt();
    }

    /** P12b：关闭流程日志双写（System.err -> debug.log + 文件 %TEMP%\dx12-java.log）。 */
    private static void appendJavaLog(String msg) {
        System.err.println("[dx12-java] " + msg);
        System.err.flush();
        try {
            String path = System.getProperty("java.io.tmpdir");
            if (path == null) path = ".";
            java.io.FileWriter fw = new java.io.FileWriter(path + "\\dx12-java.log", true);
            fw.write("[dx12-java] " + msg + "\n");
            fw.close();
        } catch (Exception ignored) {
        }
    }

    // -----------------------------------------------------------------------
    // P4: pipeline + shader compilation（镜像官方 VulkanDevice）
    // -----------------------------------------------------------------------

    /**
     * 编译一条管线：取顶点/片元 shader（缓存）-> GLSL 编译 -> HLSL -> 打包
     * desc -> 原生层 D3DCompile + root signature + 双 PSO。任何失败都返回
     * handle=0 的无效管线（isValid()==false），镜像官方 compilePipeline。
     */
    private Dx12CompiledRenderPipeline compilePipeline(RenderPipeline pipeline,
        @Nullable ShaderSource shaderSource) {
        String pipeName = pipeline.getLocation().toString();
        Dx12IntermediaryShaderModule vertexShader = this.getOrCompileShader(
            pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
        Dx12IntermediaryShaderModule fragmentShader = this.getOrCompileShader(
            pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        if (vertexShader == Dx12IntermediaryShaderModule.INVALID) {
            LOGGER.error("[dx12-java] {} COMPILE FAILED (vertex): shader {} invalid",
                pipeName, pipeline.getVertexShader());
            return new Dx12CompiledRenderPipeline(pipeline, 0L, vertexShader, fragmentShader, "", "");
        }
        if (fragmentShader == Dx12IntermediaryShaderModule.INVALID) {
            LOGGER.error("[dx12-java] {} COMPILE FAILED (fragment): shader {} invalid",
                pipeName, pipeline.getFragmentShader());
            return new Dx12CompiledRenderPipeline(pipeline, 0L, vertexShader, fragmentShader, "", "");
        }
        try {
            Dx12CompiledShader compiled = this.getOrCreateCompiler()
                .compile(pipeline, vertexShader, fragmentShader);
            ByteBuffer desc = buildNativeDesc(compiled, pipeline);
            long handle = Dx12Native.dx12CreateGraphicsPipeline(desc);
            if (handle == 0) {
                LOGGER.error("[dx12-java] {} COMPILE FAILED (native): dx12CreateGraphicsPipeline returned 0",
                    pipeName);
                return new Dx12CompiledRenderPipeline(pipeline, 0L, vertexShader, fragmentShader, "", "");
            }
            LOGGER.info("[dx12-java] {} COMPILE OK (handle={})", pipeName, handle);
            return new Dx12CompiledRenderPipeline(pipeline, handle,
                vertexShader, fragmentShader, compiled.vertexHlsl(), compiled.fragmentHlsl());
        } catch (ShaderCompileException e) {
            LOGGER.error("[dx12-java] {} COMPILE FAILED (compile): {}", pipeName, e.getMessage());
            return new Dx12CompiledRenderPipeline(pipeline, 0L, vertexShader, fragmentShader, "", "");
        }
    }

    private Dx12IntermediaryShaderModule getOrCompileShader(Identifier id, ShaderType type,
        ShaderDefines defines, @Nullable ShaderSource shaderSource) {
        ShaderCompilationKey key = new ShaderCompilationKey(id, type, defines);
        return this.shaderCache.computeIfAbsent(key, ignored -> this.compileShader(key, shaderSource));
    }

    private Dx12IntermediaryShaderModule compileShader(ShaderCompilationKey key,
        @Nullable ShaderSource shaderSource) {
        if (shaderSource == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", key.type(), key.id());
            return Dx12IntermediaryShaderModule.INVALID;
        }
        String source = shaderSource.get(key.id(), key.type());
        if (source == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", key.type(), key.id());
            return Dx12IntermediaryShaderModule.INVALID;
        }
        String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines());
        try {
            return this.getOrCreateCompiler().createIntermediary(
                key.id().toDebugFileName(), sourceWithDefines, key.type());
        } catch (ShaderCompileException e) {
            LOGGER.error("Couldn't compile {} shader {}: {}", key.type(), key.id(), e.getMessage());
            return Dx12IntermediaryShaderModule.INVALID;
        }
    }

    private Dx12ShaderCompiler getOrCreateCompiler() {
        Dx12ShaderCompiler compiler = this.glslCompiler;
        if (compiler == null) {
            compiler = new Dx12ShaderCompiler();
            this.glslCompiler = compiler;
        }
        return compiler;
    }

    /**
     * 生成 D3D12 输入布局元素：仅包含顶点着色器实际声明的输入（与
     * {@link Dx12IntermediaryShaderModule#rebind} 的 attribLocation 分配
     * 完全一致），每元素 {location, binding, formatOrdinal, offset, stride, stepRate}。
     */
    private static List<int[]> buildVertexInputElements(RenderPipeline pipeline,
        List<String> vertexShaderInputs) {
        List<int[]> elements = new ArrayList<>();
        int attribLocation = 0;
        VertexFormat[] bindings = pipeline.getVertexFormatBindings();
        if (bindings == null || bindings.length == 0) {
            LOGGER.warn("[dx12-java] {}: formatBindings={} inputs={}",
                pipeline.getLocation(), bindings == null ? "null" : bindings.length, vertexShaderInputs);
            if (bindings != null) {
                for (int i = 0; i < bindings.length; i++) {
                    VertexFormat fmt = bindings[i];
                    if (fmt == null) continue;
                    StringBuilder sb = new StringBuilder();
                    for (VertexFormatElement e : fmt.getElements()) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(e.name()).append('(').append(e.format()).append(')');
                    }
                    LOGGER.warn("[dx12-java]   slot[{}] stride={} elements=[{}]", i,
                        fmt.getVertexSize(), sb);
                }
            }
        }
        for (int i = 0; i < bindings.length; i++) {
            VertexFormat format = bindings[i];
            if (format == null) continue;
            int stride = format.getVertexSize();
            int stepRate = format.getStepRate();
            for (VertexFormatElement element : format.getElements()) {
                if (!vertexShaderInputs.contains(element.name())) continue;
                int fmtOrd = element.format().ordinal();
                System.err.printf("[dx12-java] [DIAG] %s slot[%d] elem: name=%s format=%s ord=%d offset=%d stride=%d%n",
                    pipeline.getLocation(), i, element.name(), element.format(), fmtOrd, element.offset(), stride);
                elements.add(new int[] {
                    attribLocation, i, fmtOrd,
                    element.offset(), stride, stepRate
                });
                attribLocation++;
            }
        }
        return elements;
    }

    /**
     * 打包原生层 {@code dx12CreateGraphicsPipeline} 的 desc（little-endian）。
     * 布局见 {@link Dx12Native#dx12CreateGraphicsPipeline} Javadoc。
     */
    private static ByteBuffer buildNativeDesc(Dx12CompiledShader compiled, RenderPipeline pipeline) {
        byte[] vsBytes = compiled.vertexHlsl().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] psBytes = compiled.fragmentHlsl().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ColorTargetState[] colorTargets = pipeline.getColorTargetStates();
        int colorCount = colorTargets == null ? 0 : colorTargets.length;
        boolean hasDepth = pipeline.getDepthStencilState() != null;
        List<int[]> inputElements = buildVertexInputElements(pipeline, compiled.vertexShaderInputs());
        List<String> semanticNames = compiled.semanticNames();
        // 补齐：semanticNames 数量可能少于 inputElements（如 gui 管线 HLSL 只有 1 个输入，
        // 但顶点格式有 2 个元素）。多余的元素自动生成 TEXCOORDn 语义。
        while (semanticNames.size() < inputElements.size()) {
            semanticNames.add("TEXCOORD" + semanticNames.size());
        }
        // P21 diagnóstico: validar que semanticNames e inputElements estão alinhados após BUG-01 fix.
        System.err.println("[dx12-java] [DIAG] " + pipeline.getLocation()
            + " inputElems=" + inputElements.size()
            + " sems=" + java.util.Arrays.toString(semanticNames.toArray()));
        List<Dx12BindGroupEntry> entries = compiled.entries();

        // 计算 semantic names 总字节数
        int semanticCount = semanticNames.size();
        int semanticTotalBytes = 0;
        for (String sn : semanticNames) {
            semanticTotalBytes += sn.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }

        int size = 4 + vsBytes.length + 4 + psBytes.length;   // vsLen+vs + psLen+ps
        size += 4 + colorCount * 12;                          // colorCount + per color (int + 8 bytes)
        size += 1 + (hasDepth ? (4 + 2) : 0);                 // hasDepth + depth state
        size += 4 + 1 + 4;                                    // topology + cullEnabled + polygonMode
        size += 4 + inputElements.size() * (4 * 6);           // inputElementCount + per element
        size += 4 + semanticCount * 4 + semanticTotalBytes;   // semanticCount + per name (len+bytes)
        size += 4 + entries.size() * 2;                       // entryCount + per entry (type, register)

        ByteBuffer desc = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
        desc.putInt(vsBytes.length);
        desc.put(vsBytes);
        desc.putInt(psBytes.length);
        desc.put(psBytes);

        desc.putInt(colorCount);
        for (int i = 0; i < colorCount; i++) {
            writeColorTarget(desc, colorTargets[i]);
        }

        desc.put((byte) (hasDepth ? 1 : 0));
        if (hasDepth) {
            writeDepthState(desc, pipeline.getDepthStencilState());
        }

        desc.putInt(pipeline.getPrimitiveTopology().ordinal());
        desc.put((byte) (pipeline.isCull() ? 1 : 0));
        desc.putInt(pipeline.getPolygonMode().ordinal());

        desc.putInt(inputElements.size());
        for (int[] element : inputElements) {
            desc.putInt(element[0]);  // location
            desc.putInt(element[1]);  // binding
            desc.putInt(element[2]);  // format ordinal
            desc.putInt(element[3]);  // offset
            desc.putInt(element[4]);  // stride
            desc.putInt(element[5]);  // stepRate
        }

        desc.putInt(semanticCount);
        for (String sn : semanticNames) {
            byte[] snBytes = sn.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            desc.putInt(snBytes.length);
            desc.put(snBytes);
        }

        desc.putInt(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Dx12BindGroupEntry entry = entries.get(i);
            byte type;
            switch (entry.type()) {
                case UNIFORM_BUFFER: type = 0; break;   // CBV
                case SAMPLED_IMAGE:  type = 1; break;   // SRV + sampler
                case TEXEL_BUFFER:   type = 2; break;   // SRV (texel buffer)
                default:             type = 0; break;
            }
            desc.put(type);
            desc.put((byte) i);   // reg = 条目序号（与 HLSL shader register 一一对应）
        }
        desc.flip();
        return desc;
    }

    /**
     * 写入一个颜色目标（12 字节）。{@code null}（withUnusedColorTargetState）
     * 表示该槽位未使用：format=-1（DXGI_FORMAT_UNKNOWN）、无混合、无写入。
     * 布局：format(4) + writeMask(1) + blendEnabled(1) + blend[6]。
     */
    private static void writeColorTarget(ByteBuffer desc, @Nullable ColorTargetState state) {
        if (state == null) {
            desc.putInt(-1);
            desc.put((byte) 0);  // writeMask
            desc.put((byte) 0);  // blendEnabled
            desc.put((byte) 0);  // srcColor
            desc.put((byte) 0);  // dstColor
            desc.put((byte) 0);  // colorOp
            desc.put((byte) 0);  // srcAlpha
            desc.put((byte) 0);  // dstAlpha
            desc.put((byte) 0);  // alphaOp
            return;
        }
        desc.putInt(state.format().ordinal());
        desc.put((byte) state.writeMask());
        desc.put((byte) (state.blendFunction().isPresent() ? 1 : 0));
        if (state.blendFunction().isPresent()) {
            BlendFunction blend = state.blendFunction().get();
            desc.put((byte) blend.color().sourceFactor().ordinal());
            desc.put((byte) blend.color().destFactor().ordinal());
            desc.put((byte) blend.color().op().ordinal());
            desc.put((byte) blend.alpha().sourceFactor().ordinal());
            desc.put((byte) blend.alpha().destFactor().ordinal());
            desc.put((byte) blend.alpha().op().ordinal());
        } else {
            desc.put((byte) 0);
            desc.put((byte) 0);
            desc.put((byte) 0);
            desc.put((byte) 0);
            desc.put((byte) 0);
            desc.put((byte) 0);
        }
    }

    /** 写入深度模板状态：depthFormat + depthWrite + depthCompareOp（深度格式固定 D32_FLOAT）。 */
    private static void writeDepthState(ByteBuffer desc, DepthStencilState state) {
        desc.putInt(GpuFormat.D32_FLOAT.ordinal());
        desc.put((byte) (state.writeDepth() ? 1 : 0));
        desc.put((byte) state.depthTest().ordinal());
    }

    /** 着色器编译缓存键：id + 阶段 + defines（镜像官方 VulkanDevice.ShaderCompilationKey）。 */
    private record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String s = this.id + " (" + this.type + ")";
            return this.defines.isEmpty() ? s : s + " with " + this.defines;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String parseAdapterName(String probe) {
        if (probe == null || probe.isBlank()) {
            return "D3D12 adapter";
        }
        int sep = probe.indexOf(" (");
        return sep < 0 ? probe : probe.substring(0, sep);
    }

    private static DeviceInfo buildDeviceInfo(String adapterName, long timestampFrequency) {
        float timestampPeriod = timestampFrequency > 0 ? 1.0f / (float) timestampFrequency : 1.0f;
        return new DeviceInfo(
            adapterName,  // name
            "D3D12",      // vendorName
            "D3D12 driver",  // driverInfo
            true,         // isZZeroToOne: D3D12 NDC depth is 0..1
            "DX12",       // backendName
            timestampPeriod,
            new DeviceLimits(16, 256, 16384, Long.MAX_VALUE, 4096, 8),
            new DeviceFeatures(true, true, false, true, true, true, true),
            Set.of(),
            new HintsAndWorkarounds(false, false),
            DeviceType.DISCRETE  // heuristic; refine via DXGI adapter info
        );
    }
}
