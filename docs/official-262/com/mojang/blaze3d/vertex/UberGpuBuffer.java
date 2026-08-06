/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.vertex.StagingBuffer;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class UberGpuBuffer<T>
implements AutoCloseable {
    private final @GpuBuffer.Usage int bufferUsage;
    private final int heapSize;
    private final int alignSize;
    private final String name;
    private final List<Pair<TlsfAllocator, UberGpuBufferHeap>> nodes = new ArrayList<Pair<TlsfAllocator, UberGpuBufferHeap>>();
    private final StagingBuffer stagingBuffer;
    private final Object2ObjectOpenHashMap<T, StagedAllocationEntry<? extends T>> stagedAllocations = new Object2ObjectOpenHashMap(32);
    private final ObjectOpenHashSet<T> skippedStagedAllocations = new ObjectOpenHashSet(32);
    private final Map<T, TlsfAllocator.Allocation> allocationMap = new HashMap<T, TlsfAllocator.Allocation>(256);

    public UberGpuBuffer(String name, @GpuBuffer.Usage int bufferUsage, int heapSize, int alignSize, StagingBuffer stagingBuffer) {
        this.name = "UberBuffer " + name;
        this.bufferUsage = bufferUsage;
        this.heapSize = heapSize;
        this.alignSize = alignSize;
        this.stagingBuffer = stagingBuffer;
    }

    public <U extends T> boolean addAllocation(U allocationKey, @Nullable UploadCallback<U> callback, ByteBuffer buffer) {
        @Nullable StagingBuffer.BufferHandle handle = this.stagingBuffer.tryAppend(buffer);
        if (handle == null) {
            return false;
        }
        StagedAllocationEntry<U> entry = new StagedAllocationEntry<U>(handle, callback);
        StagedAllocationEntry oldEntry = (StagedAllocationEntry)this.stagedAllocations.put(allocationKey, entry);
        if (oldEntry != null) {
            oldEntry.close();
        }
        return true;
    }

    public boolean uploadStagedAllocations(GpuDevice gpuDevice, StagingBuffer.Uploader uploader) {
        Object key2;
        uploader.checkValidFor(this.stagingBuffer);
        for (Object key2 : this.stagedAllocations.keySet()) {
            this.freeAllocation(key2);
        }
        boolean newHeapCreatedOrDestroyed = false;
        key2 = Profiler.get().zone("uploadStagedAllocations");
        try {
            for (Map.Entry entry : this.stagedAllocations.entrySet()) {
                StagedAllocationEntry staged = (StagedAllocationEntry)entry.getValue();
                try {
                    Pair<TlsfAllocator, UberGpuBufferHeap> node;
                    long allocationSize = staged.buffer.size();
                    if (this.skippedStagedAllocations.contains(entry.getKey())) continue;
                    TlsfAllocator.Allocation allocation = null;
                    Object object = this.nodes.iterator();
                    while (object.hasNext() && (allocation = ((TlsfAllocator)(node = object.next()).getFirst()).allocate(allocationSize, this.alignSize)) == null) {
                    }
                    if (allocation == null) {
                        object = Profiler.get().zone("createNewHeap");
                        try {
                            assert (allocationSize <= (long)this.heapSize);
                            String heapName = String.format(Locale.ROOT, "%s %d", this.name, this.nodes.size());
                            UberGpuBufferHeap newHeap = new UberGpuBufferHeap(this.heapSize, gpuDevice, this.bufferUsage, heapName);
                            TlsfAllocator newTlsfAllocator = new TlsfAllocator(newHeap);
                            this.nodes.add((Pair<TlsfAllocator, UberGpuBufferHeap>)new Pair((Object)newTlsfAllocator, (Object)newHeap));
                            allocation = newTlsfAllocator.allocate(allocationSize, this.alignSize);
                            newHeapCreatedOrDestroyed = true;
                        }
                        finally {
                            if (object != null) {
                                ((Zone)object).close();
                            }
                        }
                    }
                    if (allocation == null) continue;
                    TlsfAllocator.Heap allocationHeap = allocation.getHeap();
                    GpuBuffer allocationDestBuffer = ((UberGpuBufferHeap)allocationHeap).gpuBuffer;
                    uploader.copyTo(staged.buffer, allocationDestBuffer, allocation.getOffsetFromHeap());
                    this.allocationMap.put(entry.getKey(), allocation);
                    UberGpuBuffer.runCallbackUnchecked(entry.getKey(), (StagedAllocationEntry)entry.getValue());
                }
                finally {
                    if (staged == null) continue;
                    staged.close();
                }
            }
            this.stagedAllocations.clear();
            this.skippedStagedAllocations.clear();
        }
        finally {
            if (key2 != null) {
                ((Zone)key2).close();
            }
        }
        Iterator<Pair<TlsfAllocator, UberGpuBufferHeap>> iterator = this.nodes.iterator();
        while (iterator.hasNext()) {
            Pair<TlsfAllocator, UberGpuBufferHeap> node = iterator.next();
            if (!((TlsfAllocator)node.getFirst()).isCompletelyFree()) continue;
            ((UberGpuBufferHeap)node.getSecond()).gpuBuffer.close();
            iterator.remove();
            newHeapCreatedOrDestroyed = true;
            break;
        }
        return newHeapCreatedOrDestroyed;
    }

    private static <T, U extends T> void runCallbackUnchecked(T key, StagedAllocationEntry<U> value) {
        if (value.callback != null) {
            value.callback.bufferHasBeenUploaded(key);
        }
    }

    public @Nullable TlsfAllocator.Allocation getAllocation(T allocationKey) {
        return this.allocationMap.get(allocationKey);
    }

    public void removeAllocation(T allocationKey) {
        this.skippedStagedAllocations.add(allocationKey);
        this.freeAllocation(allocationKey);
    }

    private void freeAllocation(T allocationKey) {
        TlsfAllocator.Allocation allocation = this.allocationMap.remove(allocationKey);
        if (allocation != null) {
            for (Pair<TlsfAllocator, UberGpuBufferHeap> node : this.nodes) {
                if (node.getSecond() != allocation.getHeap()) continue;
                ((TlsfAllocator)node.getFirst()).free(allocation);
                break;
            }
        }
    }

    public GpuBuffer getGpuBuffer(TlsfAllocator.Allocation allocation) {
        return ((UberGpuBufferHeap)allocation.getHeap()).gpuBuffer;
    }

    @VisibleForDebug
    public void printStatistics() {
        for (int i = 0; i < this.nodes.size(); ++i) {
            Pair<TlsfAllocator, UberGpuBufferHeap> node = this.nodes.get(i);
            String heapName = String.format(Locale.ROOT, "%s %d", this.name, i);
            ((TlsfAllocator)node.getFirst()).printAllocatorStatistics(heapName);
        }
    }

    @Override
    public void close() {
        this.stagedAllocations.values().forEach(StagedAllocationEntry::close);
        this.stagedAllocations.clear();
        this.allocationMap.clear();
        for (Pair<TlsfAllocator, UberGpuBufferHeap> node : this.nodes) {
            ((UberGpuBufferHeap)node.getSecond()).gpuBuffer.close();
        }
        this.nodes.clear();
    }

    @Environment(value=EnvType.CLIENT)
    private record StagedAllocationEntry<T>(StagingBuffer.BufferHandle buffer, @Nullable UploadCallback<T> callback) implements AutoCloseable
    {
        @Override
        public void close() {
            this.buffer.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface UploadCallback<T> {
        public void bufferHasBeenUploaded(T var1);
    }

    @Environment(value=EnvType.CLIENT)
    public static class UberGpuBufferHeap
    extends TlsfAllocator.Heap {
        private final GpuBuffer gpuBuffer;

        public UberGpuBufferHeap(long size, GpuDevice gpuDevice, @GpuBuffer.Usage int usage, String name) {
            super(size);
            this.gpuBuffer = gpuDevice.createBuffer(() -> name, usage | 8 | 0x10, size);
        }
    }
}

