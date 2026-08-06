/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.Arrays;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class SectionBufferBuilderPack
implements AutoCloseable {
    public static final int TOTAL_BUFFERS_SIZE = Arrays.stream(ChunkSectionLayer.values()).mapToInt(ChunkSectionLayer::bufferSize).sum();
    private final Map<ChunkSectionLayer, ByteBufferBuilder> buffers = Util.makeEnumMap(ChunkSectionLayer.class, layer -> new ByteBufferBuilder(layer.bufferSize()));

    public ByteBufferBuilder buffer(ChunkSectionLayer layer) {
        return this.buffers.get((Object)layer);
    }

    public void clearAll() {
        this.buffers.values().forEach(ByteBufferBuilder::clear);
    }

    public void discardAll() {
        this.buffers.values().forEach(ByteBufferBuilder::discard);
    }

    @Override
    public void close() {
        this.buffers.values().forEach(ByteBufferBuilder::close);
    }
}

