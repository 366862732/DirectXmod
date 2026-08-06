/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.Plot
 *  com.mojang.jtracy.TracyClient
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class GlStateManager {
    private static final Plot PLOT_TEXTURES = TracyClient.createPlot((String)"GPU Textures");
    private static int numTextures = 0;
    private static final Plot PLOT_BUFFERS = TracyClient.createPlot((String)"GPU Buffers");
    private static int numBuffers = 0;
    private static final BlendState[] BLEND = new BlendState[8];
    private static final DepthState DEPTH = new DepthState();
    private static final CullState CULL = new CullState();
    private static final PolygonOffsetState POLY_OFFSET = new PolygonOffsetState();
    private static final ColorLogicState COLOR_LOGIC = new ColorLogicState();
    private static final ScissorState SCISSOR = new ScissorState();
    private static int activeTexture;
    private static final int TEXTURE_COUNT = 12;
    private static final TextureState[] TEXTURES;
    private static final @ColorTargetState.WriteMask int[] COLOR_MASK;
    private static int readFbo;
    private static int writeFbo;

    public static void _disableScissorTest() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.SCISSOR.mode.disable();
    }

    public static void _enableScissorTest() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.SCISSOR.mode.enable();
    }

    public static void _scissorBox(int x, int y, int width, int height) {
        RenderSystem.assertOnRenderThread();
        GL33C.glScissor((int)x, (int)y, (int)width, (int)height);
    }

    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.DEPTH.mode.disable();
    }

    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.DEPTH.mode.enable();
    }

    public static void _depthFunc(int func) {
        RenderSystem.assertOnRenderThread();
        if (func != GlStateManager.DEPTH.func) {
            GlStateManager.DEPTH.func = func;
            GL33C.glDepthFunc((int)func);
        }
    }

    public static void _depthMask(boolean mask) {
        RenderSystem.assertOnRenderThread();
        if (mask != GlStateManager.DEPTH.mask) {
            GlStateManager.DEPTH.mask = mask;
            GL33C.glDepthMask((boolean)mask);
        }
    }

    public static void _disableBlend(int index) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.BLEND[index].mode.disable();
    }

    public static void _enableBlend(int index) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.BLEND[index].mode.enable();
    }

    public static void _blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        RenderSystem.assertOnRenderThread();
        BlendState firstBlend = BLEND[0];
        if (srcRgb != firstBlend.srcRgb || dstRgb != firstBlend.dstRgb || srcAlpha != firstBlend.srcAlpha || dstAlpha != firstBlend.dstAlpha) {
            firstBlend.srcRgb = srcRgb;
            firstBlend.dstRgb = dstRgb;
            firstBlend.srcAlpha = srcAlpha;
            firstBlend.dstAlpha = dstAlpha;
            GlStateManager.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        }
    }

    public static void _blendEquationSeparate(int modeRgb, int modeAlpha) {
        RenderSystem.assertOnRenderThread();
        BlendState firstBlend = BLEND[0];
        if (modeRgb != firstBlend.modeRgb || modeAlpha != firstBlend.modeAlpha) {
            firstBlend.modeRgb = modeRgb;
            firstBlend.modeAlpha = modeAlpha;
            GlStateManager.glBlendEquationSeparate(modeRgb, modeAlpha);
        }
    }

    public static int glGetProgrami(int program, int pname) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetProgrami((int)program, (int)pname);
    }

    public static void glAttachShader(int program, int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glAttachShader((int)program, (int)shader);
    }

    public static void glDeleteShader(int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteShader((int)shader);
    }

    public static int glCreateShader(int type) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glCreateShader((int)type);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void glShaderSource(int shader, String source) {
        RenderSystem.assertOnRenderThread();
        byte[] encoded = source.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = MemoryUtil.memAlloc((int)(encoded.length + 1));
        buffer.put(encoded);
        buffer.put((byte)0);
        buffer.flip();
        try (MemoryStack stack = MemoryStack.stackPush();){
            PointerBuffer pointers = stack.mallocPointer(1);
            pointers.put(buffer);
            GL33C.nglShaderSource((int)shader, (int)1, (long)pointers.address0(), (long)0L);
        }
        finally {
            MemoryUtil.memFree((ByteBuffer)buffer);
        }
    }

    public static void glCompileShader(int shader) {
        RenderSystem.assertOnRenderThread();
        GL33C.glCompileShader((int)shader);
    }

    public static int glGetShaderi(int shader, int pname) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetShaderi((int)shader, (int)pname);
    }

    public static void _glUseProgram(int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUseProgram((int)program);
    }

    public static int glCreateProgram() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glCreateProgram();
    }

    public static void glDeleteProgram(int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteProgram((int)program);
    }

    public static void glLinkProgram(int program) {
        RenderSystem.assertOnRenderThread();
        GL33C.glLinkProgram((int)program);
    }

    public static int _glGetUniformLocation(int program, CharSequence name) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetUniformLocation((int)program, (CharSequence)name);
    }

    public static void _glUniform1i(int location, int v0) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUniform1i((int)location, (int)v0);
    }

    public static void _glBindAttribLocation(int program, int location, CharSequence name) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindAttribLocation((int)program, (int)location, (CharSequence)name);
    }

    static void incrementTrackedBuffers() {
        PLOT_BUFFERS.setValue((double)(++numBuffers));
    }

    public static int _glGenBuffers() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.incrementTrackedBuffers();
        return GL33C.glGenBuffers();
    }

    public static int _glGenVertexArrays() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGenVertexArrays();
    }

    public static void _glBindBuffer(int target, int buffer) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindBuffer((int)target, (int)buffer);
    }

    public static void _glBindVertexArray(int arrayId) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBindVertexArray((int)arrayId);
    }

    public static void _glBufferData(int target, ByteBuffer data, int usage) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferData((int)target, (ByteBuffer)data, (int)usage);
    }

    public static void _glBufferSubData(int target, long offset, ByteBuffer data) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferSubData((int)target, (long)offset, (ByteBuffer)data);
    }

    public static void _glBufferData(int target, long size, int usage) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBufferData((int)target, (long)size, (int)usage);
    }

    public static @Nullable ByteBuffer _glMapBufferRange(int target, long offset, long length, int access) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glMapBufferRange((int)target, (long)offset, (long)length, (int)access);
    }

    public static void _glUnmapBuffer(int target) {
        RenderSystem.assertOnRenderThread();
        GL33C.glUnmapBuffer((int)target);
    }

    public static void _glDeleteBuffers(int buffer) {
        RenderSystem.assertOnRenderThread();
        PLOT_BUFFERS.setValue((double)(--numBuffers));
        GL33C.glDeleteBuffers((int)buffer);
    }

    public static void _glBindFramebuffer(int target, int framebuffer) {
        if ((target == 36008 || target == 36160) && readFbo != framebuffer) {
            GL33C.glBindFramebuffer((int)36008, (int)framebuffer);
            readFbo = framebuffer;
        }
        if ((target == 36009 || target == 36160) && writeFbo != framebuffer) {
            GL33C.glBindFramebuffer((int)36009, (int)framebuffer);
            writeFbo = framebuffer;
        }
    }

    public static int getFrameBuffer(int target) {
        if (target == 36008) {
            return readFbo;
        }
        if (target == 36009) {
            return writeFbo;
        }
        return 0;
    }

    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlitFramebuffer((int)srcX0, (int)srcY0, (int)srcX1, (int)srcY1, (int)dstX0, (int)dstY0, (int)dstX1, (int)dstY1, (int)mask, (int)filter);
    }

    public static void _glDeleteFramebuffers(int framebuffer) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteFramebuffers((int)framebuffer);
        if (readFbo == framebuffer) {
            readFbo = 0;
        }
        if (writeFbo == framebuffer) {
            writeFbo = 0;
        }
    }

    public static int glGenFramebuffers() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGenFramebuffers();
    }

    public static void _glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        RenderSystem.assertOnRenderThread();
        GL33C.glFramebufferTexture2D((int)target, (int)attachment, (int)textarget, (int)texture, (int)level);
    }

    public static void glBlendFuncSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlendFuncSeparate((int)srcColor, (int)dstColor, (int)srcAlpha, (int)dstAlpha);
    }

    public static void glBlendEquationSeparate(int modeRgb, int modeAlpha) {
        RenderSystem.assertOnRenderThread();
        GL33C.glBlendEquationSeparate((int)modeRgb, (int)modeAlpha);
    }

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetShaderInfoLog((int)shader, (int)maxLength);
    }

    public static String glGetProgramInfoLog(int program, int maxLength) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetProgramInfoLog((int)program, (int)maxLength);
    }

    public static void _enableCull() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.CULL.enable.enable();
    }

    public static void _disableCull() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.CULL.enable.disable();
    }

    public static void _polygonMode(int face, int mode) {
        RenderSystem.assertOnRenderThread();
        GL33C.glPolygonMode((int)face, (int)mode);
    }

    public static void _enablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.POLY_OFFSET.fill.enable();
    }

    public static void _disablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.POLY_OFFSET.fill.disable();
    }

    public static void _polygonOffset(float factor, float units) {
        RenderSystem.assertOnRenderThread();
        if (factor != GlStateManager.POLY_OFFSET.factor || units != GlStateManager.POLY_OFFSET.units) {
            GlStateManager.POLY_OFFSET.factor = factor;
            GlStateManager.POLY_OFFSET.units = units;
            GL33C.glPolygonOffset((float)factor, (float)units);
        }
    }

    public static void _enableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.COLOR_LOGIC.enable.enable();
    }

    public static void _disableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.COLOR_LOGIC.enable.disable();
    }

    public static void _logicOp(int op) {
        RenderSystem.assertOnRenderThread();
        if (op != GlStateManager.COLOR_LOGIC.op) {
            GlStateManager.COLOR_LOGIC.op = op;
            GL33C.glLogicOp((int)op);
        }
    }

    public static void _activeTexture(int texture) {
        RenderSystem.assertOnRenderThread();
        if (activeTexture != texture - 33984) {
            activeTexture = texture - 33984;
            GL33C.glActiveTexture((int)texture);
        }
    }

    public static void _texParameter(int target, int name, int value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexParameteri((int)target, (int)name, (int)value);
    }

    public static int _getTexLevelParameter(int target, int level, int name) {
        return GL33C.glGetTexLevelParameteri((int)target, (int)level, (int)name);
    }

    public static int _genTexture() {
        RenderSystem.assertOnRenderThread();
        PLOT_TEXTURES.setValue((double)(++numTextures));
        return GL33C.glGenTextures();
    }

    public static void _deleteTexture(int id) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteTextures((int)id);
        for (TextureState state : TEXTURES) {
            if (state.binding != id) continue;
            state.binding = -1;
        }
        PLOT_TEXTURES.setValue((double)(--numTextures));
    }

    public static void _bindTexture(int id) {
        RenderSystem.assertOnRenderThread();
        if (id != GlStateManager.TEXTURES[GlStateManager.activeTexture].binding) {
            GlStateManager.TEXTURES[GlStateManager.activeTexture].binding = id;
            GL33C.glBindTexture((int)3553, (int)id);
        }
    }

    public static void _texImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexImage2D((int)target, (int)level, (int)internalformat, (int)width, (int)height, (int)border, (int)format, (int)type, (ByteBuffer)pixels);
    }

    public static void _texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexSubImage2D((int)target, (int)level, (int)xoffset, (int)yoffset, (int)width, (int)height, (int)format, (int)type, (long)pixels);
    }

    public static void _texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer pixels) {
        RenderSystem.assertOnRenderThread();
        GL33C.glTexSubImage2D((int)target, (int)level, (int)xoffset, (int)yoffset, (int)width, (int)height, (int)format, (int)type, (ByteBuffer)pixels);
    }

    public static void _viewport(int x, int y, int width, int height) {
        GL33C.glViewport((int)x, (int)y, (int)width, (int)height);
    }

    public static void _colorMask(@ColorTargetState.WriteMask int writeMask) {
        RenderSystem.assertOnRenderThread();
        for (int i = 0; i < COLOR_MASK.length; ++i) {
            if (writeMask == COLOR_MASK[i]) continue;
            GlStateManager.COLOR_MASK[i] = writeMask;
            GL33C.glColorMaski((int)i, ((writeMask & 1) != 0 ? 1 : 0) != 0, ((writeMask & 2) != 0 ? 1 : 0) != 0, ((writeMask & 4) != 0 ? 1 : 0) != 0, ((writeMask & 8) != 0 ? 1 : 0) != 0);
        }
    }

    public static void _colorMask(int index, @ColorTargetState.WriteMask int writeMask) {
        RenderSystem.assertOnRenderThread();
        if (writeMask != COLOR_MASK[index]) {
            GlStateManager.COLOR_MASK[index] = writeMask;
            GL33C.glColorMaski((int)index, ((writeMask & 1) != 0 ? 1 : 0) != 0, ((writeMask & 2) != 0 ? 1 : 0) != 0, ((writeMask & 4) != 0 ? 1 : 0) != 0, ((writeMask & 8) != 0 ? 1 : 0) != 0);
        }
    }

    public static void _clear(int mask) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClear((int)mask);
        if (MacosUtil.IS_MACOS) {
            GlStateManager._getError();
        }
    }

    public static void _clearBuffer(int index, Vector4fc clearColor) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClearBufferfv((int)6144, (int)index, (float[])new float[]{clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w()});
        if (MacosUtil.IS_MACOS) {
            GlStateManager._getError();
        }
    }

    public static void _clearBuffer(double clearDepth) {
        RenderSystem.assertOnRenderThread();
        GL33C.glClearBufferfv((int)6145, (int)0, (float[])new float[]{(float)clearDepth});
        if (MacosUtil.IS_MACOS) {
            GlStateManager._getError();
        }
    }

    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glVertexAttribPointer((int)index, (int)size, (int)type, (boolean)normalized, (int)stride, (long)value);
    }

    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glVertexAttribIPointer((int)index, (int)size, (int)type, (int)stride, (long)value);
    }

    public static void _enableVertexAttribArray(int index) {
        RenderSystem.assertOnRenderThread();
        GL33C.glEnableVertexAttribArray((int)index);
    }

    public static void _drawElements(int mode, int count, int type, long indices) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDrawElements((int)mode, (int)count, (int)type, (long)indices);
    }

    public static void _drawArrays(int mode, int first, int count) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDrawArrays((int)mode, (int)first, (int)count);
    }

    public static void _pixelStore(int name, int value) {
        RenderSystem.assertOnRenderThread();
        GL33C.glPixelStorei((int)name, (int)value);
    }

    public static void _readPixels(int x, int y, int width, int height, int format, int type, long pixels) {
        RenderSystem.assertOnRenderThread();
        GL33C.glReadPixels((int)x, (int)y, (int)width, (int)height, (int)format, (int)type, (long)pixels);
    }

    public static int _getError() {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetError();
    }

    public static void clearGlErrors() {
        RenderSystem.assertOnRenderThread();
        while (GL33C.glGetError() != 0) {
        }
    }

    public static String _getString(int id) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetString((int)id);
    }

    public static int _getInteger(int name) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glGetInteger((int)name);
    }

    public static long _glFenceSync(int condition, int flags) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glFenceSync((int)condition, (int)flags);
    }

    public static int _glClientWaitSync(long sync, int flags, long timeout) {
        RenderSystem.assertOnRenderThread();
        return GL33C.glClientWaitSync((long)sync, (int)flags, (long)timeout);
    }

    public static void _glDeleteSync(long sync) {
        RenderSystem.assertOnRenderThread();
        GL33C.glDeleteSync((long)sync);
    }

    static {
        TEXTURES = (TextureState[])IntStream.range(0, 12).mapToObj(i -> new TextureState()).toArray(TextureState[]::new);
        COLOR_MASK = new int[8];
        Arrays.setAll(COLOR_MASK, n -> 15);
        Arrays.setAll(BLEND, n -> new BlendState());
    }

    @Environment(value=EnvType.CLIENT)
    private static class ScissorState {
        public final BooleanState mode = new BooleanState(3089);

        private ScissorState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class BooleanState {
        private final int state;
        private boolean enabled;

        public BooleanState(int state) {
            this.state = state;
        }

        public void disable() {
            this.setEnabled(false);
        }

        public void enable() {
            this.setEnabled(true);
        }

        public void setEnabled(boolean enabled) {
            RenderSystem.assertOnRenderThread();
            if (enabled != this.enabled) {
                this.enabled = enabled;
                if (enabled) {
                    GL33C.glEnable((int)this.state);
                } else {
                    GL33C.glDisable((int)this.state);
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class DepthState {
        public final BooleanState mode = new BooleanState(2929);
        public boolean mask = true;
        public int func = 513;

        private DepthState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class BlendState {
        public final BooleanState mode = new BooleanState(3042);
        public int srcRgb = 1;
        public int dstRgb = 0;
        public int modeRgb = 32774;
        public int srcAlpha = 1;
        public int dstAlpha = 0;
        public int modeAlpha = 32774;

        private BlendState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class CullState {
        public final BooleanState enable = new BooleanState(2884);

        private CullState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class PolygonOffsetState {
        public final BooleanState fill = new BooleanState(32823);
        public float factor;
        public float units;

        private PolygonOffsetState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ColorLogicState {
        public final BooleanState enable = new BooleanState(3058);
        public int op = 5379;

        private ColorLogicState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class TextureState {
        public int binding;

        private TextureState() {
        }
    }
}

