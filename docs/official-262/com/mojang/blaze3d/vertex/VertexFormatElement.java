/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.GpuFormat;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record VertexFormatElement(String name, int offset, GpuFormat format) {
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s offset:%d", new Object[]{this.name, this.format, this.offset});
    }
}

