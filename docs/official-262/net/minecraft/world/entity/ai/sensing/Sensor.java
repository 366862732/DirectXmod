/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.sensing;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public abstract class Sensor<E extends LivingEntity> {
    private static final int DEFAULT_SCAN_RATE = 20;
    private static final int DEFAULT_TARGETING_RANGE = 16;
    private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forNonCombat().range(16.0);
    private static final TargetingConditions TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forNonCombat().range(16.0).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0);
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forCombat().range(16.0).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();
    private final int scanRate;
    private long timeToTick;

    public Sensor(int scanRate) {
        this.scanRate = scanRate;
    }

    public Sensor() {
        this(20);
    }

    public void randomlyDelayStart(RandomSource randomSource) {
        this.timeToTick = randomSource.nextInt(this.scanRate);
    }

    public final void tick(ServerLevel level, E body) {
        if (--this.timeToTick <= 0L) {
            this.timeToTick = this.scanRate;
            this.updateTargetingConditionRanges(body);
            this.doTick(level, body);
        }
    }

    private void updateTargetingConditionRanges(E body) {
        double followRange = ((LivingEntity)body).getAttributeValue(Attributes.FOLLOW_RANGE);
        TARGET_CONDITIONS.range(followRange);
        TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(followRange);
        ATTACK_TARGET_CONDITIONS.range(followRange);
        ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(followRange);
        ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.range(followRange);
        ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.range(followRange);
    }

    protected abstract void doTick(ServerLevel var1, E var2);

    public abstract Set<MemoryModuleType<?>> requires();

    public static boolean isEntityTargetable(ServerLevel level, LivingEntity body, LivingEntity entity) {
        if (body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, entity)) {
            return TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(level, body, entity);
        }
        return TARGET_CONDITIONS.test(level, body, entity);
    }

    public static boolean isEntityAttackable(ServerLevel level, LivingEntity body, LivingEntity target) {
        if (body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)) {
            return ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(level, body, target);
        }
        return ATTACK_TARGET_CONDITIONS.test(level, body, target);
    }

    public static BiPredicate<ServerLevel, LivingEntity> wasEntityAttackableLastNTicks(LivingEntity body, int ticks) {
        return Sensor.rememberPositives(ticks, (level, target) -> Sensor.isEntityAttackable(level, body, target));
    }

    public static boolean isEntityAttackableIgnoringLineOfSight(ServerLevel level, LivingEntity body, LivingEntity target) {
        if (body.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target)) {
            return ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(level, body, target);
        }
        return ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(level, body, target);
    }

    public static <T, U> BiPredicate<T, U> rememberPositives(int invocations, BiPredicate<T, U> predicate) {
        AtomicInteger positivesLeft = new AtomicInteger(0);
        return (t, u) -> {
            if (predicate.test(t, u)) {
                positivesLeft.set(invocations);
                return true;
            }
            return positivesLeft.decrementAndGet() >= 0;
        };
    }
}

