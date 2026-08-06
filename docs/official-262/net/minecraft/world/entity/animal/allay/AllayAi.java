/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.GoAndGiveItemsToTarget;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StayCloseToTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class AllayAi {
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_DEPOSIT_TARGET = 2.25f;
    private static final float SPEED_MULTIPLIER_WHEN_RETRIEVING_ITEM = 1.75f;
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.5f;
    private static final int CLOSE_ENOUGH_TO_TARGET = 4;
    private static final int TOO_FAR_FROM_TARGET = 16;
    private static final int MAX_LOOK_DISTANCE = 6;
    private static final int MIN_WAIT_DURATION = 30;
    private static final int MAX_WAIT_DURATION = 60;
    private static final int TIME_TO_FORGET_NOTEBLOCK = 600;
    private static final int DISTANCE_TO_WANTED_ITEM = 32;
    private static final int GIVE_ITEM_TIMEOUT_DURATION = 20;

    protected static List<ActivityData<Allay>> getActivities() {
        return List.of(AllayAi.initCoreActivity(), AllayAi.initIdleActivity());
    }

    private static ActivityData<Allay> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), new AnimalPanic(2.5f), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), (Object)new CountDownCooldownTicks(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS), (Object)new CountDownCooldownTicks(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)));
    }

    private static ActivityData<Allay> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, 0, ImmutableList.of(GoToWantedItem.create(mob -> true, 1.75f, true, 32), new GoAndGiveItemsToTarget<Allay>(AllayAi::getItemDepositPosition, 2.25f, 20, AllayAi::onItemThrown), StayCloseToTarget.create(AllayAi::getItemDepositPosition, Predicate.not(AllayAi::hasWantedItem), 4, 16, 2.25f), SetEntityLookTargetSometimes.create(6.0f, UniformInt.of(30, 60)), new RunOne(ImmutableList.of((Object)Pair.of(RandomStroll.fly(1.0f), (Object)2), (Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)2), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)))));
    }

    public static void updateActivity(Allay body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.IDLE));
    }

    public static void hearNoteblock(LivingEntity allay, BlockPos pos) {
        Brain<? extends LivingEntity> brain = allay.getBrain();
        GlobalPos globalPos = GlobalPos.of(allay.level().dimension(), pos);
        Optional<GlobalPos> likedNoteblockPos = brain.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
        if (likedNoteblockPos.isEmpty()) {
            brain.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION, globalPos);
            brain.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, 600);
        } else if (likedNoteblockPos.get().equals(globalPos)) {
            brain.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, 600);
        }
    }

    private static Optional<PositionTracker> getItemDepositPosition(LivingEntity allay) {
        Brain<? extends LivingEntity> brain = allay.getBrain();
        Optional<GlobalPos> likedNoteblockPos = brain.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
        if (likedNoteblockPos.isPresent()) {
            GlobalPos position = likedNoteblockPos.get();
            if (AllayAi.shouldDepositItemsAtLikedNoteblock(allay, brain, position)) {
                return Optional.of(new BlockPosTracker(position.pos().above()));
            }
            brain.eraseMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
        }
        return AllayAi.getLikedPlayerPositionTracker(allay);
    }

    private static boolean hasWantedItem(LivingEntity allay) {
        Brain<? extends LivingEntity> brain = allay.getBrain();
        return brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
    }

    private static boolean shouldDepositItemsAtLikedNoteblock(LivingEntity allay, Brain<?> brain, GlobalPos likedNoteblockPos) {
        Optional<Integer> likedNoteblockCooldown = brain.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS);
        Level level = allay.level();
        return likedNoteblockPos.isCloseEnough(level.dimension(), allay.blockPosition(), 1024) && level.getBlockState(likedNoteblockPos.pos()).is(Blocks.NOTE_BLOCK) && likedNoteblockCooldown.isPresent();
    }

    private static Optional<PositionTracker> getLikedPlayerPositionTracker(LivingEntity allay) {
        return AllayAi.getLikedPlayer(allay).map(serverPlayer -> new EntityTracker((Entity)serverPlayer, true));
    }

    public static Optional<ServerPlayer> getLikedPlayer(LivingEntity allay) {
        Level level = allay.level();
        if (!level.isClientSide() && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Optional<UUID> likedPlayer = allay.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayer.isPresent()) {
                Entity entity = serverLevel.getEntity(likedPlayer.get());
                if (entity instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)entity;
                    if ((serverPlayer.gameMode.isSurvival() || serverPlayer.gameMode.isCreative()) && serverPlayer.closerThan(allay, 64.0)) {
                        return Optional.of(serverPlayer);
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static void onItemThrown(ServerLevel level, Allay thrower, ItemStack item, BlockPos targetPos) {
        AllayAi.getLikedPlayer(thrower).ifPresent(player -> CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.trigger((ServerPlayer)player, targetPos.below(), item));
        if (level.getGameTime() % 7L == 0L && level.getRandom().nextDouble() < 0.9) {
            float pitch = Util.getRandom(Allay.THROW_SOUND_PITCHES, level.getRandom()).floatValue();
            level.playSound(null, thrower, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1.0f, pitch);
        }
    }
}

