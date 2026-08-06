/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class UpwardsBranchingTrunkPlacer
extends TrunkPlacer {
    public static final MapCodec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> UpwardsBranchingTrunkPlacer.trunkPlacerParts(i).and(i.group((App)IntProviders.POSITIVE_CODEC.fieldOf("extra_branch_steps").forGetter(p -> p.extraBranchSteps), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("place_branch_per_log_probability").forGetter(p -> Float.valueOf(p.placeBranchPerLogProbability)), (App)IntProviders.NON_NEGATIVE_CODEC.fieldOf("extra_branch_length").forGetter(c -> c.extraBranchLength), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_grow_through").forGetter(t -> t.canGrowThrough))).apply((Applicative)i, UpwardsBranchingTrunkPlacer::new));
    private final IntProvider extraBranchSteps;
    private final float placeBranchPerLogProbability;
    private final IntProvider extraBranchLength;
    private final HolderSet<Block> canGrowThrough;

    public UpwardsBranchingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider extraBranchSteps, float placeBranchPerLogProbability, IntProvider extraBranchLength, HolderSet<Block> canGrowThrough) {
        super(baseHeight, heightRandA, heightRandB);
        this.extraBranchSteps = extraBranchSteps;
        this.placeBranchPerLogProbability = placeBranchPerLogProbability;
        this.extraBranchLength = extraBranchLength;
        this.canGrowThrough = canGrowThrough;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        ArrayList attachments = Lists.newArrayList();
        BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();
        for (int heightPos = 0; heightPos < treeHeight; ++heightPos) {
            int currentHeight = origin.getY() + heightPos;
            if (this.placeLog(level, trunkSetter, random, logPos.set(origin.getX(), currentHeight, origin.getZ()), config) && heightPos < treeHeight - 1 && random.nextFloat() < this.placeBranchPerLogProbability) {
                Direction branchDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                int branchLen = this.extraBranchLength.sample(random);
                int branchPos = Math.max(0, branchLen - this.extraBranchLength.sample(random) - 1);
                int branchSteps = this.extraBranchSteps.sample(random);
                this.placeBranch(level, trunkSetter, random, treeHeight, config, attachments, logPos, currentHeight, branchDir, branchPos, branchSteps);
            }
            if (heightPos != treeHeight - 1) continue;
            attachments.add(new FoliagePlacer.FoliageAttachment(logPos.set(origin.getX(), currentHeight + 1, origin.getZ()), 0, false));
        }
        return attachments;
    }

    private void placeBranch(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, TreeConfiguration config, List<FoliagePlacer.FoliageAttachment> attachments, BlockPos.MutableBlockPos logPos, int currentHeight, Direction branchDir, int branchPos, int branchSteps) {
        int heightAlongBranch = currentHeight + branchPos;
        int logX = logPos.getX();
        int logZ = logPos.getZ();
        for (int branchPlacementIndex = branchPos; branchPlacementIndex < treeHeight && branchSteps > 0; ++branchPlacementIndex, --branchSteps) {
            if (branchPlacementIndex < 1) continue;
            int placementHeight = currentHeight + branchPlacementIndex;
            heightAlongBranch = placementHeight;
            if (this.placeLog(level, trunkSetter, random, logPos.set(logX += branchDir.getStepX(), placementHeight, logZ += branchDir.getStepZ()), config)) {
                ++heightAlongBranch;
            }
            attachments.add(new FoliagePlacer.FoliageAttachment(logPos.immutable(), 0, false));
        }
        if (heightAlongBranch - currentHeight > 1) {
            BlockPos foliagePos = new BlockPos(logX, heightAlongBranch, logZ);
            attachments.add(new FoliagePlacer.FoliageAttachment(foliagePos, 0, false));
            attachments.add(new FoliagePlacer.FoliageAttachment(foliagePos.below(2), 0, false));
        }
    }

    @Override
    protected boolean validTreePos(WorldGenLevel level, BlockPos pos) {
        return super.validTreePos(level, pos) || level.isStateAtPosition(pos, s -> s.is(this.canGrowThrough));
    }
}

