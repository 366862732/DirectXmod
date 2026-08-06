/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core.cauldron;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface CauldronInteraction {
    public static final CauldronInteraction DEFAULT = (blockState, level, blockPos, player, interactionHand, itemStack) -> InteractionResult.TRY_WITH_EMPTY_HAND;

    public InteractionResult interact(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, ItemStack var6);

    public static class Dispatcher {
        private final Map<TagKey<Item>, CauldronInteraction> tags = new HashMap<TagKey<Item>, CauldronInteraction>();
        private final Map<Item, CauldronInteraction> items = new HashMap<Item, CauldronInteraction>();

        void put(Item item, CauldronInteraction interaction) {
            this.items.put(item, interaction);
        }

        void put(TagKey<Item> tag, CauldronInteraction interaction) {
            this.tags.put(tag, interaction);
        }

        public CauldronInteraction get(ItemStack itemStack) {
            for (Map.Entry<TagKey<Item>, CauldronInteraction> e : this.tags.entrySet()) {
                if (!itemStack.is(e.getKey())) continue;
                return e.getValue();
            }
            return this.items.getOrDefault(itemStack.getItem(), DEFAULT);
        }
    }
}

