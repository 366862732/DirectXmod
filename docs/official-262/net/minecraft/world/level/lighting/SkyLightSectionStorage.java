/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

public class SkyLightSectionStorage
extends LayerLightSectionStorage<SkyDataLayerStorageMap> {
    protected SkyLightSectionStorage(LightChunkGetter chunkSource) {
        super(LightLayer.SKY, chunkSource, new SkyDataLayerStorageMap((Long2ObjectOpenHashMap<DataLayer>)new Long2ObjectOpenHashMap(), new Long2IntOpenHashMap(), Integer.MAX_VALUE));
    }

    @Override
    protected int getLightValue(long blockNode) {
        return this.getLightValue(blockNode, false);
    }

    protected int getLightValue(long blockNode, boolean updating) {
        long sectionNode = SectionPos.blockToSection(blockNode);
        int sectionY = SectionPos.y(sectionNode);
        SkyDataLayerStorageMap sections = updating ? (SkyDataLayerStorageMap)this.updatingSectionData : (SkyDataLayerStorageMap)this.visibleSectionData;
        int topSection = sections.topSections.get(SectionPos.getZeroNode(sectionNode));
        if (topSection == sections.currentLowestY || sectionY >= topSection) {
            if (updating && !this.lightOnInSection(sectionNode)) {
                return 0;
            }
            return 15;
        }
        DataLayer layer = this.getDataLayer(sections, sectionNode);
        if (layer == null) {
            blockNode = BlockPos.getFlatIndex(blockNode);
            while (layer == null) {
                if (++sectionY >= topSection) {
                    return 15;
                }
                sectionNode = SectionPos.offset(sectionNode, Direction.UP);
                layer = this.getDataLayer(sections, sectionNode);
            }
        }
        return layer.get(SectionPos.sectionRelative(BlockPos.getX(blockNode)), SectionPos.sectionRelative(BlockPos.getY(blockNode)), SectionPos.sectionRelative(BlockPos.getZ(blockNode)));
    }

    @Override
    protected void onNodeAdded(long sectionNode) {
        long zeroNode;
        int oldTop;
        int y = SectionPos.y(sectionNode);
        if (((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY > y) {
            ((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY = y;
            ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.defaultReturnValue(((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY);
        }
        if ((oldTop = ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.get(zeroNode = SectionPos.getZeroNode(sectionNode))) < y + 1) {
            ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.put(zeroNode, y + 1);
        }
    }

    @Override
    protected void onNodeRemoved(long sectionNode) {
        long zeroNode = SectionPos.getZeroNode(sectionNode);
        int y = SectionPos.y(sectionNode);
        if (((SkyDataLayerStorageMap)this.updatingSectionData).topSections.get(zeroNode) == y + 1) {
            long newTopSection = sectionNode;
            while (!this.storingLightForSection(newTopSection) && this.hasLightDataAtOrBelow(y)) {
                --y;
                newTopSection = SectionPos.offset(newTopSection, Direction.DOWN);
            }
            if (this.storingLightForSection(newTopSection)) {
                ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.put(zeroNode, y + 1);
            } else {
                ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.remove(zeroNode);
            }
        }
    }

    @Override
    protected DataLayer createDataLayer(long sectionNode) {
        DataLayer aboveData;
        DataLayer queuedLayer = (DataLayer)this.queuedSections.get(sectionNode);
        if (queuedLayer != null) {
            return queuedLayer;
        }
        int topSection = ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.get(SectionPos.getZeroNode(sectionNode));
        if (topSection == ((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY || SectionPos.y(sectionNode) >= topSection) {
            if (this.lightOnInSection(sectionNode)) {
                return new DataLayer(15);
            }
            return new DataLayer();
        }
        long aboveSection = SectionPos.offset(sectionNode, Direction.UP);
        while ((aboveData = this.getDataLayer(aboveSection, true)) == null) {
            aboveSection = SectionPos.offset(aboveSection, Direction.UP);
        }
        return SkyLightSectionStorage.repeatFirstLayer(aboveData);
    }

    private static DataLayer repeatFirstLayer(DataLayer data) {
        if (data.isDefinitelyHomogenous()) {
            return data.copy();
        }
        byte[] input = data.getData();
        byte[] output = new byte[2048];
        for (int i = 0; i < 16; ++i) {
            System.arraycopy(input, 0, output, i * 128, 128);
        }
        return new DataLayer(output);
    }

    protected boolean hasLightDataAtOrBelow(int sectionY) {
        return sectionY >= ((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY;
    }

    protected boolean isAboveData(long sectionNode) {
        long zeroNode = SectionPos.getZeroNode(sectionNode);
        int topSection = ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.get(zeroNode);
        return topSection == ((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY || SectionPos.y(sectionNode) >= topSection;
    }

    protected int getTopSectionY(long zeroNode) {
        return ((SkyDataLayerStorageMap)this.updatingSectionData).topSections.get(zeroNode);
    }

    protected int getBottomSectionY() {
        return ((SkyDataLayerStorageMap)this.updatingSectionData).currentLowestY;
    }

    protected static final class SkyDataLayerStorageMap
    extends DataLayerStorageMap<SkyDataLayerStorageMap> {
        private int currentLowestY;
        private final Long2IntOpenHashMap topSections;

        public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> map, Long2IntOpenHashMap topSections, int currentLowestY) {
            super(map);
            this.topSections = topSections;
            topSections.defaultReturnValue(currentLowestY);
            this.currentLowestY = currentLowestY;
        }

        @Override
        public SkyDataLayerStorageMap copy() {
            return new SkyDataLayerStorageMap((Long2ObjectOpenHashMap<DataLayer>)this.map.clone(), this.topSections.clone(), this.currentLowestY);
        }
    }
}

