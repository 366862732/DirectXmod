/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature
extends Feature<VegetationPatchConfiguration> {
    public VegetationPatchFeature(Codec<VegetationPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        VegetationPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        Predicate<BlockState> replaceable = s -> s.is(config.replaceable());
        int xRadius = config.xzRadius().sample(random) + 1;
        int zRadius = config.xzRadius().sample(random) + 1;
        Set<BlockPos> surface = this.placeGroundPatch(level, config, random, origin, replaceable, xRadius, zRadius);
        this.distributeVegetation(context, level, config, random, surface, xRadius, zRadius);
        return !surface.isEmpty();
    }

    protected Set<BlockPos> placeGroundPatch(WorldGenLevel level, VegetationPatchConfiguration config, RandomSource random, BlockPos origin, Predicate<BlockState> replaceable, int xRadius, int zRadius) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        BlockPos.MutableBlockPos belowPos = pos.mutable();
        Direction inwards = config.surface().getDirection();
        Direction outwards = inwards.getOpposite();
        HashSet<BlockPos> surface = new HashSet<BlockPos>();
        for (int dx = -xRadius; dx <= xRadius; ++dx) {
            boolean isXEdge = dx == -xRadius || dx == xRadius;
            for (int dz = -zRadius; dz <= zRadius; ++dz) {
                int offset;
                boolean isEdgeButNotCorner;
                boolean isZEdge = dz == -zRadius || dz == zRadius;
                boolean isEdge = isXEdge || isZEdge;
                boolean isCorner = isXEdge && isZEdge;
                boolean bl = isEdgeButNotCorner = isEdge && !isCorner;
                if (isCorner || isEdgeButNotCorner && (config.extraEdgeColumnChance() == 0.0f || random.nextFloat() > config.extraEdgeColumnChance())) continue;
                pos.setWithOffset(origin, dx, 0, dz);
                for (offset = 0; level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::isAir) && offset < config.verticalRange(); ++offset) {
                    pos.move(inwards);
                }
                for (offset = 0; level.isStateAtPosition(pos, s -> !s.isAir()) && offset < config.verticalRange(); ++offset) {
                    pos.move(outwards);
                }
                belowPos.setWithOffset((Vec3i)pos, config.surface().getDirection());
                BlockState belowState = level.getBlockState(belowPos);
                if (!level.isEmptyBlock(pos) || !belowState.isFaceSturdy(level, belowPos, config.surface().getDirection().getOpposite())) continue;
                int depth = config.depth().sample(random) + (config.extraBottomBlockChance() > 0.0f && random.nextFloat() < config.extraBottomBlockChance() ? 1 : 0);
                BlockPos groundPos = belowPos.immutable();
                boolean groundPlaced = this.placeGround(level, config, replaceable, random, belowPos, depth);
                if (!groundPlaced) continue;
                surface.add(groundPos);
            }
        }
        return surface;
    }

    protected void distributeVegetation(FeaturePlaceContext<VegetationPatchConfiguration> context, WorldGenLevel level, VegetationPatchConfiguration config, RandomSource random, Set<BlockPos> surface, int xRadius, int zRadius) {
        for (BlockPos surfacePos : surface) {
            if (!(config.vegetationChance() > 0.0f) || !(random.nextFloat() < config.vegetationChance())) continue;
            this.placeVegetation(level, config, context.chunkGenerator(), random, surfacePos);
        }
    }

    protected boolean placeVegetation(WorldGenLevel level, VegetationPatchConfiguration config, ChunkGenerator generator, RandomSource random, BlockPos vegetationPos) {
        return config.vegetationFeature().value().place(level, generator, random, vegetationPos.relative(config.surface().getDirection().getOpposite()));
    }

    protected boolean placeGround(WorldGenLevel level, VegetationPatchConfiguration config, Predicate<BlockState> replaceable, RandomSource random, BlockPos.MutableBlockPos belowPos, int depth) {
        for (int i = 0; i < depth; ++i) {
            BlockState belowState;
            BlockState stateToPlace = config.groundState().getState(level, random, belowPos);
            if (stateToPlace.is((belowState = level.getBlockState(belowPos)).getBlock())) continue;
            if (!replaceable.test(belowState)) {
                return i != 0;
            }
            level.setBlock(belowPos, stateToPlace, 2);
            belowPos.move(config.surface().getDirection());
        }
        return true;
    }
}

