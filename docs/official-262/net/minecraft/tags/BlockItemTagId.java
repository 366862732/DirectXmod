/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public record BlockItemTagId(TagKey<Block> block, TagKey<Item> item) {
    public static BlockItemTagId create(Identifier blockId, Identifier itemId) {
        return new BlockItemTagId(TagKey.create(Registries.BLOCK, blockId), TagKey.create(Registries.ITEM, itemId));
    }

    public static BlockItemTagId create(String blockName, String itemName) {
        return BlockItemTagId.create(Identifier.withDefaultNamespace(blockName), Identifier.withDefaultNamespace(itemName));
    }

    public static BlockItemTagId create(String name) {
        Identifier id = Identifier.withDefaultNamespace(name);
        return BlockItemTagId.create(id, id);
    }
}

