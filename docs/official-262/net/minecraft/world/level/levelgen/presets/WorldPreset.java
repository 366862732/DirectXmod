/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.presets;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public class WorldPreset {
    public static final Codec<WorldPreset> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(e -> e.dimensions)).apply((Applicative)i, WorldPreset::new)).validate(WorldPreset::requireOverworld);
    public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registries.WORLD_PRESET, DIRECT_CODEC);
    private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    public WorldPreset(Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
        this.dimensions = dimensions;
    }

    private ImmutableMap<ResourceKey<LevelStem>, LevelStem> dimensionsInOrder() {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        WorldDimensions.keysInOrder(this.dimensions.keySet()).forEach(key -> {
            LevelStem levelStem = this.dimensions.get(key);
            if (levelStem != null) {
                builder.put(key, (Object)levelStem);
            }
        });
        return builder.build();
    }

    public WorldDimensions createWorldDimensions() {
        return new WorldDimensions((Map<ResourceKey<LevelStem>, LevelStem>)this.dimensionsInOrder());
    }

    public Optional<LevelStem> overworld() {
        return Optional.ofNullable(this.dimensions.get(LevelStem.OVERWORLD));
    }

    private static DataResult<WorldPreset> requireOverworld(WorldPreset preset) {
        if (preset.overworld().isEmpty()) {
            return DataResult.error(() -> "Missing overworld dimension");
        }
        return DataResult.success((Object)preset, (Lifecycle)Lifecycle.stable());
    }
}

