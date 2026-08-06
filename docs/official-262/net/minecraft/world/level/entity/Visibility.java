/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.entity;

import net.minecraft.server.level.FullChunkStatus;

public enum Visibility {
    HIDDEN(false, false),
    TRACKED(true, false),
    TICKING(true, true);

    private final boolean accessible;
    private final boolean ticking;

    private Visibility(boolean accessible, boolean ticking) {
        this.accessible = accessible;
        this.ticking = ticking;
    }

    public boolean isTicking() {
        return this.ticking;
    }

    public boolean isAccessible() {
        return this.accessible;
    }

    public static Visibility fromFullChunkStatus(FullChunkStatus status) {
        if (status.isOrAfter(FullChunkStatus.ENTITY_TICKING)) {
            return TICKING;
        }
        if (status.isOrAfter(FullChunkStatus.FULL)) {
            return TRACKED;
        }
        return HIDDEN;
    }
}

