/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public interface EquipmentUser {
    public void setItemSlot(EquipmentSlot var1, ItemStack var2);

    public ItemStack getItemBySlot(EquipmentSlot var1);

    public void setDropChance(EquipmentSlot var1, float var2);

    default public void equip(EquipmentTable equipment, LootParams lootParams) {
        this.equip(equipment.lootTable(), lootParams, equipment.slotDropChances());
    }

    default public void equip(ResourceKey<LootTable> lootTable, LootParams lootParams, Map<EquipmentSlot, Float> dropChances) {
        this.equip(lootTable, lootParams, 0L, dropChances);
    }

    default public void equip(ResourceKey<LootTable> lootTable, LootParams lootParams, long optionalLootTableSeed, Map<EquipmentSlot, Float> dropChances) {
        LootTable table = lootParams.getLevel().getServer().reloadableRegistries().getLootTable(lootTable);
        if (table == LootTable.EMPTY) {
            return;
        }
        ObjectArrayList<ItemStack> possibleEquipment = table.getRandomItems(lootParams, optionalLootTableSeed);
        ArrayList<EquipmentSlot> insertedIntoSlots = new ArrayList<EquipmentSlot>();
        for (ItemStack toEquip : possibleEquipment) {
            EquipmentSlot slot = this.resolveSlot(toEquip, insertedIntoSlots);
            if (slot == null) continue;
            ItemStack equipped = slot.limit(toEquip);
            this.setItemSlot(slot, equipped);
            Float dropChance = dropChances.get(slot);
            if (dropChance != null) {
                this.setDropChance(slot, dropChance.floatValue());
            }
            insertedIntoSlots.add(slot);
        }
    }

    default public @Nullable EquipmentSlot resolveSlot(ItemStack toEquip, List<EquipmentSlot> alreadyInsertedIntoSlots) {
        if (toEquip.isEmpty()) {
            return null;
        }
        Equippable equippable = toEquip.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (!alreadyInsertedIntoSlots.contains(slot)) {
                return slot;
            }
        } else if (!alreadyInsertedIntoSlots.contains(EquipmentSlot.MAINHAND)) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }
}

