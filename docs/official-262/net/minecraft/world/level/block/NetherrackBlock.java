/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherrackBlock
extends Block
implements BonemealableBlock {
    public static final MapCodec<NetherrackBlock> CODEC = NetherrackBlock.simpleCodec(NetherrackBlock::new);

    public MapCodec<NetherrackBlock> codec() {
        return CODEC;
    }

    public NetherrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos.above()).propagatesSkylightDown()) {
            return false;
        }
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!level.getBlockState(blockPos).is(BlockTags.NYLIUM)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        boolean foundRed = false;
        boolean foundBlue = false;
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.WARPED_NYLIUM)) {
                foundBlue = true;
            }
            if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
                foundRed = true;
            }
            if (!foundBlue || !foundRed) continue;
            break;
        }
        if (foundBlue && foundRed) {
            level.setBlock(pos, random.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        } else if (foundBlue) {
            level.setBlock(pos, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
        } else if (foundRed) {
            level.setBlock(pos, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}

