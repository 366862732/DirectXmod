/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class ExplosionDamageCalculator {
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid) {
        if (block.isAir() && fluid.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Float.valueOf(Math.max(block.getBlock().getExplosionResistance(), fluid.getExplosionResistance())));
    }

    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return true;
    }

    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        return true;
    }

    public float getKnockbackMultiplier(Entity entity) {
        return 1.0f;
    }

    public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
        float doubleRadius = explosion.radius() * 2.0f;
        Vec3 center = explosion.center();
        double dist = Math.sqrt(entity.distanceToSqr(center)) / (double)doubleRadius;
        double pow = (1.0 - dist) * (double)exposure;
        return (float)((pow * pow + pow) / 2.0 * 7.0 * (double)doubleRadius + 1.0);
    }
}

