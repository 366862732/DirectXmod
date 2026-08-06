/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.Vec3;

public class EntityTracker
implements PositionTracker {
    private final Entity entity;
    private final boolean trackEyeHeight;
    private final boolean targetEyeHeight;

    public EntityTracker(Entity entity, boolean trackEyeHeight) {
        this(entity, trackEyeHeight, false);
    }

    public EntityTracker(Entity entity, boolean trackEyeHeight, boolean targetEyeHeight) {
        this.entity = entity;
        this.trackEyeHeight = trackEyeHeight;
        this.targetEyeHeight = targetEyeHeight;
    }

    @Override
    public Vec3 currentPosition() {
        return this.trackEyeHeight ? this.entity.position().add(0.0, this.entity.getEyeHeight(), 0.0) : this.entity.position();
    }

    @Override
    public BlockPos currentBlockPosition() {
        return this.targetEyeHeight ? BlockPos.containing(this.entity.getEyePosition()) : this.entity.blockPosition();
    }

    @Override
    public boolean isVisibleBy(LivingEntity body) {
        Entity entity = this.entity;
        if (!(entity instanceof LivingEntity)) {
            return true;
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        if (!livingEntity.isAlive()) {
            return false;
        }
        Optional<NearestVisibleLivingEntities> visibleEntities = body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return visibleEntities.isPresent() && visibleEntities.get().contains(livingEntity);
    }

    public Entity getEntity() {
        return this.entity;
    }

    public String toString() {
        return "EntityTracker for " + String.valueOf(this.entity);
    }
}

