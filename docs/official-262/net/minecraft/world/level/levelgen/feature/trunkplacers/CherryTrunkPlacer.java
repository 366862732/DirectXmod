/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class CherryTrunkPlacer
extends TrunkPlacer {
    private static final Codec<UniformInt> BRANCH_START_CODEC = UniformInt.MAP_CODEC.codec().validate(u -> {
        if (u.maxInclusive() - u.minInclusive() < 1) {
            return DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches");
        }
        return DataResult.success((Object)u);
    });
    public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> CherryTrunkPlacer.trunkPlacerParts(i).and(i.group((App)IntProviders.codec(1, 3).fieldOf("branch_count").forGetter(t -> t.branchCount), (App)IntProviders.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(t -> t.branchHorizontalLength), (App)IntProviders.validateCodec(-16, 0, BRANCH_START_CODEC).fieldOf("branch_start_offset_from_top").forGetter(t -> t.branchStartOffsetFromTop), (App)IntProviders.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(t -> t.branchEndOffsetFromTop))).apply((Applicative)i, CherryTrunkPlacer::new));
    private final IntProvider branchCount;
    private final IntProvider branchHorizontalLength;
    private final UniformInt branchStartOffsetFromTop;
    private final UniformInt secondBranchStartOffsetFromTop;
    private final IntProvider branchEndOffsetFromTop;

    public CherryTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider branchCount, IntProvider branchHorizontalLength, UniformInt branchStartOffsetFromTop, IntProvider branchEndOffsetFromTop) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
        this.branchHorizontalLength = branchHorizontalLength;
        this.branchStartOffsetFromTop = branchStartOffsetFromTop;
        this.secondBranchStartOffsetFromTop = UniformInt.of(branchStartOffsetFromTop.minInclusive(), branchStartOffsetFromTop.maxInclusive() - 1);
        this.branchEndOffsetFromTop = branchEndOffsetFromTop;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.CHERRY_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        boolean hasBothSideBranches;
        int branchCount;
        CherryTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), config);
        int firstBranchOffsetFromOrigin = Math.max(0, treeHeight - 1 + this.branchStartOffsetFromTop.sample(random));
        int secondBranchOffsetFromOrigin = Math.max(0, treeHeight - 1 + this.secondBranchStartOffsetFromTop.sample(random));
        if (secondBranchOffsetFromOrigin >= firstBranchOffsetFromOrigin) {
            ++secondBranchOffsetFromOrigin;
        }
        boolean hasMiddleBranch = (branchCount = this.branchCount.sample(random)) == 3;
        boolean bl = hasBothSideBranches = branchCount >= 2;
        int trunkHeight = hasMiddleBranch ? treeHeight : (hasBothSideBranches ? Math.max(firstBranchOffsetFromOrigin, secondBranchOffsetFromOrigin) + 1 : firstBranchOffsetFromOrigin + 1);
        for (int y = 0; y < trunkHeight; ++y) {
            this.placeLog(level, trunkSetter, random, origin.above(y), config);
        }
        ArrayList<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<FoliagePlacer.FoliageAttachment>();
        if (hasMiddleBranch) {
            attachments.add(new FoliagePlacer.FoliageAttachment(origin.above(trunkHeight), 0, false));
        }
        BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();
        Direction treeDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Function<BlockState, BlockState> sidewaysStateModifier = state -> (BlockState)state.trySetValue(RotatedPillarBlock.AXIS, treeDirection.getAxis());
        attachments.add(this.generateBranch(level, trunkSetter, random, treeHeight, origin, config, sidewaysStateModifier, treeDirection, firstBranchOffsetFromOrigin, firstBranchOffsetFromOrigin < trunkHeight - 1, logPos));
        if (hasBothSideBranches) {
            attachments.add(this.generateBranch(level, trunkSetter, random, treeHeight, origin, config, sidewaysStateModifier, treeDirection.getOpposite(), secondBranchOffsetFromOrigin, secondBranchOffsetFromOrigin < trunkHeight - 1, logPos));
        }
        return attachments;
    }

    private FoliagePlacer.FoliageAttachment generateBranch(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config, Function<BlockState, BlockState> sidewaysStateModifier, Direction branchDirection, int offsetFromOrigin, boolean middleContinuesUpwards, BlockPos.MutableBlockPos logPos) {
        int distance;
        Direction verticalDirection;
        logPos.set(origin).move(Direction.UP, offsetFromOrigin);
        int branchEndPosOffsetFromOrigin = treeHeight - 1 + this.branchEndOffsetFromTop.sample(random);
        boolean extendBranchAwayFromTrunk = middleContinuesUpwards || branchEndPosOffsetFromOrigin < offsetFromOrigin;
        int distanceToTrunk = this.branchHorizontalLength.sample(random) + (extendBranchAwayFromTrunk ? 1 : 0);
        BlockPos branchEndPos = origin.relative(branchDirection, distanceToTrunk).above(branchEndPosOffsetFromOrigin);
        int stepsHorizontally = extendBranchAwayFromTrunk ? 2 : 1;
        for (int i = 0; i < stepsHorizontally; ++i) {
            this.placeLog(level, trunkSetter, random, logPos.move(branchDirection), config, sidewaysStateModifier);
        }
        Direction direction = verticalDirection = branchEndPos.getY() > logPos.getY() ? Direction.UP : Direction.DOWN;
        while ((distance = logPos.distManhattan(branchEndPos)) != 0) {
            float chanceToGrowVertically = (float)Math.abs(branchEndPos.getY() - logPos.getY()) / (float)distance;
            boolean growVertically = random.nextFloat() < chanceToGrowVertically;
            logPos.move(growVertically ? verticalDirection : branchDirection);
            this.placeLog(level, trunkSetter, random, logPos, config, growVertically ? Function.identity() : sidewaysStateModifier);
        }
        return new FoliagePlacer.FoliageAttachment(branchEndPos.above(), 0, false);
    }
}

