/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LongJumpToRandomPos<E extends Mob>
extends Behavior<E> {
    protected static final int FIND_JUMP_TRIES = 20;
    private static final int PREPARE_JUMP_DURATION = 40;
    protected static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
    private static final int TIME_OUT_DURATION = 200;
    private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList((Object[])new Integer[]{65, 70, 75, 80});
    private final UniformInt timeBetweenLongJumps;
    protected final int maxLongJumpHeight;
    protected final int maxLongJumpWidth;
    protected final float maxJumpVelocityMultiplier;
    protected List<PossibleJump> jumpCandidates = Lists.newArrayList();
    protected Optional<Vec3> initialPosition = Optional.empty();
    protected @Nullable Vec3 chosenJump;
    protected int findJumpTries;
    protected long prepareJumpStart;
    private final Function<E, SoundEvent> getJumpSound;
    private final BiPredicate<E, BlockPos> acceptableLandingSpot;

    public LongJumpToRandomPos(UniformInt timeBetweenLongJumps, int maxLongJumpHeight, int maxLongJumpWidth, float maxJumpVelocityMultiplier, Function<E, SoundEvent> getJumpSound) {
        this(timeBetweenLongJumps, maxLongJumpHeight, maxLongJumpWidth, maxJumpVelocityMultiplier, getJumpSound, LongJumpToRandomPos::defaultAcceptableLandingSpot);
    }

    public static <E extends Mob> boolean defaultAcceptableLandingSpot(E body, BlockPos targetPos) {
        BlockPos below;
        Level level = body.level();
        return level.getBlockState(below = targetPos.below()).isSolidRender() && body.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(body, targetPos)) == 0.0f;
    }

    public LongJumpToRandomPos(UniformInt timeBetweenLongJumps, int maxLongJumpHeight, int maxLongJumpWidth, float maxJumpVelocityMultiplier, Function<E, SoundEvent> getJumpSound, BiPredicate<E, BlockPos> acceptableLandingSpot) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.LONG_JUMP_MID_JUMP, (Object)((Object)MemoryStatus.VALUE_ABSENT)), 200);
        this.timeBetweenLongJumps = timeBetweenLongJumps;
        this.maxLongJumpHeight = maxLongJumpHeight;
        this.maxLongJumpWidth = maxLongJumpWidth;
        this.maxJumpVelocityMultiplier = maxJumpVelocityMultiplier;
        this.getJumpSound = getJumpSound;
        this.acceptableLandingSpot = acceptableLandingSpot;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
        boolean canStart;
        boolean bl = canStart = body.onGround() && !body.isInWater() && !body.isInLava() && !level.getBlockState(body.blockPosition()).is(Blocks.HONEY_BLOCK);
        if (!canStart) {
            body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()) / 2);
        }
        return canStart;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        boolean isValid;
        boolean bl = isValid = this.initialPosition.isPresent() && this.initialPosition.get().equals(body.position()) && this.findJumpTries > 0 && !body.isInWater() && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
        if (!isValid && body.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
            body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()) / 2);
            body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
        return isValid;
    }

    @Override
    protected void start(ServerLevel level, E body, long timestamp) {
        this.chosenJump = null;
        this.findJumpTries = 20;
        this.initialPosition = Optional.of(((Entity)body).position());
        BlockPos mobPos = ((Entity)body).blockPosition();
        int mobX = mobPos.getX();
        int mobY = mobPos.getY();
        int mobZ = mobPos.getZ();
        this.jumpCandidates = BlockPos.betweenClosedStream(mobX - this.maxLongJumpWidth, mobY - this.maxLongJumpHeight, mobZ - this.maxLongJumpWidth, mobX + this.maxLongJumpWidth, mobY + this.maxLongJumpHeight, mobZ + this.maxLongJumpWidth).filter(pos -> !pos.equals(mobPos)).map(pos -> new PossibleJump(pos.immutable(), Mth.ceil(mobPos.distSqr((Vec3i)pos)))).collect(Collectors.toCollection(Lists::newArrayList));
    }

    @Override
    protected void tick(ServerLevel level, E body, long timestamp) {
        if (this.chosenJump != null) {
            if (timestamp - this.prepareJumpStart >= 40L) {
                ((Entity)body).setYRot(((Mob)body).yBodyRot);
                ((LivingEntity)body).setDiscardFriction(true);
                double orgLength = this.chosenJump.length();
                double lengthWithJumpBoost = orgLength + (double)((LivingEntity)body).getJumpBoostPower();
                ((Entity)body).setDeltaMovement(this.chosenJump.scale(lengthWithJumpBoost / orgLength));
                ((LivingEntity)body).getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
                level.playSound(null, (Entity)body, this.getJumpSound.apply(body), SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
        } else {
            --this.findJumpTries;
            this.pickCandidate(level, body, timestamp);
        }
    }

    protected void pickCandidate(ServerLevel level, E body, long timestamp) {
        while (!this.jumpCandidates.isEmpty()) {
            Vec3 targetPosition;
            Vec3 jumpVector;
            PossibleJump position;
            BlockPos targetPos;
            Optional<PossibleJump> optionalPosition = this.getJumpCandidate(level);
            if (optionalPosition.isEmpty() || !this.isAcceptableLandingPosition(level, body, targetPos = (position = optionalPosition.get()).targetPos()) || (jumpVector = this.calculateOptimalJumpVector((Mob)body, targetPosition = Vec3.atCenterOf(targetPos))) == null) continue;
            ((LivingEntity)body).getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
            PathNavigation navigation = ((Mob)body).getNavigation();
            Path path = navigation.createPath(targetPos, 0, 8);
            if (path != null && path.canReach()) continue;
            this.chosenJump = jumpVector;
            this.prepareJumpStart = timestamp;
            return;
        }
    }

    protected Optional<PossibleJump> getJumpCandidate(ServerLevel level) {
        Optional<PossibleJump> randomItem = WeightedRandom.getRandomItem(level.getRandom(), this.jumpCandidates, PossibleJump::weight);
        randomItem.ifPresent(this.jumpCandidates::remove);
        return randomItem;
    }

    private boolean isAcceptableLandingPosition(ServerLevel level, E body, BlockPos targetPos) {
        BlockPos bodyPos = ((Entity)body).blockPosition();
        int mobX = bodyPos.getX();
        int mobZ = bodyPos.getZ();
        if (mobX == targetPos.getX() && mobZ == targetPos.getZ()) {
            return false;
        }
        return this.acceptableLandingSpot.test(body, targetPos);
    }

    protected @Nullable Vec3 calculateOptimalJumpVector(Mob body, Vec3 targetPos) {
        ArrayList allowedAngles = Lists.newArrayList(ALLOWED_ANGLES);
        Collections.shuffle(allowedAngles);
        float maxJumpVelocity = (float)(body.getAttributeValue(Attributes.JUMP_STRENGTH) * (double)this.maxJumpVelocityMultiplier);
        Iterator iterator = allowedAngles.iterator();
        while (iterator.hasNext()) {
            int angle = (Integer)iterator.next();
            Optional<Vec3> velocityVector = LongJumpUtil.calculateJumpVectorForAngle(body, targetPos, maxJumpVelocity, angle, true);
            if (!velocityVector.isPresent()) continue;
            return velocityVector.get();
        }
        return null;
    }

    public record PossibleJump(BlockPos targetPos, int weight) {
    }
}

