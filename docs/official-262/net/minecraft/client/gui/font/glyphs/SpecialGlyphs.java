/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public enum SpecialGlyphs implements GlyphInfo
{
    WHITE(() -> SpecialGlyphs.generate(5, 8, (x, y) -> -1)),
    MISSING(() -> {
        int width = 5;
        int height = 8;
        return SpecialGlyphs.generate(5, 8, (x, y) -> {
            boolean edge = x == 0 || x + 1 == 5 || y == 0 || y + 1 == 8;
            return edge ? -1 : 0;
        });
    });

    private final NativeImage image;

    private static NativeImage generate(int width, int height, PixelProvider pixelProvider) {
        NativeImage result = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result.setPixel(x, y, pixelProvider.getColor(x, y));
            }
        }
        result.untrack();
        return result;
    }

    private SpecialGlyphs(Supplier<NativeImage> image) {
        this.image = image.get();
    }

    @Override
    public float getAdvance() {
        return this.image.getWidth() + 1;
    }

    public @Nullable BakedSheetGlyph bake(GlyphStitcher stitcher) {
        return stitcher.stitch(this, new GlyphBitmap(this){
            final /* synthetic */ SpecialGlyphs this$0;
            {
                SpecialGlyphs specialGlyphs = this$0;
                Objects.requireNonNull(specialGlyphs);
                this.this$0 = specialGlyphs;
            }

            @Override
            public int getPixelWidth() {
                return this.this$0.image.getWidth();
            }

            @Override
            public int getPixelHeight() {
                return this.this$0.image.getHeight();
            }

            @Override
            public float getOversample() {
                return 1.0f;
            }

            @Override
            public void upload(int x, int y, GpuTexture texture) {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, this.this$0.image, 0, 0, x, y);
            }

            @Override
            public boolean isColored() {
                return true;
            }
        });
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface PixelProvider {
        public int getColor(int var1, int var2);
    }
}

