/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LightEventListener;
import org.jspecify.annotations.Nullable;

public interface LayerLightEventListener
extends LightEventListener {
    public @Nullable DataLayer getDataLayerData(SectionPos var1);

    public int getLightValue(BlockPos var1);

    public static enum DummyLightLayerEventListener implements LayerLightEventListener
    {
        INSTANCE;


        @Override
        public @Nullable DataLayer getDataLayerData(SectionPos pos) {
            return null;
        }

        @Override
        public int getLightValue(BlockPos pos) {
            return 0;
        }

        @Override
        public void checkBlock(BlockPos pos) {
        }

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runLightUpdates() {
            return 0;
        }

        @Override
        public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {
        }

        @Override
        public void setLightEnabled(ChunkPos pos, boolean enable) {
        }

        @Override
        public void propagateLightSources(ChunkPos pos) {
        }
    }
}

