/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SlimeBlock
extends HalfTransparentBlock {
    public static final MapCodec<SlimeBlock> CODEC = SlimeBlock.simpleCodec(SlimeBlock::new);

    public MapCodec<SlimeBlock> codec() {
        return CODEC;
    }

    public SlimeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!entity.isSuppressingBounce()) {
            entity.causeFallDamage(fallDistance, 0.0f, level.damageSources().fall());
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        double absDeltaY = Math.abs(entity.getDeltaMovement().y);
        if (absDeltaY < 0.1 && !entity.isSteppingCarefully()) {
            double scale = 0.4 + absDeltaY * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(scale, 1.0, scale));
        }
        super.stepOn(level, pos, onState, entity);
    }
}

