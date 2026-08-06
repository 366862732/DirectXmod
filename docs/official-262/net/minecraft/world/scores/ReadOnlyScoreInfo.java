/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import java.util.Objects;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.jspecify.annotations.Nullable;

public interface ReadOnlyScoreInfo {
    public int value();

    public boolean isLocked();

    public @Nullable NumberFormat numberFormat();

    default public MutableComponent formatValue(NumberFormat defaultFormat) {
        return Objects.requireNonNullElse(this.numberFormat(), defaultFormat).format(this.value());
    }
}

