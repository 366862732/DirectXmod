package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D3D11 {@link GpuDevice} for Minecraft 1.21.11.
 *
 * <p>All commands execute on the immediate context, so the command encoder is
 * effectively stateless.  Resources are tracked in a registry so a partial
 * shutdown still releases every native struct before the device itself is
 * destroyed.</p>
 */
final class Dx11Device implements GpuDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    private final long handle;
    private final String adapterName;
    private final String vendorName;
    private final String versionText;
    private final String rendererString;
    private final int maxTextureSize;
    private final Dx11ShaderCompiler shaderCompiler;
    private final Dx11CommandEncoder encoder;
    private final Set<Dx11Resource> resources =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<RenderPipeline, Dx11RenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<ShaderKey, Dx11IntermediaryShaderModule> shaderCache = new HashMap<>();
    private @Nullable ShaderSource defaultShaderSource;
    private boolean closed;
    private boolean vsync = true;
    private long frameCount;
    private long pipelineCompileCount;
    private long shaderCompileCount;

    private Dx11Device(long handle, String[] info, @Nullable ShaderSource defaultShaderSource) {
        this.handle = handle;
        this.adapterName = info[0];
        this.vendorName = info[1];
        this.versionText = info[2];
        this.maxTextureSize = Integer.parseInt(info[6]);
        this.rendererString = "D3D11 (" + shortFeatureLevel(this.versionText) + ") - " + this.adapterName;
        this.shaderCompiler = new Dx11ShaderCompiler();
        this.defaultShaderSource = defaultShaderSource;
        this.encoder = new Dx11CommandEncoder(this);
    }

    static Dx11Device create(long windowHandle, @Nullable ShaderSource shaderSource) {
        long handle = Dx11Native.nativeCreateDevice(nativeWindowHandle(windowHandle));
        if (handle == 0L) {
            throw new IllegalStateException("nativeCreateDevice returned 0");
        }
        String[] info = Dx11Native.nativeGetDeviceInfo(handle);
        if (info == null || info.length < 7) {
            Dx11Native.nativeDestroyDevice(handle);
            throw new IllegalStateException("nativeGetDeviceInfo returned no data");
        }
        LOGGER.info(
            "[dx11] Created device: {} ({}, {}), max texture {}",
            info[0], info[1], info[2], info[6]);
        if (info.length > 7) {
            LOGGER.info("[dx11] Device state: {}", info[7]);
        }
        return new Dx11Device(handle, info, shaderSource);
    }

    /** Directory for the temporary first-frame BMP dumps, or null when disabled. */
    static @Nullable String debugDumpDir() {
        String dir = System.getProperty("gl4dx11.debug.dumpDir");
        if (dir == null) {
            return null;
        }
        return dir.isEmpty() ? System.getProperty("user.dir") : dir;
    }

    /**
     * The renderer receives the GLFW window pointer, but DXGI needs the native
     * Win32 HWND.  They are not the same value on Windows.
     */
    private static long nativeWindowHandle(long windowHandle) {
        if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.WINDOWS) {
            return org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        }
        return windowHandle;
    }

    private static String shortFeatureLevel(String level) {
        return level.startsWith("D3D_FEATURE_LEVEL_")
            ? "FL" + level.substring("D3D_FEATURE_LEVEL_".length())
            : level;
    }

    long nativeHandle() {
        return this.handle;
    }

    boolean isClosed() {
        return this.closed;
    }

    void register(Dx11Resource resource) {
        if (this.closed) {
            resource.destroyNative();
            throw new IllegalStateException("Device is closed");
        }
        this.resources.add(resource);
    }

    void unregister(Dx11Resource resource) {
        this.resources.remove(resource);
    }

    void setVsync(boolean vsync) {
        this.vsync = vsync;
        if (!this.closed) {
            Dx11Native.nativeSetVsync(this.handle, vsync);
        }
    }

    boolean vsync() {
        return this.vsync;
    }

    /**
     * Presents the current back buffer.  The size comes from GLFW so this also
     * works on frames that did not call {@code presentTexture} (loading
     * screens).
     */
    void flipFrame(long windowHandle) {
        if (this.closed) {
            return;
        }
        int[] width = new int[1];
        int[] height = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(windowHandle, width, height);
        final long frame = ++this.frameCount;
        if (frame <= 5 || frame % 600 == 0) {
            LOGGER.info("[dx11] flipFrame #{} size={}x{} vsync={}",
                frame, width[0], height[0], this.vsync);
        }
        String dumpDir = debugDumpDir();
        if (frame == 1 && dumpDir != null) {
            try {
                Dx11Native.nativeDumpBackBuffer(this.handle, dumpDir + "/dx11-backbuffer.bmp");
                LOGGER.info("[dx11] Dumped back buffer to {}/dx11-backbuffer.bmp", dumpDir);
            } catch (RuntimeException e) {
                LOGGER.warn("[dx11] Could not dump the back buffer", e);
            }
        }
        if (!Dx11Native.nativePresent(this.handle, width[0], height[0])) {
            LOGGER.warn("[dx11] Present failed");
        } else if (frame <= 5) {
            LOGGER.info("[dx11] flipFrame #{} presented ({})",
                frame, Dx11Native.nativeGetPresentInfo(this.handle));
        }
    }

    Dx11CommandEncoder encoder() {
        return this.encoder;
    }

    Dx11RenderPipeline getOrCompilePipeline(RenderPipeline pipeline) {
        Dx11RenderPipeline cached = this.pipelineCache.get(pipeline);
        if (cached != null) {
            return cached;
        }
        final long index = ++this.pipelineCompileCount;
        final long start = System.nanoTime();
        if (index <= 40) {
            LOGGER.info("[dx11] Compiling pipeline #{} {}", index, pipeline.getLocation());
        }
        Dx11RenderPipeline compiled = this.compilePipeline(pipeline);
        this.pipelineCache.put(pipeline, compiled);
        if (index <= 40) {
            LOGGER.info(
                "[dx11] Compiled pipeline #{} {} in {} ms (valid={})",
                index,
                pipeline.getLocation(),
                (System.nanoTime() - start) / 1_000_000L,
                compiled.isValid());
        }
        return compiled;
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        return this.encoder;
    }

    @Override
    public GpuSampler createSampler(
        AddressMode addressModeU,
        AddressMode addressModeV,
        FilterMode minFilter,
        FilterMode magFilter,
        int maxAnisotropy,
        OptionalDouble maxLod) {
        return new Dx11Sampler(
            this, addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(
        @Nullable Supplier<String> label,
        int usage,
        TextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels) {
        return new Dx11Texture(
            this, label == null ? null : label.get(), usage, format,
            width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(
        @Nullable String label,
        int usage,
        TextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels) {
        return new Dx11Texture(
            this, label, usage, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return new Dx11TextureView(
            this, (Dx11Texture) texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new Dx11TextureView(
            this, (Dx11Texture) texture, baseMipLevel, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(
        @Nullable Supplier<String> label, int usage, long size) {
        return new Dx11Buffer(this, label == null ? null : label.get(), usage, size);
    }

    @Override
    public GpuBuffer createBuffer(
        @Nullable Supplier<String> label, int usage, ByteBuffer data) {
        return new Dx11Buffer(this, label == null ? null : label.get(), usage, data);
    }

    @Override
    public String getImplementationInformation() {
        return this.rendererString;
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
    public String getVendor() {
        return this.vendorName;
    }

    @Override
    public String getBackendName() {
        return "D3D11";
    }

    @Override
    public String getVersion() {
        return this.versionText;
    }

    @Override
    public String getRenderer() {
        return this.rendererString;
    }

    @Override
    public int getMaxTextureSize() {
        return this.maxTextureSize;
    }

    @Override
    public int getUniformOffsetAlignment() {
        // Matches DynamicUniformStorage's 256-byte slices.
        return 256;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(
        RenderPipeline pipeline, @Nullable ShaderSource shaderSource) {
        if (shaderSource != null) {
            this.defaultShaderSource = shaderSource;
        }
        return this.getOrCompilePipeline(pipeline);
    }

    @Override
    public void clearPipelineCache() {
        List<Dx11RenderPipeline> pipelines = new ArrayList<>(this.pipelineCache.values());
        this.pipelineCache.clear();
        for (Dx11RenderPipeline pipeline : pipelines) {
            pipeline.close();
        }
        for (Dx11IntermediaryShaderModule module : this.shaderCache.values()) {
            module.close();
        }
        this.shaderCache.clear();
    }

    @Override
    public List<String> getEnabledExtensions() {
        return List.of("D3D11", "SM4.0");
    }

    @Override
    public int getMaxSupportedAnisotropy() {
        return 16;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        List<Dx11Resource> snapshot = new ArrayList<>(this.resources);
        this.resources.clear();
        for (Dx11Resource resource : snapshot) {
            try {
                resource.destroyNative();
            } catch (Throwable t) {
                LOGGER.warn("[dx11] Failed to destroy resource", t);
            }
        }
        this.pipelineCache.clear();
        for (Dx11IntermediaryShaderModule module : this.shaderCache.values()) {
            module.close();
        }
        this.shaderCache.clear();
        this.shaderCompiler.close();
        Dx11Native.nativeDestroyDevice(this.handle);
        LOGGER.info("[dx11] Device closed");
    }

    // -----------------------------------------------------------------------
    // Pipeline compilation
    // -----------------------------------------------------------------------
    private Dx11RenderPipeline compilePipeline(RenderPipeline pipeline) {
        try {
            ShaderSource source = this.defaultShaderSource;
            if (source == null) {
                throw new Dx11ShaderCompileException("No ShaderSource available");
            }
            String vertexSource = source.get(pipeline.getVertexShader(), ShaderType.VERTEX);
            String fragmentSource = source.get(pipeline.getFragmentShader(), ShaderType.FRAGMENT);
            if (vertexSource == null || fragmentSource == null) {
                throw new Dx11ShaderCompileException(
                    "Missing shader source for " + pipeline.getLocation());
            }
            ShaderDefines defines = pipeline.getShaderDefines();
            Dx11IntermediaryShaderModule vertex = this.getOrCompileShader(
                pipeline.getVertexShader(), ShaderType.VERTEX, defines, vertexSource);
            Dx11IntermediaryShaderModule fragment = this.getOrCompileShader(
                pipeline.getFragmentShader(), ShaderType.FRAGMENT, defines, fragmentSource);
            Dx11CompiledShader compiled = this.shaderCompiler.compile(pipeline, vertex, fragment);
            return this.createNativePipeline(pipeline, compiled);
        } catch (Dx11ShaderCompileException | RuntimeException e) {
            LOGGER.error("[dx11] Couldn't compile pipeline {}", pipeline.getLocation(), e);
            return Dx11RenderPipeline.invalid(this, pipeline);
        }
    }

    private Dx11IntermediaryShaderModule getOrCompileShader(
        Identifier id, ShaderType type, ShaderDefines defines, String source)
        throws Dx11ShaderCompileException {
        ShaderKey key = new ShaderKey(id, type, defines);
        Dx11IntermediaryShaderModule cached = this.shaderCache.get(key);
        if (cached != null) {
            return cached;
        }
        final long index = ++this.shaderCompileCount;
        final long start = System.nanoTime();
        if (index <= 80) {
            LOGGER.info("[dx11] Compiling {} shader #{} {}", type, index, id);
        }
        Dx11IntermediaryShaderModule module = this.shaderCompiler.createIntermediary(
            id.toString(), source, type, defines);
        this.shaderCache.put(key, module);
        if (index <= 80) {
            LOGGER.info(
                "[dx11] Compiled {} shader #{} {} in {} ms",
                type,
                index,
                id,
                (System.nanoTime() - start) / 1_000_000L);
        }
        return module;
    }

    private Dx11RenderPipeline createNativePipeline(
        RenderPipeline pipeline, Dx11CompiledShader compiled) {
        VertexFormat format = pipeline.getVertexFormat();
        List<VertexFormatElement> elements = format.getElements();
        List<String> elementNames = format.getElementAttributeNames();
        List<String> vertexInputs = compiled.vertexInputNames();
        int elementCount = vertexInputs.size();
        String[] semantics = new String[elementCount];
        int[] semanticIndices = new int[elementCount];
        int[] formats = new int[elementCount];
        int[] offsets = new int[elementCount];
        for (int i = 0; i < elementCount; ++i) {
            String inputName = vertexInputs.get(i);
            int elementIndex = elementNames.indexOf(inputName);
            if (elementIndex < 0) {
                throw new IllegalStateException(
                    "Shader input " + inputName + " has no vertex format element in "
                        + pipeline.getLocation());
            }
            VertexFormatElement element = elements.get(elementIndex);
            semantics[i] = i == 0 ? "POSITION" : "TEXCOORD";
            semanticIndices[i] = i == 0 ? 0 : i - 1;
            formats[i] = dxgiFormat(element);
            offsets[i] = format.getOffset(element);
        }

        boolean blend = pipeline.getBlendFunction().isPresent();
        int srcColor = 2;  // D3D11_BLEND_ONE
        int dstColor = 1;  // D3D11_BLEND_ZERO
        int srcAlpha = 2;
        int dstAlpha = 1;
        if (blend) {
            var blendFunction = pipeline.getBlendFunction().get();
            srcColor = blendFactor(blendFunction.sourceColor(), false);
            dstColor = blendFactor(blendFunction.destColor(), false);
            srcAlpha = blendFactor(blendFunction.sourceAlpha(), true);
            dstAlpha = blendFactor(blendFunction.destAlpha(), true);
        }

        boolean depthTest = pipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST;
        int depthFunc = depthFunction(pipeline.getDepthTestFunction());
        boolean cull = pipeline.isCull();
        int fillMode = pipeline.getPolygonMode() == PolygonMode.WIREFRAME ? 2 : 3;
        int colorWriteMask = (pipeline.isWriteColor() ? 7 : 0)
            | (pipeline.isWriteAlpha() ? 8 : 0);

        long nativePipeline = Dx11Native.nativeCreatePipeline(
            this.handle,
            pipeline.getLocation().toString(),
            compiled.vertexHlsl(),
            compiled.fragmentHlsl(),
            format.getVertexSize(),
            topology(pipeline.getVertexFormatMode()),
            semantics,
            semanticIndices,
            formats,
            offsets,
            compiled.uniformSizes(),
            blend,
            srcColor,
            dstColor,
            srcAlpha,
            dstAlpha,
            depthTest,
            pipeline.isWriteDepth(),
            depthFunc,
            cull,
            fillMode,
            Math.round(pipeline.getDepthBiasConstant()),
            pipeline.getDepthBiasScaleFactor(),
            colorWriteMask);

        Map<String, Integer> uniformSlots = new HashMap<>();
        Map<String, Integer> textureSlots = new HashMap<>();
        Map<String, TextureFormat> texelFormats = new HashMap<>();
        for (Dx11CompiledShader.Binding binding : compiled.bindings()) {
            switch (binding.type()) {
                case UNIFORM_BUFFER -> uniformSlots.put(binding.name(), binding.slot());
                case SAMPLED_IMAGE, TEXEL_BUFFER -> {
                    textureSlots.put(binding.name(), binding.slot());
                    if (binding.texelFormat() != null) {
                        texelFormats.put(binding.name(), binding.texelFormat());
                    }
                }
            }
        }
        Dx11RenderPipeline result = new Dx11RenderPipeline(
            this, pipeline, nativePipeline, uniformSlots, textureSlots, texelFormats);
        this.register(result);
        return result;
    }

    private static int dxgiFormat(VertexFormatElement element) {
        // Values are DXGI_FORMAT enum constants.  The component type must match
        // the GLSL shader input: Color is normalized ubyte, Normal is signed
        // byte, UV1/UV2 are ivec2 (signed shorts) and so on.
        return switch (element.type()) {
            case FLOAT -> switch (element.count()) {
                case 1 -> 41;   // DXGI_FORMAT_R32_FLOAT
                case 2 -> 16;   // DXGI_FORMAT_R32G32_FLOAT
                case 3 -> 6;    // DXGI_FORMAT_R32G32B32_FLOAT
                default -> 2;   // DXGI_FORMAT_R32G32B32A32_FLOAT
            };
            case UBYTE -> 28;   // DXGI_FORMAT_R8G8B8A8_UNORM
            case BYTE -> 31;    // DXGI_FORMAT_R8G8B8A8_SNORM
            case USHORT -> 36;  // DXGI_FORMAT_R16G16_UINT
            case SHORT -> 38;   // DXGI_FORMAT_R16G16_SINT
            case UINT -> 42;    // DXGI_FORMAT_R32_UINT
            case INT -> 43;     // DXGI_FORMAT_R32_SINT
        };
    }

    private static int topology(VertexFormat.Mode mode) {
        return switch (mode) {
            case LINES, DEBUG_LINES -> 2;        // D3D11_PRIMITIVE_TOPOLOGY_LINELIST
            case DEBUG_LINE_STRIP -> 3;          // LINESTRIP
            case POINTS -> 1;                    // POINTLIST
            case TRIANGLES, QUADS -> 4;          // TRIANGLELIST
            case TRIANGLE_STRIP -> 5;            // TRIANGLESTRIP
            case TRIANGLE_FAN -> 6;              // TRIANGLEFAN (emulated natively)
        };
    }

    private static int depthFunction(DepthTestFunction function) {
        return switch (function) {
            case NO_DEPTH_TEST -> 8;             // ALWAYS
            case EQUAL_DEPTH_TEST -> 3;          // EQUAL
            case LEQUAL_DEPTH_TEST -> 4;         // LESS_EQUAL
            case LESS_DEPTH_TEST -> 2;           // LESS
            case GREATER_DEPTH_TEST -> 5;        // GREATER
        };
    }

    private static int blendFactor(SourceFactor factor, boolean alpha) {
        return blendFactor(factor.name(), alpha);
    }

    private static int blendFactor(DestFactor factor, boolean alpha) {
        return blendFactor(factor.name(), alpha);
    }

    private static int blendFactor(String name, boolean alpha) {
        return switch (name) {
            case "ZERO" -> 1;
            case "ONE" -> 2;
            // D3D11 rejects *_COLOR factors for SrcBlendAlpha/DestBlendAlpha.
            // GL uses the factor's alpha component there, which is exactly
            // SRC_ALPHA / INV_SRC_ALPHA / DST_ALPHA / INV_DST_ALPHA.
            case "SRC_COLOR" -> alpha ? 5 : 3;
            case "ONE_MINUS_SRC_COLOR" -> alpha ? 6 : 4;
            case "SRC_ALPHA" -> 5;
            case "ONE_MINUS_SRC_ALPHA" -> 6;
            case "DST_ALPHA" -> 7;
            case "ONE_MINUS_DST_ALPHA" -> 8;
            case "DST_COLOR" -> alpha ? 7 : 9;
            case "ONE_MINUS_DST_COLOR" -> alpha ? 8 : 10;
            // SRC_ALPHA_SATURATE is also invalid for alpha; GL's alpha
            // component of that factor is 1.
            case "SRC_ALPHA_SATURATE" -> alpha ? 2 : 11;
            case "CONSTANT_COLOR", "CONSTANT_ALPHA" -> 14;           // BLEND_FACTOR
            case "ONE_MINUS_CONSTANT_COLOR", "ONE_MINUS_CONSTANT_ALPHA" -> 15;
            default -> 2;
        };
    }

    private record ShaderKey(Identifier id, ShaderType type, ShaderDefines defines) {
    }
}
