/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.placement;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class EnvironmentScanPlacement
extends PlacementModifier {
    private final Direction directionOfSearch;
    private final BlockPredicate targetCondition;
    private final BlockPredicate allowedSearchCondition;
    private final int maxSteps;
    public static final MapCodec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(c -> c.directionOfSearch), (App)BlockPredicate.CODEC.fieldOf("target_condition").forGetter(c -> c.targetCondition), (App)BlockPredicate.CODEC.optionalFieldOf("allowed_search_condition", (Object)BlockPredicate.alwaysTrue()).forGetter(c -> c.allowedSearchCondition), (App)Codec.intRange((int)1, (int)32).fieldOf("max_steps").forGetter(c -> c.maxSteps)).apply((Applicative)i, EnvironmentScanPlacement::new));

    private EnvironmentScanPlacement(Direction directionOfSearch, BlockPredicate targetCondition, BlockPredicate allowedSearchCondition, int maxSteps) {
        this.directionOfSearch = directionOfSearch;
        this.targetCondition = targetCondition;
        this.allowedSearchCondition = allowedSearchCondition;
        this.maxSteps = maxSteps;
    }

    public static EnvironmentScanPlacement scanningFor(Direction directionOfSearch, BlockPredicate targetCondition, BlockPredicate allowedSearchCondition, int maxSteps) {
        return new EnvironmentScanPlacement(directionOfSearch, targetCondition, allowedSearchCondition, maxSteps);
    }

    public static EnvironmentScanPlacement scanningFor(Direction directionOfSearch, BlockPredicate targetCondition, int maxSteps) {
        return EnvironmentScanPlacement.scanningFor(directionOfSearch, targetCondition, BlockPredicate.alwaysTrue(), maxSteps);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        WorldGenLevel level = context.getLevel();
        if (!this.allowedSearchCondition.test(level, pos)) {
            return Stream.of(new BlockPos[0]);
        }
        for (int i = 0; i < this.maxSteps; ++i) {
            if (this.targetCondition.test(level, pos)) {
                return Stream.of(pos);
            }
            pos.move(this.directionOfSearch);
            if (level.isOutsideBuildHeight(pos.getY())) {
                return Stream.of(new BlockPos[0]);
            }
            if (!this.allowedSearchCondition.test(level, pos)) break;
        }
        if (this.targetCondition.test(level, pos)) {
            return Stream.of(pos);
        }
        return Stream.of(new BlockPos[0]);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.ENVIRONMENT_SCAN;
    }
}

