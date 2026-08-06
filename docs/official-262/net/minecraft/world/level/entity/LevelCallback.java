/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.entity;

public interface LevelCallback<T> {
    public void onCreated(T var1);

    public void onDestroyed(T var1);

    public void onTickingStart(T var1);

    public void onTickingEnd(T var1);

    public void onTrackingStart(T var1);

    public void onTrackingEnd(T var1);

    public void onSectionChange(T var1);
}

