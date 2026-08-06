/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.Applicative;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class PlayTagWithOtherKids {
    private static final int MAX_FLEE_XZ_DIST = 20;
    private static final int MAX_FLEE_Y_DIST = 8;
    private static final float FLEE_SPEED_MODIFIER = 0.6f;
    private static final float CHASE_SPEED_MODIFIER = 0.6f;
    private static final int MAX_CHASERS_PER_TARGET = 5;
    private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

    public static BehaviorControl<PathfinderMob> create() {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.VISIBLE_VILLAGER_BABIES), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET), i.registered(MemoryModuleType.INTERACTION_TARGET)).apply((Applicative)i, (babies, walkTarget, lookTarget, interactionTarget) -> (level, me, timestamp) -> {
            if (level.getRandom().nextInt(10) != 0) {
                return false;
            }
            List friendsNearby = (List)i.get(babies);
            Optional<LivingEntity> otherKidChasingMe = friendsNearby.stream().filter(friend -> PlayTagWithOtherKids.isFriendChasingMe(me, friend)).findAny();
            if (otherKidChasingMe.isPresent()) {
                for (int j = 0; j < 10; ++j) {
                    Vec3 pos = LandRandomPos.getPos(me, 20, 8);
                    if (pos == null || !level.isVillage(BlockPos.containing(pos))) continue;
                    walkTarget.set(new WalkTarget(pos, 0.6f, 0));
                    break;
                }
                return true;
            }
            Optional<LivingEntity> otherKidBeingChased = PlayTagWithOtherKids.findSomeoneBeingChased(friendsNearby);
            if (otherKidBeingChased.isPresent()) {
                PlayTagWithOtherKids.chaseKid(interactionTarget, lookTarget, walkTarget, otherKidBeingChased.get());
                return true;
            }
            friendsNearby.stream().findAny().ifPresent(entity -> PlayTagWithOtherKids.chaseKid(interactionTarget, lookTarget, walkTarget, entity));
            return true;
        }));
    }

    private static void chaseKid(MemoryAccessor<?, LivingEntity> interactionTarget, MemoryAccessor<?, PositionTracker> lookTarget, MemoryAccessor<?, WalkTarget> walkTarget, LivingEntity kidToChase) {
        interactionTarget.set(kidToChase);
        lookTarget.set(new EntityTracker(kidToChase, true));
        walkTarget.set(new WalkTarget(new EntityTracker(kidToChase, false), 0.6f, 1));
    }

    private static Optional<LivingEntity> findSomeoneBeingChased(List<LivingEntity> friendsNearby) {
        Map<LivingEntity, Integer> chasedKids = PlayTagWithOtherKids.checkHowManyChasersEachFriendHas(friendsNearby);
        return chasedKids.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).filter(entry -> (Integer)entry.getValue() > 0 && (Integer)entry.getValue() <= 5).map(Map.Entry::getKey).findFirst();
    }

    private static Map<LivingEntity, Integer> checkHowManyChasersEachFriendHas(List<LivingEntity> friendsNearby) {
        HashMap chasedKids = Maps.newHashMap();
        friendsNearby.stream().filter(PlayTagWithOtherKids::isChasingSomeone).forEach(chaser -> chasedKids.compute(PlayTagWithOtherKids.whoAreYouChasing(chaser), (k, count) -> count == null ? 1 : count + 1));
        return chasedKids;
    }

    private static LivingEntity whoAreYouChasing(LivingEntity friend) {
        return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
    }

    private static boolean isChasingSomeone(LivingEntity friend) {
        return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    private static boolean isFriendChasingMe(LivingEntity me, LivingEntity friend) {
        return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter(mob -> mob == me).isPresent();
    }
}

