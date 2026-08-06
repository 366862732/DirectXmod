/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.placement.RepeatingPlacement;

public class CountPlacement
extends RepeatingPlacement {
    public static final MapCodec<CountPlacement> CODEC = IntProviders.codec(0, 4096).fieldOf("count").xmap(CountPlacement::new, c -> c.count);
    private final IntProvider count;

    private CountPlacement(IntProvider count) {
        this.count = count;
    }

    public static CountPlacement of(IntProvider count) {
        return new CountPlacement(count);
    }

    public static CountPlacement of(int count) {
        return CountPlacement.of(ConstantInt.of(count));
    }

    @Override
    protected int count(RandomSource random, BlockPos origin) {
        return this.count.sample(random);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.COUNT;
    }
}

