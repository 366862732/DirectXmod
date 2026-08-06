/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.feline;

import java.util.List;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.variant.MoonBrightnessCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.entity.variant.StructureCheck;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface CatVariants {
    public static final ResourceKey<CatVariant> TABBY = CatVariants.createKey("tabby");
    public static final ResourceKey<CatVariant> BLACK = CatVariants.createKey("black");
    public static final ResourceKey<CatVariant> RED = CatVariants.createKey("red");
    public static final ResourceKey<CatVariant> SIAMESE = CatVariants.createKey("siamese");
    public static final ResourceKey<CatVariant> BRITISH_SHORTHAIR = CatVariants.createKey("british_shorthair");
    public static final ResourceKey<CatVariant> CALICO = CatVariants.createKey("calico");
    public static final ResourceKey<CatVariant> PERSIAN = CatVariants.createKey("persian");
    public static final ResourceKey<CatVariant> RAGDOLL = CatVariants.createKey("ragdoll");
    public static final ResourceKey<CatVariant> WHITE = CatVariants.createKey("white");
    public static final ResourceKey<CatVariant> JELLIE = CatVariants.createKey("jellie");
    public static final ResourceKey<CatVariant> ALL_BLACK = CatVariants.createKey("all_black");

    private static ResourceKey<CatVariant> createKey(String name) {
        return ResourceKey.create(Registries.CAT_VARIANT, Identifier.withDefaultNamespace(name));
    }

    public static void bootstrap(BootstrapContext<CatVariant> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        CatVariants.registerForAnyConditions(context, TABBY, "entity/cat/cat_tabby", "entity/cat/cat_tabby_baby");
        CatVariants.registerForAnyConditions(context, BLACK, "entity/cat/cat_black", "entity/cat/cat_black_baby");
        CatVariants.registerForAnyConditions(context, RED, "entity/cat/cat_red", "entity/cat/cat_red_baby");
        CatVariants.registerForAnyConditions(context, SIAMESE, "entity/cat/cat_siamese", "entity/cat/cat_siamese_baby");
        CatVariants.registerForAnyConditions(context, BRITISH_SHORTHAIR, "entity/cat/cat_british_shorthair", "entity/cat/cat_british_shorthair_baby");
        CatVariants.registerForAnyConditions(context, CALICO, "entity/cat/cat_calico", "entity/cat/cat_calico_baby");
        CatVariants.registerForAnyConditions(context, PERSIAN, "entity/cat/cat_persian", "entity/cat/cat_persian_baby");
        CatVariants.registerForAnyConditions(context, RAGDOLL, "entity/cat/cat_ragdoll", "entity/cat/cat_ragdoll_baby");
        CatVariants.registerForAnyConditions(context, WHITE, "entity/cat/cat_white", "entity/cat/cat_white_baby");
        CatVariants.registerForAnyConditions(context, JELLIE, "entity/cat/cat_jellie", "entity/cat/cat_jellie_baby");
        CatVariants.register(context, ALL_BLACK, "entity/cat/cat_all_black", "entity/cat/cat_all_black_baby", new SpawnPrioritySelectors(List.of(new PriorityProvider.Selector(new StructureCheck(structures.getOrThrow(StructureTags.CATS_SPAWN_AS_BLACK)), 1), new PriorityProvider.Selector(new MoonBrightnessCheck(MinMaxBounds.Doubles.atLeast(0.9)), 0))));
    }

    private static void registerForAnyConditions(BootstrapContext<CatVariant> context, ResourceKey<CatVariant> name, String adultTexture, String babyTexture) {
        CatVariants.register(context, name, adultTexture, babyTexture, SpawnPrioritySelectors.fallback(0));
    }

    private static void register(BootstrapContext<CatVariant> context, ResourceKey<CatVariant> name, String adultTexture, String babyTexture, SpawnPrioritySelectors spawnConditions) {
        context.register(name, new CatVariant(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(adultTexture)), new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(babyTexture)), spawnConditions));
    }
}

