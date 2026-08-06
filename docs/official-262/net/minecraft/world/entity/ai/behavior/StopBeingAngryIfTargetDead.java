/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StopBeingAngryIfTargetDead {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.ANGRY_AT)).apply((Applicative)i, angryAt -> (level, body, timestamp) -> {
            Optional.ofNullable(level.getEntity((UUID)i.get(angryAt))).map(entity -> {
                LivingEntity livingEntity;
                return entity instanceof LivingEntity ? (livingEntity = (LivingEntity)entity) : null;
            }).filter(LivingEntity::isDeadOrDying).filter(angerTarget -> !angerTarget.is(EntityTypes.PLAYER) || level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS) != false).ifPresent(angerTarget -> angryAt.erase());
            return true;
        }));
    }
}

