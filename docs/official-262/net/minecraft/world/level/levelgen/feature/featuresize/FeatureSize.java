/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;

public abstract class FeatureSize {
    public static final Codec<FeatureSize> CODEC = BuiltInRegistries.FEATURE_SIZE_TYPE.byNameCodec().dispatch(FeatureSize::type, FeatureSizeType::codec);
    protected static final int MAX_WIDTH = 16;
    protected final OptionalInt minClippedHeight;

    protected static <S extends FeatureSize> RecordCodecBuilder<S, OptionalInt> minClippedHeightCodec() {
        return Codec.intRange((int)0, (int)80).optionalFieldOf("min_clipped_height").xmap(o -> o.map(OptionalInt::of).orElse(OptionalInt.empty()), o -> o.isPresent() ? Optional.of(o.getAsInt()) : Optional.empty()).forGetter(f -> f.minClippedHeight);
    }

    public FeatureSize(OptionalInt minClippedHeight) {
        this.minClippedHeight = minClippedHeight;
    }

    protected abstract FeatureSizeType<?> type();

    public abstract int getSizeAtHeight(int var1, int var2);

    public OptionalInt minClippedHeight() {
        return this.minClippedHeight;
    }
}

