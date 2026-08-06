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
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public class ForkingTrunkPlacer
extends TrunkPlacer {
    public static final MapCodec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> ForkingTrunkPlacer.trunkPlacerParts(i).apply((Applicative)i, ForkingTrunkPlacer::new));

    public ForkingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return TrunkPlacerType.FORKING_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, int treeHeight, BlockPos origin, TreeConfiguration config) {
        ForkingTrunkPlacer.placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), config);
        ArrayList attachments = Lists.newArrayList();
        Direction leanDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int leanHeight = treeHeight - random.nextInt(4) - 1;
        int leanSteps = 3 - random.nextInt(3);
        BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();
        int tx = origin.getX();
        int tz = origin.getZ();
        OptionalInt ey = OptionalInt.empty();
        for (int yo = 0; yo < treeHeight; ++yo) {
            int yy = origin.getY() + yo;
            if (yo >= leanHeight && leanSteps > 0) {
                tx += leanDirection.getStepX();
                tz += leanDirection.getStepZ();
                --leanSteps;
            }
            if (!this.placeLog(level, trunkSetter, random, logPos.set(tx, yy, tz), config)) continue;
            ey = OptionalInt.of(yy + 1);
        }
        if (ey.isPresent()) {
            attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(tx, ey.getAsInt(), tz), 1, false));
        }
        tx = origin.getX();
        tz = origin.getZ();
        Direction branchDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        if (branchDirection != leanDirection) {
            int branchPos = leanHeight - random.nextInt(2) - 1;
            int branchSteps = 1 + random.nextInt(3);
            ey = OptionalInt.empty();
            for (int yo = branchPos; yo < treeHeight && branchSteps > 0; ++yo, --branchSteps) {
                if (yo < 1) continue;
                int yy = origin.getY() + yo;
                if (!this.placeLog(level, trunkSetter, random, logPos.set(tx += branchDirection.getStepX(), yy, tz += branchDirection.getStepZ()), config)) continue;
                ey = OptionalInt.of(yy + 1);
            }
            if (ey.isPresent()) {
                attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(tx, ey.getAsInt(), tz), 0, false));
            }
        }
        return attachments;
    }
}

