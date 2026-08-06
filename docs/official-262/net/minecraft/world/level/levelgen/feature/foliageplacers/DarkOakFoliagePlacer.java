/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public class DarkOakFoliagePlacer
extends FoliagePlacer {
    public static final MapCodec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> DarkOakFoliagePlacer.foliagePlacerParts(i).apply((Applicative)i, DarkOakFoliagePlacer::new));

    public DarkOakFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(WorldGenLevel level, FoliagePlacer.FoliageSetter foliageSetter, RandomSource random, TreeConfiguration config, int treeHeight, FoliagePlacer.FoliageAttachment foliageAttachment, int foliageHeight, int leafRadius, int offset) {
        BlockPos pos = foliageAttachment.pos().above(offset);
        boolean doubleTrunk = foliageAttachment.doubleTrunk();
        if (doubleTrunk) {
            this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius + 2, -1, doubleTrunk);
            this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius + 3, 0, doubleTrunk);
            this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius + 2, 1, doubleTrunk);
            if (random.nextBoolean()) {
                this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius, 2, doubleTrunk);
            }
        } else {
            this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius + 2, -1, doubleTrunk);
            this.placeLeavesRow(level, foliageSetter, random, config, pos, leafRadius + 1, 0, doubleTrunk);
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int treeHeight, TreeConfiguration config) {
        return 4;
    }

    @Override
    protected boolean shouldSkipLocationSigned(RandomSource random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
        if (!(y != 0 || !doubleTrunk || dx != -currentRadius && dx < currentRadius || dz != -currentRadius && dz < currentRadius)) {
            return true;
        }
        return super.shouldSkipLocationSigned(random, dx, y, dz, currentRadius, doubleTrunk);
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
        if (y == -1 && !doubleTrunk) {
            return dx == currentRadius && dz == currentRadius;
        }
        if (y == 1) {
            return dx + dz > currentRadius * 2 - 2;
        }
        return false;
    }
}

