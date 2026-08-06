/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.frog;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public interface FrogVariants {
    public static final ResourceKey<FrogVariant> TEMPERATE = FrogVariants.createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<FrogVariant> WARM = FrogVariants.createKey(TemperatureVariants.WARM);
    public static final ResourceKey<FrogVariant> COLD = FrogVariants.createKey(TemperatureVariants.COLD);

    private static ResourceKey<FrogVariant> createKey(Identifier id) {
        return ResourceKey.create(Registries.FROG_VARIANT, id);
    }

    public static void bootstrap(BootstrapContext<FrogVariant> registry) {
        FrogVariants.register(registry, TEMPERATE, "entity/frog/frog_temperate", SpawnPrioritySelectors.fallback(0));
        FrogVariants.register(registry, WARM, "entity/frog/frog_warm", BiomeTags.SPAWNS_WARM_VARIANT_FROGS);
        FrogVariants.register(registry, COLD, "entity/frog/frog_cold", BiomeTags.SPAWNS_COLD_VARIANT_FROGS);
    }

    private static void register(BootstrapContext<FrogVariant> context, ResourceKey<FrogVariant> name, String assetId, TagKey<Biome> limitToBiome) {
        HolderSet.Named<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(limitToBiome);
        FrogVariants.register(context, name, assetId, SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1));
    }

    private static void register(BootstrapContext<FrogVariant> context, ResourceKey<FrogVariant> name, String assetId, SpawnPrioritySelectors selectors) {
        context.register(name, new FrogVariant(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(assetId)), selectors));
    }
}

