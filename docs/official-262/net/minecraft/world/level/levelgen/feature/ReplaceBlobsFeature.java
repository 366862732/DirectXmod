/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;
import org.jspecify.annotations.Nullable;

public class ReplaceBlobsFeature
extends Feature<ReplaceSphereConfiguration> {
    public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> context) {
        ReplaceSphereConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Block targetBlock = config.targetState.getBlock();
        BlockPos centerPos = ReplaceBlobsFeature.findTarget(level, context.origin().mutable().clamp(Direction.Axis.Y, level.getMinY() + 1, level.getMaxY()), targetBlock);
        if (centerPos == null) {
            return false;
        }
        int radiusX = config.radius().sample(random);
        int radiusY = config.radius().sample(random);
        int radiusZ = config.radius().sample(random);
        int maximumRadius = Math.max(radiusX, Math.max(radiusY, radiusZ));
        boolean replacedAny = false;
        for (BlockPos pos : BlockPos.withinManhattan(centerPos, radiusX, radiusY, radiusZ)) {
            if (pos.distManhattan(centerPos) > maximumRadius) break;
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.is(targetBlock)) continue;
            this.setBlock(level, pos, config.replaceState);
            replacedAny = true;
        }
        return replacedAny;
    }

    private static @Nullable BlockPos findTarget(LevelAccessor level, BlockPos.MutableBlockPos cursor, Block target) {
        while (cursor.getY() > level.getMinY() + 1) {
            BlockState blockState = level.getBlockState(cursor);
            if (blockState.is(target)) {
                return cursor;
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }
}

