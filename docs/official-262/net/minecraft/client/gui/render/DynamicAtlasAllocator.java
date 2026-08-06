/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableInt
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.render;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DynamicAtlasAllocator<K> {
    private final int width;
    private final List<Slot> slots;
    private final Map<K, Slot> usedSlotByKey = new HashMap<K, Slot>();
    private final BitSet freeSlots;

    public DynamicAtlasAllocator(int width, int height) {
        this.width = width;
        int size = width * height;
        this.slots = new ArrayList<Slot>(size);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                this.slots.add(new Slot(x, y));
            }
        }
        this.freeSlots = new BitSet(size);
        this.freeSlots.set(0, size);
    }

    public boolean reclaimSpaceFor(Set<K> keys) {
        int preexistingKeyCount = Sets.intersection(this.usedSlotByKey.keySet(), keys).size();
        if (preexistingKeyCount == keys.size()) {
            return true;
        }
        MutableInt needSpaceFor = new MutableInt(keys.size() - preexistingKeyCount);
        this.freeSlotIf((key, slot) -> {
            if (needSpaceFor.intValue() == 0 || keys.contains(key)) {
                return false;
            }
            needSpaceFor.decrement();
            return true;
        });
        return needSpaceFor.intValue() == 0;
    }

    public void endFrame() {
        this.freeSlotIf((object, slot) -> slot.discardAfterFrame);
    }

    private void freeSlotIf(BiPredicate<K, Slot> predicate) {
        this.usedSlotByKey.entrySet().removeIf(entry -> {
            if (!predicate.test(entry.getKey(), (Slot)entry.getValue())) {
                return false;
            }
            Slot slot = (Slot)entry.getValue();
            this.freeSlots.set(slot.x + slot.y * this.width);
            slot.discardAfterFrame = false;
            return true;
        });
    }

    public boolean hasSpaceForAll(Set<K> keys) {
        Sets.SetView predictedUsedSlots = Sets.union(this.usedSlotByKey.keySet(), keys);
        return predictedUsedSlots.size() <= this.slots.size();
    }

    public @Nullable Slot getOrAllocate(K key, boolean discardAfterFrame) {
        Slot usedSlot = this.usedSlotByKey.get(key);
        if (usedSlot != null) {
            usedSlot.discardAfterFrame |= discardAfterFrame;
            usedSlot.externalState = SlotState.READY;
            return usedSlot;
        }
        int freeSlotIndex = this.freeSlots.nextSetBit(0);
        if (freeSlotIndex == -1) {
            return null;
        }
        Slot freeSlot = this.slots.get(freeSlotIndex);
        freeSlot.externalState = freeSlot.fresh ? SlotState.EMPTY : SlotState.STALE;
        freeSlot.fresh = false;
        freeSlot.discardAfterFrame = discardAfterFrame;
        this.usedSlotByKey.put(key, freeSlot);
        this.freeSlots.clear(freeSlotIndex);
        return freeSlot;
    }

    @VisibleForTesting
    public int freeSlotCount() {
        return this.slots.size() - this.usedSlotByKey.size();
    }

    @VisibleForTesting
    public Set<K> usedSlotKeys() {
        return Collections.unmodifiableSet(this.usedSlotByKey.keySet());
    }

    @Environment(value=EnvType.CLIENT)
    public static class Slot {
        private final int x;
        private final int y;
        private boolean fresh = true;
        private boolean discardAfterFrame;
        private SlotState externalState = SlotState.EMPTY;

        private Slot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public SlotState state() {
            return this.externalState;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum SlotState {
        EMPTY,
        STALE,
        READY;

    }
}

