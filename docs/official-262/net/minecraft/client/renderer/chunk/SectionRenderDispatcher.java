/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.StagingBuffer;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SectionRenderDispatcher {
    public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
    private final SectionTaskDynamicQueue queue = new SectionTaskDynamicQueue();
    private final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    private volatile boolean closed;
    private final TracingExecutor executor;
    private final Consumer<RenderSection> onSectionMeshUpdate;
    private final AtomicReference<Vec3> cameraPosition = new AtomicReference<Vec3>(Vec3.ZERO);
    private volatile SectionCompiler sectionCompiler;
    private final StagingBuffer stagingBuffer;
    private final Map<ChunkSectionLayer, SectionUberBuffers> chunkUberBuffers;
    private final ReentrantLock copyLock = new ReentrantLock();

    public SectionRenderDispatcher(TracingExecutor executor, RenderBuffers renderBuffers, SectionCompiler sectionCompiler, Consumer<RenderSection> onSectionMeshUpdate) {
        this.onSectionMeshUpdate = onSectionMeshUpdate;
        this.fixedBuffers = renderBuffers.fixedBufferPack();
        this.bufferPool = renderBuffers.sectionBufferPool();
        this.executor = executor;
        this.sectionCompiler = sectionCompiler;
        int vertexBufferHeapSize = 0x8000000;
        int indexBufferHeapSize = 0x2000000;
        int stagingBufferSize = 0x6200000;
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.stagingBuffer = StagingBuffer.create("Chunk", gpuDevice, 0x6200000);
        this.chunkUberBuffers = Util.makeEnumMap(ChunkSectionLayer.class, layer -> {
            VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
            UberGpuBuffer<SectionMesh> vertexUberBuffer = new UberGpuBuffer<SectionMesh>(layer.label(), 32, 0x8000000, vertexFormat.getVertexSize(), this.stagingBuffer);
            UberGpuBuffer<SectionMesh> indexUberBuffer = new UberGpuBuffer<SectionMesh>(layer.label(), 64, 0x2000000, 8, this.stagingBuffer);
            return new SectionUberBuffers(vertexUberBuffer, indexUberBuffer);
        });
    }

    public void setCompiler(SectionCompiler sectionCompiler) {
        this.sectionCompiler = sectionCompiler;
    }

    private void runTask() {
        if (this.closed) {
            return;
        }
        RenderSection.SectionTask task = this.queue.poll(this.cameraPosition.get());
        if (task == null || task.isCompleted.get() || task.isCancelled.get()) {
            return;
        }
        try {
            SectionBufferBuilderPack buffer = Objects.requireNonNull(this.bufferPool.acquire());
            RenderSection.SectionTask.SectionTaskResult result = task.doTask(buffer);
            task.isCompleted.set(true);
            if (result == RenderSection.SectionTask.SectionTaskResult.SUCCESSFUL) {
                buffer.clearAll();
            } else {
                buffer.discardAll();
            }
            this.bufferPool.release(buffer);
            this.executor.execute(this::runTask);
        }
        catch (NullPointerException e) {
            this.queue.add(task);
        }
        catch (Exception e) {
            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(e, "Batching sections"));
        }
    }

    public void setCameraPosition(Vec3 cameraPosition) {
        this.cameraPosition.set(cameraPosition);
    }

    public @Nullable RenderSectionBufferSlice getRenderSectionSlice(SectionMesh sectionMesh, ChunkSectionLayer layer) {
        SectionUberBuffers uberBuffers = this.chunkUberBuffers.get((Object)layer);
        TlsfAllocator.Allocation vertexSlice = uberBuffers.vertexBuffer.getAllocation(sectionMesh);
        if (vertexSlice == null) {
            return null;
        }
        long vertexBufferOffset = vertexSlice.getOffsetFromHeap();
        TlsfAllocator.Allocation indexSlice = uberBuffers.indexBuffer.getAllocation(sectionMesh);
        long indexBufferOffset = 0L;
        GpuBuffer indexBuffer = null;
        if (indexSlice != null) {
            indexBufferOffset = indexSlice.getOffsetFromHeap();
            indexBuffer = uberBuffers.indexBuffer.getGpuBuffer(indexSlice);
        }
        return new RenderSectionBufferSlice(uberBuffers.vertexBuffer.getGpuBuffer(vertexSlice), vertexBufferOffset, indexBuffer, indexBufferOffset);
    }

    public void lock() {
        this.copyLock.lock();
    }

    public void unlock() {
        this.copyLock.unlock();
    }

    public void uploadTerrainBuffersToGpu() {
        GpuDevice device = RenderSystem.getDevice();
        try (StagingBuffer.Uploader uploader = this.stagingBuffer.startUploading(device.createCommandEncoder());){
            for (SectionUberBuffers buffers : this.chunkUberBuffers.values()) {
                boolean performedBufferResize = buffers.vertexBuffer.uploadStagedAllocations(device, uploader);
                buffers.indexBuffer.uploadStagedAllocations(device, uploader);
                if (!performedBufferResize) continue;
                break;
            }
        }
    }

    private void schedule(RenderSection.SectionTask task) {
        if (this.closed) {
            return;
        }
        this.queue.add(task);
        this.executor.execute(this::runTask);
    }

    public void clearCompileQueue() {
        this.queue.clear();
    }

    public boolean isQueueEmpty() {
        return this.queue.size() == 0;
    }

    public void dispose() {
        this.closed = true;
        this.clearCompileQueue();
        this.copyLock.lock();
        try {
            for (SectionUberBuffers buffers : this.chunkUberBuffers.values()) {
                buffers.vertexBuffer.close();
                buffers.indexBuffer.close();
            }
            this.stagingBuffer.close();
        }
        finally {
            this.copyLock.unlock();
        }
    }

    @VisibleForDebug
    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, aB: %02d", this.queue.size(), this.bufferPool.getFreeBufferCount());
    }

    @VisibleForDebug
    public int getCompileQueueSize() {
        return this.queue.size();
    }

    @VisibleForDebug
    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    @Environment(value=EnvType.CLIENT)
    public class RenderSection
    implements RotatingSectionStorage.Value {
        public final int index;
        public final AtomicReference<SectionMesh> sectionMesh;
        private @Nullable CompileTask lastCompileTask;
        private @Nullable ResortTransparencyTask lastResortTransparencyTask;
        private AABB bb;
        private volatile long sectionNode;
        private final BlockPos.MutableBlockPos renderOrigin;
        private long uploadedTime;
        private long fadeDuration;
        private boolean wasPreviouslyEmpty;
        final /* synthetic */ SectionRenderDispatcher this$0;

        public RenderSection(SectionRenderDispatcher this$0, int index, long sectionNode) {
            SectionRenderDispatcher sectionRenderDispatcher = this$0;
            Objects.requireNonNull(sectionRenderDispatcher);
            this.this$0 = sectionRenderDispatcher;
            this.sectionMesh = new AtomicReference<SectionMesh>(CompiledSectionMesh.UNCOMPILED);
            this.sectionNode = SectionPos.asLong(-1, -1, -1);
            this.renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
            this.index = index;
            this.setSectionNode(sectionNode);
        }

        public float getVisibility(long now) {
            long elapsed = now - this.uploadedTime;
            if (elapsed >= this.fadeDuration) {
                return 1.0f;
            }
            return (float)elapsed / (float)this.fadeDuration;
        }

        public void setFadeDuration(long fadeDuration) {
            this.fadeDuration = fadeDuration;
        }

        public void setWasPreviouslyEmpty(boolean wasPreviouslyEmpty) {
            this.wasPreviouslyEmpty = wasPreviouslyEmpty;
        }

        public boolean wasPreviouslyEmpty() {
            return this.wasPreviouslyEmpty;
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        @Override
        public void setSectionNode(long sectionNode) {
            this.reset();
            this.sectionNode = sectionNode;
            int x = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
            int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
            int z = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
            this.renderOrigin.set(x, y, z);
            this.bb = new AABB(x, y, z, x + 16, y + 16, z + 16);
        }

        public SectionMesh getSectionMesh() {
            return this.sectionMesh.get();
        }

        public void reset() {
            this.cancelTasks();
            SectionMesh mesh = this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED);
            this.this$0.copyLock.lock();
            try {
                this.releaseSectionMesh(mesh);
            }
            finally {
                this.this$0.copyLock.unlock();
            }
            this.uploadedTime = 0L;
            this.wasPreviouslyEmpty = false;
        }

        public BlockPos getRenderOrigin() {
            return this.renderOrigin;
        }

        @Override
        public long getSectionNode() {
            return this.sectionNode;
        }

        public long getNeighborSectionNode(Direction direction) {
            return SectionPos.offset(this.sectionNode, direction);
        }

        public void resortTransparency() {
            SectionMesh sectionMesh = this.getSectionMesh();
            if (sectionMesh instanceof CompiledSectionMesh) {
                CompiledSectionMesh mesh = (CompiledSectionMesh)sectionMesh;
                this.lastResortTransparencyTask = new ResortTransparencyTask(this, mesh);
                this.this$0.schedule(this.lastResortTransparencyTask);
            }
        }

        public boolean hasTranslucentGeometry() {
            return this.getSectionMesh().hasTranslucentGeometry();
        }

        public boolean transparencyResortingScheduled() {
            return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
        }

        private void cancelTasks() {
            if (this.lastCompileTask != null) {
                this.lastCompileTask.cancel();
                this.lastCompileTask = null;
            }
            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }
        }

        private SectionTask createCompileTask(RenderSectionRegion region) {
            this.cancelTasks();
            boolean isRecompile = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
            this.lastCompileTask = new CompileTask(this, region, isRecompile);
            return this.lastCompileTask;
        }

        public void compileAsync(RenderSectionRegion region) {
            SectionTask task = this.createCompileTask(region);
            this.this$0.schedule(task);
        }

        public void compileSync(RenderSectionRegion region) {
            SectionTask task = this.createCompileTask(region);
            task.doTask(this.this$0.fixedBuffers);
        }

        private SectionMesh setSectionMesh(SectionMesh sectionMesh) {
            SectionMesh oldMesh = this.sectionMesh.getAndSet(sectionMesh);
            this.this$0.onSectionMeshUpdate.accept(this);
            if (this.uploadedTime == 0L) {
                this.uploadedTime = Util.getMillis();
            }
            return oldMesh;
        }

        private void releaseSectionMesh(SectionMesh oldMesh) {
            oldMesh.close();
            for (SectionUberBuffers buffers : this.this$0.chunkUberBuffers.values()) {
                buffers.vertexBuffer.removeAllocation(oldMesh);
                buffers.indexBuffer.removeAllocation(oldMesh);
            }
        }

        private VertexSorting createVertexSorting(SectionPos sectionPos, Vec3 cameraPos) {
            return VertexSorting.byDistance((float)(cameraPos.x - (double)sectionPos.minBlockX()), (float)(cameraPos.y - (double)sectionPos.minBlockY()), (float)(cameraPos.z - (double)sectionPos.minBlockZ()));
        }

        private void checkSectionMesh(CompiledSectionMesh compiledSectionMesh) {
            boolean allBuffersUpdated = true;
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                SectionMesh.SectionDraw draw = compiledSectionMesh.getSectionDraw(layer);
                if (draw == null) continue;
                allBuffersUpdated &= compiledSectionMesh.isIndexBufferUploaded(layer);
                allBuffersUpdated &= compiledSectionMesh.isVertexBufferUploaded(layer);
            }
            if (allBuffersUpdated && this.sectionMesh.get() != compiledSectionMesh) {
                SectionMesh oldMesh = this.setSectionMesh(compiledSectionMesh);
                this.releaseSectionMesh(oldMesh);
            }
        }

        private void vertexBufferUploadCallback(CompiledSectionMesh sectionMesh, ChunkSectionLayer layer) {
            sectionMesh.setVertexBufferUploaded(layer);
            this.checkSectionMesh(sectionMesh);
        }

        private void indexBufferUploadCallback(CompiledSectionMesh sectionMesh, ChunkSectionLayer layer, boolean sortedIndexBuffer) {
            sectionMesh.setIndexBufferUploaded(layer);
            if (!sortedIndexBuffer) {
                this.checkSectionMesh(sectionMesh);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private boolean addSectionBuffersToUberBuffer(ChunkSectionLayer layer, CompiledSectionMesh key, @Nullable ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer) {
            boolean success = true;
            this.this$0.copyLock.lock();
            try {
                SectionMesh.SectionDraw draw = key.getSectionDraw(layer);
                if (draw != null) {
                    SectionUberBuffers sectionBuffers = this.this$0.chunkUberBuffers.get((Object)layer);
                    assert (sectionBuffers != null);
                    if (vertexBuffer != null) {
                        UberGpuBuffer.UploadCallback<CompiledSectionMesh> callback = mesh -> this.vertexBufferUploadCallback((CompiledSectionMesh)mesh, layer);
                        success &= sectionBuffers.vertexBuffer.addAllocation(key, callback, vertexBuffer);
                    }
                    if (indexBuffer != null) {
                        boolean sortedIndexBuffer = vertexBuffer == null;
                        UberGpuBuffer.UploadCallback<CompiledSectionMesh> callback = mesh -> this.indexBufferUploadCallback((CompiledSectionMesh)mesh, layer, sortedIndexBuffer);
                        success &= sectionBuffers.indexBuffer.addAllocation(key, callback, indexBuffer);
                    } else {
                        key.setIndexBufferUploaded(layer);
                    }
                }
                if (!success && RenderSystem.isOnRenderThread()) {
                    this.this$0.uploadTerrainBuffersToGpu();
                }
            }
            finally {
                this.this$0.copyLock.unlock();
            }
            return success;
        }

        @Environment(value=EnvType.CLIENT)
        private class ResortTransparencyTask
        extends SectionTask {
            private final CompiledSectionMesh compiledSectionMesh;
            final /* synthetic */ RenderSection this$1;

            public ResortTransparencyTask(RenderSection renderSection, CompiledSectionMesh compiledSectionMesh) {
                RenderSection renderSection2 = renderSection;
                Objects.requireNonNull(renderSection2);
                this.this$1 = renderSection2;
                super(renderSection, true);
                this.compiledSectionMesh = compiledSectionMesh;
            }

            @Override
            public SectionTask.SectionTaskResult doTask(SectionBufferBuilderPack buffers) {
                if (this.isCancelled.get()) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                MeshData.SortState state = this.compiledSectionMesh.getTransparencyState();
                if (state == null || this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                Vec3 cameraPos = this.this$1.this$0.cameraPosition.get();
                long sectionNode = this.this$1.sectionNode;
                VertexSorting vertexSorting = this.this$1.createVertexSorting(SectionPos.of(sectionNode), cameraPos);
                TranslucencyPointOfView translucencyPointOfView = TranslucencyPointOfView.of(cameraPos, sectionNode);
                if (!this.compiledSectionMesh.isDifferentPointOfView(translucencyPointOfView) && !translucencyPointOfView.isAxisAligned()) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                ByteBufferBuilder.Result indexBuffer = state.buildSortedIndexBuffer(buffers.buffer(ChunkSectionLayer.TRANSLUCENT), vertexSorting);
                if (indexBuffer == null) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                boolean success = false;
                while (!success) {
                    if (this.isCancelled.get()) {
                        indexBuffer.close();
                        return SectionTask.SectionTaskResult.CANCELLED;
                    }
                    success = this.this$1.addSectionBuffersToUberBuffer(ChunkSectionLayer.TRANSLUCENT, this.compiledSectionMesh, null, indexBuffer.byteBuffer());
                    if (success || RenderSystem.isOnRenderThread()) continue;
                    Thread.onSpinWait();
                }
                indexBuffer.close();
                this.compiledSectionMesh.setTranslucencyPointOfView(translucencyPointOfView);
                return SectionTask.SectionTaskResult.SUCCESSFUL;
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }

        @Environment(value=EnvType.CLIENT)
        public abstract class SectionTask {
            protected final AtomicBoolean isCancelled;
            protected final AtomicBoolean isCompleted;
            private final boolean isRecompile;
            final /* synthetic */ RenderSection this$1;

            public SectionTask(RenderSection this$1, boolean isRecompile) {
                RenderSection renderSection = this$1;
                Objects.requireNonNull(renderSection);
                this.this$1 = renderSection;
                this.isCancelled = new AtomicBoolean(false);
                this.isCompleted = new AtomicBoolean(false);
                this.isRecompile = isRecompile;
            }

            public abstract SectionTaskResult doTask(SectionBufferBuilderPack var1);

            public abstract void cancel();

            public boolean isRecompile() {
                return this.isRecompile;
            }

            public BlockPos getRenderOrigin() {
                return this.this$1.renderOrigin;
            }

            @Environment(value=EnvType.CLIENT)
            public static enum SectionTaskResult {
                SUCCESSFUL,
                CANCELLED;

            }
        }

        @Environment(value=EnvType.CLIENT)
        private class CompileTask
        extends SectionTask {
            private final RenderSectionRegion region;
            final /* synthetic */ RenderSection this$1;

            public CompileTask(RenderSection renderSection, RenderSectionRegion region, boolean isRecompile) {
                RenderSection renderSection2 = renderSection;
                Objects.requireNonNull(renderSection2);
                this.this$1 = renderSection2;
                super(renderSection, isRecompile);
                this.region = region;
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public SectionTask.SectionTaskResult doTask(SectionBufferBuilderPack buffers) {
                SectionCompiler.Results results;
                if (this.isCancelled.get()) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                long sectionNode = this.this$1.sectionNode;
                SectionPos sectionPos = SectionPos.of(sectionNode);
                if (this.isCancelled.get()) {
                    return SectionTask.SectionTaskResult.CANCELLED;
                }
                Vec3 cameraPos = this.this$1.this$0.cameraPosition.get();
                try (Zone ignored = Profiler.get().zone("Compile Section");){
                    results = this.this$1.this$0.sectionCompiler.compile(sectionPos, this.region, this.this$1.createVertexSorting(sectionPos, cameraPos), buffers);
                }
                TranslucencyPointOfView translucencyPointOfView = TranslucencyPointOfView.of(cameraPos, sectionNode);
                CompiledSectionMesh compiledSectionMesh = new CompiledSectionMesh(translucencyPointOfView, results);
                if (results.renderedLayers.isEmpty()) {
                    SectionMesh oldMesh = this.this$1.setSectionMesh(compiledSectionMesh);
                    this.this$1.this$0.copyLock.lock();
                    try {
                        this.this$1.releaseSectionMesh(oldMesh);
                    }
                    finally {
                        this.this$1.this$0.copyLock.unlock();
                    }
                    return SectionTask.SectionTaskResult.SUCCESSFUL;
                }
                for (Map.Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
                    MeshData meshData = entry.getValue();
                    boolean success = false;
                    while (!success) {
                        if (this.isCancelled.get()) {
                            results.release();
                            this.this$1.this$0.copyLock.lock();
                            try {
                                this.this$1.releaseSectionMesh(compiledSectionMesh);
                            }
                            finally {
                                this.this$1.this$0.copyLock.unlock();
                            }
                            return SectionTask.SectionTaskResult.CANCELLED;
                        }
                        success = this.this$1.addSectionBuffersToUberBuffer(entry.getKey(), compiledSectionMesh, meshData.vertexBuffer(), meshData.indexBuffer());
                        if (success || RenderSystem.isOnRenderThread()) continue;
                        Thread.onSpinWait();
                    }
                    meshData.close();
                }
                return SectionTask.SectionTaskResult.SUCCESSFUL;
            }

            @Override
            public void cancel() {
                this.isCancelled.compareAndSet(false, true);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record SectionUberBuffers(UberGpuBuffer<SectionMesh> vertexBuffer, UberGpuBuffer<SectionMesh> indexBuffer) {
    }

    @Environment(value=EnvType.CLIENT)
    public record RenderSectionBufferSlice(GpuBuffer vertexBuffer, long vertexBufferOffset, @Nullable GpuBuffer indexBuffer, long indexBufferOffset) {
    }
}

