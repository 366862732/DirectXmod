/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public record SingleSpriteSource(BakedGlyph glyph) implements GlyphSource
{
    @Override
    public BakedGlyph getGlyph(int codepoint) {
        return this.glyph;
    }

    @Override
    public BakedGlyph getRandomGlyph(RandomSource random, int width) {
        return this.glyph;
    }
}

