/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.jspecify.annotations.Nullable;

public record PlayerScoreEntry(String owner, int value, @Nullable Component display, @Nullable NumberFormat numberFormatOverride) {
    public boolean isHidden() {
        return this.owner.startsWith("#");
    }

    public Component ownerName() {
        if (this.display != null) {
            return this.display;
        }
        return Component.literal(this.owner());
    }

    public MutableComponent formatValue(NumberFormat _default) {
        return Objects.requireNonNullElse(this.numberFormatOverride, _default).format(this.value);
    }
}

