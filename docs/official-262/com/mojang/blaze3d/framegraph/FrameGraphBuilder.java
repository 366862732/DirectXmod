/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FrameGraphBuilder {
    private final List<InternalVirtualResource<?>> internalResources = new ArrayList();
    private final List<ExternalResource<?>> externalResources = new ArrayList();
    private final List<Pass> passes = new ArrayList<Pass>();

    public FramePass addPass(String name) {
        Pass pass = new Pass(this, this.passes.size(), name);
        this.passes.add(pass);
        return pass;
    }

    public <T> ResourceHandle<T> importExternal(String name, T resource) {
        ExternalResource<T> holder = new ExternalResource<T>(name, null, resource);
        this.externalResources.add(holder);
        return holder.handle;
    }

    public <T> ResourceHandle<T> createInternal(String name, ResourceDescriptor<T> descriptor) {
        return this.createInternalResource((String)name, descriptor, null).handle;
    }

    private <T> InternalVirtualResource<T> createInternalResource(String name, ResourceDescriptor<T> descriptor, @Nullable Pass createdBy) {
        int id = this.internalResources.size();
        InternalVirtualResource<T> resource = new InternalVirtualResource<T>(id, name, createdBy, descriptor);
        this.internalResources.add(resource);
        return resource;
    }

    public void execute(GraphicsResourceAllocator resourceAllocator) {
        this.execute(resourceAllocator, Inspector.NONE);
    }

    public void execute(GraphicsResourceAllocator resourceAllocator, Inspector inspector) {
        BitSet passesToKeep = this.identifyPassesToKeep();
        ArrayList<Pass> passesInOrder = new ArrayList<Pass>(passesToKeep.cardinality());
        BitSet visiting = new BitSet(this.passes.size());
        for (Pass pass : this.passes) {
            this.resolvePassOrder(pass, passesToKeep, visiting, passesInOrder);
        }
        this.assignResourceLifetimes(passesInOrder);
        for (Pass pass : passesInOrder) {
            for (InternalVirtualResource<?> resource : pass.resourcesToAcquire) {
                inspector.acquireResource(resource.name);
                resource.acquire(resourceAllocator);
            }
            inspector.beforeExecutePass(pass.name);
            pass.task.run();
            inspector.afterExecutePass(pass.name);
            int id = pass.resourcesToRelease.nextSetBit(0);
            while (id >= 0) {
                InternalVirtualResource<?> resource;
                resource = this.internalResources.get(id);
                inspector.releaseResource(resource.name);
                resource.release(resourceAllocator);
                id = pass.resourcesToRelease.nextSetBit(id + 1);
            }
        }
    }

    private BitSet identifyPassesToKeep() {
        ArrayDeque<Pass> scratchQueue = new ArrayDeque<Pass>(this.passes.size());
        BitSet passesToKeep = new BitSet(this.passes.size());
        for (VirtualResource virtualResource : this.externalResources) {
            Pass pass = virtualResource.handle.createdBy;
            if (pass == null) continue;
            this.discoverAllRequiredPasses(pass, passesToKeep, scratchQueue);
        }
        for (Pass pass : this.passes) {
            if (!pass.disableCulling) continue;
            this.discoverAllRequiredPasses(pass, passesToKeep, scratchQueue);
        }
        return passesToKeep;
    }

    private void discoverAllRequiredPasses(Pass sourcePass, BitSet visited, Deque<Pass> passesToTrace) {
        passesToTrace.add(sourcePass);
        while (!passesToTrace.isEmpty()) {
            Pass pass = passesToTrace.poll();
            if (visited.get(pass.id)) continue;
            visited.set(pass.id);
            int id = pass.requiredPassIds.nextSetBit(0);
            while (id >= 0) {
                passesToTrace.add(this.passes.get(id));
                id = pass.requiredPassIds.nextSetBit(id + 1);
            }
        }
    }

    private void resolvePassOrder(Pass pass, BitSet passesToFind, BitSet visiting, List<Pass> output) {
        if (visiting.get(pass.id)) {
            String involvedPasses = visiting.stream().mapToObj(id -> this.passes.get((int)id).name).collect(Collectors.joining(", "));
            throw new IllegalStateException("Frame graph cycle detected between " + involvedPasses);
        }
        if (!passesToFind.get(pass.id)) {
            return;
        }
        visiting.set(pass.id);
        passesToFind.clear(pass.id);
        int id2 = pass.requiredPassIds.nextSetBit(0);
        while (id2 >= 0) {
            this.resolvePassOrder(this.passes.get(id2), passesToFind, visiting, output);
            id2 = pass.requiredPassIds.nextSetBit(id2 + 1);
        }
        for (Handle<?> handle : pass.writesFrom) {
            int id3 = handle.readBy.nextSetBit(0);
            while (id3 >= 0) {
                if (id3 != pass.id) {
                    this.resolvePassOrder(this.passes.get(id3), passesToFind, visiting, output);
                }
                id3 = handle.readBy.nextSetBit(id3 + 1);
            }
        }
        output.add(pass);
        visiting.clear(pass.id);
    }

    private void assignResourceLifetimes(Collection<Pass> passesInOrder) {
        @Nullable Pass[] lastPassByResource = new Pass[this.internalResources.size()];
        for (Pass pass : passesInOrder) {
            int id = pass.requiredResourceIds.nextSetBit(0);
            while (id >= 0) {
                InternalVirtualResource<?> resource = this.internalResources.get(id);
                Pass lastPass = lastPassByResource[id];
                lastPassByResource[id] = pass;
                if (lastPass == null) {
                    pass.resourcesToAcquire.add(resource);
                } else {
                    lastPass.resourcesToRelease.clear(id);
                }
                pass.resourcesToRelease.set(id);
                id = pass.requiredResourceIds.nextSetBit(id + 1);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class Pass
    implements FramePass {
        private final int id;
        private final String name;
        private final List<Handle<?>> writesFrom;
        private final BitSet requiredResourceIds;
        private final BitSet requiredPassIds;
        private Runnable task;
        private final List<InternalVirtualResource<?>> resourcesToAcquire;
        private final BitSet resourcesToRelease;
        private boolean disableCulling;
        final /* synthetic */ FrameGraphBuilder this$0;

        public Pass(FrameGraphBuilder frameGraphBuilder, int id, String name) {
            FrameGraphBuilder frameGraphBuilder2 = frameGraphBuilder;
            Objects.requireNonNull(frameGraphBuilder2);
            this.this$0 = frameGraphBuilder2;
            this.writesFrom = new ArrayList();
            this.requiredResourceIds = new BitSet();
            this.requiredPassIds = new BitSet();
            this.task = () -> {};
            this.resourcesToAcquire = new ArrayList();
            this.resourcesToRelease = new BitSet();
            this.id = id;
            this.name = name;
        }

        private <T> void markResourceRequired(Handle<T> handle) {
            VirtualResource virtualResource = handle.holder;
            if (virtualResource instanceof InternalVirtualResource) {
                InternalVirtualResource resource = (InternalVirtualResource)virtualResource;
                this.requiredResourceIds.set(resource.id);
            }
        }

        private void markPassRequired(Pass pass) {
            this.requiredPassIds.set(pass.id);
        }

        @Override
        public <T> ResourceHandle<T> createsInternal(String name, ResourceDescriptor<T> descriptor) {
            InternalVirtualResource<T> resource = this.this$0.createInternalResource(name, descriptor, this);
            this.requiredResourceIds.set(resource.id);
            return resource.handle;
        }

        @Override
        public <T> void reads(ResourceHandle<T> handle) {
            this._reads((Handle)handle);
        }

        private <T> void _reads(Handle<T> handle) {
            this.markResourceRequired(handle);
            if (handle.createdBy != null) {
                this.markPassRequired(handle.createdBy);
            }
            handle.readBy.set(this.id);
        }

        @Override
        public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> handle) {
            return this._readsAndWrites((Handle)handle);
        }

        @Override
        public void requires(FramePass pass) {
            this.requiredPassIds.set(((Pass)pass).id);
        }

        @Override
        public void disableCulling() {
            this.disableCulling = true;
        }

        private <T> Handle<T> _readsAndWrites(Handle<T> handle) {
            this.writesFrom.add(handle);
            this._reads(handle);
            return handle.writeAndAlias(this);
        }

        @Override
        public void executes(Runnable task) {
            this.task = task;
        }

        public String toString() {
            return this.name;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ExternalResource<T>
    extends VirtualResource<T> {
        private final T resource;

        public ExternalResource(String name, @Nullable Pass createdBy, T resource) {
            super(name, createdBy);
            this.resource = resource;
        }

        @Override
        public T get() {
            return this.resource;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Handle<T>
    implements ResourceHandle<T> {
        private final VirtualResource<T> holder;
        private final int version;
        private final @Nullable Pass createdBy;
        private final BitSet readBy = new BitSet();
        private @Nullable Handle<T> aliasedBy;

        private Handle(VirtualResource<T> holder, int version, @Nullable Pass createdBy) {
            this.holder = holder;
            this.version = version;
            this.createdBy = createdBy;
        }

        @Override
        public T get() {
            return this.holder.get();
        }

        private Handle<T> writeAndAlias(Pass pass) {
            if (this.holder.handle != this) {
                throw new IllegalStateException("Handle " + String.valueOf(this) + " is no longer valid, as its contents were moved into " + String.valueOf(this.aliasedBy));
            }
            Handle<T> newHandle = new Handle<T>(this.holder, this.version + 1, pass);
            this.holder.handle = newHandle;
            this.aliasedBy = newHandle;
            return newHandle;
        }

        public String toString() {
            if (this.createdBy != null) {
                return String.valueOf(this.holder) + "#" + this.version + " (from " + String.valueOf(this.createdBy) + ")";
            }
            return String.valueOf(this.holder) + "#" + this.version;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class InternalVirtualResource<T>
    extends VirtualResource<T> {
        private final int id;
        private final ResourceDescriptor<T> descriptor;
        private @Nullable T physicalResource;

        public InternalVirtualResource(int id, String name, @Nullable Pass createdBy, ResourceDescriptor<T> descriptor) {
            super(name, createdBy);
            this.id = id;
            this.descriptor = descriptor;
        }

        @Override
        public T get() {
            return Objects.requireNonNull(this.physicalResource, "Resource is not currently available");
        }

        public void acquire(GraphicsResourceAllocator allocator) {
            if (this.physicalResource != null) {
                throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
            }
            this.physicalResource = allocator.acquire(this.descriptor);
        }

        public void release(GraphicsResourceAllocator allocator) {
            if (this.physicalResource == null) {
                throw new IllegalStateException("Tried to release physical resource that was not allocated");
            }
            allocator.release(this.descriptor, this.physicalResource);
            this.physicalResource = null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Inspector {
        public static final Inspector NONE = new Inspector(){};

        default public void acquireResource(String name) {
        }

        default public void releaseResource(String name) {
        }

        default public void beforeExecutePass(String name) {
        }

        default public void afterExecutePass(String name) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static abstract class VirtualResource<T> {
        public final String name;
        public Handle<T> handle;

        public VirtualResource(String name, @Nullable Pass createdBy) {
            this.name = name;
            this.handle = new Handle(this, 0, createdBy);
        }

        public abstract T get();

        public String toString() {
            return this.name;
        }
    }
}

