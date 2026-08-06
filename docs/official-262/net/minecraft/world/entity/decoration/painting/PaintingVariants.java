/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.decoration.painting;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

public class PaintingVariants {
    public static final ResourceKey<PaintingVariant> KEBAB = PaintingVariants.create("kebab");
    public static final ResourceKey<PaintingVariant> AZTEC = PaintingVariants.create("aztec");
    public static final ResourceKey<PaintingVariant> ALBAN = PaintingVariants.create("alban");
    public static final ResourceKey<PaintingVariant> AZTEC2 = PaintingVariants.create("aztec2");
    public static final ResourceKey<PaintingVariant> BOMB = PaintingVariants.create("bomb");
    public static final ResourceKey<PaintingVariant> PLANT = PaintingVariants.create("plant");
    public static final ResourceKey<PaintingVariant> WASTELAND = PaintingVariants.create("wasteland");
    public static final ResourceKey<PaintingVariant> POOL = PaintingVariants.create("pool");
    public static final ResourceKey<PaintingVariant> COURBET = PaintingVariants.create("courbet");
    public static final ResourceKey<PaintingVariant> SEA = PaintingVariants.create("sea");
    public static final ResourceKey<PaintingVariant> SUNSET = PaintingVariants.create("sunset");
    public static final ResourceKey<PaintingVariant> CREEBET = PaintingVariants.create("creebet");
    public static final ResourceKey<PaintingVariant> WANDERER = PaintingVariants.create("wanderer");
    public static final ResourceKey<PaintingVariant> GRAHAM = PaintingVariants.create("graham");
    public static final ResourceKey<PaintingVariant> MATCH = PaintingVariants.create("match");
    public static final ResourceKey<PaintingVariant> BUST = PaintingVariants.create("bust");
    public static final ResourceKey<PaintingVariant> STAGE = PaintingVariants.create("stage");
    public static final ResourceKey<PaintingVariant> VOID = PaintingVariants.create("void");
    public static final ResourceKey<PaintingVariant> SKULL_AND_ROSES = PaintingVariants.create("skull_and_roses");
    public static final ResourceKey<PaintingVariant> WITHER = PaintingVariants.create("wither");
    public static final ResourceKey<PaintingVariant> FIGHTERS = PaintingVariants.create("fighters");
    public static final ResourceKey<PaintingVariant> POINTER = PaintingVariants.create("pointer");
    public static final ResourceKey<PaintingVariant> PIGSCENE = PaintingVariants.create("pigscene");
    public static final ResourceKey<PaintingVariant> BURNING_SKULL = PaintingVariants.create("burning_skull");
    public static final ResourceKey<PaintingVariant> SKELETON = PaintingVariants.create("skeleton");
    public static final ResourceKey<PaintingVariant> DONKEY_KONG = PaintingVariants.create("donkey_kong");
    public static final ResourceKey<PaintingVariant> EARTH = PaintingVariants.create("earth");
    public static final ResourceKey<PaintingVariant> WIND = PaintingVariants.create("wind");
    public static final ResourceKey<PaintingVariant> WATER = PaintingVariants.create("water");
    public static final ResourceKey<PaintingVariant> FIRE = PaintingVariants.create("fire");
    public static final ResourceKey<PaintingVariant> BAROQUE = PaintingVariants.create("baroque");
    public static final ResourceKey<PaintingVariant> HUMBLE = PaintingVariants.create("humble");
    public static final ResourceKey<PaintingVariant> MEDITATIVE = PaintingVariants.create("meditative");
    public static final ResourceKey<PaintingVariant> PRAIRIE_RIDE = PaintingVariants.create("prairie_ride");
    public static final ResourceKey<PaintingVariant> UNPACKED = PaintingVariants.create("unpacked");
    public static final ResourceKey<PaintingVariant> BACKYARD = PaintingVariants.create("backyard");
    public static final ResourceKey<PaintingVariant> BOUQUET = PaintingVariants.create("bouquet");
    public static final ResourceKey<PaintingVariant> CAVEBIRD = PaintingVariants.create("cavebird");
    public static final ResourceKey<PaintingVariant> CHANGING = PaintingVariants.create("changing");
    public static final ResourceKey<PaintingVariant> COTAN = PaintingVariants.create("cotan");
    public static final ResourceKey<PaintingVariant> ENDBOSS = PaintingVariants.create("endboss");
    public static final ResourceKey<PaintingVariant> FERN = PaintingVariants.create("fern");
    public static final ResourceKey<PaintingVariant> FINDING = PaintingVariants.create("finding");
    public static final ResourceKey<PaintingVariant> LOWMIST = PaintingVariants.create("lowmist");
    public static final ResourceKey<PaintingVariant> ORB = PaintingVariants.create("orb");
    public static final ResourceKey<PaintingVariant> OWLEMONS = PaintingVariants.create("owlemons");
    public static final ResourceKey<PaintingVariant> PASSAGE = PaintingVariants.create("passage");
    public static final ResourceKey<PaintingVariant> POND = PaintingVariants.create("pond");
    public static final ResourceKey<PaintingVariant> SUNFLOWERS = PaintingVariants.create("sunflowers");
    public static final ResourceKey<PaintingVariant> TIDES = PaintingVariants.create("tides");
    public static final ResourceKey<PaintingVariant> DENNIS = PaintingVariants.create("dennis");

