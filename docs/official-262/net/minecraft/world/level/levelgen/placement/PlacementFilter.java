/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen.placement;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public abstract class PlacementFilter
extends PlacementModifier {
    @Override
    public final Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        if (this.shouldPlace(context, random, origin)) {
            return Stream.of(origin);
        }
        return Stream.of(new BlockPos[0]);
    }

    protected abstract boolean shouldPlace(PlacementContext var1, RandomSource var2, BlockPos var3);
}

