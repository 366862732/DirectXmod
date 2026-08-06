/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class AmbientDesertBlockSoundsPlayer {
    private static final int IDLE_SOUND_CHANCE = 2100;
    private static final int DRY_GRASS_SOUND_CHANCE = 200;
    private static final int DEAD_BUSH_SOUND_CHANCE = 130;
    private static final int DEAD_BUSH_SOUND_BADLANDS_DECREASED_CHANCE = 3;
    private static final int SURROUNDING_BLOCKS_PLAY_SOUND_THRESHOLD = 3;
    private static final int SURROUNDING_BLOCKS_DISTANCE_HORIZONTAL_CHECK = 8;
    private static final int SURROUNDING_BLOCKS_DISTANCE_VERTICAL_CHECK = 5;
    private static final int HORIZONTAL_DIRECTIONS = 4;

    public static void playAmbientSandSounds(Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).is(Blocks.AIR)) {
            return;
        }
        if (random.nextInt(2100) == 0 && AmbientDesertBlockSoundsPlayer.shouldPlayAmbientSandSound(level, pos)) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0f, 1.0f, false);
        }
    }

    public static void playAmbientDryGrassSounds(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(200) == 0 && AmbientDesertBlockSoundsPlayer.shouldPlayDesertDryVegetationBlockSounds(level, pos.below())) {
            level.playPlayerSound(SoundEvents.DRY_GRASS, SoundSource.AMBIENT, 1.0f, 1.0f);
        }
    }

    public static void playAmbientDeadBushSounds(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(130) == 0) {
            BlockState belowPos = level.getBlockState(pos.below());
            if ((belowPos.is(Blocks.RED_SAND) || belowPos.is(BlockTags.TERRACOTTA)) && random.nextInt(3) != 0) {
                return;
            }
            if (AmbientDesertBlockSoundsPlayer.shouldPlayDesertDryVegetationBlockSounds(level, pos.below())) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0f, 1.0f, false);
            }
        }
    }

    public static boolean shouldPlayDesertDryVegetationBlockSounds(Level level, BlockPos belowPos) {
        return level.getBlockState(belowPos).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS) && level.getBlockState(belowPos.below()).is(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS);
    }

    private static boolean shouldPlayAmbientSandSound(Level level, BlockPos pos) {
        int matchingBlocksFound = 0;
        int sidesChecked = 0;
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int remainingSides;
            int potentialMatches;
            boolean canStillFindRequiredSoundTriggerBlocks;
            mutablePos.set(pos).move(dir, 8);
            if (AmbientDesertBlockSoundsPlayer.columnContainsTriggeringBlock(level, mutablePos) && matchingBlocksFound++ >= 3) {
                return true;
            }
            if (canStillFindRequiredSoundTriggerBlocks = (potentialMatches = (remainingSides = 4 - ++sidesChecked) + matchingBlocksFound) >= 3) continue;
            return false;
        }
        return false;
    }

    private static boolean columnContainsTriggeringBlock(Level level, BlockPos.MutableBlockPos mutablePos) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, mutablePos) - 1;
        if (Math.abs(surfaceY - mutablePos.getY()) <= 5) {
            boolean hasAirAbove = level.getBlockState(mutablePos.setY(surfaceY + 1)).isAir();
            return hasAirAbove && AmbientDesertBlockSoundsPlayer.canTriggerAmbientDesertSandSounds(level.getBlockState(mutablePos.setY(surfaceY)));
        }
        mutablePos.move(Direction.UP, 6);
        BlockState aboveBlockState = level.getBlockState(mutablePos);
        mutablePos.move(Direction.DOWN);
        for (int i = 0; i < 10; ++i) {
            BlockState currentBlockState = level.getBlockState(mutablePos);
            if (aboveBlockState.isAir() && AmbientDesertBlockSoundsPlayer.canTriggerAmbientDesertSandSounds(currentBlockState)) {
                return true;
            }
            aboveBlockState = currentBlockState;
            mutablePos.move(Direction.DOWN);
        }
        return false;
    }

    private static boolean canTriggerAmbientDesertSandSounds(BlockState blockState) {
        return blockState.is(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS);
    }
}

