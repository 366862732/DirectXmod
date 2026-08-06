/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.npc.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.ServerLevelAccessor;

public interface VillagerDataHolder {
    public VillagerData getVillagerData();

    public void setVillagerData(VillagerData var1);

    public boolean getVillagerDataFinalized();

    public void setVillagerDataFinalized(boolean var1);

    default public void finalizeVillagerType(ServerLevelAccessor level, BlockPos pos) {
        if (!this.getVillagerDataFinalized()) {
            this.setVillagerData(this.getVillagerData().withType(level.registryAccess(), VillagerType.byBiome(level.getBiome(pos))));
            this.setVillagerDataFinalized(true);
        }
    }
}

