/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
    public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(items -> {
        EnumMap<EquipmentSlot, ItemStack> map = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
        map.putAll((Map<EquipmentSlot, ItemStack>)items);
        return new EntityEquipment(map);
    }, equipment -> {
        EnumMap<EquipmentSlot, ItemStack> items = new EnumMap<EquipmentSlot, ItemStack>(equipment.items);
        items.values().removeIf(ItemStack::isEmpty);
        return items;
    });
    private final EnumMap<EquipmentSlot, ItemStack> items;

    private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> items) {
        this.items = items;
    }

    public EntityEquipment() {
        this(new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class));
    }

    public ItemStack set(EquipmentSlot slot, ItemStack itemStack) {
        return Objects.requireNonNullElse(this.items.put(slot, itemStack), ItemStack.EMPTY);
    }

    public ItemStack get(EquipmentSlot slot) {
        return this.items.getOrDefault(slot, ItemStack.EMPTY);
    }

    public boolean isEmpty() {
        for (ItemStack item : this.items.values()) {
            if (item.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public void tick(Entity owner) {
        for (Map.Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
            ItemStack item = entry.getValue();
            if (item.isEmpty()) continue;
            item.inventoryTick(owner.level(), owner, entry.getKey());
        }
    }

    public void setAll(EntityEquipment equipment) {
        this.items.clear();
        this.items.putAll(equipment.items);
    }

    public void dropAll(LivingEntity dropper) {
        for (ItemStack item : this.items.values()) {
            dropper.drop(item, true, false);
        }
        this.clear();
    }

    public void clear() {
        this.items.replaceAll((s, v) -> ItemStack.EMPTY);
    }
}

