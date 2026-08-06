/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.EntryGroup;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.SequentialEntry;
import net.minecraft.world.level.storage.loot.entries.SlotLoot;
import net.minecraft.world.level.storage.loot.entries.TagEntry;

public class LootPoolEntries {
    public static final Codec<LootPoolEntryContainer> CODEC = BuiltInRegistries.LOOT_POOL_ENTRY_TYPE.byNameCodec().dispatch(LootPoolEntryContainer::codec, c -> c);

    public static MapCodec<? extends LootPoolEntryContainer> bootstrap(Registry<MapCodec<? extends LootPoolEntryContainer>> registry) {
        Registry.register(registry, "empty", EmptyLootItem.MAP_CODEC);
        Registry.register(registry, "item", LootItem.MAP_CODEC);
        Registry.register(registry, "loot_table", NestedLootTable.MAP_CODEC);
        Registry.register(registry, "dynamic", DynamicLoot.MAP_CODEC);
        Registry.register(registry, "tag", TagEntry.MAP_CODEC);
        Registry.register(registry, "slots", SlotLoot.MAP_CODEC);
        Registry.register(registry, "alternatives", AlternativesEntry.MAP_CODEC);
        Registry.register(registry, "sequence", SequentialEntry.MAP_CODEC);
        return Registry.register(registry, "group", EntryGroup.MAP_CODEC);
    }
}

