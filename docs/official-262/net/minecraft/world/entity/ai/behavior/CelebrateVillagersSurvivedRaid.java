/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.MoveToSkySeeingSpot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import org.jspecify.annotations.Nullable;

public class CelebrateVillagersSurvivedRaid
extends Behavior<Villager> {
    private @Nullable Raid currentRaid;

    public CelebrateVillagersSurvivedRaid(int minDuration, int maxDuration) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(), minDuration, maxDuration);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        BlockPos testPos = body.blockPosition();
        this.currentRaid = level.getRaidAt(testPos);
        return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(level, body, testPos);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return this.currentRaid != null && !this.currentRaid.isStopped();
    }

    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        this.currentRaid = null;
        body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        RandomSource random = body.getRandom();
        if (random.nextInt(100) == 0) {
            body.playCelebrateSound();
        }
        if (random.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(level, body, body.blockPosition())) {
            DyeColor color = Util.getRandom(DyeColor.values(), random);
            int flightDuration = random.nextInt(3);
            ItemStack firework = this.getFirework(color, flightDuration);
            Projectile.spawnProjectile(new FireworkRocketEntity(body.level(), body, body.getX(), body.getEyeY(), body.getZ(), firework), level, firework);
        }
    }

    private ItemStack getFirework(DyeColor color, int flightDuration) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks((byte)flightDuration, List.of(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of((int)color.getFireworkColor()), IntList.of(), false, false))));
        return rocket;
    }
}

