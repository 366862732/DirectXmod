/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.spider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CaveSpider
extends Spider {
    public CaveSpider(EntityType<? extends CaveSpider> type, Level level) {
        super((EntityType<? extends Spider>)type, level);
    }

    public static AttributeSupplier.Builder createCaveSpider() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 12.0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            if (target instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)target;
                int poisonTime = 0;
                if (this.level().getDifficulty() == Difficulty.NORMAL) {
                    poisonTime = 7;
                } else if (this.level().getDifficulty() == Difficulty.HARD) {
                    poisonTime = 15;
                }
                if (poisonTime > 0) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTime * 20, 0), this);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        return groupData;
    }

    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        if (vehicle.getBbWidth() <= this.getBbWidth()) {
            return new Vec3(0.0, 0.21875 * (double)this.getScale(), 0.0);
        }
        return super.getVehicleAttachmentPoint(vehicle);
    }
}

