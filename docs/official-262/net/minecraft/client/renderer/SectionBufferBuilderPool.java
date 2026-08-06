/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SectionBufferBuilderPool
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ArrayBlockingQueue<SectionBufferBuilderPack> freeBuffers;

    private SectionBufferBuilderPool(List<SectionBufferBuilderPack> buffers) {
        this.freeBuffers = Queues.newArrayBlockingQueue((int)buffers.size());
        this.freeBuffers.addAll(buffers);
    }

    public static SectionBufferBuilderPool allocate(int maxWorkers) {
        int maxBuffers = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3) / SectionBufferBuilderPack.TOTAL_BUFFERS_SIZE);
        int targetBufferCount = Math.max(1, Math.min(maxWorkers, maxBuffers));
        ArrayList<SectionBufferBuilderPack> buffers = new ArrayList<SectionBufferBuilderPack>(targetBufferCount);
        try {
            for (int i = 0; i < targetBufferCount; ++i) {
                buffers.add(new SectionBufferBuilderPack());
            }
        }
        catch (OutOfMemoryError e) {
            LOGGER.warn("Allocated only {}/{} buffers", (Object)buffers.size(), (Object)targetBufferCount);
            int buffersToDrop = Math.min(buffers.size() * 2 / 3, buffers.size() - 1);
            for (int i = 0; i < buffersToDrop; ++i) {
                ((SectionBufferBuilderPack)buffers.remove(buffers.size() - 1)).close();
            }
        }
        return new SectionBufferBuilderPool(buffers);
    }

    public @Nullable SectionBufferBuilderPack acquire() {
        return this.freeBuffers.poll();
    }

    public void release(SectionBufferBuilderPack buffer) {
        this.freeBuffers.offer(buffer);
    }

    public boolean isEmpty() {
        return this.freeBuffers.isEmpty();
    }

    public int getFreeBufferCount() {
        return this.freeBuffers.size();
    }

    @Override
    public void close() {
        this.freeBuffers.forEach(SectionBufferBuilderPack::close);
        this.freeBuffers.clear();
    }
}

