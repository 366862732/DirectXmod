/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;

public interface ContainerUser {
    public boolean hasContainerOpen(ContainerOpenersCounter var1, BlockPos var2);

    public double getContainerInteractionRange();

    default public LivingEntity getLivingEntity() {
        ContainerUser containerUser = this;
        if (containerUser instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)((Object)containerUser);
            return livingEntity;
        }
        throw new IllegalStateException("A container user must be a LivingEntity");
    }
}

