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
package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;

public class ThreeLayersFeatureSize
extends FeatureSize {
    public static final MapCodec<ThreeLayersFeatureSize> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.intRange((int)0, (int)80).optionalFieldOf("limit", (Object)1).forGetter(s -> s.limit), (App)Codec.intRange((int)0, (int)80).optionalFieldOf("upper_limit", (Object)1).forGetter(s -> s.upperLimit), (App)Codec.intRange((int)0, (int)16).optionalFieldOf("lower_size", (Object)0).forGetter(s -> s.lowerSize), (App)Codec.intRange((int)0, (int)16).optionalFieldOf("middle_size", (Object)1).forGetter(s -> s.middleSize), (App)Codec.intRange((int)0, (int)16).optionalFieldOf("upper_size", (Object)1).forGetter(s -> s.upperSize), ThreeLayersFeatureSize.minClippedHeightCodec()).apply((Applicative)i, ThreeLayersFeatureSize::new));
    private final int limit;
    private final int upperLimit;
    private final int lowerSize;
    private final int middleSize;
    private final int upperSize;

    public ThreeLayersFeatureSize(int limit, int upperLimit, int lowerSize, int middleSize, int upperSize, OptionalInt minClippedHeight) {
        super(minClippedHeight);
        this.limit = limit;
        this.upperLimit = upperLimit;
        this.lowerSize = lowerSize;
        this.middleSize = middleSize;
        this.upperSize = upperSize;
    }

    @Override
    protected FeatureSizeType<?> type() {
        return FeatureSizeType.THREE_LAYERS_FEATURE_SIZE;
    }

    @Override
    public int getSizeAtHeight(int treeHeight, int yo) {
        if (yo < this.limit) {
            return this.lowerSize;
        }
        if (yo >= treeHeight - this.upperLimit) {
            return this.upperSize;
        }
        return this.middleSize;
    }
}

