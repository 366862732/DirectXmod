/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature
extends Feature<RootSystemConfiguration> {
    public RootSystemFeature(Codec<RootSystemConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RootSystemConfiguration> context) {
        BlockPos origin;
        WorldGenLevel level = context.level();
        if (!level.getBlockState(origin = context.origin()).isAir()) {
            return false;
        }
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        RootSystemConfiguration config = context.config();
        BlockPos.MutableBlockPos workingPos = pos.mutable();
        if (RootSystemFeature.placeDirtAndTree(level, context.chunkGenerator(), config, random, workingPos, pos)) {
            RootSystemFeature.placeRoots(level, config, random, pos, workingPos);
        }
        return true;
    }

    private static boolean spaceForTree(WorldGenLevel level, RootSystemConfiguration config, BlockPos pos) {
        BlockPos.MutableBlockPos columnUpPos = pos.mutable();
        for (int i = 1; i <= config.requiredVerticalSpaceForTree(); ++i) {
            columnUpPos.move(Direction.UP);
            BlockState state = level.getBlockState(columnUpPos);
            if (RootSystemFeature.isAllowedTreeSpace(state, i, config.allowedVerticalWaterForTree())) continue;
            return false;
        }
        if (config.levelTestDistance() > 0) {
            BlockPos.MutableBlockPos cornerPos = pos.mutable();
            for (int i = 0; i < 4; ++i) {
                cornerPos.move(Direction.from2DDataValue(i), config.levelTestDistance());
                BlockState below = level.getBlockState((BlockPos)cornerPos.below(config.maxLevelDeviation()));
                BlockState above = level.getBlockState((BlockPos)cornerPos.above(config.maxLevelDeviation()));
                if (below.isAir() || !above.isAir()) {
                    return false;
                }
                cornerPos.set(pos);
            }
        }
        return true;
    }

    private static boolean isAllowedTreeSpace(BlockState state, int blocksAboveOrigin, int allowedVerticalWaterHeight) {
        if (state.isAir()) {
            return true;
        }
        int blocksAboveGround = blocksAboveOrigin + 1;
        return blocksAboveGround <= allowedVerticalWaterHeight && state.getFluidState().is(FluidTags.WATER);
    }

    private static boolean placeDirtAndTree(WorldGenLevel level, ChunkGenerator generator, RootSystemConfiguration config, RandomSource random, BlockPos.MutableBlockPos workingPos, BlockPos pos) {
        for (int y = 0; y < config.rootColumnMaxHeight(); ++y) {
            workingPos.move(Direction.UP);
            if (level.getHeight(Heightmap.Types.WORLD_SURFACE, workingPos) < workingPos.getY()) {
                return false;
            }
            if (!config.allowedTreePosition().test(level, workingPos) || !RootSystemFeature.spaceForTree(level, config, workingPos)) continue;
            Vec3i belowPos = workingPos.below();
            if (level.getFluidState((BlockPos)belowPos).is(FluidTags.LAVA) || !level.getBlockState((BlockPos)belowPos).isSolid()) {
                return false;
            }
            if (!config.treeFeature().value().place(level, generator, random, workingPos)) continue;
            RootSystemFeature.placeDirt(pos, pos.getY() + y, level, config, random);
            return true;
        }
        return false;
    }

    private static void placeDirt(BlockPos origin, int targetHeight, WorldGenLevel level, RootSystemConfiguration config, RandomSource random) {
        int originX = origin.getX();
        int originZ = origin.getZ();
        BlockPos.MutableBlockPos workingPos = origin.mutable();
        for (int y = origin.getY(); y < targetHeight; ++y) {
            RootSystemFeature.placeRootedDirt(level, config, random, originX, originZ, workingPos.set(originX, y, originZ));
        }
    }

    private static void placeRootedDirt(WorldGenLevel level, RootSystemConfiguration config, RandomSource random, int originX, int originZ, BlockPos.MutableBlockPos workingPos) {
        int rootRadius = config.rootRadius();
        Predicate<BlockState> stateTest = s -> s.is(config.rootReplaceable());
        for (int i = 0; i < config.rootPlacementAttempts(); ++i) {
            workingPos.setWithOffset(workingPos, random.nextInt(rootRadius) - random.nextInt(rootRadius), 0, random.nextInt(rootRadius) - random.nextInt(rootRadius));
            if (stateTest.test(level.getBlockState(workingPos))) {
                level.setBlock(workingPos, config.rootStateProvider().getState(level, random, workingPos), 2);
            }
            workingPos.setX(originX);
            workingPos.setZ(originZ);
        }
    }

    private static void placeRoots(WorldGenLevel level, RootSystemConfiguration config, RandomSource random, BlockPos pos, BlockPos.MutableBlockPos workingPos) {
        int rootRadius = config.hangingRootRadius();
        int verticalSpan = config.hangingRootsVerticalSpan();
        for (int i = 0; i < config.hangingRootPlacementAttempts(); ++i) {
            BlockState targetState;
            workingPos.setWithOffset(pos, random.nextInt(rootRadius) - random.nextInt(rootRadius), random.nextInt(verticalSpan) - random.nextInt(verticalSpan), random.nextInt(rootRadius) - random.nextInt(rootRadius));
            if (!level.isEmptyBlock(workingPos) || !(targetState = config.hangingRootStateProvider().getState(level, random, workingPos)).canSurvive(level, workingPos) || !level.getBlockState((BlockPos)workingPos.above()).isFaceSturdy(level, workingPos, Direction.DOWN)) continue;
            level.setBlock(workingPos, targetState, 2);
        }
    }
}

