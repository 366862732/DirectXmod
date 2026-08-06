/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.golem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CopperGolemAi {
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 1.5f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0f;
    private static final int TRANSPORT_ITEM_HORIZONTAL_SEARCH_RADIUS = 32;
    private static final int TRANSPORT_ITEM_VERTICAL_SEARCH_RADIUS = 8;
    private static final int TICK_TO_START_ON_REACHED_INTERACTION = 1;
    private static final int TICK_TO_PLAY_ON_REACHED_SOUND = 9;
    private static final Predicate<BlockState> TRANSPORT_ITEM_SOURCE_BLOCK = block -> block.is(BlockTags.COPPER_CHESTS);
    private static final Predicate<BlockState> TRANSPORT_ITEM_DESTINATION_BLOCK = block -> block.is(Blocks.CHEST) || block.is(Blocks.TRAPPED_CHEST);

    protected static List<ActivityData<CopperGolem>> getActivities() {
        return List.of(CopperGolemAi.initCoreActivity(), CopperGolemAi.initIdleActivity());
    }

    public static void updateActivity(CopperGolem body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.IDLE));
    }

    private static ActivityData<CopperGolem> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new AnimalPanic(1.5f), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), InteractWithDoor.create(), (Object)new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS), (Object)new CountDownCooldownTicks(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS)));
    }

    private static ActivityData<CopperGolem> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, (Object)new TransportItemsBetweenContainers(1.0f, TRANSPORT_ITEM_SOURCE_BLOCK, TRANSPORT_ITEM_DESTINATION_BLOCK, 32, 8, CopperGolemAi.getTargetReachedInteractions(), CopperGolemAi.onTravelling(), CopperGolemAi.shouldQueueForTarget())), (Object)Pair.of((Object)1, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(40, 80))), (Object)Pair.of((Object)2, new RunOne((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_PRESENT)), ImmutableList.of((Object)Pair.of(RandomStroll.stroll(1.0f, 2, 2), (Object)1), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1))))));
    }

    private static Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> getTargetReachedInteractions() {
        return Map.of(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM, CopperGolemAi.onReachedTargetInteraction(CopperGolemState.GETTING_ITEM, SoundEvents.COPPER_GOLEM_ITEM_GET), TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM, CopperGolemAi.onReachedTargetInteraction(CopperGolemState.GETTING_NO_ITEM, SoundEvents.COPPER_GOLEM_ITEM_NO_GET), TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM, CopperGolemAi.onReachedTargetInteraction(CopperGolemState.DROPPING_ITEM, SoundEvents.COPPER_GOLEM_ITEM_DROP), TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM, CopperGolemAi.onReachedTargetInteraction(CopperGolemState.DROPPING_NO_ITEM, SoundEvents.COPPER_GOLEM_ITEM_NO_DROP));
    }

    private static TransportItemsBetweenContainers.OnTargetReachedInteraction onReachedTargetInteraction(CopperGolemState state, @Nullable SoundEvent sound) {
        return (body, target, ticksSinceReachingTarget) -> {
            if (body instanceof CopperGolem) {
                CopperGolem copperGolem = (CopperGolem)body;
                Container container = target.container();
                if (ticksSinceReachingTarget == 1) {
                    container.startOpen(copperGolem);
                    copperGolem.setOpenedChestPos(target.pos());
                    copperGolem.setState(state);
                }
                if (ticksSinceReachingTarget == 9 && sound != null) {
                    copperGolem.playSound(sound);
                }
                if (ticksSinceReachingTarget == 60) {
                    if (container.getEntitiesWithContainerOpen().contains(body)) {
                        container.stopOpen(copperGolem);
                    }
                    copperGolem.clearOpenedChestPos();
                }
            }
        };
    }

    private static Consumer<PathfinderMob> onTravelling() {
        return body -> {
            if (body instanceof CopperGolem) {
                CopperGolem copperGolem = (CopperGolem)body;
                copperGolem.clearOpenedChestPos();
                copperGolem.setState(CopperGolemState.IDLE);
            }
        };
    }

    private static Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget() {
        return transportTarget -> {
            BlockEntity patt0$temp = transportTarget.blockEntity();
            if (patt0$temp instanceof ChestBlockEntity) {
                ChestBlockEntity chestBlockEntity = (ChestBlockEntity)patt0$temp;
                return !chestBlockEntity.getEntitiesWithContainerOpen().isEmpty();
            }
            return false;
        };
    }
}

