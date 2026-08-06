/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;

public class DecoratedPotPatterns {
    public static final ResourceKey<DecoratedPotPattern> BLANK = DecoratedPotPatterns.create("blank");
    public static final ResourceKey<DecoratedPotPattern> ANGLER = DecoratedPotPatterns.create("angler");
    public static final ResourceKey<DecoratedPotPattern> ARCHER = DecoratedPotPatterns.create("archer");
    public static final ResourceKey<DecoratedPotPattern> ARMS_UP = DecoratedPotPatterns.create("arms_up");
    public static final ResourceKey<DecoratedPotPattern> BLADE = DecoratedPotPatterns.create("blade");
    public static final ResourceKey<DecoratedPotPattern> BREWER = DecoratedPotPatterns.create("brewer");
    public static final ResourceKey<DecoratedPotPattern> BURN = DecoratedPotPatterns.create("burn");
    public static final ResourceKey<DecoratedPotPattern> DANGER = DecoratedPotPatterns.create("danger");
    public static final ResourceKey<DecoratedPotPattern> EXPLORER = DecoratedPotPatterns.create("explorer");
    public static final ResourceKey<DecoratedPotPattern> FLOW = DecoratedPotPatterns.create("flow");
    public static final ResourceKey<DecoratedPotPattern> FRIEND = DecoratedPotPatterns.create("friend");
    public static final ResourceKey<DecoratedPotPattern> GUSTER = DecoratedPotPatterns.create("guster");
    public static final ResourceKey<DecoratedPotPattern> HEART = DecoratedPotPatterns.create("heart");
    public static final ResourceKey<DecoratedPotPattern> HEARTBREAK = DecoratedPotPatterns.create("heartbreak");
    public static final ResourceKey<DecoratedPotPattern> HOWL = DecoratedPotPatterns.create("howl");
    public static final ResourceKey<DecoratedPotPattern> MINER = DecoratedPotPatterns.create("miner");
    public static final ResourceKey<DecoratedPotPattern> MOURNER = DecoratedPotPatterns.create("mourner");
    public static final ResourceKey<DecoratedPotPattern> PLENTY = DecoratedPotPatterns.create("plenty");
    public static final ResourceKey<DecoratedPotPattern> PRIZE = DecoratedPotPatterns.create("prize");
    public static final ResourceKey<DecoratedPotPattern> SCRAPE = DecoratedPotPatterns.create("scrape");
    public static final ResourceKey<DecoratedPotPattern> SHEAF = DecoratedPotPatterns.create("sheaf");
    public static final ResourceKey<DecoratedPotPattern> SHELTER = DecoratedPotPatterns.create("shelter");
    public static final ResourceKey<DecoratedPotPattern> SKULL = DecoratedPotPatterns.create("skull");
    public static final ResourceKey<DecoratedPotPattern> SNORT = DecoratedPotPatterns.create("snort");

