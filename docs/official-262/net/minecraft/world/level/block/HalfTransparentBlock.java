/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HalfTransparentBlock
extends Block {
    public static final MapCodec<HalfTransparentBlock> CODEC = HalfTransparentBlock.simpleCodec(HalfTransparentBlock::new);

    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }

    protected HalfTransparentBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        if (neighborState.is(this)) {
            return true;
        }
        return super.skipRendering(state, neighborState, direction);
    }
}

