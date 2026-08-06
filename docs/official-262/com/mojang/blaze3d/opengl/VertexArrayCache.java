/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.opengl.ARBVertexAttribBinding
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.opengl.GLCapabilities
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

@Environment(value=EnvType.CLIENT)
public abstract class VertexArrayCache {
    public static VertexArrayCache create(GLCapabilities capabilities, GlDebugLabel debugLabels, Set<String> enabledExtensions) {
        if (capabilities.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
            enabledExtensions.add("GL_ARB_vertex_attrib_binding");
            return new Separate(debugLabels);
        }
        return new Emulated(debugLabels);
    }

    public abstract VertexArray bindVertexArray(@Nullable VertexFormat[] var1, @Nullable GpuBufferSlice[] var2, @Nullable VertexArray var3);

    @Environment(value=EnvType.CLIENT)
    private static class Separate
    extends VertexArrayCache {
        private final Map<List<@Nullable VertexFormat>, VertexArray> cache = new HashMap<List<VertexFormat>, VertexArray>();
        private final GlDebugLabel debugLabels;
        private final boolean needsMesaWorkaround;

        public Separate(GlDebugLabel debugLabels) {
            String version;
            this.debugLabels = debugLabels;
            this.needsMesaWorkaround = "Mesa".equals(GlStateManager._getString(7936)) ? (version = GlStateManager._getString(7938)).contains("25.0.0") || version.contains("25.0.1") || version.contains("25.0.2") : false;
        }

        @Override
        public VertexArray bindVertexArray(@Nullable VertexFormat[] vertexBindings, @Nullable GpuBufferSlice[] vertexBuffers, @Nullable VertexArray lastBoundVertexArray) {
            List<@Nullable VertexFormat> listBindings = Arrays.asList(vertexBindings);
            VertexArray vertexArray = this.cache.get(listBindings);
            if (vertexArray == null) {
                int i;
                int id = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(id);
                int attribLocation = 0;
                for (i = 0; i < vertexBindings.length; ++i) {
                    VertexFormat vertexBinding = vertexBindings[i];
                    if (vertexBinding == null) continue;
                    for (VertexFormatElement element : vertexBinding.getElements()) {
                        if (element == null) continue;
                        GlStateManager._enableVertexAttribArray(attribLocation);
                        int glExternalId = GlConst.toGlExternalId(element.format());
                        int glType = GlConst.toGlType(element.format());
                        boolean isIntegerFormat = GlConst.isGlFormatInteger(glExternalId);
                        boolean isNormalizedFormat = GlConst.isFormatNormalized(element.format());
                        int channelCount = GlConst.glFormatChannelCount(glExternalId);
                        if (isIntegerFormat) {
                            ARBVertexAttribBinding.glVertexAttribIFormat((int)attribLocation, (int)channelCount, (int)glType, (int)element.offset());
                        } else {
                            ARBVertexAttribBinding.glVertexAttribFormat((int)attribLocation, (int)channelCount, (int)glType, (boolean)isNormalizedFormat, (int)element.offset());
                        }
                        ARBVertexAttribBinding.glVertexAttribBinding((int)attribLocation, (int)i);
                        ++attribLocation;
                    }
                    ARBVertexAttribBinding.glVertexBindingDivisor((int)i, (int)vertexBinding.getStepRate());
                }
                for (i = 0; i < vertexBuffers.length; ++i) {
                    GpuBufferSlice vertexBufferSlice = vertexBuffers[i];
                    if (vertexBufferSlice == null) continue;
                    GlBuffer vertexBuffer = (GlBuffer)vertexBufferSlice.buffer();
                    ARBVertexAttribBinding.glBindVertexBuffer((int)i, (int)vertexBuffer.handle(), (long)vertexBufferSlice.offset(), (int)vertexBindings[i].getVertexSize());
                }
                VertexArray vao = new VertexArray(id, vertexBindings);
                this.debugLabels.applyLabel(vao);
                this.cache.put(listBindings, vao);
                return vao;
            }
            GlStateManager._glBindVertexArray(vertexArray.id);
            if (vertexArray != lastBoundVertexArray) {
                for (int i = 0; i < vertexBuffers.length; ++i) {
                    GpuBufferSlice vertexBufferSlice = vertexBuffers[i];
                    if (vertexBufferSlice == null) continue;
                    GlBuffer vertexBuffer = (GlBuffer)vertexBufferSlice.buffer();
                    if (this.needsMesaWorkaround) {
                        ARBVertexAttribBinding.glBindVertexBuffer((int)i, (int)0, (long)0L, (int)0);
                    }
                    ARBVertexAttribBinding.glBindVertexBuffer((int)i, (int)vertexBuffer.handle(), (long)vertexBufferSlice.offset(), (int)vertexBindings[i].getVertexSize());
                }
            }
            return vertexArray;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Emulated
    extends VertexArrayCache {
        private final Map<List<@Nullable VertexFormat>, VertexArray> cache = new HashMap<List<VertexFormat>, VertexArray>();
        private final GlDebugLabel debugLabels;

        public Emulated(GlDebugLabel debugLabels) {
            this.debugLabels = debugLabels;
        }

        @Override
        public VertexArray bindVertexArray(@Nullable VertexFormat[] vertexBindings, @Nullable GpuBufferSlice[] vertexBuffers, @Nullable VertexArray lastBoundVertexArray) {
            List<@Nullable VertexFormat> listBindings = Arrays.asList(vertexBindings);
            VertexArray vertexArray = this.cache.get(listBindings);
            if (vertexArray == null) {
                int id = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(id);
                Emulated.setupCombinedAttributes(vertexBindings, true, vertexBuffers);
                VertexArray vao = new VertexArray(id, vertexBindings);
                this.debugLabels.applyLabel(vao);
                this.cache.put(listBindings, vao);
                return vao;
            }
            GlStateManager._glBindVertexArray(vertexArray.id);
            if (vertexArray != lastBoundVertexArray) {
                Emulated.setupCombinedAttributes(vertexBindings, false, vertexBuffers);
            }
            return vertexArray;
        }

        private static void setupCombinedAttributes(@Nullable VertexFormat[] vertexBindings, boolean enable, @Nullable GpuBufferSlice[] vertexBuffers) {
            int attributeIndex = 0;
            for (int i = 0; i < vertexBindings.length; ++i) {
                VertexFormat vertexBinding = vertexBindings[i];
                if (vertexBinding == null) continue;
                GlBuffer buffer = (GlBuffer)vertexBuffers[i].buffer();
                GlStateManager._glBindBuffer(34962, buffer.handle());
                int vertexSize = vertexBinding.getVertexSize();
                for (VertexFormatElement element : vertexBinding.getElements()) {
                    long totalOffset = vertexBuffers[i].offset() + (long)element.offset();
                    int glExternalId = GlConst.toGlExternalId(element.format());
                    int glType = GlConst.toGlType(element.format());
                    boolean isIntegerFormat = GlConst.isGlFormatInteger(glExternalId);
                    boolean isNormalizedFormat = GlConst.isFormatNormalized(element.format());
                    int channelCount = GlConst.glFormatChannelCount(glExternalId);
                    if (enable) {
                        GlStateManager._enableVertexAttribArray(attributeIndex);
                    }
                    if (isIntegerFormat) {
                        GlStateManager._vertexAttribIPointer(attributeIndex, channelCount, glType, vertexSize, totalOffset);
                    } else {
                        GlStateManager._vertexAttribPointer(attributeIndex, channelCount, glType, isNormalizedFormat, vertexSize, totalOffset);
                    }
                    GL33C.glVertexAttribDivisor((int)attributeIndex, (int)vertexBinding.getStepRate());
                    ++attributeIndex;
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class VertexArray {
        @VisibleForDebug
        final int id;
        @VisibleForDebug
        final String formatName;

        private VertexArray(int id, @Nullable VertexFormat[] vertexBindings) {
            this.id = id;
            this.formatName = Arrays.stream(vertexBindings).filter(Objects::nonNull).map(VertexFormat::toString).collect(Collectors.joining(", "));
        }
    }
}

