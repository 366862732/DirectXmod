/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class VillagerType {
    public static final ResourceKey<VillagerType> DESERT = VillagerType.createKey("desert");
    public static final ResourceKey<VillagerType> JUNGLE = VillagerType.createKey("jungle");
    public static final ResourceKey<VillagerType> PLAINS = VillagerType.createKey("plains");
    public static final ResourceKey<VillagerType> SAVANNA = VillagerType.createKey("savanna");
    public static final ResourceKey<VillagerType> SNOW = VillagerType.createKey("snow");
    public static final ResourceKey<VillagerType> SWAMP = VillagerType.createKey("swamp");
    public static final ResourceKey<VillagerType> TAIGA = VillagerType.createKey("taiga");
    public static final Codec<Holder<VillagerType>> CODEC = RegistryFixedCodec.create(Registries.VILLAGER_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<VillagerType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE);
    private static final Map<ResourceKey<Biome>, ResourceKey<VillagerType>> BY_BIOME = Util.make(Maps.newHashMap(), map -> {
        map.put(Biomes.BADLANDS, DESERT);
        map.put(Biomes.DESERT, DESERT);
        map.put(Biomes.ERODED_BADLANDS, DESERT);
        map.put(Biomes.WOODED_BADLANDS, DESERT);
        map.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
        map.put(Biomes.JUNGLE, JUNGLE);
        map.put(Biomes.SPARSE_JUNGLE, JUNGLE);
        map.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
        map.put(Biomes.SAVANNA, SAVANNA);
        map.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
        map.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_RIVER, SNOW);
        map.put(Biomes.ICE_SPIKES, SNOW);
        map.put(Biomes.SNOWY_BEACH, SNOW);
        map.put(Biomes.SNOWY_TAIGA, SNOW);
        map.put(Biomes.SNOWY_PLAINS, SNOW);
        map.put(Biomes.GROVE, SNOW);
        map.put(Biomes.SNOWY_SLOPES, SNOW);
        map.put(Biomes.FROZEN_PEAKS, SNOW);
        map.put(Biomes.JAGGED_PEAKS, SNOW);
        map.put(Biomes.SWAMP, SWAMP);
        map.put(Biomes.MANGROVE_SWAMP, SWAMP);
        map.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
        map.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
        map.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
        map.put(Biomes.WINDSWEPT_HILLS, TAIGA);
        map.put(Biomes.TAIGA, TAIGA);
        map.put(Biomes.WINDSWEPT_FOREST, TAIGA);
    });

    private static ResourceKey<VillagerType> createKey(String name) {
        return ResourceKey.create(Registries.VILLAGER_TYPE, Identifier.withDefaultNamespace(name));
    }

    private static VillagerType register(Registry<VillagerType> registry, ResourceKey<VillagerType> name) {
        return Registry.register(registry, name, new VillagerType());
    }

    public static VillagerType bootstrap(Registry<VillagerType> registry) {
        VillagerType.register(registry, DESERT);
        VillagerType.register(registry, JUNGLE);
        VillagerType.register(registry, PLAINS);
        VillagerType.register(registry, SAVANNA);
        VillagerType.register(registry, SNOW);
        VillagerType.register(registry, SWAMP);
        return VillagerType.register(registry, TAIGA);
    }

    public static ResourceKey<VillagerType> byBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(BY_BIOME::get).orElse(VillagerData.DEFAULT_TYPE);
    }
}

