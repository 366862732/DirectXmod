/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.resource;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class CrossFrameResourcePool
implements GraphicsResourceAllocator,
AutoCloseable {
    private final int framesToKeepResource;
    private final Deque<ResourceEntry<?>> pool = new ArrayDeque();

    public CrossFrameResourcePool(int framesToKeepResource) {
        this.framesToKeepResource = framesToKeepResource;
    }

    public void endFrame() {
        Iterator<ResourceEntry<?>> iterator = this.pool.iterator();
        while (iterator.hasNext()) {
            ResourceEntry<?> entry = iterator.next();
            if (entry.framesToLive-- != 0) continue;
            entry.close();
            iterator.remove();
        }
    }

    @Override
    public <T> T acquire(ResourceDescriptor<T> descriptor) {
        T resource = this.acquireWithoutPreparing(descriptor);
        descriptor.prepare(resource);
        return resource;
    }

    private <T> T acquireWithoutPreparing(ResourceDescriptor<T> descriptor) {
        Iterator<ResourceEntry<?>> iterator = this.pool.iterator();
        while (iterator.hasNext()) {
            ResourceEntry<?> entry = iterator.next();
            if (!descriptor.canUsePhysicalResource(entry.descriptor)) continue;
            iterator.remove();
            return entry.value;
        }
        return descriptor.allocate();
    }

    @Override
    public <T> void release(ResourceDescriptor<T> descriptor, T resource) {
        this.pool.addFirst(new ResourceEntry<T>(descriptor, resource, this.framesToKeepResource));
    }

    public void clear() {
        this.pool.forEach(ResourceEntry::close);
        this.pool.clear();
    }

    @Override
    public void close() {
        this.clear();
    }

    @VisibleForTesting
    protected Collection<ResourceEntry<?>> entries() {
        return this.pool;
    }

    @Environment(value=EnvType.CLIENT)
    @VisibleForTesting
    protected static final class ResourceEntry<T>
    implements AutoCloseable {
        private final ResourceDescriptor<T> descriptor;
        private final T value;
        private int framesToLive;

        private ResourceEntry(ResourceDescriptor<T> descriptor, T value, int framesToLive) {
            this.descriptor = descriptor;
            this.value = value;
            this.framesToLive = framesToLive;
        }

        @Override
        public void close() {
            this.descriptor.free(this.value);
        }
    }
}