    public static void itemToPatternMappings(BiConsumer<ResourceKey<Item>, ResourceKey<DecoratedPotPattern>> itemToPattern) {
        itemToPattern.accept(ItemIds.BRICK, BLANK);
        itemToPattern.accept(ItemIds.ANGLER_POTTERY_SHERD, ANGLER);
        itemToPattern.accept(ItemIds.ARCHER_POTTERY_SHERD, ARCHER);
        itemToPattern.accept(ItemIds.ARMS_UP_POTTERY_SHERD, ARMS_UP);
        itemToPattern.accept(ItemIds.BLADE_POTTERY_SHERD, BLADE);
        itemToPattern.accept(ItemIds.BREWER_POTTERY_SHERD, BREWER);
        itemToPattern.accept(ItemIds.BURN_POTTERY_SHERD, BURN);
        itemToPattern.accept(ItemIds.DANGER_POTTERY_SHERD, DANGER);
        itemToPattern.accept(ItemIds.EXPLORER_POTTERY_SHERD, EXPLORER);
        itemToPattern.accept(ItemIds.FLOW_POTTERY_SHERD, FLOW);
        itemToPattern.accept(ItemIds.FRIEND_POTTERY_SHERD, FRIEND);
        itemToPattern.accept(ItemIds.GUSTER_POTTERY_SHERD, GUSTER);
        itemToPattern.accept(ItemIds.HEART_POTTERY_SHERD, HEART);
        itemToPattern.accept(ItemIds.HEARTBREAK_POTTERY_SHERD, HEARTBREAK);
        itemToPattern.accept(ItemIds.HOWL_POTTERY_SHERD, HOWL);
        itemToPattern.accept(ItemIds.MINER_POTTERY_SHERD, MINER);
        itemToPattern.accept(ItemIds.MOURNER_POTTERY_SHERD, MOURNER);
        itemToPattern.accept(ItemIds.PLENTY_POTTERY_SHERD, PLENTY);
        itemToPattern.accept(ItemIds.PRIZE_POTTERY_SHERD, PRIZE);
        itemToPattern.accept(ItemIds.SCRAPE_POTTERY_SHERD, SCRAPE);
        itemToPattern.accept(ItemIds.SHEAF_POTTERY_SHERD, SHEAF);
        itemToPattern.accept(ItemIds.SHELTER_POTTERY_SHERD, SHELTER);
        itemToPattern.accept(ItemIds.SKULL_POTTERY_SHERD, SKULL);
        itemToPattern.accept(ItemIds.SNORT_POTTERY_SHERD, SNORT);
    }

    private static ResourceKey<DecoratedPotPattern> create(String id) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, Identifier.withDefaultNamespace(id));
    }

    public static DecoratedPotPattern bootstrap(Registry<DecoratedPotPattern> registry) {
        DecoratedPotPatterns.register(registry, ANGLER, "angler_pottery_pattern");
        DecoratedPotPatterns.register(registry, ARCHER, "archer_pottery_pattern");
        DecoratedPotPatterns.register(registry, ARMS_UP, "arms_up_pottery_pattern");
        DecoratedPotPatterns.register(registry, BLADE, "blade_pottery_pattern");
        DecoratedPotPatterns.register(registry, BREWER, "brewer_pottery_pattern");
        DecoratedPotPatterns.register(registry, BURN, "burn_pottery_pattern");
        DecoratedPotPatterns.register(registry, DANGER, "danger_pottery_pattern");
        DecoratedPotPatterns.register(registry, EXPLORER, "explorer_pottery_pattern");
        DecoratedPotPatterns.register(registry, FLOW, "flow_pottery_pattern");
        DecoratedPotPatterns.register(registry, FRIEND, "friend_pottery_pattern");
        DecoratedPotPatterns.register(registry, GUSTER, "guster_pottery_pattern");
        DecoratedPotPatterns.register(registry, HEART, "heart_pottery_pattern");
        DecoratedPotPatterns.register(registry, HEARTBREAK, "heartbreak_pottery_pattern");
        DecoratedPotPatterns.register(registry, HOWL, "howl_pottery_pattern");
        DecoratedPotPatterns.register(registry, MINER, "miner_pottery_pattern");
        DecoratedPotPatterns.register(registry, MOURNER, "mourner_pottery_pattern");
        DecoratedPotPatterns.register(registry, PLENTY, "plenty_pottery_pattern");
        DecoratedPotPatterns.register(registry, PRIZE, "prize_pottery_pattern");
        DecoratedPotPatterns.register(registry, SCRAPE, "scrape_pottery_pattern");
        DecoratedPotPatterns.register(registry, SHEAF, "sheaf_pottery_pattern");
        DecoratedPotPatterns.register(registry, SHELTER, "shelter_pottery_pattern");
        DecoratedPotPatterns.register(registry, SKULL, "skull_pottery_pattern");
        DecoratedPotPatterns.register(registry, SNORT, "snort_pottery_pattern");
        return DecoratedPotPatterns.register(registry, BLANK, "decorated_pot_side");
    }

    private static DecoratedPotPattern register(Registry<DecoratedPotPattern> registry, ResourceKey<DecoratedPotPattern> id, String assetId) {
        return Registry.register(registry, id, new DecoratedPotPattern(Identifier.withDefaultNamespace(assetId)));
    }
}

