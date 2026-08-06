/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.world.level.ColorMapColorUtil;

public class GrassColor {
    private static int[] pixels = new int[65536];

    public static void init(int[] pixels) {
        GrassColor.pixels = pixels;
    }

    public static int get(double temp, double rain) {
        return ColorMapColorUtil.get(temp, rain, pixels, -65281);
    }

    public static int getDefaultColor() {
        return GrassColor.get(0.5, 1.0);
    }
}

