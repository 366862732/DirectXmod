/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteOrder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class BufferBuilder
implements VertexConsumer {
    private static final int MAX_VERTEX_COUNT = 0xFFFFFF;
    private static final long NOT_BUILDING = -1L;
    private static final long UNKNOWN_ELEMENT = -1L;
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private final ByteBufferBuilder buffer;
    private long vertexPointer = -1L;
    private int vertices;
    private final VertexFormat format;
    private final PrimitiveTopology primitiveTopology;
    private final boolean blockFormat;
    private final boolean entityFormat;
    private final int vertexSize;
    private final int initialElementsToFill;
    private int elementsToFill;
    private boolean building = true;
    private static final int POSITION_SEMANTIC_ID = 0;
    private static final int COLOR_SEMANTIC_ID = 1;
    private static final int UV0_SEMANTIC_ID = 2;
    private static final int UV1_SEMANTIC_ID = 3;
    private static final int UV2_SEMANTIC_ID = 4;
    private static final int NORMAL_SEMANTIC_ID = 5;
    private static final int LINE_WIDTH_SEMANTIC_ID = 6;
    private static final String[] elementNames = new String[]{"Position", "Color", "UV0", "UV1", "UV2", "Normal", "LineWidth"};
    private final @Nullable VertexFormatElement[] elements = new VertexFormatElement[elementNames.length];

    public BufferBuilder(ByteBufferBuilder buffer, PrimitiveTopology primitiveTopology, VertexFormat format) {
        if (!format.contains("Position")) {
            throw new IllegalArgumentException("Cannot build mesh with no position element");
        }
        this.buffer = buffer;
        this.primitiveTopology = primitiveTopology;
        this.format = format;
        this.vertexSize = format.getVertexSize();
        int elementsMask = 0;
        for (int i = 0; i < elementNames.length; ++i) {
            String elementName = elementNames[i];
            VertexFormatElement element = format.getElement(elementName);
            if (element != null) {
                elementsMask |= 1 << i;
            }
            this.elements[i] = element;
        }
        this.initialElementsToFill = elementsMask & 0xFFFFFFFE;
        this.blockFormat = format == DefaultVertexFormat.BLOCK;
        this.entityFormat = format == DefaultVertexFormat.ENTITY;
    }

    public @Nullable MeshData build() {
        this.ensureBuilding();
        this.endLastVertex();
        MeshData mesh = this.storeMesh();
        this.building = false;
        this.vertexPointer = -1L;
        return mesh;
    }

    public MeshData buildOrThrow() {
        MeshData buffer = this.build();
        if (buffer == null) {
            throw new IllegalStateException("BufferBuilder was empty");
        }
        return buffer;
    }

    private void ensureBuilding() {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
    }

    private @Nullable MeshData storeMesh() {
        if (this.vertices == 0) {
            return null;
        }
        ByteBufferBuilder.Result vertexBuffer = this.buffer.build();
        if (vertexBuffer == null) {
            return null;
        }
        int indices = this.primitiveTopology.indexCount(this.vertices);
        IndexType indexType = IndexType.least(this.vertices);
        return new MeshData(vertexBuffer, new MeshData.DrawState(this.format, this.vertices, indices, this.primitiveTopology, indexType));
    }

    private long beginVertex() {
        long pointer;
        this.ensureBuilding();
        this.endLastVertex();
        if (this.vertices >= 0xFFFFFF) {
            throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
        }
        ++this.vertices;
        this.vertexPointer = pointer = this.buffer.reserve(this.vertexSize);
        return pointer;
    }

    private long beginElement(int semanticID) {
        int oldElements = this.elementsToFill;
        int newElements = oldElements & ~(1 << semanticID);
        VertexFormatElement element = this.elements[semanticID];
        if (newElements == oldElements || element == null) {
            return -1L;
        }
        this.elementsToFill = newElements;
        long vertexPointer = this.vertexPointer;
        if (vertexPointer == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
        }
        return vertexPointer + (long)element.offset();
    }

    private void endLastVertex() {
        if (this.vertices == 0) {
            return;
        }
        if (this.elementsToFill != 0) {
            String missingElements = IntStream.range(0, elementNames.length).filter(i -> (this.elementsToFill & i) != 0).mapToObj(i -> elementNames[i]).collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + missingElements);
        }
        if (this.primitiveTopology == PrimitiveTopology.LINES) {
            long pointer = this.buffer.reserve(this.vertexSize);
            MemoryUtil.memCopy((long)(pointer - (long)this.vertexSize), (long)pointer, (long)this.vertexSize);
            ++this.vertices;
        }
    }

    private static void putRgba(long pointer, int argb) {
        int abgr = ARGB.toABGR(argb);
        MemoryUtil.memPutInt((long)pointer, (int)(IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr)));
    }

    private static void putPackedUv(long pointer, int packedUv) {
        if (IS_LITTLE_ENDIAN) {
            MemoryUtil.memPutInt((long)pointer, (int)packedUv);
        } else {
            MemoryUtil.memPutShort((long)pointer, (short)((short)(packedUv & 0xFFFF)));
            MemoryUtil.memPutShort((long)(pointer + 2L), (short)((short)(packedUv >> 16 & 0xFFFF)));
        }
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        VertexFormatElement positionElement = this.elements[0];
        long pointer = this.beginVertex() + (long)positionElement.offset();
        this.elementsToFill = this.initialElementsToFill;
        BufferBuilder.putVec3f(pointer, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        long pointer = this.beginElement(1);
        if (pointer != -1L) {
            MemoryUtil.memPutByte((long)pointer, (byte)((byte)r));
            MemoryUtil.memPutByte((long)(pointer + 1L), (byte)((byte)g));
            MemoryUtil.memPutByte((long)(pointer + 2L), (byte)((byte)b));
            MemoryUtil.memPutByte((long)(pointer + 3L), (byte)((byte)a));
        }
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        long pointer = this.beginElement(1);
        if (pointer != -1L) {
            BufferBuilder.putRgba(pointer, color);
        }
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        long pointer = this.beginElement(2);
        if (pointer != -1L) {
            MemoryUtil.memPutFloat((long)pointer, (float)u);
            MemoryUtil.memPutFloat((long)(pointer + 4L), (float)v);
        }
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this.uvShort((short)u, (short)v, 3);
    }

    @Override
    public VertexConsumer setOverlay(int packedOverlayCoords) {
        long pointer = this.beginElement(3);
        if (pointer != -1L) {
            BufferBuilder.putPackedUv(pointer, packedOverlayCoords);
        }
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this.uvShort((short)u, (short)v, 4);
    }

    @Override
    public VertexConsumer setLight(int packedLightCoords) {
        long pointer = this.beginElement(4);
        if (pointer != -1L) {
            BufferBuilder.putPackedUv(pointer, packedLightCoords);
        }
        return this;
    }

    private VertexConsumer uvShort(short u, short v, int semanticID) {
        long pointer = this.beginElement(semanticID);
        if (pointer != -1L) {
            MemoryUtil.memPutShort((long)pointer, (short)u);
            MemoryUtil.memPutShort((long)(pointer + 2L), (short)v);
        }
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        long pointer = this.beginElement(5);
        if (pointer != -1L) {
            BufferBuilder.putNormals(pointer, x, y, z);
        }
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        long pointer = this.beginElement(6);
        if (pointer != -1L) {
            MemoryUtil.memPutFloat((long)pointer, (float)width);
        }
        return this;
    }

    private static byte normalIntValue(float c) {
        return (byte)((int)(Mth.clamp(c, -1.0f, 1.0f) * 127.0f) & 0xFF);
    }

    private static void putVec3f(long pointer, float x, float y, float z) {
        MemoryUtil.memPutFloat((long)pointer, (float)x);
        MemoryUtil.memPutFloat((long)(pointer + 4L), (float)y);
        MemoryUtil.memPutFloat((long)(pointer + 8L), (float)z);
    }

    private static void putNormals(long pointer, float nx, float ny, float nz) {
        MemoryUtil.memPutByte((long)pointer, (byte)BufferBuilder.normalIntValue(nx));
        MemoryUtil.memPutByte((long)(pointer + 1L), (byte)BufferBuilder.normalIntValue(ny));
        MemoryUtil.memPutByte((long)(pointer + 2L), (byte)BufferBuilder.normalIntValue(nz));
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlayCoords, int lightCoords, float nx, float ny, float nz) {
        if (this.blockFormat) {
            long pointer = this.beginVertex();
            BufferBuilder.putVec3f(pointer, x, y, z);
            BufferBuilder.putRgba(pointer + 12L, color);
            MemoryUtil.memPutFloat((long)(pointer + 16L), (float)u);
            MemoryUtil.memPutFloat((long)(pointer + 20L), (float)v);
            BufferBuilder.putPackedUv(pointer + 24L, lightCoords);
        } else if (this.entityFormat) {
            long pointer = this.beginVertex();
            BufferBuilder.putVec3f(pointer, x, y, z);
            BufferBuilder.putRgba(pointer + 12L, color);
            MemoryUtil.memPutFloat((long)(pointer + 16L), (float)u);
            MemoryUtil.memPutFloat((long)(pointer + 20L), (float)v);
            BufferBuilder.putPackedUv(pointer + 24L, overlayCoords);
            BufferBuilder.putPackedUv(pointer + 28L, lightCoords);
            BufferBuilder.putNormals(pointer + 32L, nx, ny, nz);
        } else {
            VertexConsumer.super.addVertex(x, y, z, color, u, v, overlayCoords, lightCoords, nx, ny, nz);
        }
    }
}

