/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.ticks;

import java.util.List;
import net.minecraft.world.ticks.SavedTick;

public interface SerializableTickContainer<T> {
    public List<SavedTick<T>> pack(long var1);
}

