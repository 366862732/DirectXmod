/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature
extends Feature<DiskConfiguration> {
    public DiskFeature(Codec<DiskConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DiskConfiguration> context) {
        DiskConfiguration config = context.config();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        boolean placedAny = false;
        int originY = origin.getY();
        int top = originY + config.halfHeight();
        int bottom = originY - config.halfHeight() - 1;
        int r = config.radius().sample(random);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (BlockPos columnPos : BlockPos.betweenClosed(origin.offset(-r, 0, -r), origin.offset(r, 0, r))) {
            int zd;
            int xd = columnPos.getX() - origin.getX();
            if (xd * xd + (zd = columnPos.getZ() - origin.getZ()) * zd > r * r) continue;
            placedAny |= this.placeColumn(config, level, random, top, bottom, mutablePos.set(columnPos));
        }
        return placedAny;
    }

    protected boolean placeColumn(DiskConfiguration config, WorldGenLevel level, RandomSource random, int top, int bottom, BlockPos.MutableBlockPos pos) {
        boolean placedAny = false;
        boolean placedAbove = false;
        for (int y = top; y > bottom; --y) {
            pos.setY(y);
            if (config.target().test(level, pos)) {
                BlockState state = config.stateProvider().getOptionalState(level, random, pos);
                if (state == null) continue;
                level.setBlock(pos, state, 2);
                if (!placedAbove) {
                    this.markAboveForPostProcessing(level, pos);
                }
                placedAny = true;
                placedAbove = true;
                continue;
            }
            placedAbove = false;
        }
        return placedAny;
    }
}

