/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.ticks;

import com.mojang.serialization.Codec;

public enum TickPriority {
    EXTREMELY_HIGH(-3),
    VERY_HIGH(-2),
    HIGH(-1),
    NORMAL(0),
    LOW(1),
    VERY_LOW(2),
    EXTREMELY_LOW(3);

    public static final Codec<TickPriority> CODEC;
    private final int value;

    private TickPriority(int value) {
        this.value = value;
    }

    public static TickPriority byValue(int value) {
        for (TickPriority priority : TickPriority.values()) {
            if (priority.value != value) continue;
            return priority;
        }
        if (value < TickPriority.EXTREMELY_HIGH.value) {
            return EXTREMELY_HIGH;
        }
        return EXTREMELY_LOW;
    }

    public int getValue() {
        return this.value;
    }

    static {
        CODEC = Codec.INT.xmap(TickPriority::byValue, TickPriority::getValue);
    }
}

