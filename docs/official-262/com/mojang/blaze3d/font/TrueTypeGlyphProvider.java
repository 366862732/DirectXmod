/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArraySet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.freetype.FT_Bitmap
 *  org.lwjgl.util.freetype.FT_Face
 *  org.lwjgl.util.freetype.FT_GlyphSlot
 *  org.lwjgl.util.freetype.FT_Vector
 *  org.lwjgl.util.freetype.FreeType
 */
package com.mojang.blaze3d.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@Environment(value=EnvType.CLIENT)
public class TrueTypeGlyphProvider
implements GlyphProvider {
    private @Nullable ByteBuffer fontMemory;
    private @Nullable FT_Face face;
    private final float oversample;
    private final CodepointMap<GlyphEntry> glyphs = new CodepointMap(GlyphEntry[]::new, x$0 -> new GlyphEntry[x$0][]);

    public TrueTypeGlyphProvider(ByteBuffer fontMemory, FT_Face face, float size, float oversample, float shiftX, float shiftY, String skip) {
        this.fontMemory = fontMemory;
        this.face = face;
        this.oversample = oversample;
        IntArraySet skipSet = new IntArraySet();
        skip.codePoints().forEach(arg_0 -> ((IntSet)skipSet).add(arg_0));
        int pixelsPerEm = Math.round(size * oversample);
        FreeType.FT_Set_Pixel_Sizes((FT_Face)face, (int)pixelsPerEm, (int)pixelsPerEm);
        float transformX = shiftX * oversample;
        float transformY = -shiftY * oversample;
        try (MemoryStack stack = MemoryStack.stackPush();){
            int index;
            FT_Vector vector = FreeTypeUtil.setVector(FT_Vector.malloc((MemoryStack)stack), transformX, transformY);
            FreeType.FT_Set_Transform((FT_Face)face, null, (FT_Vector)vector);
            IntBuffer indexPtr = stack.mallocInt(1);
            int codepoint = (int)FreeType.FT_Get_First_Char((FT_Face)face, (IntBuffer)indexPtr);
            while ((index = indexPtr.get(0)) != 0) {
                if (!skipSet.contains(codepoint)) {
                    this.glyphs.put(codepoint, new GlyphEntry(index));
                }
                codepoint = (int)FreeType.FT_Get_Next_Char((FT_Face)face, (long)codepoint, (IntBuffer)indexPtr);
            }
        }
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        GlyphEntry entry = this.glyphs.get(codepoint);
        return entry != null ? this.getOrLoadGlyphInfo(codepoint, entry) : null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private UnbakedGlyph getOrLoadGlyphInfo(int codepoint, GlyphEntry entry) {
        UnbakedGlyph result = entry.glyph;
        if (result == null) {
            FT_Face face;
            FT_Face fT_Face = face = this.validateFontOpen();
            synchronized (fT_Face) {
                result = entry.glyph;
                if (result == null) {
                    entry.glyph = result = this.loadGlyph(codepoint, face, entry.index);
                }
            }
        }
        return result;
    }

    private UnbakedGlyph loadGlyph(int codepoint, FT_Face face, int index) {
        FT_GlyphSlot glyph;
        int errorCode = FreeType.FT_Load_Glyph((FT_Face)face, (int)index, (int)0x400008);
        if (errorCode != 0) {
            FreeTypeUtil.assertError(errorCode, String.format(Locale.ROOT, "Loading glyph U+%06X", codepoint));
        }
        if ((glyph = face.glyph()) == null) {
            throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", codepoint));
        }
        float scaledAdvance = FreeTypeUtil.x(glyph.advance());
        FT_Bitmap bitmap = glyph.bitmap();
        int left = glyph.bitmap_left();
        int top = glyph.bitmap_top();
        int width = bitmap.width();
        int height = bitmap.rows();
        if (width <= 0 || height <= 0) {
            return new EmptyGlyph(scaledAdvance / this.oversample);
        }
        return new Glyph(this, left, top, width, height, scaledAdvance, index);
    }

    private FT_Face validateFontOpen() {
        if (this.fontMemory == null || this.face == null) {
            throw new IllegalStateException("Provider already closed");
        }
        return this.face;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        if (this.face != null) {
            Object object = FreeTypeUtil.LIBRARY_LOCK;
            synchronized (object) {
                FreeTypeUtil.checkError(FreeType.FT_Done_Face((FT_Face)this.face), "Deleting face");
            }
            this.face = null;
        }
        MemoryUtil.memFree((ByteBuffer)this.fontMemory);
        this.fontMemory = null;
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    @Environment(value=EnvType.CLIENT)
    private static class GlyphEntry {
        private final int index;
        private volatile @Nullable UnbakedGlyph glyph;

        private GlyphEntry(int index) {
            this.index = index;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class Glyph
    implements UnbakedGlyph {
        private final int width;
        private final int height;
        private final float bearingX;
        private final float bearingY;
        private final GlyphInfo info;
        private final int index;
        final /* synthetic */ TrueTypeGlyphProvider this$0;

        private Glyph(TrueTypeGlyphProvider trueTypeGlyphProvider, float left, float top, int width, int height, float advance, int index) {
            TrueTypeGlyphProvider trueTypeGlyphProvider2 = trueTypeGlyphProvider;
            Objects.requireNonNull(trueTypeGlyphProvider2);
            this.this$0 = trueTypeGlyphProvider2;
            this.width = width;
            this.height = height;
            this.info = GlyphInfo.simple(advance / trueTypeGlyphProvider.oversample);
            this.bearingX = left / trueTypeGlyphProvider.oversample;
            this.bearingY = top / trueTypeGlyphProvider.oversample;
            this.index = index;
        }

        @Override
        public GlyphInfo info() {
            return this.info;
        }

        @Override
        public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(this.info, new GlyphBitmap(this){
                final /* synthetic */ Glyph this$1;
                {
                    Glyph glyph = this$1;
                    Objects.requireNonNull(glyph);
                    this.this$1 = glyph;
                }

                @Override
                public int getPixelWidth() {
                    return this.this$1.width;
                }

                @Override
                public int getPixelHeight() {
                    return this.this$1.height;
                }

                @Override
                public float getOversample() {
                    return this.this$1.this$0.oversample;
                }

                @Override
                public float getBearingLeft() {
                    return this.this$1.bearingX;
                }

                @Override
                public float getBearingTop() {
                    return this.this$1.bearingY;
                }

                @Override
                public void upload(int x, int y, GpuTexture texture) {
                    FT_Face face = this.this$1.this$0.validateFontOpen();
                    try (NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, this.this$1.width, this.this$1.height, false);){
                        if (image.copyFromFont(face, this.this$1.index)) {
                            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image, 0, 0, x, y);
                        }
                    }
                }

                @Override
                public boolean isColored() {
                    return false;
                }
            });
        }
    }
}

