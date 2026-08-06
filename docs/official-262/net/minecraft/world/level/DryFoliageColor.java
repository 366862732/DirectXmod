/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.world.level.ColorMapColorUtil;

public class DryFoliageColor {
    public static final int FOLIAGE_DRY_DEFAULT = -10732494;
    private static int[] pixels = new int[65536];

    public static void init(int[] pixels) {
        DryFoliageColor.pixels = pixels;
    }

    public static int get(double temp, double rain) {
        return ColorMapColorUtil.get(temp, rain, pixels, -10732494);
    }
}

