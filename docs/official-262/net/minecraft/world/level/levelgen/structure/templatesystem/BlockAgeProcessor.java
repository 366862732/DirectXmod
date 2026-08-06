/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public class BlockAgeProcessor
implements StructureProcessor {
    public static final MapCodec<BlockAgeProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("mossiness").forGetter(p -> Float.valueOf(p.mossiness))).apply((Applicative)i, BlockAgeProcessor::new));
    private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5f;
    private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5f;
    private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15f;
    private static final BlockState[] NON_MOSSY_REPLACEMENTS = new BlockState[]{Blocks.STONE_SLAB.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState()};
    private final float mossiness;

    public BlockAgeProcessor(float mossiness) {
        this.mossiness = mossiness;
    }

    @Override
    public  @Nullable StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition, BlockPos referencePos, BlockPos templateRelativePos, StructureTemplate.StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings) {
        RandomSource random = settings.getRandom(processedBlockInfo.pos());
        BlockState state = processedBlockInfo.state();
        BlockPos pos = processedBlockInfo.pos();
        BlockState newState = null;
        if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.STONE) || state.is(Blocks.CHISELED_STONE_BRICKS)) {
            newState = this.maybeReplaceFullStoneBlock(random);
        } else if (state.is(BlockTags.STAIRS)) {
            newState = this.maybeReplaceStairs(state, random);
        } else if (state.is(BlockTags.SLABS)) {
            newState = this.maybeReplaceSlab(state, random);
        } else if (state.is(BlockTags.WALLS)) {
            newState = this.maybeReplaceWall(state, random);
        } else if (state.is(Blocks.OBSIDIAN)) {
            newState = this.maybeReplaceObsidian(random);
        }
        if (newState != null) {
            return new StructureTemplate.StructureBlockInfo(pos, newState, processedBlockInfo.nbt());
        }
        return processedBlockInfo;
    }

    private @Nullable BlockState maybeReplaceFullStoneBlock(RandomSource random) {
        if (random.nextFloat() >= 0.5f) {
            return null;
        }
        BlockState[] nonMossyReplacements = new BlockState[]{Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), BlockAgeProcessor.getRandomFacingStairs(random, Blocks.STONE_BRICK_STAIRS)};
        BlockState[] mossyReplacements = new BlockState[]{Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), BlockAgeProcessor.getRandomFacingStairs(random, Blocks.MOSSY_STONE_BRICK_STAIRS)};
        return this.getRandomBlock(random, nonMossyReplacements, mossyReplacements);
    }

    private @Nullable BlockState maybeReplaceStairs(BlockState blockState, RandomSource random) {
        if (random.nextFloat() >= 0.5f) {
            return null;
        }
        BlockState[] mossyReplacements = new BlockState[]{Blocks.MOSSY_STONE_BRICK_STAIRS.withPropertiesOf(blockState), Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()};
        return this.getRandomBlock(random, NON_MOSSY_REPLACEMENTS, mossyReplacements);
    }

    private @Nullable BlockState maybeReplaceSlab(BlockState blockState, RandomSource random) {
        if (random.nextFloat() < this.mossiness) {
            return Blocks.MOSSY_STONE_BRICK_SLAB.withPropertiesOf(blockState);
        }
        return null;
    }

    private @Nullable BlockState maybeReplaceWall(BlockState blockState, RandomSource random) {
        if (random.nextFloat() < this.mossiness) {
            return Blocks.MOSSY_STONE_BRICK_WALL.withPropertiesOf(blockState);
        }
        return null;
    }

    private @Nullable BlockState maybeReplaceObsidian(RandomSource random) {
        if (random.nextFloat() < 0.15f) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        return null;
    }

    private static BlockState getRandomFacingStairs(RandomSource random, Block stairBlock) {
        return (BlockState)((BlockState)stairBlock.defaultBlockState().setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))).setValue(StairBlock.HALF, Util.getRandom(Half.values(), random));
    }

    private BlockState getRandomBlock(RandomSource random, BlockState[] nonMossyBlocks, BlockState[] mossyBlocks) {
        if (random.nextFloat() < this.mossiness) {
            return BlockAgeProcessor.getRandomBlock(random, mossyBlocks);
        }
        return BlockAgeProcessor.getRandomBlock(random, nonMossyBlocks);
    }

    private static BlockState getRandomBlock(RandomSource random, BlockState[] blocks) {
        return blocks[random.nextInt(blocks.length)];
    }

    public MapCodec<BlockAgeProcessor> codec() {
        return MAP_CODEC;
    }
}

