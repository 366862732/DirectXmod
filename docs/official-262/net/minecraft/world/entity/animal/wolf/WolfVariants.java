/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
    public static final ResourceKey<WolfVariant> PALE = WolfVariants.createKey("pale");
    public static final ResourceKey<WolfVariant> SPOTTED = WolfVariants.createKey("spotted");
    public static final ResourceKey<WolfVariant> SNOWY = WolfVariants.createKey("snowy");
    public static final ResourceKey<WolfVariant> BLACK = WolfVariants.createKey("black");
    public static final ResourceKey<WolfVariant> ASHEN = WolfVariants.createKey("ashen");
    public static final ResourceKey<WolfVariant> RUSTY = WolfVariants.createKey("rusty");
    public static final ResourceKey<WolfVariant> WOODS = WolfVariants.createKey("woods");
    public static final ResourceKey<WolfVariant> CHESTNUT = WolfVariants.createKey("chestnut");
    public static final ResourceKey<WolfVariant> STRIPED = WolfVariants.createKey("striped");
    public static final ResourceKey<WolfVariant> DEFAULT = PALE;

    private static ResourceKey<WolfVariant> createKey(String name) {
        return ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace(name));
    }

    private static void register(BootstrapContext<WolfVariant> context, ResourceKey<WolfVariant> name, String fileName, ResourceKey<Biome> spawnBiome) {
        WolfVariants.register(context, name, fileName, WolfVariants.highPrioBiome(HolderSet.direct(context.lookup(Registries.BIOME).getOrThrow(spawnBiome))));
    }

    private static void register(BootstrapContext<WolfVariant> context, ResourceKey<WolfVariant> name, String fileName, TagKey<Biome> spawnBiome) {
        WolfVariants.register(context, name, fileName, WolfVariants.highPrioBiome(context.lookup(Registries.BIOME).getOrThrow(spawnBiome)));
    }

    private static SpawnPrioritySelectors highPrioBiome(HolderSet<Biome> biomes) {
        return SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1);
    }

    private static void register(BootstrapContext<WolfVariant> context, ResourceKey<WolfVariant> name, String fileName, SpawnPrioritySelectors selectors) {
        Identifier wildTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName);
        Identifier tameTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_tame");
        Identifier angryTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_angry");
        Identifier babyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_baby");
        Identifier tameBabyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_tame_baby");
        Identifier angryBabyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_angry_baby");
        context.register(name, new WolfVariant(new WolfVariant.AssetInfo(new ClientAsset.ResourceTexture(wildTexture), new ClientAsset.ResourceTexture(tameTexture), new ClientAsset.ResourceTexture(angryTexture)), new WolfVariant.AssetInfo(new ClientAsset.ResourceTexture(babyTexture), new ClientAsset.ResourceTexture(tameBabyTexture), new ClientAsset.ResourceTexture(angryBabyTexture)), selectors));
    }

    public static void bootstrap(BootstrapContext<WolfVariant> context) {
        WolfVariants.register(context, PALE, "wolf", SpawnPrioritySelectors.fallback(0));
        WolfVariants.register(context, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
        WolfVariants.register(context, SNOWY, "wolf_snowy", Biomes.GROVE);
        WolfVariants.register(context, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
        WolfVariants.register(context, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
        WolfVariants.register(context, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
        WolfVariants.register(context, WOODS, "wolf_woods", Biomes.FOREST);
        WolfVariants.register(context, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        WolfVariants.register(context, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
    }
}

