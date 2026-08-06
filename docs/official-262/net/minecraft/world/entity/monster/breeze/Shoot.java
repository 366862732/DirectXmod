/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Shoot
extends Behavior<Breeze> {
    private static final int ATTACK_RANGE_MAX_SQRT = 256;
    private static final int UNCERTAINTY_BASE = 5;
    private static final int UNCERTAINTY_MULTIPLIER = 4;
    private static final float PROJECTILE_MOVEMENT_SCALE = 0.7f;
    private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0f);
    private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0f);
    private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0f);

    @VisibleForTesting
    public Shoot() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.BREEZE_SHOOT_COOLDOWN, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.BREEZE_SHOOT_CHARGING, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.BREEZE_SHOOT_RECOVERING, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.BREEZE_SHOOT, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.BREEZE_JUMP_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Breeze breeze) {
        if (breeze.getPose() != Pose.STANDING) {
            return false;
        }
        return breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(target -> Shoot.isTargetWithinRange(breeze, target)).map(withinRange -> {
            if (!withinRange.booleanValue()) {
                breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
            }
            return withinRange;
        }).orElse(false);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Breeze body, long timestamp) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && body.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
    }

    @Override
    protected void start(ServerLevel level, Breeze breeze, long timestamp) {
        breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> breeze.setPose(Pose.SHOOTING));
        breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, SHOOT_INITIAL_DELAY_TICKS);
        breeze.playSound(SoundEvents.BREEZE_INHALE, 1.0f, 1.0f);
    }

    @Override
    protected void stop(ServerLevel level, Breeze breeze, long timestamp) {
        if (breeze.getPose() == Pose.SHOOTING) {
            breeze.setPose(Pose.STANDING);
        }
        breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, SHOOT_COOLDOWN_TICKS);
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
    }

    @Override
    protected void tick(ServerLevel level, Breeze breeze, long timestamp) {
        Brain<Breeze> brain = breeze.getBrain();
        LivingEntity target = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }
        breeze.lookAt(EntityAnchorArgument.Anchor.EYES, target.position());
        if (brain.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() || brain.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent()) {
            return;
        }
        brain.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, SHOOT_RECOVER_DELAY_TICKS);
        double xd = target.getX() - breeze.getX();
        double yd = target.getY(target.isPassenger() ? 0.8 : 0.3) - breeze.getFiringYPosition();
        double zd = target.getZ() - breeze.getZ();
        Projectile.spawnProjectileUsingShoot(new BreezeWindCharge(breeze, (Level)level), level, ItemStack.EMPTY, xd, yd, zd, 0.7f, 5 - level.getDifficulty().getId() * 4);
        breeze.playSound(SoundEvents.BREEZE_SHOOT, 1.5f, 1.0f);
    }

    private static boolean isTargetWithinRange(Breeze body, LivingEntity target) {
        double distanceSqrt = body.position().distanceToSqr(target.position());
        return distanceSqrt < 256.0;
    }
}

