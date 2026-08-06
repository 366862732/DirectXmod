/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BasaltPillarFeature
extends Feature<NoneFeatureConfiguration> {
    public BasaltPillarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (!level.isEmptyBlock(origin) || level.isEmptyBlock(origin.above())) {
            return false;
        }
        BlockPos.MutableBlockPos pos = origin.mutable();
        BlockPos.MutableBlockPos tmpPos = origin.mutable();
        boolean placeNorthHangoff = true;
        boolean placeSouthHangoff = true;
        boolean placeWestHangoff = true;
        boolean placeEastHangoff = true;
        while (level.isEmptyBlock(pos)) {
            if (level.isOutsideBuildHeight(pos)) {
                return true;
            }
            level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 2);
            placeNorthHangoff = placeNorthHangoff && this.placeHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.NORTH));
            placeSouthHangoff = placeSouthHangoff && this.placeHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.SOUTH));
            placeWestHangoff = placeWestHangoff && this.placeHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.WEST));
            placeEastHangoff = placeEastHangoff && this.placeHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.EAST));
            pos.move(Direction.DOWN);
        }
        pos.move(Direction.UP);
        this.placeBaseHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.NORTH));
        this.placeBaseHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.SOUTH));
        this.placeBaseHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.WEST));
        this.placeBaseHangOff(level, random, tmpPos.setWithOffset((Vec3i)pos, Direction.EAST));
        pos.move(Direction.DOWN);
        BlockPos.MutableBlockPos basePos = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx < 4; ++dx) {
            for (int dz = -3; dz < 4; ++dz) {
                int probability = Mth.abs(dx) * Mth.abs(dz);
                if (random.nextInt(10) >= 10 - probability) continue;
                basePos.set(pos.offset(dx, 0, dz));
                int maxDrop = 3;
                while (level.isEmptyBlock(tmpPos.setWithOffset((Vec3i)basePos, Direction.DOWN))) {
                    basePos.move(Direction.DOWN);
                    if (--maxDrop > 0) continue;
                }
                if (level.isEmptyBlock(tmpPos.setWithOffset((Vec3i)basePos, Direction.DOWN))) continue;
                level.setBlock(basePos, Blocks.BASALT.defaultBlockState(), 2);
            }
        }
        return true;
    }

    private void placeBaseHangOff(LevelAccessor level, RandomSource random, BlockPos pos) {
        if (random.nextBoolean()) {
            level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 2);
        }
    }

    private boolean placeHangOff(LevelAccessor level, RandomSource random, BlockPos hangOffPos) {
        if (random.nextInt(10) != 0) {
            level.setBlock(hangOffPos, Blocks.BASALT.defaultBlockState(), 2);
            return true;
        }
        return false;
    }
}

