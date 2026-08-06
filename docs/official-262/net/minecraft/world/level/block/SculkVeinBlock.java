/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class SculkVeinBlock
extends MultifaceSpreadeableBlock
implements SculkBehaviour {
    public static final MapCodec<SculkVeinBlock> CODEC = SculkVeinBlock.simpleCodec(SculkVeinBlock::new);
    private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinSpreaderConfig(this, MultifaceSpreader.DEFAULT_SPREAD_ORDER));
    private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(new SculkVeinSpreaderConfig(this, MultifaceSpreader.SpreadType.SAME_POSITION));

    public MapCodec<SculkVeinBlock> codec() {
        return CODEC;
    }

    public SculkVeinBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.veinSpreader;
    }

    public MultifaceSpreader getSameSpaceSpreader() {
        return this.sameSpaceSpreader;
    }

    public static boolean regrow(LevelAccessor level, BlockPos pos, BlockState existing, Collection<Direction> faces) {
        boolean hasAtLeastOneFace = false;
        BlockState newState = Blocks.SCULK_VEIN.defaultBlockState();
        for (Direction face : faces) {
            if (!SculkVeinBlock.canAttachTo(level, pos, face)) continue;
            newState = (BlockState)newState.setValue(SculkVeinBlock.getFaceProperty(face), true);
            hasAtLeastOneFace = true;
        }
        if (!hasAtLeastOneFace) {
            return false;
        }
        if (!existing.getFluidState().isEmpty()) {
            newState = (BlockState)newState.setValue(MultifaceBlock.WATERLOGGED, true);
        }
        level.setBlock(pos, newState, 3);
        return true;
    }

    @Override
    public void onDischarged(LevelAccessor level, BlockState state, BlockPos pos, RandomSource random) {
        if (!state.is(this)) {
            return;
        }
        for (Direction dir : DIRECTIONS) {
            BooleanProperty sideProperty = SculkVeinBlock.getFaceProperty(dir);
            if (!state.getValue(sideProperty).booleanValue() || !level.getBlockState(pos.relative(dir)).is(Blocks.SCULK)) continue;
            state = (BlockState)state.setValue(sideProperty, false);
        }
        if (!SculkVeinBlock.hasAnyFace(state)) {
            FluidState fluidState = level.getFluidState(pos);
            state = (fluidState.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
        }
        level.setBlock(pos, state, 3);
        SculkBehaviour.super.onDischarged(level, state, pos, random);
    }

    @Override
    public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos originPos, RandomSource random, SculkSpreader spreader, boolean spreadVeins) {
        if (spreadVeins && this.attemptPlaceSculk(spreader, level, cursor.getPos(), random)) {
            return cursor.getCharge() - 1;
        }
        return random.nextInt(spreader.chargeDecayRate()) == 0 ? Mth.floor((float)cursor.getCharge() * 0.5f) : cursor.getCharge();
    }

    private boolean attemptPlaceSculk(SculkSpreader spreader, LevelAccessor level, BlockPos pos, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        TagKey<Block> replaceTag = spreader.replaceableBlocks();
        for (Direction support : Direction.allShuffled(random)) {
            BlockPos supportPos;
            BlockState supportState;
            if (!SculkVeinBlock.hasFace(state, support) || !(supportState = level.getBlockState(supportPos = pos.relative(support))).is(replaceTag)) continue;
            BlockState defaultSculk = Blocks.SCULK.defaultBlockState();
            level.setBlock(supportPos, defaultSculk, 3);
            Block.pushEntitiesUp(supportState, defaultSculk, level, supportPos);
            level.playSound(null, supportPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0f, 1.0f);
            this.veinSpreader.spreadAll(defaultSculk, level, supportPos, spreader.isWorldGeneration());
            Direction skip = support.getOpposite();
            for (Direction veinBlocks : DIRECTIONS) {
                BlockPos veinPos;
                BlockState possibleVeinBlock;
                if (veinBlocks == skip || !(possibleVeinBlock = level.getBlockState(veinPos = supportPos.relative(veinBlocks))).is(this)) continue;
                this.onDischarged(level, possibleVeinBlock, veinPos, random);
            }
            return true;
        }
        return false;
    }

    public static boolean hasSubstrateAccess(LevelAccessor level, BlockState state, BlockPos pos) {
        if (!state.is(Blocks.SCULK_VEIN)) {
            return false;
        }
        for (Direction direction : DIRECTIONS) {
            if (!SculkVeinBlock.hasFace(state, direction) || !level.getBlockState(pos.relative(direction)).is(BlockTags.SCULK_REPLACEABLE)) continue;
            return true;
        }
        return false;
    }

    private class SculkVeinSpreaderConfig
    extends MultifaceSpreader.DefaultSpreaderConfig {
        private final MultifaceSpreader.SpreadType[] spreadTypes;

        public SculkVeinSpreaderConfig(SculkVeinBlock sculkVeinBlock, MultifaceSpreader.SpreadType ... spreadTypes) {
            Objects.requireNonNull(sculkVeinBlock);
            super(sculkVeinBlock);
            this.spreadTypes = spreadTypes;
        }

        @Override
        public boolean stateCanBeReplaced(BlockGetter level, BlockPos sourcePos, BlockPos placementPos, Direction placementDirection, BlockState existingState) {
            BlockPos neighourPos;
            BlockState againstState = level.getBlockState(placementPos.relative(placementDirection));
            if (againstState.is(Blocks.SCULK) || againstState.is(Blocks.SCULK_CATALYST) || againstState.is(Blocks.MOVING_PISTON)) {
                return false;
            }
            if (sourcePos.distManhattan(placementPos) == 2 && level.getBlockState(neighourPos = sourcePos.relative(placementDirection.getOpposite())).isFaceSturdy(level, neighourPos, placementDirection)) {
                return false;
            }
            FluidState fluidState = existingState.getFluidState();
            if (!fluidState.isEmpty() && !fluidState.is(Fluids.WATER)) {
                return false;
            }
            if (existingState.is(BlockTags.FIRE)) {
                return false;
            }
            return existingState.canBeReplaced() || super.stateCanBeReplaced(level, sourcePos, placementPos, placementDirection, existingState);
        }

        @Override
        public MultifaceSpreader.SpreadType[] getSpreadTypes() {
            return this.spreadTypes;
        }

        @Override
        public boolean isOtherBlockValidAsSource(BlockState state) {
            return !state.is(Blocks.SCULK_VEIN);
        }
    }
}

