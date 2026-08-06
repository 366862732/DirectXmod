/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class FancyTrunkPlacer
extends TrunkPlacer {
    public static final MapCodec<FancyTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> FancyTrunkPlacer.trunkPlacerParts(i).apply((Applicative)i, FancyTrunkPlacer::new));
    private static final double TRUNK_HEIGHT_SCALE = 0.618;
    private static final double CLUSTER_DENSITY_MAGIC = 1.382;
    private static final double BRANCH_SLOPE = 0.381;
    private static final double BRANCH_LENGTH_MAGIC = 0.328;

    public FancyTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.FANCY_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        int relativeY;
        int assumedFoliageHeight = 5;
        int height = treeHeight + 2;
        int trunkHeight = Mth.floor((double)height * 0.618);
        FancyTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), config);
        double foliageDensity = 1.0;
        int clustersPerY = Math.min(1, Mth.floor(1.382 + Math.pow(1.0 * (double)height / 13.0, 2.0)));
        int trunkTop = origin.getY() + trunkHeight;
        ArrayList foliageCoords = Lists.newArrayList();
        foliageCoords.add(new FoliageCoords(origin.above(relativeY), trunkTop));
        for (relativeY = height - 5; relativeY >= 0; --relativeY) {
            float treeShape = FancyTrunkPlacer.treeShape(height, relativeY);
            if (treeShape < 0.0f) continue;
            for (int i = 0; i < clustersPerY; ++i) {
                BlockPos checkEnd;
                double widthScale = 1.0;
                double radius = 1.0 * (double)treeShape * ((double)random.nextFloat() + 0.328);
                double angle = (double)(random.nextFloat() * 2.0f) * Math.PI;
                double x = radius * Math.sin(angle) + 0.5;
                double z = radius * Math.cos(angle) + 0.5;
                BlockPos checkStart = origin.offset(Mth.floor(x), relativeY - 1, Mth.floor(z));
                if (!this.makeLimb(level, trunkSetter, random, checkStart, checkEnd = checkStart.above(5), false, config)) continue;
                int dx = origin.getX() - checkStart.getX();
                int dz = origin.getZ() - checkStart.getZ();
                double branchHeight = (double)checkStart.getY() - Math.sqrt(dx * dx + dz * dz) * 0.381;
                int branchTop = branchHeight > (double)trunkTop ? trunkTop : (int)branchHeight;
                BlockPos checkBranchBase = new BlockPos(origin.getX(), branchTop, origin.getZ());
                if (!this.makeLimb(level, trunkSetter, random, checkBranchBase, checkStart, false, config)) continue;
                foliageCoords.add(new FoliageCoords(checkStart, checkBranchBase.getY()));
            }
        }
        this.makeLimb(level, trunkSetter, random, origin, origin.above(trunkHeight), true, config);
        this.makeBranches(level, trunkSetter, random, height, origin, foliageCoords, config);
        ArrayList attachments = Lists.newArrayList();
        for (FoliageCoords foliageCoord : foliageCoords) {
            if (!this.trimBranches(height, foliageCoord.getBranchBase() - origin.getY())) continue;
            attachments.add(foliageCoord.attachment);
        }
        return attachments;
    }

    private boolean makeLimb(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos startPos, BlockPos endPos, boolean doPlace, TreeConfiguration config) {
        if (!doPlace && Objects.equals(startPos, endPos)) {
            return true;
        }
        BlockPos delta = endPos.offset(-startPos.getX(), -startPos.getY(), -startPos.getZ());
        int steps = this.getSteps(delta);
        float dx = (float)delta.getX() / (float)steps;
        float dy = (float)delta.getY() / (float)steps;
        float dz = (float)delta.getZ() / (float)steps;
        for (int i = 0; i <= steps; ++i) {
            BlockPos blockPos = startPos.offset(Mth.floor(0.5f + (float)i * dx), Mth.floor(0.5f + (float)i * dy), Mth.floor(0.5f + (float)i * dz));
            if (doPlace) {
                this.placeLog(level, trunkSetter, random, blockPos, config, state -> (BlockState)state.trySetValue(RotatedPillarBlock.AXIS, this.getLogAxis(startPos, blockPos)));
                continue;
            }
            if (this.isFree(level, blockPos)) continue;
            return false;
        }
        return true;
    }

    private int getSteps(BlockPos pos) {
        int absX = Mth.abs(pos.getX());
        int absY = Mth.abs(pos.getY());
        int absZ = Mth.abs(pos.getZ());
        return Math.max(absX, Math.max(absY, absZ));
    }

    private Direction.Axis getLogAxis(BlockPos startPos, BlockPos blockPos) {
        int zdiff;
        Direction.Axis axis = Direction.Axis.Y;
        int xdiff = Math.abs(blockPos.getX() - startPos.getX());
        int maxdiff = Math.max(xdiff, zdiff = Math.abs(blockPos.getZ() - startPos.getZ()));
        if (maxdiff > 0) {
            axis = xdiff == maxdiff ? Direction.Axis.X : Direction.Axis.Z;
        }
        return axis;
    }

    private boolean trimBranches(int height, int localY) {
        return (double)localY >= (double)height * 0.2;
    }

    private void makeBranches(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int height, BlockPos origin, List<FoliageCoords> foliageCoords, TreeConfiguration config) {
        for (FoliageCoords endCoord : foliageCoords) {
            int branchBase = endCoord.getBranchBase();
            BlockPos baseCoord = new BlockPos(origin.getX(), branchBase, origin.getZ());
            if (baseCoord.equals(endCoord.attachment.pos()) || !this.trimBranches(height, branchBase - origin.getY())) continue;
            this.makeLimb(level, trunkSetter, random, baseCoord, endCoord.attachment.pos(), true, config);
        }
    }

    private static float treeShape(int height, int y) {
        if ((float)y < (float)height * 0.3f) {
            return -1.0f;
        }
        float radius = (float)height / 2.0f;
        float adjacent = radius - (float)y;
        float distance = Mth.sqrt(radius * radius - adjacent * adjacent);
        if (adjacent == 0.0f) {
            distance = radius;
        } else if (Math.abs(adjacent) >= radius) {
            return 0.0f;
        }
        return distance * 0.5f;
    }

    private static class FoliageCoords {
        private final FoliagePlacer.FoliageAttachment attachment;
        private final int branchBase;

        public FoliageCoords(BlockPos pos, int branchBase) {
            this.attachment = new FoliagePlacer.FoliageAttachment(pos, 0, false);
            this.branchBase = branchBase;
        }

        public int getBranchBase() {
            return this.branchBase;
        }
    }
}

