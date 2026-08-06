/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PiglinSpecificSensor
extends Sensor<LivingEntity> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, (Object[])new MemoryModuleType[]{MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_REPELLENT});
    }

    @Override
    protected void doTick(ServerLevel level, LivingEntity body) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, PiglinSpecificSensor.findNearestRepellent(level, body));
        Optional<Object> nemesis = Optional.empty();
        Optional<Object> huntableHoglin = Optional.empty();
        Optional<Object> babyHoglin = Optional.empty();
        Optional<Object> babyPiglin = Optional.empty();
        Optional<Object> zombified = Optional.empty();
        Optional<Object> playerNotWearingGold = Optional.empty();
        Optional<Object> playerHoldingWantedItem = Optional.empty();
        int visibleAdultHoglinCount = 0;
        ArrayList<AbstractPiglin> visibleAdultPiglins = new ArrayList<AbstractPiglin>();
        NearestVisibleLivingEntities visibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());
        for (LivingEntity entity : visibleLivingEntities.findAll(ignored -> true)) {
            if (entity instanceof Hoglin) {
                Hoglin hoglin = (Hoglin)entity;
                if (hoglin.isBaby() && babyHoglin.isEmpty()) {
                    babyHoglin = Optional.of(hoglin);
                    continue;
                }
                if (!hoglin.isAdult()) continue;
                ++visibleAdultHoglinCount;
                if (!huntableHoglin.isEmpty() || !hoglin.canBeHunted()) continue;
                huntableHoglin = Optional.of(hoglin);
                continue;
            }
            if (entity instanceof PiglinBrute) {
                PiglinBrute piglinBrute = (PiglinBrute)entity;
                visibleAdultPiglins.add(piglinBrute);
                continue;
            }
            if (entity instanceof Piglin) {
                Piglin piglin = (Piglin)entity;
                if (piglin.isBaby() && babyPiglin.isEmpty()) {
                    babyPiglin = Optional.of(piglin);
                    continue;
                }
                if (!piglin.isAdult()) continue;
                visibleAdultPiglins.add(piglin);
                continue;
            }
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (playerNotWearingGold.isEmpty() && !PiglinAi.isWearingSafeArmor(player) && body.canAttack(entity)) {
                    playerNotWearingGold = Optional.of(player);
                }
                if (!playerHoldingWantedItem.isEmpty() || player.isSpectator() || !PiglinAi.isPlayerHoldingLovedItem(player)) continue;
                playerHoldingWantedItem = Optional.of(player);
                continue;
            }
            if (nemesis.isEmpty() && (entity instanceof WitherSkeleton || entity instanceof WitherBoss)) {
                nemesis = Optional.of((Mob)entity);
                continue;
            }
            if (!zombified.isEmpty() || !PiglinAi.isZombified(entity)) continue;
            zombified = Optional.of(entity);
        }
        List<AbstractPiglin> adultPiglins = PiglinAi.findNearbyAdultPiglins(brain);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nemesis);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, huntableHoglin);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, babyHoglin);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, zombified);
        brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, playerNotWearingGold);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, playerHoldingWantedItem);
        brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, adultPiglins);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, visibleAdultPiglins);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, visibleAdultPiglins.size());
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, visibleAdultHoglinCount);
    }

    private static Optional<BlockPos> findNearestRepellent(ServerLevel level, LivingEntity body) {
        return BlockPos.findClosestMatch(body.blockPosition(), 8, 4, pos -> PiglinSpecificSensor.isValidRepellent(level, pos));
    }

    private static boolean isValidRepellent(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        boolean isRepellent = blockState.is(BlockTags.PIGLIN_REPELLENTS);
        if (isRepellent && blockState.is(Blocks.SOUL_CAMPFIRE)) {
            return CampfireBlock.isLitCampfire(blockState);
        }
        return isRepellent;
    }
}

