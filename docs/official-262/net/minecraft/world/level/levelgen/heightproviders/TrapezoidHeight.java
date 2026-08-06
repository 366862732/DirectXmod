/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import org.slf4j.Logger;

public class TrapezoidHeight
extends HeightProvider {
    public static final MapCodec<TrapezoidHeight> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(u -> u.minInclusive), (App)VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(u -> u.maxInclusive), (App)Codec.INT.optionalFieldOf("plateau", (Object)0).forGetter(u -> u.plateau)).apply((Applicative)i, TrapezoidHeight::new));
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int plateau;

    private TrapezoidHeight(VerticalAnchor minInclusive, VerticalAnchor maxInclusive, int plateau) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
        this.plateau = plateau;
    }

    public static TrapezoidHeight of(VerticalAnchor minInclusive, VerticalAnchor maxInclusive, int plateau) {
        return new TrapezoidHeight(minInclusive, maxInclusive, plateau);
    }

    public static TrapezoidHeight of(VerticalAnchor minInclusive, VerticalAnchor maxInclusive) {
        return TrapezoidHeight.of(minInclusive, maxInclusive, 0);
    }

    @Override
    public int sample(RandomSource random, WorldGenerationContext context) {
        int max;
        int min = this.minInclusive.resolveY(context);
        if (min > (max = this.maxInclusive.resolveY(context))) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return min;
        }
        int range = max - min;
        if (this.plateau >= range) {
            return Mth.randomBetweenInclusive(random, min, max);
        }
        int plateauStart = (range - this.plateau) / 2;
        int plateauEnd = range - plateauStart;
        return min + Mth.randomBetweenInclusive(random, 0, plateauEnd) + Mth.randomBetweenInclusive(random, 0, plateauStart);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.TRAPEZOID;
    }

    public String toString() {
        if (this.plateau == 0) {
            return "triangle (" + String.valueOf(this.minInclusive) + "-" + String.valueOf(this.maxInclusive) + ")";
        }
        return "trapezoid(" + this.plateau + ") in [" + String.valueOf(this.minInclusive) + "-" + String.valueOf(this.maxInclusive) + "]";
    }
}

