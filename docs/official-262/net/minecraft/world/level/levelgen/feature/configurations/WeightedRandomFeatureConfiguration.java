/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record WeightedRandomFeatureConfiguration(WeightedList<Holder<PlacedFeature>> features) implements FeatureConfiguration
{
    public static final Codec<WeightedRandomFeatureConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)WeightedList.codec(PlacedFeature.CODEC).fieldOf("features").forGetter(WeightedRandomFeatureConfiguration::features)).apply((Applicative)i, WeightedRandomFeatureConfiguration::new));

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return this.features.unwrap().stream().flatMap(weighted -> ((PlacedFeature)((Holder)weighted.value()).value()).getFeatures());
    }
}

