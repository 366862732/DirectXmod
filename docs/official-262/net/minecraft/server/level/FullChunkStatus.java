/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

public enum FullChunkStatus {
    INACCESSIBLE,
    FULL,
    BLOCK_TICKING,
    ENTITY_TICKING;


    public boolean isOrAfter(FullChunkStatus step) {
        return this.ordinal() >= step.ordinal();
    }
}

