/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class CowVariants {
    public static final ResourceKey<CowVariant> TEMPERATE = CowVariants.createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<CowVariant> WARM = CowVariants.createKey(TemperatureVariants.WARM);
    public static final ResourceKey<CowVariant> COLD = CowVariants.createKey(TemperatureVariants.COLD);
    public static final ResourceKey<CowVariant> DEFAULT = TEMPERATE;

    private static ResourceKey<CowVariant> createKey(Identifier id) {
        return ResourceKey.create(Registries.COW_VARIANT, id);
    }

    public static void bootstrap(BootstrapContext<CowVariant> context) {
        CowVariants.register(context, TEMPERATE, CowVariant.ModelType.NORMAL, "cow_temperate", "cow_temperate_baby", SpawnPrioritySelectors.fallback(0));
        CowVariants.register(context, WARM, CowVariant.ModelType.WARM, "cow_warm", "cow_warm_baby", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        CowVariants.register(context, COLD, CowVariant.ModelType.COLD, "cow_cold", "cow_cold_baby", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
    }

    private static void register(BootstrapContext<CowVariant> context, ResourceKey<CowVariant> name, CowVariant.ModelType modelType, String textureName, String babyTextureName, TagKey<Biome> spawnBiome) {
        HolderSet.Named<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(spawnBiome);
        CowVariants.register(context, name, modelType, textureName, babyTextureName, SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1));
    }

    private static void register(BootstrapContext<CowVariant> context, ResourceKey<CowVariant> name, CowVariant.ModelType modelType, String textureName, String babyTextureName, SpawnPrioritySelectors selectors) {
        Identifier textureId = Identifier.withDefaultNamespace("entity/cow/" + textureName);
        Identifier babyTextureId = Identifier.withDefaultNamespace("entity/cow/" + babyTextureName);
        context.register(name, new CowVariant(new ModelAndTexture<CowVariant.ModelType>(modelType, textureId), new ClientAsset.ResourceTexture(babyTextureId), selectors));
    }
}

