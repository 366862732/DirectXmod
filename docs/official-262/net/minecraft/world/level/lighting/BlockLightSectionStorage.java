/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

public class BlockLightSectionStorage
extends LayerLightSectionStorage<BlockDataLayerStorageMap> {
    protected BlockLightSectionStorage(LightChunkGetter chunkSource) {
        super(LightLayer.BLOCK, chunkSource, new BlockDataLayerStorageMap((Long2ObjectOpenHashMap<DataLayer>)new Long2ObjectOpenHashMap()));
    }

    @Override
    protected int getLightValue(long blockNode) {
        long sectionNode = SectionPos.blockToSection(blockNode);
        DataLayer layer = this.getDataLayer(sectionNode, false);
        if (layer == null) {
            return 0;
        }
        return layer.get(SectionPos.sectionRelative(BlockPos.getX(blockNode)), SectionPos.sectionRelative(BlockPos.getY(blockNode)), SectionPos.sectionRelative(BlockPos.getZ(blockNode)));
    }

    protected static final class BlockDataLayerStorageMap
    extends DataLayerStorageMap<BlockDataLayerStorageMap> {
        public BlockDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> map) {
            super(map);
        }

        @Override
        public BlockDataLayerStorageMap copy() {
            return new BlockDataLayerStorageMap((Long2ObjectOpenHashMap<DataLayer>)this.map.clone());
        }
    }
}

