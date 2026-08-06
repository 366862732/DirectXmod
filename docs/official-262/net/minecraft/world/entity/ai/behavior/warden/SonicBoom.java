/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public class SonicBoom
extends Behavior<Warden> {
    private static final int DISTANCE_XZ = 15;
    private static final int DISTANCE_Y = 20;
    private static final double KNOCKBACK_VERTICAL = 0.5;
    private static final double KNOCKBACK_HORIZONTAL = 2.5;
    public static final int COOLDOWN = 40;
    private static final int TICKS_BEFORE_PLAYING_SOUND = Mth.ceil(34.0);
    private static final int DURATION = Mth.ceil(60.0f);

    public SonicBoom() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.SONIC_BOOM_COOLDOWN, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.SONIC_BOOM_SOUND_DELAY, (Object)((Object)MemoryStatus.REGISTERED)), DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Warden body) {
        return body.closerThan(body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 15.0, 20.0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Warden body, long timestamp) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, Warden body, long timestamp) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, DURATION);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_DELAY, Unit.INSTANCE, TICKS_BEFORE_PLAYING_SOUND);
        level.broadcastEntityEvent(body, (byte)62);
        body.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 1.0f);
    }

    @Override
    protected void tick(ServerLevel level, Warden body, long timestamp) {
        body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> body.getLookControl().setLookAt(target.position()));
        if (body.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_DELAY) || body.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN)) {
            return;
        }
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, Unit.INSTANCE, DURATION - TICKS_BEFORE_PLAYING_SOUND);
        body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(body::canTargetEntity).filter(target -> body.closerThan((Entity)target, 15.0, 20.0)).ifPresent(target -> {
            Vec3 source = body.position().add(body.getAttachments().get(EntityAttachment.WARDEN_CHEST, 0, body.getYRot()));
            Vec3 delta = target.getEyePosition().subtract(source);
            Vec3 normalize = delta.normalize();
            int steps = Mth.floor(delta.length()) + 7;
            for (int i = 1; i < steps; ++i) {
                Vec3 particlePos = source.add(normalize.scale(i));
                level.sendParticles(ParticleTypes.SONIC_BOOM, particlePos.x, particlePos.y, particlePos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            body.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 1.0f);
            if (target.hurtServer(level, level.damageSources().sonicBoom(body), 10.0f)) {
                double knockbackVertical = 0.5 * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                double knockbackHorizontal = 2.5 * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                target.push(normalize.x() * knockbackHorizontal, normalize.y() * knockbackVertical, normalize.z() * knockbackHorizontal);
            }
        });
    }

    @Override
    protected void stop(ServerLevel level, Warden body, long timestamp) {
        SonicBoom.setCooldown(body, 40);
    }

    public static void setCooldown(LivingEntity body, int cooldown) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, cooldown);
    }
}

