/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GlyphStitcher
implements AutoCloseable {
    private final TextureManager textureManager;
    private final Identifier texturePrefix;
    private final List<FontTexture> textures = new ArrayList<FontTexture>();

    public GlyphStitcher(TextureManager textureManager, Identifier texturePrefix) {
        this.textureManager = textureManager;
        this.texturePrefix = texturePrefix;
    }

    public void reset() {
        int textureCount = this.textures.size();
        this.textures.clear();
        for (int i = 0; i < textureCount; ++i) {
            this.textureManager.release(this.textureName(i));
        }
    }

    @Override
    public void close() {
        this.reset();
    }

    public @Nullable BakedSheetGlyph stitch(GlyphInfo info, GlyphBitmap glyphBitmap) {
        for (FontTexture texture : this.textures) {
            BakedSheetGlyph glyph = texture.add(info, glyphBitmap);
            if (glyph == null) continue;
            return glyph;
        }
        int nextIndex = this.textures.size();
        Identifier name = this.textureName(nextIndex);
        boolean isColored = glyphBitmap.isColored();
        GlyphRenderTypes renderTypes = isColored ? GlyphRenderTypes.createForColorTexture(name) : GlyphRenderTypes.createForGrayscaleTexture(name);
        FontTexture texture = new FontTexture(name::toString, renderTypes, isColored);
        this.textures.add(texture);
        this.textureManager.register(name, texture);
        return texture.add(info, glyphBitmap);
    }

    private Identifier textureName(int index) {
        return this.texturePrefix.withSuffix("/" + index);
    }
}

