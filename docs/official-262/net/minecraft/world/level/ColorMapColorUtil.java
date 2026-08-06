/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

public interface ColorMapColorUtil {
    public static int get(double temp, double rain, int[] pixels, int defaultMapColor) {
        int y = (int)((1.0 - (rain *= temp)) * 255.0);
        int x = (int)((1.0 - temp) * 255.0);
        int index = y << 8 | x;
        if (index >= pixels.length) {
            return defaultMapColor;
        }
        return pixels[index];
    }
}

