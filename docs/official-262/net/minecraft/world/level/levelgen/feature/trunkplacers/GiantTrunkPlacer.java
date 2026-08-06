/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class GiantTrunkPlacer
extends TrunkPlacer {
    public static final MapCodec<GiantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> GiantTrunkPlacer.trunkPlacerParts(i).apply((Applicative)i, GiantTrunkPlacer::new));

    public GiantTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.GIANT_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        BlockPos below = origin.below();
        GiantTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, below, config);
        GiantTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, below.east(), config);
        GiantTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, below.south(), config);
        GiantTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, below.south().east(), config);
        BlockPos.MutableBlockPos trunkPos = new BlockPos.MutableBlockPos();
        for (int hh = 0; hh < treeHeight; ++hh) {
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, config, origin, 0, hh, 0);
            if (hh >= treeHeight - 1) continue;
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, config, origin, 1, hh, 0);
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, config, origin, 1, hh, 1);
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, config, origin, 0, hh, 1);
        }
        return ImmutableList.of((Object)new FoliagePlacer.FoliageAttachment(origin.above(treeHeight), 0, true));
    }

    private void placeLogIfFreeWithOffset(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos.MutableBlockPos trunkPos, TreeConfiguration config, BlockPos treePos, int x, int y, int z) {
        trunkPos.setWithOffset(treePos, x, y, z);
        this.placeLogIfFree(level, trunkSetter, random, trunkPos, config);
    }
}

