/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.loot;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@FunctionalInterface
public interface LootTableSubProvider {
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> var1);
}

