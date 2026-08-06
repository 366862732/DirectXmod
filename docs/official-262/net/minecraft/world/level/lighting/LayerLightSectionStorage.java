/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMaps
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;

public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> {
    private final LightLayer layer;
    protected final LightChunkGetter chunkSource;
    protected final Long2ByteMap sectionStates = new Long2ByteOpenHashMap();
    private final LongSet columnsWithSources = new LongOpenHashSet();
    protected volatile M visibleSectionData;
    protected final M updatingSectionData;
    protected final LongSet changedSections = new LongOpenHashSet();
    protected final LongSet sectionsAffectedByLightUpdates = new LongOpenHashSet();
    protected final Long2ObjectMap<DataLayer> queuedSections = Long2ObjectMaps.synchronize((Long2ObjectMap)new Long2ObjectOpenHashMap());
    private final LongSet columnsToRetainQueuedDataFor = new LongOpenHashSet();
    private final LongSet toRemove = new LongOpenHashSet();
    protected volatile boolean hasInconsistencies;

    protected LayerLightSectionStorage(LightLayer layer, LightChunkGetter chunkSource, M initialMap) {
        this.layer = layer;
        this.chunkSource = chunkSource;
        this.updatingSectionData = initialMap;
        this.visibleSectionData = ((DataLayerStorageMap)initialMap).copy();
        ((DataLayerStorageMap)this.visibleSectionData).disableCache();
        this.sectionStates.defaultReturnValue((byte)0);
    }

    protected boolean storingLightForSection(long sectionNode) {
        return this.getDataLayer(sectionNode, true) != null;
    }

    protected @Nullable DataLayer getDataLayer(long sectionNode, boolean updating) {
        return this.getDataLayer(updating ? this.updatingSectionData : this.visibleSectionData, sectionNode);
    }

    protected @Nullable DataLayer getDataLayer(M sections, long sectionNode) {
        return ((DataLayerStorageMap)sections).getLayer(sectionNode);
    }

    protected @Nullable DataLayer getDataLayerToWrite(long sectionNode) {
        DataLayer dataLayer = ((DataLayerStorageMap)this.updatingSectionData).getLayer(sectionNode);
        if (dataLayer == null) {
            return null;
        }
        if (this.changedSections.add(sectionNode)) {
            dataLayer = dataLayer.copy();
            ((DataLayerStorageMap)this.updatingSectionData).setLayer(sectionNode, dataLayer);
            ((DataLayerStorageMap)this.updatingSectionData).clearCache();
        }
        return dataLayer;
    }

    public @Nullable DataLayer getDataLayerData(long sectionNode) {
        DataLayer layer = (DataLayer)this.queuedSections.get(sectionNode);
        if (layer != null) {
            return layer;
        }
        return this.getDataLayer(sectionNode, false);
    }

    protected abstract int getLightValue(long var1);

    protected int getStoredLevel(long blockNode) {
        long sectionNode = SectionPos.blockToSection(blockNode);
        DataLayer layer = this.getDataLayer(sectionNode, true);
        return layer.get(SectionPos.sectionRelative(BlockPos.getX(blockNode)), SectionPos.sectionRelative(BlockPos.getY(blockNode)), SectionPos.sectionRelative(BlockPos.getZ(blockNode)));
    }

    protected void setStoredLevel(long blockNode, int level) {
        long sectionNode = SectionPos.blockToSection(blockNode);
        DataLayer layer = this.changedSections.add(sectionNode) ? ((DataLayerStorageMap)this.updatingSectionData).copyDataLayer(sectionNode) : this.getDataLayer(sectionNode, true);
        layer.set(SectionPos.sectionRelative(BlockPos.getX(blockNode)), SectionPos.sectionRelative(BlockPos.getY(blockNode)), SectionPos.sectionRelative(BlockPos.getZ(blockNode)), level);
        SectionPos.aroundAndAtBlockPos(blockNode, arg_0 -> ((LongSet)this.sectionsAffectedByLightUpdates).add(arg_0));
    }

    protected void markSectionAndNeighborsAsAffected(long sectionNode) {
        int x = SectionPos.x(sectionNode);
        int y = SectionPos.y(sectionNode);
        int z = SectionPos.z(sectionNode);
        for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
            for (int offsetX = -1; offsetX <= 1; ++offsetX) {
                for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                    this.sectionsAffectedByLightUpdates.add(SectionPos.asLong(x + offsetX, y + offsetY, z + offsetZ));
                }
            }
        }
    }

    protected DataLayer createDataLayer(long sectionNode) {
        DataLayer queuedLayer = (DataLayer)this.queuedSections.get(sectionNode);
        if (queuedLayer != null) {
            return queuedLayer;
        }
        return new DataLayer();
    }

    protected boolean hasInconsistencies() {
        return this.hasInconsistencies;
    }

    protected void markNewInconsistencies(LightEngine<M, ?> engine) {
        long node;
        if (!this.hasInconsistencies) {
            return;
        }
        this.hasInconsistencies = false;
        LongIterator longIterator = this.toRemove.iterator();
        while (longIterator.hasNext()) {
            node = (Long)longIterator.next();
            DataLayer queued = (DataLayer)this.queuedSections.remove(node);
            DataLayer stored = ((DataLayerStorageMap)this.updatingSectionData).removeLayer(node);
            if (!this.columnsToRetainQueuedDataFor.contains(SectionPos.getZeroNode(node))) continue;
            if (queued != null) {
                this.queuedSections.put(node, (Object)queued);
                continue;
            }
            if (stored == null) continue;
            this.queuedSections.put(node, (Object)stored);
        }
        ((DataLayerStorageMap)this.updatingSectionData).clearCache();
        longIterator = this.toRemove.iterator();
        while (longIterator.hasNext()) {
            node = (Long)longIterator.next();
            this.onNodeRemoved(node);
            this.changedSections.add(node);
        }
        this.toRemove.clear();
        ObjectIterator iterator = Long2ObjectMaps.fastIterator(this.queuedSections);
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)iterator.next();
            long sectionNode = entry.getLongKey();
            if (!this.storingLightForSection(sectionNode)) continue;
            DataLayer data = (DataLayer)entry.getValue();
            if (((DataLayerStorageMap)this.updatingSectionData).getLayer(sectionNode) != data) {
                ((DataLayerStorageMap)this.updatingSectionData).setLayer(sectionNode, data);
                this.changedSections.add(sectionNode);
            }
            iterator.remove();
        }
        ((DataLayerStorageMap)this.updatingSectionData).clearCache();
    }

    protected void onNodeAdded(long sectionNode) {
    }

    protected void onNodeRemoved(long sectionNode) {
    }

    protected void setLightEnabled(long zeroNode, boolean enable) {
        if (enable) {
            this.columnsWithSources.add(zeroNode);
        } else {
            this.columnsWithSources.remove(zeroNode);
        }
    }

    protected boolean lightOnInSection(long sectionNode) {
        long zeroNode = SectionPos.getZeroNode(sectionNode);
        return this.columnsWithSources.contains(zeroNode);
    }

    protected boolean lightOnInColumn(long sectionZeroNode) {
        return this.columnsWithSources.contains(sectionZeroNode);
    }

    public void retainData(long zeroNode, boolean retain) {
        if (retain) {
            this.columnsToRetainQueuedDataFor.add(zeroNode);
        } else {
            this.columnsToRetainQueuedDataFor.remove(zeroNode);
        }
    }

    protected void queueSectionData(long sectionNode, @Nullable DataLayer data) {
        if (data != null) {
            this.queuedSections.put(sectionNode, (Object)data);
            this.hasInconsistencies = true;
        } else {
            this.queuedSections.remove(sectionNode);
        }
    }

    protected void updateSectionStatus(long sectionNode, boolean sectionEmpty) {
        byte newState;
        byte state = this.sectionStates.get(sectionNode);
        if (state == (newState = SectionState.hasData(state, !sectionEmpty))) {
            return;
        }
        this.putSectionState(sectionNode, newState);
        int neighborIncrement = sectionEmpty ? -1 : 1;
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
                    long neighborNode = SectionPos.offset(sectionNode, offsetX, offsetY, offsetZ);
                    byte neighborState = this.sectionStates.get(neighborNode);
                    this.putSectionState(neighborNode, SectionState.neighborCount(neighborState, SectionState.neighborCount(neighborState) + neighborIncrement));
                }
            }
        }
    }

    protected void putSectionState(long sectionNode, byte state) {
        if (state != 0) {
            if (this.sectionStates.put(sectionNode, state) == 0) {
                this.initializeSection(sectionNode);
            }
        } else if (this.sectionStates.remove(sectionNode) != 0) {
            this.removeSection(sectionNode);
        }
    }

    private void initializeSection(long sectionNode) {
        if (!this.toRemove.remove(sectionNode)) {
            ((DataLayerStorageMap)this.updatingSectionData).setLayer(sectionNode, this.createDataLayer(sectionNode));
            this.changedSections.add(sectionNode);
            this.onNodeAdded(sectionNode);
            this.markSectionAndNeighborsAsAffected(sectionNode);
            this.hasInconsistencies = true;
        }
    }

    private void removeSection(long sectionNode) {
        this.toRemove.add(sectionNode);
        this.hasInconsistencies = true;
    }

    protected void swapSectionMap() {
        if (!this.changedSections.isEmpty()) {
            Object copy = ((DataLayerStorageMap)this.updatingSectionData).copy();
            ((DataLayerStorageMap)copy).disableCache();
            this.visibleSectionData = copy;
            this.changedSections.clear();
        }
        if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
            LongIterator iterator = this.sectionsAffectedByLightUpdates.iterator();
            while (iterator.hasNext()) {
                long sectionNode = iterator.nextLong();
                this.chunkSource.onLightUpdate(this.layer, SectionPos.of(sectionNode));
            }
            this.sectionsAffectedByLightUpdates.clear();
        }
    }

    public SectionType getDebugSectionType(long sectionNode) {
        return SectionState.type(this.sectionStates.get(sectionNode));
    }

    protected static class SectionState {
        public static final byte EMPTY = 0;
        private static final int MIN_NEIGHBORS = 0;
        private static final int MAX_NEIGHBORS = 26;
        private static final byte HAS_DATA_BIT = 32;
        private static final byte NEIGHBOR_COUNT_BITS = 31;

        protected SectionState() {
        }

        public static byte hasData(byte state, boolean hasData) {
            return (byte)(hasData ? state | 0x20 : state & 0xFFFFFFDF);
        }

        public static byte neighborCount(byte state, int neighborCount) {
            if (neighborCount < 0 || neighborCount > 26) {
                throw new IllegalArgumentException("Neighbor count was not within range [0; 26]");
            }
            return (byte)(state & 0xFFFFFFE0 | neighborCount & 0x1F);
        }

        public static boolean hasData(byte state) {
            return (state & 0x20) != 0;
        }

        public static int neighborCount(byte state) {
            return state & 0x1F;
        }

        public static SectionType type(byte state) {
            if (state == 0) {
                return SectionType.EMPTY;
            }
            if (SectionState.hasData(state)) {
                return SectionType.LIGHT_AND_DATA;
            }
            return SectionType.LIGHT_ONLY;
        }
    }

    public static enum SectionType {
        EMPTY("2"),
        LIGHT_ONLY("1"),
        LIGHT_AND_DATA("0");

        private final String display;

        private SectionType(String display) {
            this.display = display;
        }

        public String display() {
            return this.display;
        }
    }
}

