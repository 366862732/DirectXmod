/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

public class MultiNoiseBiomeSourceParameterList {
    public static final Codec<MultiNoiseBiomeSourceParameterList> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)Preset.CODEC.fieldOf("preset").forGetter(e -> e.preset), RegistryOps.retrieveGetter(Registries.BIOME)).apply((Applicative)i, MultiNoiseBiomeSourceParameterList::new));
    public static final Codec<Holder<MultiNoiseBiomeSourceParameterList>> CODEC = RegistryFileCodec.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, DIRECT_CODEC);
    private final Preset preset;
    private final Climate.ParameterList<Holder<Biome>> parameters;

    public MultiNoiseBiomeSourceParameterList(Preset preset, HolderGetter<Biome> biomes) {
        this.preset = preset;
        this.parameters = preset.provider.apply(biomes::getOrThrow);
    }

    public Climate.ParameterList<Holder<Biome>> parameters() {
        return this.parameters;
    }

    public static Map<Preset, Climate.ParameterList<ResourceKey<Biome>>> knownPresets() {
        return Preset.BY_NAME.values().stream().collect(Collectors.toMap(e -> e, e -> e.provider().apply(k -> k)));
    }

    public record Preset(Identifier id, SourceProvider provider) {
        public static final Preset NETHER = new Preset(Identifier.withDefaultNamespace("nether"), new SourceProvider(){

            @Override
            public <T> Climate.ParameterList<T> apply(Function<ResourceKey<Biome>, T> lookup) {
                return new Climate.ParameterList(List.of(Pair.of((Object)Climate.parameters(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), lookup.apply(Biomes.NETHER_WASTES)), Pair.of((Object)Climate.parameters(0.0f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), lookup.apply(Biomes.SOUL_SAND_VALLEY)), Pair.of((Object)Climate.parameters(0.4f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), lookup.apply(Biomes.CRIMSON_FOREST)), Pair.of((Object)Climate.parameters(0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.375f), lookup.apply(Biomes.WARPED_FOREST)), Pair.of((Object)Climate.parameters(-0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.175f), lookup.apply(Biomes.BASALT_DELTAS))));
            }
        });
        public static final Preset OVERWORLD = new Preset(Identifier.withDefaultNamespace("overworld"), new SourceProvider(){

            @Override
            public <T> Climate.ParameterList<T> apply(Function<ResourceKey<Biome>, T> lookup) {
                return Preset.generateOverworldBiomes(lookup);
            }
        });
        private static final Map<Identifier, Preset> BY_NAME = Stream.of(NETHER, OVERWORLD).collect(Collectors.toMap(Preset::id, p -> p));
        public static final Codec<Preset> CODEC = Identifier.CODEC.flatXmap(name -> Optional.ofNullable(BY_NAME.get(name)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown preset: " + String.valueOf(name))), p -> DataResult.success((Object)p.id));

        private static <T> Climate.ParameterList<T> generateOverworldBiomes(Function<ResourceKey<Biome>, T> lookup) {
            ImmutableList.Builder builder = ImmutableList.builder();
            new OverworldBiomeBuilder().addBiomes(p -> builder.add((Object)p.mapSecond(lookup)));
            return new Climate.ParameterList(builder.build());
        }

        public Stream<ResourceKey<Biome>> usedBiomes() {
            return this.provider.apply(e -> e).values().stream().map(Pair::getSecond).distinct();
        }

        @FunctionalInterface
        private static interface SourceProvider {
            public <T> Climate.ParameterList<T> apply(Function<ResourceKey<Biome>, T> var1);
        }
    }
}

