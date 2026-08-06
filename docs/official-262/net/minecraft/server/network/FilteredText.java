/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.network;

import java.util.Objects;
import net.minecraft.network.chat.FilterMask;
import org.jspecify.annotations.Nullable;

public record FilteredText(String raw, FilterMask mask) {
    public static final FilteredText EMPTY = FilteredText.passThrough("");

    public static FilteredText passThrough(String message) {
        return new FilteredText(message, FilterMask.PASS_THROUGH);
    }

    public static FilteredText fullyFiltered(String message) {
        return new FilteredText(message, FilterMask.FULLY_FILTERED);
    }

    public @Nullable String filtered() {
        return this.mask.apply(this.raw);
    }

    public String filteredOrEmpty() {
        return Objects.requireNonNullElse(this.filtered(), "");
    }

    public boolean isFiltered() {
        return !this.mask.isEmpty();
    }
}

