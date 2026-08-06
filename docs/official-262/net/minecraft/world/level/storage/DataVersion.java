/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage;

import net.minecraft.SharedConstants;

public record DataVersion(int version, String series) {
    public static final String MAIN_SERIES = "main";

    public boolean isSideSeries() {
        return !this.series.equals(MAIN_SERIES);
    }

    public boolean isCompatible(DataVersion other) {
        if (SharedConstants.DEBUG_OPEN_INCOMPATIBLE_WORLDS) {
            return true;
        }
        return this.series().equals(other.series());
    }
}

