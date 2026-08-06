/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.slot.SlotCollection;
import org.jspecify.annotations.Nullable;

public interface SlotProvider {
    public @Nullable SlotAccess getSlot(int var1);

    default public SlotCollection getSlotsFromRange(IntList slots) {
        List<SlotAccess> slotList = slots.intStream().mapToObj(this::getSlot).filter(Objects::nonNull).toList();
        return SlotCollection.of(slotList);
    }
}

