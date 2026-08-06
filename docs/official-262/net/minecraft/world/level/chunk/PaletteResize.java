/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

public interface PaletteResize<T> {
    public int onResize(int var1, T var2);

    public static <T> PaletteResize<T> noResizeExpected() {
        return (bits, lastAddedValue) -> {
            throw new IllegalArgumentException("Unexpected palette resize, bits = " + bits + ", added value = " + String.valueOf(lastAddedValue));
        };
    }
}