    public static void bootstrap(BootstrapContext<PaintingVariant> context) {
        PaintingVariants.register(context, KEBAB, 1, 1);
        PaintingVariants.register(context, AZTEC, 1, 1);
        PaintingVariants.register(context, ALBAN, 1, 1);
        PaintingVariants.register(context, AZTEC2, 1, 1);
        PaintingVariants.register(context, BOMB, 1, 1);
        PaintingVariants.register(context, PLANT, 1, 1);
        PaintingVariants.register(context, WASTELAND, 1, 1);
        PaintingVariants.register(context, POOL, 2, 1);
        PaintingVariants.register(context, COURBET, 2, 1);
        PaintingVariants.register(context, SEA, 2, 1);
        PaintingVariants.register(context, SUNSET, 2, 1);
        PaintingVariants.register(context, CREEBET, 2, 1);
        PaintingVariants.register(context, WANDERER, 1, 2);
        PaintingVariants.register(context, GRAHAM, 1, 2);
        PaintingVariants.register(context, MATCH, 2, 2);
        PaintingVariants.register(context, BUST, 2, 2);
        PaintingVariants.register(context, STAGE, 2, 2);
        PaintingVariants.register(context, VOID, 2, 2);
        PaintingVariants.register(context, SKULL_AND_ROSES, 2, 2);
        PaintingVariants.register(context, WITHER, 2, 2, false);
        PaintingVariants.register(context, FIGHTERS, 4, 2);
        PaintingVariants.register(context, POINTER, 4, 4);
        PaintingVariants.register(context, PIGSCENE, 4, 4);
        PaintingVariants.register(context, BURNING_SKULL, 4, 4);
        PaintingVariants.register(context, SKELETON, 4, 3);
        PaintingVariants.register(context, EARTH, 2, 2, false);
        PaintingVariants.register(context, WIND, 2, 2, false);
        PaintingVariants.register(context, WATER, 2, 2, false);
        PaintingVariants.register(context, FIRE, 2, 2, false);
        PaintingVariants.register(context, DONKEY_KONG, 4, 3);
        PaintingVariants.register(context, BAROQUE, 2, 2);
        PaintingVariants.register(context, HUMBLE, 2, 2);
        PaintingVariants.register(context, MEDITATIVE, 1, 1);
        PaintingVariants.register(context, PRAIRIE_RIDE, 1, 2);
        PaintingVariants.register(context, UNPACKED, 4, 4);
        PaintingVariants.register(context, BACKYARD, 3, 4);
        PaintingVariants.register(context, BOUQUET, 3, 3);
        PaintingVariants.register(context, CAVEBIRD, 3, 3);
        PaintingVariants.register(context, CHANGING, 4, 2);
        PaintingVariants.register(context, COTAN, 3, 3);
        PaintingVariants.register(context, ENDBOSS, 3, 3);
        PaintingVariants.register(context, FERN, 3, 3);
        PaintingVariants.register(context, FINDING, 4, 2);
        PaintingVariants.register(context, LOWMIST, 4, 2);
        PaintingVariants.register(context, ORB, 4, 4);
        PaintingVariants.register(context, OWLEMONS, 3, 3);
        PaintingVariants.register(context, PASSAGE, 4, 2);
        PaintingVariants.register(context, POND, 3, 4);
        PaintingVariants.register(context, SUNFLOWERS, 3, 3);
        PaintingVariants.register(context, TIDES, 3, 3);
        PaintingVariants.register(context, DENNIS, 3, 3);
    }

    private static void register(BootstrapContext<PaintingVariant> context, ResourceKey<PaintingVariant> id, int width, int height) {
        PaintingVariants.register(context, id, width, height, true);
    }

    private static void register(BootstrapContext<PaintingVariant> context, ResourceKey<PaintingVariant> id, int width, int height, boolean hasAuthor) {
        context.register(id, new PaintingVariant(width, height, id.identifier(), Optional.of(Component.translatable(id.identifier().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW)), hasAuthor ? Optional.of(Component.translatable(id.identifier().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY)) : Optional.empty()));
    }

    private static ResourceKey<PaintingVariant> create(String name) {
        return ResourceKey.create(Registries.PAINTING_VARIANT, Identifier.withDefaultNamespace(name));
    }
}

