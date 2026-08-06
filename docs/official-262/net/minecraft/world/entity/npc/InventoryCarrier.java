/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface InventoryCarrier {
    public static final String TAG_INVENTORY = "Inventory";

    public SimpleContainer getInventory();

    public static void pickUpItem(ServerLevel level, Mob mob, InventoryCarrier inventoryCarrier, ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        if (mob.wantsToPickUp(level, itemStack)) {
            SimpleContainer inventory = inventoryCarrier.getInventory();
            boolean hasSpace = inventory.canAddItem(itemStack);
            if (!hasSpace) {
                return;
            }
            mob.onItemPickup(itemEntity);
            int count = itemStack.getCount();
            ItemStack remainder = inventory.addItem(itemStack);
            mob.take(itemEntity, count - remainder.getCount());
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemStack.setCount(remainder.getCount());
            }
        }
    }

    default public void readInventoryFromTag(ValueInput input) {
        input.list(TAG_INVENTORY, ItemStack.CODEC).ifPresent(list -> this.getInventory().fromItemList((ValueInput.TypedInputList<ItemStack>)list));
    }

    default public void writeInventoryToTag(ValueOutput output) {
        this.getInventory().storeAsItemList(output.list(TAG_INVENTORY, ItemStack.CODEC));
    }
}

