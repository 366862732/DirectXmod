/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

@Environment(value=EnvType.CLIENT)
public interface UnbakedGlyph {
    public GlyphInfo info();

    public BakedGlyph bake(Stitcher var1);

    @Environment(value=EnvType.CLIENT)
    public static interface Stitcher {
        public BakedGlyph stitch(GlyphInfo var1, GlyphBitmap var2);

        public BakedGlyph getMissing();
    }
}

