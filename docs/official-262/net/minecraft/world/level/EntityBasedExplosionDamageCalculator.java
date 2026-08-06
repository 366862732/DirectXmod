/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class EntityBasedExplosionDamageCalculator
extends ExplosionDamageCalculator {
    private final Entity source;

    public EntityBasedExplosionDamageCalculator(Entity source) {
        this.source = source;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid) {
        return super.getBlockExplosionResistance(explosion, level, pos, block, fluid).map(resistance -> Float.valueOf(this.source.getBlockExplosionResistance(explosion, level, pos, block, fluid, resistance.floatValue())));
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return this.source.shouldBlockExplode(explosion, level, pos, state, power);
    }
}

