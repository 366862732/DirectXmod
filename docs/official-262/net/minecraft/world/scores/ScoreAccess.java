/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.jspecify.annotations.Nullable;

public interface ScoreAccess {
    public int get();

    public void set(int var1);

    default public int add(int count) {
        int newValue = this.get() + count;
        this.set(newValue);
        return newValue;
    }

    default public int increment() {
        return this.add(1);
    }

    default public void reset() {
        this.set(0);
    }

    public boolean locked();

    public void unlock();

    public void lock();

    public @Nullable Component display();

    public void display(@Nullable Component var1);

    public void numberFormatOverride(@Nullable NumberFormat var1);
}

