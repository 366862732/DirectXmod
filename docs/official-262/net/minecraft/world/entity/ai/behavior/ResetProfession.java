/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class ResetProfession {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.JOB_SITE)).apply((Applicative)i, jobSite -> (level, body, timestamp) -> {
            boolean canBeFired;
            VillagerData bodyData = body.getVillagerData();
            boolean bl = canBeFired = !bodyData.profession().is(VillagerProfession.NONE) && !bodyData.profession().is(VillagerProfession.NITWIT);
            if (canBeFired && body.getVillagerXp() == 0 && bodyData.level() <= 1) {
                body.setVillagerData(body.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
                body.refreshBrain(level);
                return true;
            }
            return false;
        }));
    }
}

