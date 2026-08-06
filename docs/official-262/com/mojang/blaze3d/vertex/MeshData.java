/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableLong
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public class MeshData
implements AutoCloseable {
    private final ByteBufferBuilder.Result vertexBuffer;
    private @Nullable ByteBufferBuilder.Result indexBuffer;
    private final DrawState drawState;

    public MeshData(ByteBufferBuilder.Result vertexBuffer, DrawState drawState) {
        this.vertexBuffer = vertexBuffer;
        this.drawState = drawState;
    }

    public static void decodeQuadCentroids(ByteBuffer vertexBuffer, int vertexCount, VertexFormat format, CompactVectorArray output, int outputIndex) {
        VertexFormatElement positionElement = format.getElement("Position");
        if (positionElement == null) {
            throw new IllegalArgumentException("Cannot identify quad centers with no position element");
        }
        int positionOffset = vertexBuffer.position() + positionElement.offset();
        int vertexStride = format.getVertexSize();
        int quadStride = vertexStride * 4;
        int quadCount = vertexCount / 4;
        for (int i = 0; i < quadCount; ++i) {
            int firstPosOffset = i * quadStride + positionOffset;
            int secondPosOffset = firstPosOffset + vertexStride * 2;
            float x0 = vertexBuffer.getFloat(firstPosOffset + 0);
            float y0 = vertexBuffer.getFloat(firstPosOffset + 4);
            float z0 = vertexBuffer.getFloat(firstPosOffset + 8);
            float x1 = vertexBuffer.getFloat(secondPosOffset + 0);
            float y1 = vertexBuffer.getFloat(secondPosOffset + 4);
            float z1 = vertexBuffer.getFloat(secondPosOffset + 8);
            float xMid = (x0 + x1) / 2.0f;
            float yMid = (y0 + y1) / 2.0f;
            float zMid = (z0 + z1) / 2.0f;
            output.set(outputIndex + i, xMid, yMid, zMid);
        }
    }

    public ByteBuffer vertexBuffer() {
        return this.vertexBuffer.byteBuffer();
    }

    public @Nullable ByteBuffer indexBuffer() {
        return this.indexBuffer != null ? this.indexBuffer.byteBuffer() : null;
    }

    public ByteBufferBuilder.Result vertexBufferSlice() {
        return this.vertexBuffer;
    }

    public DrawState drawState() {
        return this.drawState;
    }

    public @Nullable SortState sortQuads(ByteBufferBuilder indexBufferTarget, VertexSorting sorting) {
        if (this.drawState.primitiveTopology() != PrimitiveTopology.QUADS) {
            return null;
        }
        CompactVectorArray centroids = new CompactVectorArray(this.drawState.vertexCount() / 4);
        MeshData.decodeQuadCentroids(this.vertexBuffer.byteBuffer(), this.drawState.vertexCount(), this.drawState.format(), centroids, 0);
        SortState sortState = new SortState(centroids, this.drawState.indexType());
        this.indexBuffer = sortState.buildSortedIndexBuffer(indexBufferTarget, sorting);
        return sortState;
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record DrawState(VertexFormat format, int vertexCount, int indexCount, PrimitiveTopology primitiveTopology, IndexType indexType) {
    }

    @Environment(value=EnvType.CLIENT)
    public record SortState(CompactVectorArray centroids, IndexType indexType) {
        public @Nullable ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder target, VertexSorting sorting) {
            int[] startIndices = sorting.sort(this.centroids);
            long pointer = target.reserve(startIndices.length * 6 * this.indexType.bytes);
            SortState.writeIndices(startIndices, this.indexWriter(pointer, this.indexType));
            return target.build();
        }

        public void writeSortedIndexBuffer(ByteBuffer target, VertexSorting sorting) {
            IntConsumer output = switch (this.indexType) {
                default -> throw new MatchException(null, null);
                case IndexType.SHORT -> value -> target.putShort((short)value);
                case IndexType.INT -> target::putInt;
            };
            SortState.writeIndices(sorting.sort(this.centroids), output);
        }

        private static void writeIndices(int[] startIndices, IntConsumer output) {
            for (int startIndex : startIndices) {
                output.accept(startIndex * 4 + 0);
                output.accept(startIndex * 4 + 1);
                output.accept(startIndex * 4 + 2);
                output.accept(startIndex * 4 + 2);
                output.accept(startIndex * 4 + 3);
                output.accept(startIndex * 4 + 0);
            }
        }

        private IntConsumer indexWriter(long pointer, IndexType indexType) {
            MutableLong nextIndex = new MutableLong(pointer);
            return switch (indexType) {
                default -> throw new MatchException(null, null);
                case IndexType.SHORT -> value -> MemoryUtil.memPutShort((long)nextIndex.getAndAdd(2L), (short)((short)value));
                case IndexType.INT -> value -> MemoryUtil.memPutInt((long)nextIndex.getAndAdd(4L), (int)value);
            };
        }
    }
}

