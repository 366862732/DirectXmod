/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public class ItemCombinerMenuSlotDefinition {
    private final List<SlotDefinition> slots;
    private final SlotDefinition resultSlot;

    private ItemCombinerMenuSlotDefinition(List<SlotDefinition> inputSlots, SlotDefinition resultSlot) {
        if (inputSlots.isEmpty() || resultSlot.equals(SlotDefinition.EMPTY)) {
            throw new IllegalArgumentException("Need to define both inputSlots and resultSlot");
        }
        this.slots = inputSlots;
        this.resultSlot = resultSlot;
    }

    public static Builder create() {
        return new Builder();
    }

    public SlotDefinition getSlot(int index) {
        return this.slots.get(index);
    }

    public SlotDefinition getResultSlot() {
        return this.resultSlot;
    }

    public List<SlotDefinition> getSlots() {
        return this.slots;
    }

    public int getNumOfInputSlots() {
        return this.slots.size();
    }

    public int getResultSlotIndex() {
        return this.getNumOfInputSlots();
    }

    public record SlotDefinition(int slotIndex, int x, int y, Predicate<ItemStack> mayPlace) {
        private static final SlotDefinition EMPTY = new SlotDefinition(0, 0, 0, itemStack -> true);
    }

    public static class Builder {
        private final List<SlotDefinition> inputSlots = new ArrayList<SlotDefinition>();
        private SlotDefinition resultSlot = SlotDefinition.EMPTY;

        public Builder withSlot(int slotIndex, int xPlacement, int yPlacement, Predicate<ItemStack> mayPlace) {
            this.inputSlots.add(new SlotDefinition(slotIndex, xPlacement, yPlacement, mayPlace));
            return this;
        }

        public Builder withResultSlot(int slotIndex, int xPlacement, int yPlacement) {
            this.resultSlot = new SlotDefinition(slotIndex, xPlacement, yPlacement, itemStack -> false);
            return this;
        }

        public ItemCombinerMenuSlotDefinition build() {
            int inputCount = this.inputSlots.size();
            for (int i = 0; i < inputCount; ++i) {
                SlotDefinition inputDefinition = this.inputSlots.get(i);
                if (inputDefinition.slotIndex == i) continue;
                throw new IllegalArgumentException("Expected input slots to have continous indexes");
            }
            if (this.resultSlot.slotIndex != inputCount) {
                throw new IllegalArgumentException("Expected result slot index to follow last input slot");
            }
            return new ItemCombinerMenuSlotDefinition(this.inputSlots, this.resultSlot);
        }
    }
}

