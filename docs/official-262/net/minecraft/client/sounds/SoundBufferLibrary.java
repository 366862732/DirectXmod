/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class SoundBufferLibrary {
    private final ResourceProvider resourceManager;
    private final Map<Identifier, CompletableFuture<SoundBuffer>> cache = Maps.newHashMap();

    public SoundBufferLibrary(ResourceProvider resourceProvider) {
        this.resourceManager = resourceProvider;
    }

    public CompletableFuture<SoundBuffer> getCompleteBuffer(Identifier location) {
        return this.cache.computeIfAbsent(location, l -> CompletableFuture.supplyAsync(() -> {
            try (InputStream is = this.resourceManager.open((Identifier)l);){
                SoundBuffer soundBuffer;
                try (JOrbisAudioStream as = new JOrbisAudioStream(is);){
                    ByteBuffer data = as.readAll();
                    soundBuffer = new SoundBuffer(data, as.getFormat());
                }
                return soundBuffer;
            }
            catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool()));
    }

    public CompletableFuture<AudioStream> getStream(Identifier location, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream is = this.resourceManager.open(location);
                return looping ? new LoopingAudioStream(JOrbisAudioStream::new, is) : new JOrbisAudioStream(is);
            }
            catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool());
    }

    public void clear() {
        this.cache.values().forEach(future -> future.thenAccept(SoundBuffer::discardAlBuffer));
        this.cache.clear();
    }

    public CompletableFuture<?> preload(Collection<Sound> sounds) {
        return CompletableFuture.allOf((CompletableFuture[])sounds.stream().map(sound -> this.getCompleteBuffer(sound.getPath())).toArray(CompletableFuture[]::new));
    }

    public void enumerate(DebugOutput debugOutput) {
        this.cache.forEach((id, bufferFuture) -> {
            SoundBuffer buffer = bufferFuture.getNow(null);
            if (buffer != null && buffer.isValid()) {
                debugOutput.accountBuffer((Identifier)id, buffer.size(), buffer.format());
            }
        });
    }

    @Environment(value=EnvType.CLIENT)
    public static interface DebugOutput {
        public void accountBuffer(Identifier var1, int var2, AudioFormat var3);

        @Environment(value=EnvType.CLIENT)
        public static class Counter
        implements DebugOutput {
            private int totalCount;
            private long totalSize;

            @Override
            public void accountBuffer(Identifier id, int size, AudioFormat format) {
                ++this.totalCount;
                this.totalSize += (long)size;
            }

            public int totalCount() {
                return this.totalCount;
            }

            public long totalSize() {
                return this.totalSize;
            }
        }
    }
}

