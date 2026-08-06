/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
    public ItemStack get();

    public boolean set(ItemStack var1);

    public static SlotAccess of(final Supplier<ItemStack> getter, final Consumer<ItemStack> setter) {
        return new SlotAccess(){

            @Override
            public ItemStack get() {
                return (ItemStack)getter.get();
            }

            @Override
            public boolean set(ItemStack itemStack) {
                setter.accept(itemStack);
                return true;
            }
        };
    }

    public static SlotAccess forEquipmentSlot(final LivingEntity entity, final EquipmentSlot slot, final Predicate<ItemStack> validator) {
        return new SlotAccess(){

            @Override
            public ItemStack get() {
                return entity.getItemBySlot(slot);
            }

            @Override
            public boolean set(ItemStack itemStack) {
                if (!validator.test(itemStack)) {
                    return false;
                }
                entity.setItemSlot(slot, itemStack);
                return true;
            }
        };
    }

    public static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot) {
        return SlotAccess.forEquipmentSlot(entity, slot, stack -> true);
    }

    public static SlotAccess forListElement(final List<ItemStack> stacks, final int index) {
        return new SlotAccess(){

            @Override
            public ItemStack get() {
                return (ItemStack)stacks.get(index);
            }

            @Override
            public boolean set(ItemStack itemStack) {
                stacks.set(index, itemStack);
                return true;
            }
        };
    }
}

