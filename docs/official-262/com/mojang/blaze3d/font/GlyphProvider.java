/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.font;

import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.FontOption;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface GlyphProvider
extends AutoCloseable {
    public static final float BASELINE = 7.0f;

    @Override
    default public void close() {
    }

    default public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        return null;
    }

    public IntSet getSupportedGlyphs();

    @Environment(value=EnvType.CLIENT)
    public record Conditional(GlyphProvider provider, FontOption.Filter filter) implements AutoCloseable
    {
        @Override
        public void close() {
            this.provider.close();
        }
    }
}

