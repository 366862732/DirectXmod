/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.HashBiMap
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootContextParamSets {
    private static final BiMap<Identifier, ContextKeySet> REGISTRY = HashBiMap.create();
    public static final Codec<ContextKeySet> CODEC = Identifier.CODEC.comapFlatMap(location -> Optional.ofNullable((ContextKeySet)REGISTRY.get(location)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "No parameter set exists with id: '" + String.valueOf(location) + "'")), arg_0 -> REGISTRY.inverse().get(arg_0));
    public static final ContextKeySet EMPTY = LootContextParamSets.register("empty", builder -> {});
    public static final ContextKeySet CHEST = LootContextParamSets.register("chest", builder -> builder.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet COMMAND = LootContextParamSets.register("command", builder -> builder.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet SELECTOR = LootContextParamSets.register("selector", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet VILLAGER_TRADE = LootContextParamSets.register("villager_trade", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED));
    public static final ContextKeySet FISHING = LootContextParamSets.register("fishing", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).optional(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet ENTITY = LootContextParamSets.register("entity", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.DAMAGE_SOURCE).optional(LootContextParams.ATTACKING_ENTITY).optional(LootContextParams.DIRECT_ATTACKING_ENTITY).optional(LootContextParams.LAST_DAMAGE_PLAYER));
    public static final ContextKeySet EQUIPMENT = LootContextParamSets.register("equipment", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet ARCHAEOLOGY = LootContextParamSets.register("archaeology", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL));
    public static final ContextKeySet GIFT = LootContextParamSets.register("gift", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet PIGLIN_BARTER = LootContextParamSets.register("barter", builder -> builder.required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet VAULT = LootContextParamSets.register("vault", builder -> builder.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.TOOL));
    public static final ContextKeySet ADVANCEMENT_REWARD = LootContextParamSets.register("advancement_reward", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN));
    public static final ContextKeySet ADVANCEMENT_ENTITY = LootContextParamSets.register("advancement_entity", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN));
    public static final ContextKeySet ADVANCEMENT_LOCATION = LootContextParamSets.register("advancement_location", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).required(LootContextParams.BLOCK_STATE));
    public static final ContextKeySet BLOCK_USE = LootContextParamSets.register("block_use", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE));
    public static final ContextKeySet ALL_PARAMS = LootContextParamSets.register("generic", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.LAST_DAMAGE_PLAYER).required(LootContextParams.DAMAGE_SOURCE).required(LootContextParams.ATTACKING_ENTITY).required(LootContextParams.DIRECT_ATTACKING_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE).required(LootContextParams.BLOCK_ENTITY).required(LootContextParams.TOOL).required(LootContextParams.EXPLOSION_RADIUS).required(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED));
    public static final ContextKeySet BLOCK = LootContextParamSets.register("block", builder -> builder.required(LootContextParams.BLOCK_STATE).required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.BLOCK_ENTITY).optional(LootContextParams.EXPLOSION_RADIUS));
    public static final ContextKeySet SHEARING = LootContextParamSets.register("shearing", builder -> builder.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL));
    public static final ContextKeySet ENTITY_INTERACT = LootContextParamSets.register("entity_interact", builder -> builder.required(LootContextParams.TARGET_ENTITY).optional(LootContextParams.INTERACTING_ENTITY).required(LootContextParams.TOOL));
    public static final ContextKeySet BLOCK_INTERACT = LootContextParamSets.register("block_interact", builder -> builder.required(LootContextParams.BLOCK_STATE).optional(LootContextParams.BLOCK_ENTITY).optional(LootContextParams.INTERACTING_ENTITY).optional(LootContextParams.TOOL));
    public static final ContextKeySet ENCHANTED_DAMAGE = LootContextParamSets.register("enchanted_damage", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(LootContextParams.DAMAGE_SOURCE).optional(LootContextParams.DIRECT_ATTACKING_ENTITY).optional(LootContextParams.ATTACKING_ENTITY));
    public static final ContextKeySet ENCHANTED_ITEM = LootContextParamSets.register("enchanted_item", builder -> builder.required(LootContextParams.TOOL).required(LootContextParams.ENCHANTMENT_LEVEL));
    public static final ContextKeySet ENCHANTED_LOCATION = LootContextParamSets.register("enchanted_location", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(LootContextParams.ENCHANTMENT_ACTIVE));
    public static final ContextKeySet ENCHANTED_ENTITY = LootContextParamSets.register("enchanted_entity", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN));
    public static final ContextKeySet HIT_BLOCK = LootContextParamSets.register("hit_block", builder -> builder.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE));

    private static ContextKeySet register(String name, Consumer<ContextKeySet.Builder> consumer) {
        ContextKeySet.Builder builder = new ContextKeySet.Builder();
        consumer.accept(builder);
        ContextKeySet result = builder.build();
        Identifier id = Identifier.withDefaultNamespace(name);
        ContextKeySet prev = (ContextKeySet)REGISTRY.put((Object)id, (Object)result);
        if (prev != null) {
            throw new IllegalStateException("Loot table parameter set " + String.valueOf(id) + " is already registered");
        }
        return result;
    }
}

