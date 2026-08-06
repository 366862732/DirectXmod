/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage.loot;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;

public interface ContainerComponentManipulator<T> {
    public DataComponentType<T> type();

    public T empty();

    public T setContents(T var1, Stream<ItemStack> var2);

    public Stream<ItemStack> getContents(T var1);

    default public void setContents(ItemStack itemStack, T defaultValue, Stream<ItemStack> newContents) {
        T currentValue = itemStack.getOrDefault(this.type(), defaultValue);
        T newValue = this.setContents(currentValue, newContents);
        itemStack.set(this.type(), newValue);
    }

    default public void setContents(ItemStack itemStack, Stream<ItemStack> newContents) {
        this.setContents(itemStack, this.empty(), newContents);
    }

    default public void modifyItems(ItemStack itemStack, UnaryOperator<ItemStack> modifier) {
        T contents = itemStack.get(this.type());
        if (contents != null) {
            UnaryOperator nonEmptyModifier = currentItemStack -> {
                if (currentItemStack.isEmpty()) {
                    return currentItemStack;
                }
                ItemStack newItemStack = (ItemStack)modifier.apply((ItemStack)currentItemStack);
                newItemStack.limitSize(newItemStack.getMaxStackSize());
                return newItemStack;
            };
            this.setContents(itemStack, this.getContents(contents).map(nonEmptyModifier));
        }
    }

    default public SlotCollection getSlots(ItemStack itemStack) {
        return () -> {
            T contents = itemStack.get(this.type());
            if (contents != null) {
                return this.getContents(contents).filter(stack -> !stack.isEmpty());
            }
            return Stream.empty();
        };
    }
}

