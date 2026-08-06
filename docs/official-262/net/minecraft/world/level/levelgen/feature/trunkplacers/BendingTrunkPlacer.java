/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class BendingTrunkPlacer
extends TrunkPlacer {
    public static final MapCodec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> BendingTrunkPlacer.trunkPlacerParts(i).and(i.group((App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", (Object)1).forGetter(c -> c.minHeightForLeaves), (App)IntProviders.codec(1, 64).fieldOf("bend_length").forGetter(c -> c.bendLength))).apply((Applicative)i, BendingTrunkPlacer::new));
    private final int minHeightForLeaves;
    private final IntProvider bendLength;

    public BendingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, int minHeightForLeaves, IntProvider bendLength) {
        super(baseHeight, heightRandA, heightRandB);
        this.minHeightForLeaves = minHeightForLeaves;
        this.bendLength = bendLength;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.BENDING_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int logHeight = treeHeight - 1;
        BlockPos.MutableBlockPos pos = origin.mutable();
        Vec3i belowPos = pos.below();
        BendingTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, (BlockPos)belowPos, config);
        ArrayList foliagePoints = Lists.newArrayList();
        for (int i = 0; i <= logHeight; ++i) {
            if (i + 1 >= logHeight + random.nextInt(2)) {
                pos.move(direction);
            }
            if (TreeFeature.validTreePos(level, pos)) {
                this.placeLog(level, trunkSetter, random, pos, config);
            }
            if (i >= this.minHeightForLeaves) {
                foliagePoints.add(new FoliagePlacer.FoliageAttachment(pos.immutable(), 0, false));
            }
            pos.move(Direction.UP);
        }
        int dirLength = this.bendLength.sample(random);
        for (int i = 0; i <= dirLength; ++i) {
            if (TreeFeature.validTreePos(level, pos)) {
                this.placeLog(level, trunkSetter, random, pos, config);
            }
            foliagePoints.add(new FoliagePlacer.FoliageAttachment(pos.immutable(), 0, false));
            pos.move(direction);
        }
        return foliagePoints;
    }
}

