/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.placement;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.TrapezoidInt;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class RandomOffsetPlacement
extends PlacementModifier {
    public static final MapCodec<RandomOffsetPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)IntProviders.codec(-16, 16).fieldOf("xz_spread").forGetter(c -> c.xzSpread), (App)IntProviders.codec(-16, 16).fieldOf("y_spread").forGetter(c -> c.ySpread)).apply((Applicative)i, RandomOffsetPlacement::new));
    private final IntProvider xzSpread;
    private final IntProvider ySpread;

    public static RandomOffsetPlacement of(IntProvider xzSpread, IntProvider ySpread) {
        return new RandomOffsetPlacement(xzSpread, ySpread);
    }

    public static RandomOffsetPlacement ofTriangle(int xzRange, int yRange) {
        return new RandomOffsetPlacement(TrapezoidInt.triangle(xzRange), TrapezoidInt.triangle(yRange));
    }

    public static RandomOffsetPlacement vertical(IntProvider ySpread) {
        return new RandomOffsetPlacement(ConstantInt.of(0), ySpread);
    }

    public static RandomOffsetPlacement horizontal(IntProvider xzSpread) {
        return new RandomOffsetPlacement(xzSpread, ConstantInt.of(0));
    }

    private RandomOffsetPlacement(IntProvider xzSpread, IntProvider ySpread) {
        this.xzSpread = xzSpread;
        this.ySpread = ySpread;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        int scatterX = origin.getX() + this.xzSpread.sample(random);
        int scatterY = origin.getY() + this.ySpread.sample(random);
        int scatterZ = origin.getZ() + this.xzSpread.sample(random);
        return Stream.of(new BlockPos(scatterX, scatterY, scatterZ));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.RANDOM_OFFSET;
    }
}

