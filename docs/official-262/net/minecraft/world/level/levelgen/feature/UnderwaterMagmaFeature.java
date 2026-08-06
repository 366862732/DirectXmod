/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UnderwaterMagmaFeature
extends Feature<UnderwaterMagmaConfiguration> {
    public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> context) {
        Vec3i radius;
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        UnderwaterMagmaConfiguration config = context.config();
        RandomSource random = context.random();
        OptionalInt floorY = UnderwaterMagmaFeature.getFloorY(level, origin, config);
        if (floorY.isEmpty()) {
            return false;
        }
        BlockPos floorPos = origin.atY(floorY.getAsInt());
        BoundingBox bounds = BoundingBox.fromCorners(floorPos.subtract(radius = new Vec3i(config.placementRadiusAroundFloor, config.placementRadiusAroundFloor, config.placementRadiusAroundFloor)), floorPos.offset(radius));
        return BlockPos.betweenClosedStream(bounds).filter(pos -> random.nextFloat() < config.placementProbabilityPerValidPosition).filter(pos -> this.isValidPlacement(level, (BlockPos)pos)).mapToInt(pos -> {
            level.setBlock((BlockPos)pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
            return 1;
        }).sum() > 0;
    }

    private static OptionalInt getFloorY(WorldGenLevel level, BlockPos origin, UnderwaterMagmaConfiguration config) {
        Predicate<BlockState> insideColumn = state -> state.is(Blocks.WATER);
        Predicate<BlockState> validEdge = state -> !state.is(Blocks.WATER);
        Optional<Column> waterColumn = Column.scan(level, origin, config.floorSearchRange, insideColumn, validEdge);
        return waterColumn.map(Column::getFloor).orElseGet(OptionalInt::empty);
    }

    private boolean isValidPlacement(WorldGenLevel level, BlockPos pos) {
        if (UnderwaterMagmaFeature.isWaterOrAir(level.getBlockState(pos)) || this.isVisibleFromOutside(level, pos.below(), Direction.UP)) {
            return false;
        }
        for (Direction neighbourDir : Direction.Plane.HORIZONTAL) {
            if (!this.isVisibleFromOutside(level, pos.relative(neighbourDir), neighbourDir.getOpposite())) continue;
            return false;
        }
        return true;
    }

    private static boolean isWaterOrAir(BlockState state) {
        return state.is(Blocks.WATER) || state.isAir();
    }

    private boolean isVisibleFromOutside(LevelAccessor level, BlockPos pos, Direction coveredDirection) {
        BlockState state = level.getBlockState(pos);
        VoxelShape faceOcclusionShape = state.getFaceOcclusionShape(coveredDirection);
        return faceOcclusionShape == Shapes.empty() || !Block.isShapeFullBlock(faceOcclusionShape);
    }
}

