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

public class VeryBiasedToBottomHeight
extends HeightProvider {
    public static final MapCodec<VeryBiasedToBottomHeight> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(u -> u.minInclusive), (App)VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(u -> u.maxInclusive), (App)Codec.intRange((int)1, (int)Integer.MAX_VALUE).optionalFieldOf("inner", (Object)1).forGetter(u -> u.inner)).apply((Applicative)i, VeryBiasedToBottomHeight::new));
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int inner;

    private VeryBiasedToBottomHeight(VerticalAnchor minInclusive, VerticalAnchor maxInclusive, int inner) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
        this.inner = inner;
    }

    public static VeryBiasedToBottomHeight of(VerticalAnchor minInclusive, VerticalAnchor maxInclusive, int offset) {
        return new VeryBiasedToBottomHeight(minInclusive, maxInclusive, offset);
    }

    @Override
    public int sample(RandomSource random, WorldGenerationContext context) {
        int min = this.minInclusive.resolveY(context);
        int max = this.maxInclusive.resolveY(context);
        if (max - min - this.inner + 1 <= 0) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return min;
        }
        int upperInclusive = Mth.nextInt(random, min + this.inner, max);
        int biasedUpperInclusive = Mth.nextInt(random, min, upperInclusive - 1);
        return Mth.nextInt(random, min, biasedUpperInclusive - 1 + this.inner);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.VERY_BIASED_TO_BOTTOM;
    }

    public String toString() {
        return "biased[" + String.valueOf(this.minInclusive) + "-" + String.valueOf(this.maxInclusive) + " inner: " + this.inner + "]";
    }
}

