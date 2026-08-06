/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import java.util.stream.Stream;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemUtils {
    public static InteractionResult startUsingInstantly(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    public static ItemStack createFilledResult(ItemStack itemStack, Player player, ItemStack newItemStack, boolean limitCreativeStackSize) {
        boolean isCreative = player.hasInfiniteMaterials();
        if (limitCreativeStackSize && isCreative) {
            if (!player.getInventory().contains(newItemStack)) {
                player.getInventory().add(newItemStack);
            }
            return itemStack;
        }
        itemStack.consume(1, player);
        if (itemStack.isEmpty()) {
            return newItemStack;
        }
        if (!player.getInventory().add(newItemStack)) {
            player.drop(newItemStack, false);
        }
        return itemStack;
    }

    public static ItemStack createFilledResult(ItemStack itemStack, Player player, ItemStack newItemStack) {
        return ItemUtils.createFilledResult(itemStack, player, newItemStack, true);
    }

    public static void onContainerDestroyed(ItemEntity container, Stream<ItemStack> contents) {
        Level level = container.level();
        if (level.isClientSide()) {
            return;
        }
        contents.forEach(stack -> level.addFreshEntity(new ItemEntity(level, container.getX(), container.getY(), container.getZ(), (ItemStack)stack)));
    }
}

