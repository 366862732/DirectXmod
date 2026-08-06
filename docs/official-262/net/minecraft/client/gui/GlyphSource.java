/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public interface GlyphSource {
    public BakedGlyph getGlyph(int var1);

    public BakedGlyph getRandomGlyph(RandomSource var1, int var2);
}

