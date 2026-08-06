/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class EmptyGlyph
implements UnbakedGlyph {
    private final GlyphInfo info;

    public EmptyGlyph(float advance) {
        this.info = GlyphInfo.simple(advance);
    }

    @Override
    public GlyphInfo info() {
        return this.info;
    }

    @Override
    public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
        return new BakedGlyph(this){
            final /* synthetic */ EmptyGlyph this$0;
            {
                EmptyGlyph emptyGlyph = this$0;
                Objects.requireNonNull(emptyGlyph);
                this.this$0 = emptyGlyph;
            }

            @Override
            public GlyphInfo info() {
                return this.this$0.info;
            }

            @Override
            public @Nullable TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
                return null;
            }
        };
    }
}

