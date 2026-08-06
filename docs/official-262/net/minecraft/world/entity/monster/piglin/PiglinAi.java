/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.SpearApproach;
import net.minecraft.world.entity.ai.behavior.SpearAttack;
import net.minecraft.world.entity.ai.behavior.SpearRetreat;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.RememberIfHoglinWasKilled;
import net.minecraft.world.entity.monster.piglin.StartAdmiringItemIfSeen;
import net.minecraft.world.entity.monster.piglin.StartHuntingHoglin;
import net.minecraft.world.entity.monster.piglin.StopAdmiringIfItemTooFarAway;
import net.minecraft.world.entity.monster.piglin.StopAdmiringIfTiredOfTryingToReachItem;
import net.minecraft.world.entity.monster.piglin.StopHoldingItemIfNoLongerAdmiring;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    private static final int PLAYER_ANGER_RANGE = 16;
    private static final int ANGER_DURATION = 600;
    private static final int ADMIRE_DURATION = 119;
    private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
    private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
    private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
    private static final int CELEBRATION_TIME = 300;
    public static final int MAX_TIME_BETWEEN_HUNTS = 120;
    protected static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
    private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
    private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
    private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
    private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
    private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final int EAT_COOLDOWN = 200;
    private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
    private static final int MAX_LOOK_DIST = 8;
    private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
    private static final int INTERACTION_RANGE = 8;
    private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
    private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75f;
    private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
    private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1f;
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8f;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6f;

    public static List<ActivityData<Piglin>> getActivities(Piglin piglin) {
        return List.of(PiglinAi.initCoreActivity(), PiglinAi.initIdleActivity(), PiglinAi.initAdmireItemActivity(), PiglinAi.initFightActivity(piglin), PiglinAi.initCelebrateActivity(), PiglinAi.initRetreatActivity(), PiglinAi.initRideHoglinActivity());
    }

    protected static void initMemories(Piglin body, RandomSource random) {
        int delayUntilFirstHunt = TIME_BETWEEN_HUNTS.sample(random);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, delayUntilFirstHunt);
    }

    private static ActivityData<Piglin> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of((Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), InteractWithDoor.create(), PiglinAi.babyAvoidNemesis(), PiglinAi.avoidZombified(), StopHoldingItemIfNoLongerAdmiring.create(), StartAdmiringItemIfSeen.create(119), StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance), StopBeingAngryIfTargetDead.create()));
    }

    private static ActivityData<Piglin> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, 10, ImmutableList.of(SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0f), StartAttacking.create((level, piglin) -> piglin.isAdult(), PiglinAi::findNearestValidAttackTarget), BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create()), PiglinAi.avoidRepellent(), PiglinAi.babySometimesRideBabyHoglin(), PiglinAi.createIdleLookBehaviors(), PiglinAi.createIdleMovementBehaviors(), SetLookAndInteract.create(EntityTypes.PLAYER, 4)));
    }

    private static ActivityData<Piglin> initFightActivity(Piglin body) {
        return ActivityData.create(Activity.FIGHT, 10, ImmutableList.of(StopAttackingIfTargetInvalid.create((level, target) -> !PiglinAi.isNearestValidAttackTarget(level, body, target)), BehaviorBuilder.triggerIf(PiglinAi::hasCrossbow, BackUpIfTooClose.create(5, 0.75f)), SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0f), (Object)new SpearApproach(1.0, 10.0f), (Object)new SpearAttack(1.0, 1.0, 2.0f), (Object)new SpearRetreat(1.0), MeleeAttack.create(20), new CrossbowAttack(), RememberIfHoglinWasKilled.create(), EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static ActivityData<Piglin> initCelebrateActivity() {
        return ActivityData.create(Activity.CELEBRATE, 10, ImmutableList.of(PiglinAi.avoidRepellent(), SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0f), StartAttacking.create((level, piglin) -> piglin.isAdult(), PiglinAi::findNearestValidAttackTarget), BehaviorBuilder.triggerIf(piglin -> !piglin.isDancing(), GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 2, 1.0f)), BehaviorBuilder.triggerIf(Piglin::isDancing, GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 4, 0.6f)), new RunOne(ImmutableList.of((Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0f), (Object)1), (Object)Pair.of(RandomStroll.stroll(0.6f, 2, 1), (Object)1), (Object)Pair.of((Object)new DoNothing(10, 20), (Object)1)))), MemoryModuleType.CELEBRATE_LOCATION);
    }

    private static ActivityData<Piglin> initAdmireItemActivity() {
        return ActivityData.create(Activity.ADMIRE_ITEM, 10, ImmutableList.of(GoToWantedItem.create(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0f, true, 9), StopAdmiringIfItemTooFarAway.create(9), StopAdmiringIfTiredOfTryingToReachItem.create(200, 200)), MemoryModuleType.ADMIRING_ITEM);
    }

    private static ActivityData<Piglin> initRetreatActivity() {
        return ActivityData.create(Activity.AVOID, 10, ImmutableList.of(SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0f, 12, true), PiglinAi.createIdleLookBehaviors(), PiglinAi.createIdleMovementBehaviors(), EraseMemoryIf.create(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static ActivityData<Piglin> initRideHoglinActivity() {
        return ActivityData.create(Activity.RIDE, 10, ImmutableList.of(Mount.create(0.8f), SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 8.0f), BehaviorBuilder.sequence(BehaviorBuilder.triggerIf(Entity::isPassenger), TriggerGate.triggerOneShuffled(ImmutableList.builder().addAll(PiglinAi.createLookBehaviors()).add((Object)Pair.of(BehaviorBuilder.triggerIf(e -> true), (Object)1)).build())), DismountOrSkipMounting.create(8, PiglinAi::wantsToStopRiding)), MemoryModuleType.RIDE_TARGET);
    }

    private static ImmutableList<Pair<OneShot<LivingEntity>, Integer>> createLookBehaviors() {
        return ImmutableList.of((Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0f), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0f), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(8.0f), (Object)1));
    }

    private static RunOne<LivingEntity> createIdleLookBehaviors() {
        return new RunOne<LivingEntity>((List<Pair<BehaviorControl<LivingEntity>, Integer>>)ImmutableList.builder().addAll(PiglinAi.createLookBehaviors()).add((Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)).build());
    }

    private static RunOne<Piglin> createIdleMovementBehaviors() {
        return new RunOne<Piglin>((List<Pair<BehaviorControl<Piglin>, Integer>>)ImmutableList.of((Object)Pair.of(RandomStroll.stroll(0.6f), (Object)2), (Object)Pair.of(InteractWith.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6f, 2), (Object)2), (Object)Pair.of(BehaviorBuilder.triggerIf(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, SetWalkTargetFromLookTarget.create(0.6f, 3)), (Object)2), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)));
    }

    private static BehaviorControl<PathfinderMob> avoidRepellent() {
        return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0f, 8, false);
    }

    private static BehaviorControl<Piglin> babyAvoidNemesis() {
        return CopyMemoryWithExpiry.create(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
    }

    private static BehaviorControl<Piglin> avoidZombified() {
        return CopyMemoryWithExpiry.create(PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION);
    }

    protected static void updateActivity(Piglin body) {
        Brain<Piglin> brain = body.getBrain();
        Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.ADMIRE_ITEM, (Object)Activity.FIGHT, (Object)Activity.AVOID, (Object)Activity.CELEBRATE, (Object)Activity.RIDE, (Object)Activity.IDLE));
        Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
        if (oldActivity != newActivity) {
            PiglinAi.getSoundForCurrentActivity(body).ifPresent(body::makeSound);
        }
        body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        if (!brain.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && PiglinAi.isBabyRidingBaby(body)) {
            body.stopRiding();
        }
        if (!brain.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
            brain.eraseMemory(MemoryModuleType.DANCING);
        }
        body.setDancing(brain.hasMemoryValue(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(Piglin body) {
        Hoglin riddenHoglin;
        Piglin riddenPiglin;
        if (!body.isBaby()) {
            return false;
        }
        Entity vehicle = body.getVehicle();
        return vehicle instanceof Piglin && (riddenPiglin = (Piglin)vehicle).isBaby() || vehicle instanceof Hoglin && (riddenHoglin = (Hoglin)vehicle).isBaby();
    }

    protected static void pickUpItem(ServerLevel level, Piglin body, ItemEntity itemEntity) {
        boolean itemEquipped;
        ItemStack taken;
        PiglinAi.stopWalking(body);
        if (itemEntity.getItem().is(Items.GOLD_NUGGET)) {
            body.take(itemEntity, itemEntity.getItem().getCount());
            taken = itemEntity.getItem();
            itemEntity.discard();
        } else {
            body.take(itemEntity, 1);
            taken = PiglinAi.removeOneItemFromItemEntity(itemEntity);
        }
        if (PiglinAi.isLovedItem(taken)) {
            body.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            PiglinAi.holdInOffhand(level, body, taken);
            PiglinAi.admireGoldItem(body);
            return;
        }
        if (PiglinAi.isFood(taken) && !PiglinAi.hasEatenRecently(body)) {
            PiglinAi.eat(body);
            return;
        }
        boolean bl = itemEquipped = !body.equipItemIfPossible(level, taken).equals(ItemStack.EMPTY);
        if (itemEquipped) {
            return;
        }
        PiglinAi.putInInventory(body, taken);
    }

    private static void holdInOffhand(ServerLevel level, Piglin body, ItemStack itemStack) {
        if (PiglinAi.isHoldingItemInOffHand(body)) {
            body.spawnAtLocation(level, body.getItemInHand(InteractionHand.OFF_HAND));
        }
        body.holdInOffHand(itemStack);
    }

    private static ItemStack removeOneItemFromItemEntity(ItemEntity itemEntity) {
        ItemStack sourceStack = itemEntity.getItem();
        ItemStack removedStack = sourceStack.split(1);
        if (sourceStack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(sourceStack);
        }
        return removedStack;
    }

    protected static void stopHoldingOffHandItem(ServerLevel level, Piglin body, boolean barteringEnabled) {
        ItemStack itemStack = body.getItemInHand(InteractionHand.OFF_HAND);
        body.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (body.isAdult()) {
            boolean barterCurrency = PiglinAi.isBarterCurrency(itemStack);
            if (barteringEnabled && barterCurrency) {
                PiglinAi.throwItems(body, PiglinAi.getBarterResponseItems(body));
            } else if (!barterCurrency) {
                boolean equipped;
                boolean bl = equipped = !body.equipItemIfPossible(level, itemStack).isEmpty();
                if (!equipped) {
                    PiglinAi.putInInventory(body, itemStack);
                }
            }
        } else {
            boolean equipped;
            boolean bl = equipped = !body.equipItemIfPossible(level, itemStack).isEmpty();
            if (!equipped) {
                ItemStack mainHandItem = body.getMainHandItem();
                if (PiglinAi.isLovedItem(mainHandItem)) {
                    PiglinAi.putInInventory(body, mainHandItem);
                } else {
                    PiglinAi.throwItems(body, Collections.singletonList(mainHandItem));
                }
                body.holdInMainHand(itemStack);
            }
        }
    }

    protected static void cancelAdmiring(ServerLevel level, Piglin body) {
        if (PiglinAi.isAdmiringItem(body) && !body.getOffhandItem().isEmpty()) {
            body.spawnAtLocation(level, body.getOffhandItem());
            body.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void putInInventory(Piglin body, ItemStack itemStack) {
        ItemStack stuffThatCouldntFitInMyInventory = body.addToInventory(itemStack);
        PiglinAi.throwItemsTowardRandomPos(body, Collections.singletonList(stuffThatCouldntFitInMyInventory));
    }

    private static void throwItems(Piglin body, List<ItemStack> itemStacks) {
        Optional<Player> player = body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (player.isPresent()) {
            PiglinAi.throwItemsTowardPlayer(body, player.get(), itemStacks);
        } else {
            PiglinAi.throwItemsTowardRandomPos(body, itemStacks);
        }
    }

    private static void throwItemsTowardRandomPos(Piglin body, List<ItemStack> itemStacks) {
        PiglinAi.throwItemsTowardPos(body, itemStacks, PiglinAi.getRandomNearbyPos(body));
    }

    private static void throwItemsTowardPlayer(Piglin body, Player player, List<ItemStack> itemStacks) {
        PiglinAi.throwItemsTowardPos(body, itemStacks, player.position());
    }

    private static void throwItemsTowardPos(Piglin body, List<ItemStack> itemStacks, Vec3 targetPos) {
        if (!itemStacks.isEmpty()) {
            body.swing(InteractionHand.OFF_HAND);
            for (ItemStack itemStack : itemStacks) {
                BehaviorUtils.throwItem(body, itemStack, targetPos.add(0.0, 1.0, 0.0));
            }
        }
    }

    private static List<ItemStack> getBarterResponseItems(Piglin body) {
        LootTable lootTable = body.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        ObjectArrayList<ItemStack> items = lootTable.getRandomItems(new LootParams.Builder((ServerLevel)body.level()).withParameter(LootContextParams.THIS_ENTITY, body).create(LootContextParamSets.PIGLIN_BARTER));
        return items;
    }

    private static boolean wantsToDance(LivingEntity body, LivingEntity killedTarget) {
        if (!killedTarget.is(EntityTypes.HOGLIN)) {
            return false;
        }
        return RandomSource.createThreadLocalInstance(body.level().getGameTime()).nextFloat() < 0.1f;
    }

    protected static boolean wantsToPickup(Piglin body, ItemStack itemStack) {
        if (body.isBaby() && itemStack.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
            return false;
        }
        if (itemStack.is(ItemTags.PIGLIN_REPELLENTS)) {
            return false;
        }
        if (PiglinAi.isAdmiringDisabled(body) && body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        }
        if (PiglinAi.isBarterCurrency(itemStack)) {
            return PiglinAi.isNotHoldingLovedItemInOffHand(body);
        }
        boolean hasSpace = body.canAddToInventory(itemStack);
        if (itemStack.is(Items.GOLD_NUGGET)) {
            return hasSpace;
        }
        if (PiglinAi.isFood(itemStack)) {
            return !PiglinAi.hasEatenRecently(body) && hasSpace;
        }
        if (PiglinAi.isLovedItem(itemStack)) {
            return PiglinAi.isNotHoldingLovedItemInOffHand(body) && hasSpace;
        }
        return body.canReplaceCurrentItem(itemStack);
    }

    protected static boolean isLovedItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.PIGLIN_LOVED);
    }

    private static boolean wantsToStopRiding(Piglin body, Entity entityBeingRidden) {
        if (entityBeingRidden instanceof Mob) {
            Mob mobBeingRidden = (Mob)entityBeingRidden;
            return !mobBeingRidden.isBaby() || !mobBeingRidden.isAlive() || PiglinAi.wasHurtRecently(body) || PiglinAi.wasHurtRecently(mobBeingRidden) || mobBeingRidden instanceof Piglin && mobBeingRidden.getVehicle() == null;
        }
        return false;
    }

    private static boolean isNearestValidAttackTarget(ServerLevel level, Piglin body, LivingEntity target) {
        return PiglinAi.findNearestValidAttackTarget(level, body).filter(nearestValidTarget -> nearestValidTarget == target).isPresent();
    }

    private static boolean isNearZombified(Piglin body) {
        Brain<Piglin> brain = body.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
            LivingEntity zombified = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
            return body.closerThan(zombified, 6.0);
        }
        return false;
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel level, Piglin body) {
        Optional<Player> player;
        Brain<Piglin> brain = body.getBrain();
        if (PiglinAi.isNearZombified(body)) {
            return Optional.empty();
        }
        Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
        if (angryAt.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(level, body, angryAt.get())) {
            return angryAt;
        }
        if (brain.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER) && (player = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)).isPresent()) {
            return player;
        }
        Optional<Mob> nemesis = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        if (nemesis.isPresent()) {
            return nemesis;
        }
        Optional<Player> playerNotWearingGold = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
        if (playerNotWearingGold.isPresent() && Sensor.isEntityAttackable(level, body, playerNotWearingGold.get())) {
            return playerNotWearingGold;
        }
        return Optional.empty();
    }

    public static void angerNearbyPiglins(ServerLevel level, Player player, boolean onlyIfTheySeeThePlayer) {
        List<Piglin> nearbyPiglins = player.level().getEntitiesOfClass(Piglin.class, player.getBoundingBox().inflate(16.0));
        nearbyPiglins.stream().filter(PiglinAi::isIdle).filter(piglin -> !onlyIfTheySeeThePlayer || BehaviorUtils.canSee(piglin, player)).forEach(piglin -> {
            if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER).booleanValue()) {
                PiglinAi.setAngerTargetToNearestTargetablePlayerIfFound(level, piglin, player);
            } else {
                PiglinAi.setAngerTarget(level, piglin, player);
            }
        });
    }

    public static InteractionResult mobInteract(ServerLevel level, Piglin body, Player player, InteractionHand hand) {
        ItemStack playerHeldItemStack = player.getItemInHand(hand);
        if (PiglinAi.canAdmire(body, playerHeldItemStack)) {
            ItemStack taken = playerHeldItemStack.consumeAndReturn(1, player);
            PiglinAi.holdInOffhand(level, body, taken);
            PiglinAi.admireGoldItem(body);
            PiglinAi.stopWalking(body);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    protected static boolean canAdmire(Piglin body, ItemStack playerHeldItemStack) {
        return !PiglinAi.isAdmiringDisabled(body) && !PiglinAi.isAdmiringItem(body) && body.isAdult() && PiglinAi.isBarterCurrency(playerHeldItemStack);
    }

    protected static void wasHurtBy(ServerLevel level, Piglin body, LivingEntity attacker) {
        if (attacker instanceof Piglin) {
            return;
        }
        if (PiglinAi.isHoldingItemInOffHand(body)) {
            PiglinAi.stopHoldingOffHandItem(level, body, false);
        }
        Brain<Piglin> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
        brain.eraseMemory(MemoryModuleType.DANCING);
        brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
        if (attacker instanceof Player) {
            brain.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
        }
        PiglinAi.getAvoidTarget(body).ifPresent(avoidTarget -> {
            if (avoidTarget.getType() != attacker.getType()) {
                brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
            }
        });
        if (body.isBaby()) {
            brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, attacker, 100L);
            if (Sensor.isEntityAttackableIgnoringLineOfSight(level, body, attacker)) {
                PiglinAi.broadcastAngerTarget(level, body, attacker);
            }
            return;
        }
        if (attacker.is(EntityTypes.HOGLIN) && PiglinAi.hoglinsOutnumberPiglins(body)) {
            PiglinAi.setAvoidTargetAndDontHuntForAWhile(body, attacker);
            PiglinAi.broadcastRetreat(body, attacker);
            return;
        }
        PiglinAi.maybeRetaliate(level, body, attacker);
    }

    protected static void maybeRetaliate(ServerLevel level, AbstractPiglin body, LivingEntity attacker) {
        if (body.getBrain().isActive(Activity.AVOID)) {
            return;
        }
        if (!Sensor.isEntityAttackableIgnoringLineOfSight(level, body, attacker)) {
            return;
        }
        if (BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(body, attacker, 4.0)) {
            return;
        }
        if (attacker.is(EntityTypes.PLAYER) && level.getGameRules().get(GameRules.UNIVERSAL_ANGER).booleanValue()) {
            PiglinAi.setAngerTargetToNearestTargetablePlayerIfFound(level, body, attacker);
            PiglinAi.broadcastUniversalAnger(level, body);
        } else {
            PiglinAi.setAngerTarget(level, body, attacker);
            PiglinAi.broadcastAngerTarget(level, body, attacker);
        }
    }

    public static Optional<SoundEvent> getSoundForCurrentActivity(Piglin body) {
        return body.getBrain().getActiveNonCoreActivity().map(activity -> PiglinAi.getSoundForActivity(body, activity));
    }

    private static SoundEvent getSoundForActivity(Piglin body, Activity activity) {
        if (activity == Activity.FIGHT) {
            return SoundEvents.PIGLIN_ANGRY;
        }
        if (body.isConverting()) {
            return SoundEvents.PIGLIN_RETREAT;
        }
        if (activity == Activity.AVOID && PiglinAi.isNearAvoidTarget(body)) {
            return SoundEvents.PIGLIN_RETREAT;
        }
        if (activity == Activity.ADMIRE_ITEM) {
            return SoundEvents.PIGLIN_ADMIRING_ITEM;
        }
        if (activity == Activity.CELEBRATE) {
            return SoundEvents.PIGLIN_CELEBRATE;
        }
        if (PiglinAi.seesPlayerHoldingLovedItem(body)) {
            return SoundEvents.PIGLIN_JEALOUS;
        }
        if (PiglinAi.isNearRepellent(body)) {
            return SoundEvents.PIGLIN_RETREAT;
        }
        return SoundEvents.PIGLIN_AMBIENT;
    }

    private static boolean isNearAvoidTarget(Piglin body) {
        Brain<Piglin> brain = body.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return false;
        }
        return brain.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(body, 12.0);
    }

    protected static List<AbstractPiglin> getVisibleAdultPiglins(Piglin body) {
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse((List<AbstractPiglin>)ImmutableList.of());
    }

    private static List<AbstractPiglin> getAdultPiglins(AbstractPiglin body) {
        return body.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse((List<AbstractPiglin>)ImmutableList.of());
    }

    public static boolean isWearingSafeArmor(LivingEntity livingEntity) {
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (!livingEntity.getItemBySlot(slot).is(ItemTags.PIGLIN_SAFE_ARMOR)) continue;
            return true;
        }
        return false;
    }

    private static void stopWalking(Piglin body) {
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        body.getNavigation().stop();
    }

    private static BehaviorControl<LivingEntity> babySometimesRideBabyHoglin() {
        SetEntityLookTargetSometimes.Ticker ticker = new SetEntityLookTargetSometimes.Ticker(RIDE_START_INTERVAL);
        return CopyMemoryWithExpiry.create(e -> e.isBaby() && ticker.tickDownAndCheck(e.level().getRandom()), MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.RIDE_TARGET, RIDE_DURATION);
    }

    protected static void broadcastAngerTarget(ServerLevel level, AbstractPiglin body, LivingEntity target) {
        PiglinAi.getAdultPiglins(body).forEach(piglin -> {
            if (target instanceof Hoglin) {
                Hoglin hoglin = (Hoglin)target;
                if (!piglin.canHunt() || !hoglin.canBeHunted()) {
                    return;
                }
            }
            PiglinAi.setAngerTargetIfCloserThanCurrent(level, piglin, target);
        });
    }

    protected static void broadcastUniversalAnger(ServerLevel level, AbstractPiglin body) {
        PiglinAi.getAdultPiglins(body).forEach(piglin -> PiglinAi.getNearestVisibleTargetablePlayer(piglin).ifPresent(player -> PiglinAi.setAngerTarget(level, piglin, player)));
    }

    protected static void setAngerTarget(ServerLevel level, AbstractPiglin body, LivingEntity target) {
        if (!Sensor.isEntityAttackableIgnoringLineOfSight(level, body, target)) {
            return;
        }
        body.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
        if (target.is(EntityTypes.HOGLIN) && body.canHunt()) {
            PiglinAi.dontKillAnyMoreHoglinsForAWhile(body);
        }
        if (target.is(EntityTypes.PLAYER) && level.getGameRules().get(GameRules.UNIVERSAL_ANGER).booleanValue()) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(ServerLevel level, AbstractPiglin body, LivingEntity targetIfNoPlayerFound) {
        Optional<Player> nearestPlayer = PiglinAi.getNearestVisibleTargetablePlayer(body);
        if (nearestPlayer.isPresent()) {
            PiglinAi.setAngerTarget(level, body, nearestPlayer.get());
        } else {
            PiglinAi.setAngerTarget(level, body, targetIfNoPlayerFound);
        }
    }

    private static void setAngerTargetIfCloserThanCurrent(ServerLevel level, AbstractPiglin body, LivingEntity newTarget) {
        Optional<LivingEntity> currentTarget = PiglinAi.getAngerTarget(body);
        LivingEntity nearest = BehaviorUtils.getNearestTarget(body, currentTarget, newTarget);
        if (currentTarget.isPresent() && currentTarget.get() == nearest) {
            return;
        }
        PiglinAi.setAngerTarget(level, body, nearest);
    }

    private static Optional<LivingEntity> getAngerTarget(AbstractPiglin body) {
        return BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
    }

    public static Optional<LivingEntity> getAvoidTarget(Piglin body) {
        if (body.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return body.getBrain().getMemory(MemoryModuleType.AVOID_TARGET);
        }
        return Optional.empty();
    }

    public static Optional<Player> getNearestVisibleTargetablePlayer(AbstractPiglin body) {
        if (body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)) {
            return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        }
        return Optional.empty();
    }

    private static void broadcastRetreat(Piglin body, LivingEntity target) {
        PiglinAi.getVisibleAdultPiglins(body).forEach(abstractPiglin -> {
            if (abstractPiglin instanceof Piglin) {
                Piglin piglin = (Piglin)abstractPiglin;
                PiglinAi.retreatFromNearestTarget(piglin, target);
            }
        });
    }

    private static void retreatFromNearestTarget(Piglin body, LivingEntity newAvoidTarget) {
        Brain<Piglin> brain = body.getBrain();
        LivingEntity nearest = newAvoidTarget;
        nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.AVOID_TARGET), nearest);
        nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
        PiglinAi.setAvoidTargetAndDontHuntForAWhile(body, nearest);
    }

    private static boolean wantsToStopFleeing(Piglin body) {
        Brain<Piglin> brain = body.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return true;
        }
        LivingEntity avoidedEntity = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
        if (avoidedEntity.is(EntityTypes.HOGLIN)) {
            return PiglinAi.piglinsEqualOrOutnumberHoglins(body);
        }
        if (PiglinAi.isZombified(avoidedEntity)) {
            return !brain.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, avoidedEntity);
        }
        return false;
    }

    private static boolean piglinsEqualOrOutnumberHoglins(Piglin body) {
        return !PiglinAi.hoglinsOutnumberPiglins(body);
    }

    private static boolean hoglinsOutnumberPiglins(Piglin body) {
        int piglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int hoglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
        return hoglinCount > piglinCount;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(Piglin body, LivingEntity target) {
        body.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, RETREAT_DURATION.sample(body.level().getRandom()));
        PiglinAi.dontKillAnyMoreHoglinsForAWhile(body);
    }

    protected static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin body) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, TIME_BETWEEN_HUNTS.sample(body.level().getRandom()));
    }

    private static void eat(Piglin body) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3 getRandomNearbyPos(Piglin body) {
        Vec3 targetVec = LandRandomPos.getPos(body, 4, 2);
        return targetVec == null ? body.position() : targetVec;
    }

    private static boolean hasEatenRecently(Piglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
    }

    protected static boolean isIdle(AbstractPiglin body) {
        return body.getBrain().isActive(Activity.IDLE);
    }

    private static boolean hasCrossbow(LivingEntity body) {
        return body.isHolding(Items.CROSSBOW);
    }

    private static void admireGoldItem(LivingEntity body) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 119L);
    }

    private static boolean isAdmiringItem(Piglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
    }

    private static boolean isBarterCurrency(ItemStack itemStack) {
        return itemStack.is(BARTERING_ITEM);
    }

    private static boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.PIGLIN_FOOD);
    }

    private static boolean isNearRepellent(Piglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(LivingEntity body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity body) {
        return !PiglinAi.seesPlayerHoldingLovedItem(body);
    }

    public static boolean isPlayerHoldingLovedItem(LivingEntity entity) {
        return entity.is(EntityTypes.PLAYER) && entity.isHolding(PiglinAi::isLovedItem);
    }

    private static boolean isAdmiringDisabled(Piglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(LivingEntity body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffHand(Piglin body) {
        return !body.getOffhandItem().isEmpty();
    }

    private static boolean isNotHoldingLovedItemInOffHand(Piglin body) {
        return body.getOffhandItem().isEmpty() || !PiglinAi.isLovedItem(body.getOffhandItem());
    }

    public static boolean isZombified(Entity entity) {
        return entity.is(EntityTypes.ZOMBIFIED_PIGLIN) || entity.is(EntityTypes.ZOGLIN);
    }

    public static List<AbstractPiglin> findNearbyAdultPiglins(Brain<?> brain) {
        List livingEntities = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
        ArrayList<AbstractPiglin> adultPiglins = new ArrayList<AbstractPiglin>();
        for (LivingEntity entity : livingEntities) {
            AbstractPiglin piglin;
            if (!(entity instanceof AbstractPiglin) || !(piglin = (AbstractPiglin)entity).isAdult()) continue;
            adultPiglins.add(piglin);
        }
        return adultPiglins;
    }
}

