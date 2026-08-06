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
package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.rootplacers.AboveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MangroveRootPlacer
extends RootPlacer {
    public static final int ROOT_WIDTH_LIMIT = 8;
    public static final int ROOT_LENGTH_LIMIT = 15;
    public static final MapCodec<MangroveRootPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> MangroveRootPlacer.rootPlacerParts(i).and((App)MangroveRootPlacement.CODEC.fieldOf("mangrove_root_placement").forGetter(c -> c.mangroveRootPlacement)).apply((Applicative)i, MangroveRootPlacer::new));
    private final MangroveRootPlacement mangroveRootPlacement;

    public MangroveRootPlacer(IntProvider trunkOffsetY, BlockStateProvider rootProvider, Optional<AboveRootPlacement> aboveRootPlacement, MangroveRootPlacement mangroveRootPlacement) {
        super(trunkOffsetY, rootProvider, aboveRootPlacement);
        this.mangroveRootPlacement = mangroveRootPlacement;
    }

    @Override
    public boolean placeRoots(WorldGenLevel level, BiConsumer<BlockPos, BlockState> rootSetter, RandomSource random, BlockPos origin, BlockPos trunkOrigin, TreeConfiguration config) {
        ArrayList rootPositions = Lists.newArrayList();
        BlockPos.MutableBlockPos columnPos = origin.mutable();
        while (columnPos.getY() < trunkOrigin.getY()) {
            if (!this.canPlaceRoot(level, columnPos)) {
                return false;
            }
            columnPos.move(Direction.UP);
        }
        rootPositions.add(trunkOrigin.below());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            ArrayList positionsInDirection;
            BlockPos pos = trunkOrigin.relative(dir);
            if (!this.simulateRoots(level, random, pos, dir, trunkOrigin, positionsInDirection = Lists.newArrayList(), 0)) {
                return false;
            }
            rootPositions.addAll(positionsInDirection);
            rootPositions.add(trunkOrigin.relative(dir));
        }
        for (BlockPos rootPos : rootPositions) {
            this.placeRoot(level, rootSetter, random, rootPos, config);
        }
        return true;
    }

    private boolean simulateRoots(LevelSimulatedReader level, RandomSource random, BlockPos rootPos, Direction dir, BlockPos rootOrigin, List<BlockPos> rootPositions, int layer) {
        int maxRootLength = this.mangroveRootPlacement.maxRootLength();
        if (layer == maxRootLength || rootPositions.size() > maxRootLength) {
            return false;
        }
        List<BlockPos> potentialRootPositions = this.potentialRootPositions(rootPos, dir, random, rootOrigin);
        for (BlockPos pos : potentialRootPositions) {
            if (!this.canPlaceRoot(level, pos)) continue;
            rootPositions.add(pos);
            if (this.simulateRoots(level, random, pos, dir, rootOrigin, rootPositions, layer + 1)) continue;
            return false;
        }
        return true;
    }

    protected List<BlockPos> potentialRootPositions(BlockPos pos, Direction prevDir, RandomSource random, BlockPos rootOrigin) {
        BlockPos below = pos.below();
        BlockPos nextTo = pos.relative(prevDir);
        int width = pos.distManhattan(rootOrigin);
        int maxRootWidth = this.mangroveRootPlacement.maxRootWidth();
        float randomSkewChance = this.mangroveRootPlacement.randomSkewChance();
        if (width > maxRootWidth - 3 && width <= maxRootWidth) {
            return random.nextFloat() < randomSkewChance ? List.of(below, nextTo.below()) : List.of(below);
        }
        if (width > maxRootWidth) {
            return List.of(below);
        }
        if (random.nextFloat() < randomSkewChance) {
            return List.of(below);
        }
        return random.nextBoolean() ? List.of(nextTo) : List.of(below);
    }

    @Override
    protected boolean canPlaceRoot(LevelSimulatedReader level, BlockPos pos) {
        return super.canPlaceRoot(level, pos) || level.isStateAtPosition(pos, state -> state.is(this.mangroveRootPlacement.canGrowThrough()));
    }

    @Override
    protected void placeRoot(WorldGenLevel level, BiConsumer<BlockPos, BlockState> rootSetter, RandomSource random, BlockPos pos, TreeConfiguration config) {
        if (level.isStateAtPosition(pos, s -> s.is(this.mangroveRootPlacement.muddyRootsIn()))) {
            BlockState muddyRoots = this.mangroveRootPlacement.muddyRootsProvider().getState(level, random, pos);
            rootSetter.accept(pos, this.getPotentiallyWaterloggedState(level, pos, muddyRoots));
        } else {
            super.placeRoot(level, rootSetter, random, pos, config);
        }
    }

    @Override
    protected RootPlacerType<?> type() {
        return RootPlacerType.MANGROVE_ROOT_PLACER;
    }
}

