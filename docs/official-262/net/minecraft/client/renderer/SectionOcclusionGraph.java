/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Queues
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongList
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.longs.LongSets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SectionOcclusionGraph {
    private static final int HALF_SECTION_SIZE = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    private @Nullable Future<?> fullUpdateTask;
    private @Nullable ViewArea viewArea;
    private final AtomicReference<@Nullable GraphState> currentGraph = new AtomicReference();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private final LongOpenHashSet emptySections = new LongOpenHashSet();
    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private volatile @Nullable BlockingQueue<SectionRenderDispatcher.RenderSection> nextSectionsToPropagateFrom;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private int prevFov = Integer.MAX_VALUE;
    private boolean lastSmartCull = true;

    public void waitAndReset(@Nullable ViewArea viewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            }
            catch (Exception e) {
                LOGGER.warn("Full update failed", (Throwable)e);
            }
        }
        this.viewArea = viewArea;
        if (viewArea != null) {
            this.currentGraph.set(new GraphState(viewArea));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
            this.emptySections.clear();
            this.loadedChunks.clear();
        }
    }

    public LongCollection expectedChunks() {
        GraphState graphState = this.currentGraph.get();
        return graphState != null ? graphState.storage.sectionsWaitingForChunkLoads.keySet() : LongSets.EMPTY_SET;
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void invalidateIfNeeded(CameraRenderState camera, int fov) {
        Vec3 cameraPos = camera.pos;
        double camX = Math.floor(cameraPos.x / 8.0);
        double camY = Math.floor(cameraPos.y / 8.0);
        double camZ = Math.floor(cameraPos.z / 8.0);
        if (camX != this.prevCamX || camY != this.prevCamY || camZ != this.prevCamZ || this.prevFov != fov || this.lastSmartCull != camera.smartCull) {
            this.invalidate();
        }
        this.prevCamX = camX;
        this.prevCamY = camY;
        this.prevCamZ = camZ;
        this.prevFov = fov;
        this.lastSmartCull = camera.smartCull;
    }

    public void addSectionsInFrustum(Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSection) {
        Frustum offsetFrustum = SectionOcclusionGraph.offsetFrustum(frustum);
        this.currentGraph.get().storage().sectionTree.visitNodes((node, fullyVisible, depth, isClose) -> {
            SectionRenderDispatcher.RenderSection renderSection = node.getSection();
            if (renderSection != null) {
                visibleSections.add(renderSection);
                if (isClose) {
                    nearbyVisibleSection.add(renderSection);
                }
            }
        }, offsetFrustum, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection section) {
        BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom;
        BlockingQueue<SectionRenderDispatcher.RenderSection> nextSectionsToPropagateFrom = this.nextSectionsToPropagateFrom;
        if (nextSectionsToPropagateFrom != null) {
            nextSectionsToPropagateFrom.add(section);
        }
        if ((sectionsToPropagateFrom = this.currentGraph.get().sectionsToPropagateFrom) != nextSectionsToPropagateFrom) {
            sectionsToPropagateFrom.add(section);
        }
    }

    public void update(CameraRenderState camera, int fov, ChunkLoadingRenderState chunkLoadingRenderState) {
        this.updateLoadedChunks(chunkLoadingRenderState.addedLoadedChunks, chunkLoadingRenderState.removedLoadedChunks);
        this.updateEmptySections(chunkLoadingRenderState.addedEmptySections, chunkLoadingRenderState.removedEmptySections);
        if (!camera.isFrustumCaptured) {
            this.invalidateIfNeeded(camera, fov);
            if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
                this.scheduleFullUpdate(camera);
            }
            this.runPartialUpdate(camera, (LongSet)chunkLoadingRenderState.loadedExpectedChunks);
        }
    }

    private void scheduleFullUpdate(CameraRenderState camera) {
        this.needsFullUpdate = false;
        LongOpenHashSet clonedEmptySections = this.emptySections.clone();
        LongOpenHashSet clonedLoadedChunks = this.loadedChunks.clone();
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            GraphState newState = new GraphState(this.viewArea);
            this.nextSectionsToPropagateFrom = newState.sectionsToPropagateFrom;
            ArrayDeque queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(camera.blockPos, queue);
            queue.forEach(node -> newState.storage.sectionToNodeMap.put(node.section, (Node)node));
            this.runUpdates(newState.storage, camera.pos, queue, camera.smartCull, node -> {}, clonedEmptySections, clonedLoadedChunks);
            this.currentGraph.set(newState);
            this.nextSectionsToPropagateFrom = null;
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(CameraRenderState camera, LongSet loadedExpectedChunks) {
        GraphState state = this.currentGraph.get();
        loadedExpectedChunks.forEach(chunkNode -> {
            LongList waitingSections = (LongList)state.storage.sectionsWaitingForChunkLoads.remove(chunkNode);
            if (waitingSections != null) {
                waitingSections.forEach(sectionNode -> {
                    SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(sectionNode);
                    if (section != null) {
                        this.schedulePropagationFrom(section);
                    }
                });
            }
        });
        if (!state.sectionsToPropagateFrom.isEmpty()) {
            ArrayDeque queue = Queues.newArrayDeque();
            while (!state.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection renderSection = (SectionRenderDispatcher.RenderSection)state.sectionsToPropagateFrom.poll();
                Node node = state.storage.sectionToNodeMap.get(renderSection);
                if (node == null || node.section != renderSection) continue;
                queue.add(node);
            }
            Frustum offsetFrustum = SectionOcclusionGraph.offsetFrustum(camera.cullFrustum);
            Consumer<SectionRenderDispatcher.RenderSection> onSectionAdded = section -> {
                if (offsetFrustum.isVisible(section.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                }
            };
            this.runUpdates(state.storage, camera.pos, queue, camera.smartCull, onSectionAdded, this.emptySections, this.loadedChunks);
        }
    }

    private void initializeQueueForFullUpdate(BlockPos cameraPosition, Queue<Node> queue) {
        long cameraSectionNode = SectionPos.asLong(cameraPosition);
        int cameraSectionY = SectionPos.y(cameraSectionNode);
        SectionRenderDispatcher.RenderSection cameraSection = this.viewArea.getRenderSection(cameraSectionNode);
        if (cameraSection == null) {
            boolean isBelowTheWorld = cameraSectionY < this.viewArea.minSectionY();
            int sectionY = isBelowTheWorld ? this.viewArea.minSectionY() : this.viewArea.maxSectionY();
            int viewDistance = this.viewArea.getViewDistance();
            ArrayList toAdd = Lists.newArrayList();
            int cameraSectionX = SectionPos.x(cameraSectionNode);
            int cameraSectionZ = SectionPos.z(cameraSectionNode);
            for (int sectionX = -viewDistance; sectionX <= viewDistance; ++sectionX) {
                for (int sectionZ = -viewDistance; sectionZ <= viewDistance; ++sectionZ) {
                    SectionRenderDispatcher.RenderSection renderSectionAt = this.viewArea.getRenderSection(SectionPos.asLong(sectionX + cameraSectionX, sectionY, sectionZ + cameraSectionZ));
                    if (renderSectionAt == null || !this.isInViewDistance(cameraSectionNode, renderSectionAt.getSectionNode())) continue;
                    Direction sourceDirection = isBelowTheWorld ? Direction.UP : Direction.DOWN;
                    Node node = new Node(renderSectionAt, sourceDirection, 0);
                    node.setDirections(node.directions, sourceDirection);
                    if (sectionX > 0) {
                        node.setDirections(node.directions, Direction.EAST);
                    } else if (sectionX < 0) {
                        node.setDirections(node.directions, Direction.WEST);
                    }
                    if (sectionZ > 0) {
                        node.setDirections(node.directions, Direction.SOUTH);
                    } else if (sectionZ < 0) {
                        node.setDirections(node.directions, Direction.NORTH);
                    }
                    toAdd.add(node);
                }
            }
            toAdd.sort(Comparator.comparingDouble(c -> cameraPosition.distSqr(SectionPos.of(c.section.getSectionNode()).center())));
            queue.addAll(toAdd);
        } else {
            queue.add(new Node(cameraSection, null, 0));
        }
    }

    private void runUpdates(GraphStorage storage, Vec3 cameraPos, Queue<Node> queue, boolean smartCull, Consumer<SectionRenderDispatcher.RenderSection> onSectionAdded, LongOpenHashSet emptySections, LongOpenHashSet loadedChunks) {
        SectionPos cameraSectionPos = SectionPos.of(cameraPos);
        long cameraSectionNode = cameraSectionPos.asLong();
        BlockPos cameraSectionCenter = cameraSectionPos.center();
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            SectionRenderDispatcher.RenderSection currentSection = node.section;
            long sectionNode = currentSection.getSectionNode();
            long chunkNode = ChunkPos.fromSectionNode(sectionNode);
            if (!loadedChunks.contains(chunkNode)) {
                ((LongList)storage.sectionsWaitingForChunkLoads.computeIfAbsent(chunkNode, l -> new LongArrayList())).add(sectionNode);
                continue;
            }
            if (!emptySections.contains(node.section.getSectionNode())) {
                if (storage.sectionTree.add(node.section)) {
                    onSectionAdded.accept(node.section);
                }
            } else {
                node.section.sectionMesh.compareAndSet(CompiledSectionMesh.UNCOMPILED, CompiledSectionMesh.EMPTY);
            }
            boolean distantFromCamera = Math.abs(SectionPos.x(sectionNode) - cameraSectionPos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE || Math.abs(SectionPos.y(sectionNode) - cameraSectionPos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE || Math.abs(SectionPos.z(sectionNode) - cameraSectionPos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;
            for (Direction direction : DIRECTIONS) {
                Node existingNode;
                SectionRenderDispatcher.RenderSection renderSectionAt = this.getRelativeFrom(cameraSectionNode, currentSection, direction);
                if (renderSectionAt == null || smartCull && node.hasDirection(direction.getOpposite())) continue;
                if (smartCull && node.hasSourceDirections()) {
                    SectionMesh sectionMesh = currentSection.getSectionMesh();
                    boolean visible = false;
                    for (int i = 0; i < DIRECTIONS.length; ++i) {
                        if (!node.hasSourceDirection(i) || !sectionMesh.facesCanSeeEachother(DIRECTIONS[i].getOpposite(), direction)) continue;
                        visible = true;
                        break;
                    }
                    if (!visible) continue;
                }
                if (smartCull && distantFromCamera) {
                    boolean maxY;
                    boolean maxX;
                    int renderSectionOriginX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
                    int renderSectionOriginY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
                    int renderSectionOriginZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
                    boolean bl = direction.getAxis() == Direction.Axis.X ? cameraSectionCenter.getX() > renderSectionOriginX : (maxX = cameraSectionCenter.getX() < renderSectionOriginX);
                    boolean bl2 = direction.getAxis() == Direction.Axis.Y ? cameraSectionCenter.getY() > renderSectionOriginY : (maxY = cameraSectionCenter.getY() < renderSectionOriginY);
                    boolean maxZ = direction.getAxis() == Direction.Axis.Z ? cameraSectionCenter.getZ() > renderSectionOriginZ : cameraSectionCenter.getZ() < renderSectionOriginZ;
                    Vector3d checkPos = new Vector3d((double)(renderSectionOriginX + (maxX ? 16 : 0)), (double)(renderSectionOriginY + (maxY ? 16 : 0)), (double)(renderSectionOriginZ + (maxZ ? 16 : 0)));
                    Vector3d step = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z).sub((Vector3dc)checkPos).normalize().mul(CEILED_SECTION_DIAGONAL);
                    boolean visible = true;
                    while (checkPos.distanceSquared(cameraPos.x, cameraPos.y, cameraPos.z) > 3600.0) {
                        checkPos.add((Vector3dc)step);
                        if (checkPos.y > (double)this.viewArea.maxY() || checkPos.y < (double)this.viewArea.minY()) break;
                        SectionRenderDispatcher.RenderSection checkSection = this.viewArea.getRenderSectionAt(BlockPos.containing(checkPos.x, checkPos.y, checkPos.z));
                        if (checkSection != null && storage.sectionToNodeMap.get(checkSection) != null) continue;
                        visible = false;
                        break;
                    }
                    if (!visible) continue;
                }
                if ((existingNode = storage.sectionToNodeMap.get(renderSectionAt)) != null) {
                    existingNode.addSourceDirection(direction);
                    continue;
                }
                Node newNode = new Node(renderSectionAt, direction, node.step + 1);
                newNode.setDirections(node.directions, direction);
                queue.add(newNode);
                storage.sectionToNodeMap.put(renderSectionAt, newNode);
            }
        }
    }

    private static Frustum offsetFrustum(Frustum frustum) {
        return new Frustum(frustum).offsetToFullyIncludeCameraCube(8);
    }

    private boolean isInViewDistance(long cameraSectionNode, long sectionNode) {
        return ChunkTrackingView.isInViewDistance(SectionPos.x(cameraSectionNode), SectionPos.z(cameraSectionNode), this.viewArea.getViewDistance(), SectionPos.x(sectionNode), SectionPos.z(sectionNode));
    }

    private @Nullable SectionRenderDispatcher.RenderSection getRelativeFrom(long cameraSectionNode, SectionRenderDispatcher.RenderSection renderSection, Direction direction) {
        long relative = renderSection.getNeighborSectionNode(direction);
        if (!this.isInViewDistance(cameraSectionNode, relative)) {
            return null;
        }
        if (Mth.abs(SectionPos.y(cameraSectionNode) - SectionPos.y(relative)) > this.viewArea.getViewDistance()) {
            return null;
        }
        return this.viewArea.getRenderSection(relative);
    }

    @VisibleForDebug
    public @Nullable Node getNode(SectionRenderDispatcher.RenderSection section) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(section);
    }

    public void updateEmptySections(LongOpenHashSet added, LongOpenHashSet removed) {
        this.emptySections.addAll((LongCollection)added);
        LongIterator iter = removed.longIterator();
        while (iter.hasNext()) {
            SectionRenderDispatcher.RenderSection section;
            long sectionNode = iter.nextLong();
            if (!this.emptySections.remove(sectionNode) || (section = this.viewArea.getRenderSection(sectionNode)) == null) continue;
            this.schedulePropagationFrom(section);
            section.setWasPreviouslyEmpty(true);
        }
    }

    public void updateLoadedChunks(LongOpenHashSet added, LongOpenHashSet removed) {
        this.loadedChunks.addAll((LongCollection)added);
        this.loadedChunks.removeAll((LongCollection)removed);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    @Environment(value=EnvType.CLIENT)
    private record GraphState(GraphStorage storage, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        private GraphState(ViewArea viewArea) {
            this(new GraphStorage(viewArea), new LinkedBlockingQueue<SectionRenderDispatcher.RenderSection>());
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class GraphStorage {
        public final SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Long2ObjectMap<LongList> sectionsWaitingForChunkLoads;

        public GraphStorage(ViewArea viewArea) {
            this.sectionToNodeMap = new SectionToNodeMap(viewArea.size());
            this.sectionTree = new Octree(viewArea.getCameraSectionPos(), viewArea.getViewDistance(), viewArea.sectionCount(), viewArea.minY());
            this.sectionsWaitingForChunkLoads = new Long2ObjectOpenHashMap();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class SectionToNodeMap {
        private final Node[] nodes;

        private SectionToNodeMap(int sectionCount) {
            this.nodes = new Node[sectionCount];
        }

        public void put(SectionRenderDispatcher.RenderSection renderSection, Node node) {
            this.nodes[renderSection.index] = node;
        }

        public @Nullable Node get(SectionRenderDispatcher.RenderSection renderSection) {
            int index = renderSection.index;
            if (index < 0 || index >= this.nodes.length) {
                return null;
            }
            return this.nodes[index];
        }
    }

    @Environment(value=EnvType.CLIENT)
    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        protected final SectionRenderDispatcher.RenderSection section;
        private byte sourceDirections;
        private byte directions;
        @VisibleForDebug
        public final int step;

        private Node(SectionRenderDispatcher.RenderSection section, @Nullable Direction sourceDirection, int step) {
            this.section = section;
            if (sourceDirection != null) {
                this.addSourceDirection(sourceDirection);
            }
            this.step = step;
        }

        private void setDirections(byte oldDirections, Direction direction) {
            this.directions = (byte)(this.directions | (oldDirections | 1 << direction.ordinal()));
        }

        private boolean hasDirection(Direction direction) {
            return (this.directions & 1 << direction.ordinal()) > 0;
        }

        private void addSourceDirection(Direction direction) {
            this.sourceDirections = (byte)(this.sourceDirections | (this.sourceDirections | 1 << direction.ordinal()));
        }

        @VisibleForDebug
        public boolean hasSourceDirection(int directionOrdinal) {
            return (this.sourceDirections & 1 << directionOrdinal) > 0;
        }

        private boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        public int hashCode() {
            return Long.hashCode(this.section.getSectionNode());
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) {
                return false;
            }
            Node other = (Node)obj;
            return this.section.getSectionNode() == other.section.getSectionNode();
        }
    }
}

