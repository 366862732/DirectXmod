/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public interface Nameable {
    public Component getName();

    default public String getPlainTextName() {
        return this.getName().getString();
    }

    default public boolean hasCustomName() {
        return this.getCustomName() != null;
    }

    default public Component getDisplayName() {
        return this.getName();
    }

    default public @Nullable Component getCustomName() {
        return null;
    }
}

