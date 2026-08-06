/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sounds;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.AudioStream;

@Environment(value=EnvType.CLIENT)
public class LoopingAudioStream
implements AudioStream {
    private final AudioStreamProvider provider;
    private AudioStream stream;
    private final BufferedInputStream bufferedInputStream;

    public LoopingAudioStream(AudioStreamProvider provider, InputStream originalInputStream) throws IOException {
        this.provider = provider;
        this.bufferedInputStream = new BufferedInputStream(originalInputStream);
        this.bufferedInputStream.mark(Integer.MAX_VALUE);
        this.stream = provider.create(new NoCloseBuffer(this.bufferedInputStream));
    }

    @Override
    public AudioFormat getFormat() {
        return this.stream.getFormat();
    }

    @Override
    public ByteBuffer read(int expectedSize) throws IOException {
        ByteBuffer result = this.stream.read(expectedSize);
        if (!result.hasRemaining()) {
            this.stream.close();
            this.bufferedInputStream.reset();
            this.stream = this.provider.create(new NoCloseBuffer(this.bufferedInputStream));
            result = this.stream.read(expectedSize);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
        this.bufferedInputStream.close();
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface AudioStreamProvider {
        public AudioStream create(InputStream var1) throws IOException;
    }

    @Environment(value=EnvType.CLIENT)
    private static class NoCloseBuffer
    extends FilterInputStream {
        private NoCloseBuffer(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
        }
    }
}

