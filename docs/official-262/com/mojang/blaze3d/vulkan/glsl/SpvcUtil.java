/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.vulkan.glsl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class SpvcUtil {
    private SpvcUtil() {
    }

    public static String imageDimensionToString(int dimension) {
        return switch (dimension) {
            case 0 -> "1D";
            case 1 -> "2D";
            case 2 -> "3D";
            case 3 -> "Cube";
            case 4 -> "Rect";
            case 5 -> "Buffer";
            case 6 -> "SubpassData";
            default -> "0x" + Integer.toHexString(dimension);
        };
    }
}

