/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public interface LightEventListener {
    public void checkBlock(BlockPos var1);

    public boolean hasLightWork();

    public int runLightUpdates();

    default public void updateSectionStatus(BlockPos pos, boolean sectionEmpty) {
        this.updateSectionStatus(SectionPos.of(pos), sectionEmpty);
    }

    public void updateSectionStatus(SectionPos var1, boolean var2);

    public void setLightEnabled(ChunkPos var1, boolean var2);

    public void propagateLightSources(ChunkPos var1);
}

