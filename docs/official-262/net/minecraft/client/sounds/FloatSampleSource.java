/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.floats.FloatConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sounds;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.ChunkedSampleByteBuf;
import net.minecraft.client.sounds.FiniteAudioStream;

@Environment(value=EnvType.CLIENT)
public interface FloatSampleSource
extends FiniteAudioStream {
    public static final int EXPECTED_MAX_FRAME_SIZE = 8192;

    public boolean readChunk(FloatConsumer var1) throws IOException;

    @Override
    default public ByteBuffer read(int expectedSize) throws IOException {
        ChunkedSampleByteBuf output = new ChunkedSampleByteBuf(expectedSize + 8192);
        while (this.readChunk(output) && output.size() < expectedSize) {
        }
        return output.get();
    }

    @Override
    default public ByteBuffer readAll() throws IOException {
        ChunkedSampleByteBuf output = new ChunkedSampleByteBuf(16384);
        while (this.readChunk(output)) {
        }
        return output.get();
    }
}

