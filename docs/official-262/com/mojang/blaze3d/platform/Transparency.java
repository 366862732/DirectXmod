/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record Transparency(boolean hasTransparent, boolean hasTranslucent) {
    public static final Transparency NONE = new Transparency(false, false);
    public static final Transparency TRANSPARENT = new Transparency(true, false);
    public static final Transparency TRANSLUCENT = new Transparency(false, true);
    public static final Transparency TRANSPARENT_AND_TRANSLUCENT = new Transparency(true, true);

    public static Transparency of(boolean hasTransparent, boolean hasTranslucent) {
        if (hasTransparent && hasTranslucent) {
            return TRANSPARENT_AND_TRANSLUCENT;
        }
        if (hasTransparent) {
            return TRANSPARENT;
        }
        if (hasTranslucent) {
            return TRANSLUCENT;
        }
        return NONE;
    }

    public Transparency or(Transparency other) {
        return Transparency.of(this.hasTransparent || other.hasTransparent, this.hasTranslucent || other.hasTranslucent);
    }

    public boolean isOpaque() {
        return !this.hasTransparent && !this.hasTranslucent;
    }
}

