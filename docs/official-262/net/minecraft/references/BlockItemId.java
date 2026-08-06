/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.references;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public record BlockItemId(ResourceKey<Block> block, ResourceKey<Item> item) {
    public static BlockItemId create(Identifier blockId, Identifier itemId) {
        return new BlockItemId(ResourceKey.create(Registries.BLOCK, blockId), ResourceKey.create(Registries.ITEM, itemId));
    }

    public static BlockItemId create(String blockName, String itemName) {
        return BlockItemId.create(Identifier.withDefaultNamespace(blockName), Identifier.withDefaultNamespace(itemName));
    }

    public static BlockItemId create(String name) {
        Identifier id = Identifier.withDefaultNamespace(name);
        return BlockItemId.create(id, id);
    }
}

