/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.platform;

import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
    public DisplayData withSize(int width, int height) {
        return new DisplayData(width, height, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
    }

    public DisplayData withFullscreen(boolean isFullscreen) {
        return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, isFullscreen);
    }
}

