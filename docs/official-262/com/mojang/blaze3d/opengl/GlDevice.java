/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.opengl.ARBClipControl
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.opengl.GLCapabilities
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.BufferStorage;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDebug;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlHeuristics;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlQueryPool;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlSurface;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.opengl.VertexArrayCache;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
class GlDevice
implements GpuDeviceBackend {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static boolean USE_GL_ARB_vertex_attrib_binding = true;
    protected static boolean USE_GL_KHR_debug = true;
    protected static boolean USE_GL_EXT_debug_label = true;
    protected static boolean USE_GL_ARB_debug_output = true;
    protected static boolean USE_GL_ARB_direct_state_access = true;
    protected static boolean USE_GL_ARB_buffer_storage = true;
    protected static boolean USE_GL_ARB_base_instance = true;
    protected static boolean USE_GL_ARB_draw_indirect = true;
    protected static boolean USE_GL_ARB_multi_draw_indirect = true;
    protected static boolean USE_GL_ARB_shader_draw_parameters = true;
    private final GlCommandEncoder encoder;
    private final @Nullable GlDebug debugLog;
    private final GlDebugLabel debugLabels;
    private final DirectStateAccess directStateAccess;
    private final ShaderSource defaultShaderSource;
    private final Map<RenderPipeline, GlRenderPipeline> pipelineCache = new IdentityHashMap<RenderPipeline, GlRenderPipeline>();
    private final Map<ShaderCompilationKey, GlShaderModule> shaderCache = new HashMap<ShaderCompilationKey, GlShaderModule>();
    private final FrameBufferCache frameBufferCache = new FrameBufferCache();
    private final VertexArrayCache vertexArrayCache;
    private final BufferStorage bufferStorage;
    private final DeviceInfo deviceInfo;

    public GlDevice(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions) {
        int maxSupportedAnisotropy;
        GLFW.glfwMakeContextCurrent((long)windowHandle);
        GLCapabilities capabilities = GL.createCapabilities();
        HashSet<String> enabledExtensions = new HashSet<String>();
        if (capabilities.GL_EXT_texture_filter_anisotropic) {
            maxSupportedAnisotropy = Mth.floor(GL33C.glGetFloat((int)34047));
            enabledExtensions.add("GL_EXT_texture_filter_anisotropic");
        } else {
            maxSupportedAnisotropy = 1;
        }
        GlHeuristics heuristics = new GlHeuristics(GlStateManager._getString(7937));
        this.debugLog = GlDebug.enableDebugCallback(debugOptions.logLevel(), debugOptions.synchronousLogs(), enabledExtensions);
        this.debugLabels = GlDebugLabel.create(capabilities, debugOptions.useLabels(), enabledExtensions);
        this.vertexArrayCache = VertexArrayCache.create(capabilities, this.debugLabels, enabledExtensions);
        this.bufferStorage = BufferStorage.create(capabilities, enabledExtensions);
        this.directStateAccess = DirectStateAccess.create(capabilities, enabledExtensions, heuristics);
        this.defaultShaderSource = defaultShaderSource;
        GL33C.glEnable((int)34895);
        GL33C.glEnable((int)34370);
        if (capabilities.GL_ARB_clip_control) {
            ARBClipControl.glClipControl((int)36001, (int)37727);
            enabledExtensions.add("GL_ARB_clip_control");
        }
        if (capabilities.GL_ARB_shader_draw_parameters && USE_GL_ARB_shader_draw_parameters) {
            enabledExtensions.add("GL_ARB_shader_draw_parameters");
        }
        if (capabilities.GL_ARB_draw_indirect && USE_GL_ARB_draw_indirect) {
            enabledExtensions.add("GL_ARB_draw_indirect");
            if (capabilities.GL_ARB_multi_draw_indirect && USE_GL_ARB_multi_draw_indirect) {
                enabledExtensions.add("GL_ARB_multi_draw_indirect");
            }
        }
        if (capabilities.GL_ARB_base_instance && USE_GL_ARB_base_instance) {
            enabledExtensions.add("GL_ARB_base_instance");
        }
        this.deviceInfo = heuristics.createDeviceInfo(capabilities, maxSupportedAnisotropy, enabledExtensions);
        this.encoder = new GlCommandEncoder(this);
    }

    public GlDebugLabel debugLabels() {
        return this.debugLabels;
    }

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        return new GlSurface(windowHandle);
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        return this.encoder;
    }

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new GlSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return this.createTexture(this.debugLabels.exists() && label != null ? label.get() : null, usage, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        int error;
        int target;
        boolean isCubemap;
        GlStateManager.clearGlErrors();
        int id = GlStateManager._genTexture();
        if (label == null) {
            label = String.valueOf(id);
        }
        boolean bl = isCubemap = (usage & 0x10) != 0;
        if (isCubemap) {
            GL33C.glBindTexture((int)34067, (int)id);
            target = 34067;
        } else {
            GlStateManager._bindTexture(id);
            target = 3553;
        }
        GlStateManager._texParameter(target, 33085, mipLevels - 1);
        GlStateManager._texParameter(target, 33082, 0);
        GlStateManager._texParameter(target, 33083, mipLevels - 1);
        if (format.hasDepthAspect()) {
            GlStateManager._texParameter(target, 34892, 0);
        }
        int glInternalID = GlConst.toGlInternalId(format);
        int glExternalID = GlConst.toGlExternalId(format);
        int glType = GlConst.toGlType(format);
        if (glInternalID == 0 || glExternalID == 0 || glType == 0) {
            throw new IllegalArgumentException(String.valueOf((Object)format) + " format cannot be used to create textures");
        }
        if (isCubemap) {
            for (int cubeTarget : GlConst.CUBEMAP_TARGETS) {
                for (int i = 0; i < mipLevels; ++i) {
                    GlStateManager._texImage2D(cubeTarget, i, glInternalID, width >> i, height >> i, 0, glExternalID, glType, null);
                }
            }
        } else {
            for (int i = 0; i < mipLevels; ++i) {
                GlStateManager._texImage2D(target, i, glInternalID, width >> i, height >> i, 0, glExternalID, glType, null);
            }
        }
        if ((error = GlStateManager._getError()) == 1285) {
            throw new GpuOutOfMemoryException("Could not allocate texture of " + width + "x" + height + " for " + label);
        }
        if (error != 0) {
            throw new IllegalStateException("OpenGL error " + error);
        }
        GlTexture texture = new GlTexture(usage, label, format, width, height, depthOrLayers, mipLevels, id, this.frameBufferCache);
        this.debugLabels.applyLabel(texture);
        return texture;
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return this.createTextureView(texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new GlTextureView((GlTexture)texture, baseMipLevel, mipLevels, this.frameBufferCache);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        GlStateManager.clearGlErrors();
        GlBuffer buffer = this.bufferStorage.createBuffer(this.directStateAccess, usage, size);
        int error = GlStateManager._getError();
        if (error == 1285) {
            throw new GpuOutOfMemoryException("Could not allocate buffer of " + size + " for " + String.valueOf(label));
        }
        if (error != 0) {
            throw new IllegalStateException("OpenGL error " + error);
        }
        this.debugLabels.applyLabel(buffer, label);
        return buffer;
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
        GlStateManager.clearGlErrors();
        long size = data.remaining();
        GlBuffer buffer = this.bufferStorage.createBuffer(this.directStateAccess, usage, data);
        int error = GlStateManager._getError();
        if (error == 1285) {
            throw new GpuOutOfMemoryException("Could not allocate buffer of " + size + " for " + String.valueOf(label));
        }
        if (error != 0) {
            throw new IllegalStateException("OpenGL error " + error);
        }
        this.debugLabels.applyLabel(buffer, label);
        return buffer;
    }

    @Override
    public List<String> getLastDebugMessages() {
        return this.debugLog == null ? Collections.emptyList() : this.debugLog.getLastOpenGlDebugMessages();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return this.debugLog != null;
    }

    @Override
    public void clearPipelineCache() {
        for (GlRenderPipeline pipeline : this.pipelineCache.values()) {
            if (pipeline.program() == GlProgram.INVALID_PROGRAM) continue;
            pipeline.program().close();
        }
        this.pipelineCache.clear();
        for (GlShaderModule shader : this.shaderCache.values()) {
            if (shader == GlShaderModule.INVALID_SHADER) continue;
            shader.close();
        }
        this.shaderCache.clear();
        String glRenderer = GlStateManager._getString(7937);
        if (glRenderer.contains("AMD")) {
            GlDevice.sacrificeShaderToOpenGlAndAmd();
        }
    }

    private static void sacrificeShaderToOpenGlAndAmd() {
        int shader = GlStateManager.glCreateShader(35633);
        int program = GlStateManager.glCreateProgram();
        GlStateManager.glAttachShader(program, shader);
        GlStateManager.glDeleteShader(shader);
        GlStateManager.glDeleteProgram(program);
    }

    @Override
    public void close() {
        this.clearPipelineCache();
        this.encoder.close();
    }

    public DirectStateAccess directStateAccess() {
        return this.directStateAccess;
    }

    protected GlRenderPipeline getOrCompilePipeline(RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline, p -> this.compilePipeline((RenderPipeline)p, this.defaultShaderSource));
    }

    protected GlShaderModule getOrCompileShader(Identifier id, ShaderType type, ShaderDefines defines, ShaderSource shaderSource) {
        ShaderCompilationKey key = new ShaderCompilationKey(id, type, defines);
        return this.shaderCache.computeIfAbsent(key, k -> this.compileShader((ShaderCompilationKey)k, shaderSource));
    }

    @Override
    public GlRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource customShaderSource) {
        ShaderSource shaderSource = customShaderSource == null ? this.defaultShaderSource : customShaderSource;
        return this.pipelineCache.computeIfAbsent(pipeline, p -> this.compilePipeline((RenderPipeline)p, shaderSource));
    }

    private GlShaderModule compileShader(ShaderCompilationKey key, ShaderSource shaderSource) {
        String source = shaderSource.get(key.id, key.type);
        if (source == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", (Object)key.type, (Object)key.id);
            return GlShaderModule.INVALID_SHADER;
        }
        String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines);
        int shaderId = GlStateManager.glCreateShader(GlConst.toGl(key.type));
        GlStateManager.glShaderSource(shaderId, sourceWithDefines);
        GlStateManager.glCompileShader(shaderId);
        if (GlStateManager.glGetShaderi(shaderId, 35713) == 0) {
            String logInfo = StringUtils.trim((String)GlStateManager.glGetShaderInfoLog(shaderId, 32768));
            LOGGER.error("Couldn't compile {} shader ({}): {}", new Object[]{key.type.getName(), key.id, logInfo});
            return GlShaderModule.INVALID_SHADER;
        }
        GlShaderModule module = new GlShaderModule(shaderId, key.id, key.type);
        this.debugLabels.applyLabel(module);
        return module;
    }

    private GlProgram compileProgram(RenderPipeline pipeline, ShaderSource shaderSource) {
        GlShaderModule vertexShader = this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
        GlShaderModule fragmentShader = this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        if (vertexShader == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", (Object)pipeline.getLocation(), (Object)pipeline.getVertexShader());
            return GlProgram.INVALID_PROGRAM;
        }
        if (fragmentShader == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", (Object)pipeline.getLocation(), (Object)pipeline.getFragmentShader());
            return GlProgram.INVALID_PROGRAM;
        }
        try {
            GlProgram compiled = GlProgram.link(vertexShader, fragmentShader, pipeline.getVertexFormatBindings(), pipeline.getLocation().toString());
            compiled.setupBindGroupLayouts(pipeline.getBindGroupLayouts());
            this.debugLabels.applyLabel(compiled);
            return compiled;
        }
        catch (IllegalArgumentException e) {
            LOGGER.error("Couldn't compile program for pipeline {}: {}", (Object)pipeline.getLocation(), (Object)e.getMessage());
            return GlProgram.INVALID_PROGRAM;
        }
        catch (ShaderManager.CompilationException e) {
            LOGGER.error("Couldn't compile program for pipeline {}: {}", (Object)pipeline.getLocation(), (Object)e);
            return GlProgram.INVALID_PROGRAM;
        }
    }

    private GlRenderPipeline compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource) {
        return new GlRenderPipeline(pipeline, this.compileProgram(pipeline, shaderSource));
    }

    public VertexArrayCache vertexArrayCache() {
        return this.vertexArrayCache;
    }

    public BufferStorage getBufferStorage() {
        return this.bufferStorage;
    }

    public FrameBufferCache frameBufferCache() {
        return this.frameBufferCache;
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int size) {
        return new GlQueryPool(size);
    }

    @Override
    public long getTimestampNow() {
        return GL33C.glGetInteger64((int)36392);
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    @Environment(value=EnvType.CLIENT)
    private record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String string = String.valueOf(this.id) + " (" + String.valueOf((Object)this.type) + ")";
            if (!this.defines.isEmpty()) {
                return string + " with " + String.valueOf(this.defines);
            }
            return string;
        }
    }
}

