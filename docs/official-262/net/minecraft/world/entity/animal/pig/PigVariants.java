/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class PigVariants {
    public static final ResourceKey<PigVariant> TEMPERATE = PigVariants.createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<PigVariant> WARM = PigVariants.createKey(TemperatureVariants.WARM);
    public static final ResourceKey<PigVariant> COLD = PigVariants.createKey(TemperatureVariants.COLD);
    public static final ResourceKey<PigVariant> DEFAULT = TEMPERATE;

    private static ResourceKey<PigVariant> createKey(Identifier id) {
        return ResourceKey.create(Registries.PIG_VARIANT, id);
    }

    public static void bootstrap(BootstrapContext<PigVariant> context) {
        PigVariants.register(context, TEMPERATE, PigVariant.ModelType.NORMAL, "pig_temperate", "pig_temperate_baby", SpawnPrioritySelectors.fallback(0));
        PigVariants.register(context, WARM, PigVariant.ModelType.NORMAL, "pig_warm", "pig_warm_baby", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        PigVariants.register(context, COLD, PigVariant.ModelType.COLD, "pig_cold", "pig_cold_baby", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
    }

    private static void register(BootstrapContext<PigVariant> context, ResourceKey<PigVariant> name, PigVariant.ModelType modelType, String textureName, String babyTextureName, TagKey<Biome> spawnBiome) {
        HolderSet.Named<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(spawnBiome);
        PigVariants.register(context, name, modelType, textureName, babyTextureName, SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1));
    }

    private static void register(BootstrapContext<PigVariant> context, ResourceKey<PigVariant> name, PigVariant.ModelType modelType, String textureName, String babyTextureName, SpawnPrioritySelectors selectors) {
        Identifier textureId = Identifier.withDefaultNamespace("entity/pig/" + textureName);
        Identifier babyTextureId = Identifier.withDefaultNamespace("entity/pig/" + babyTextureName);
        context.register(name, new PigVariant(new ModelAndTexture<PigVariant.ModelType>(modelType, textureId), new ClientAsset.ResourceTexture(babyTextureId), selectors));
    }
}

