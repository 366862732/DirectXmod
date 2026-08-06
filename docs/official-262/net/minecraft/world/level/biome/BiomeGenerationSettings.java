/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FeatureTags;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(HolderSet.empty(), List.of());
    public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ConfiguredWorldCarver.LIST_CODEC.promotePartial(Util.prefix("Carver: ", arg_0 -> ((Logger)LOGGER).error(arg_0))).fieldOf("carvers").forGetter(b -> b.carvers), (App)PlacedFeature.LIST_OF_LISTS_CODEC.promotePartial(Util.prefix("Features: ", arg_0 -> ((Logger)LOGGER).error(arg_0))).fieldOf("features").forGetter(b -> b.features)).apply((Applicative)i, BiomeGenerationSettings::new));
    private final HolderSet<ConfiguredWorldCarver<?>> carvers;
    private final List<HolderSet<PlacedFeature>> features;
    private final Supplier<List<ConfiguredFeature<?, ?>>> boneMealFeatures;
    private final Supplier<Set<PlacedFeature>> featureSet;

    private BiomeGenerationSettings(HolderSet<ConfiguredWorldCarver<?>> carvers, List<HolderSet<PlacedFeature>> features) {
        this.carvers = carvers;
        this.features = features;
        this.boneMealFeatures = Suppliers.memoize(() -> (List)features.stream().flatMap(HolderSet::stream).flatMap(feature -> ((PlacedFeature)feature.value()).getFeatures()).filter(feature -> feature.is(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL)).map(Holder::value).collect(ImmutableList.toImmutableList()));
        this.featureSet = Suppliers.memoize(() -> features.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet()));
    }

    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers() {
        return this.carvers;
    }

    public List<ConfiguredFeature<?, ?>> getBoneMealFeatures() {
        return this.boneMealFeatures.get();
    }

    public List<HolderSet<PlacedFeature>> features() {
        return this.features;
    }

    public boolean hasFeature(PlacedFeature feature) {
        return this.featureSet.get().contains(feature);
    }

    public static class Builder
    extends PlainBuilder {
        private final HolderGetter<PlacedFeature> placedFeatures;
        private final HolderGetter<ConfiguredWorldCarver<?>> worldCarvers;

        public Builder(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
            this.placedFeatures = placedFeatures;
            this.worldCarvers = worldCarvers;
        }

        public Builder addFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
            this.addFeature(step.ordinal(), this.placedFeatures.getOrThrow(feature));
            return this;
        }

        public Builder addCarver(ResourceKey<ConfiguredWorldCarver<?>> carver) {
            this.addCarver(this.worldCarvers.getOrThrow(carver));
            return this;
        }
    }

    public static class PlainBuilder {
        private final List<Holder<ConfiguredWorldCarver<?>>> carvers = new ArrayList();
        private final List<List<Holder<PlacedFeature>>> features = new ArrayList<List<Holder<PlacedFeature>>>();

        public PlainBuilder addFeature(GenerationStep.Decoration step, Holder<PlacedFeature> feature) {
            return this.addFeature(step.ordinal(), feature);
        }

        public PlainBuilder addFeature(int index, Holder<PlacedFeature> feature) {
            this.addFeatureStepsUpTo(index);
            this.features.get(index).add(feature);
            return this;
        }

        public PlainBuilder addCarver(Holder<ConfiguredWorldCarver<?>> carver) {
            this.carvers.add(carver);
            return this;
        }

        private void addFeatureStepsUpTo(int index) {
            while (this.features.size() <= index) {
                this.features.add(Lists.newArrayList());
            }
        }

        public BiomeGenerationSettings build() {
            return new BiomeGenerationSettings(HolderSet.direct(this.carvers), (List)this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList()));
        }
    }
}

