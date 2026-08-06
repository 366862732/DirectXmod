/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  it.unimi.dsi.fastutil.ints.IntSets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class BitmapProvider
implements GlyphProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ImageDataHolder imageData;
    private final CodepointMap<Glyph> glyphs;

    private BitmapProvider(ImageDataHolder imageData, CodepointMap<Glyph> glyphs) {
        this.imageData = imageData;
        this.glyphs = glyphs;
    }

    @Override
    public void close() {
        this.imageData.close();
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        return this.glyphs.get(codepoint);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return IntSets.unmodifiable((IntSet)this.glyphs.keySet());
    }

    @Environment(value=EnvType.CLIENT)
    private static class ImageDataHolder
    implements AutoCloseable {
        private final Identifier identifier;
        private final NativeImage image;
        private @Nullable GpuBuffer gpuBuffer = null;

        private ImageDataHolder(Identifier identifier, NativeImage image) {
            this.identifier = identifier;
            this.image = image;
        }

        private GpuBuffer gpuData() {
            if (this.gpuBuffer == null) {
                ByteBuffer imageBytes = this.image.getPixelBytes();
                this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> String.valueOf(this.identifier) + " staging buffer", 22, imageBytes.remaining());
                try (GpuBufferSlice.MappedView mappedView = this.gpuBuffer.map(false, true);){
                    MemoryUtil.memCopy((ByteBuffer)imageBytes, (ByteBuffer)mappedView.data());
                }
            }
            return this.gpuBuffer;
        }

        @Override
        public void close() {
            this.image.close();
            if (this.gpuBuffer != null) {
                this.gpuBuffer.close();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record Glyph(float scale, ImageDataHolder imageData, int offsetX, int offsetY, int width, int height, int advance, int ascent) implements UnbakedGlyph
    {
        @Override
        public GlyphInfo info() {
            return GlyphInfo.simple(this.advance);
        }

        @Override
        public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(this.info(), new GlyphBitmap(this){
                final /* synthetic */ Glyph this$0;
                {
                    Glyph glyph = this$0;
                    Objects.requireNonNull(glyph);
                    this.this$0 = glyph;
                }

                @Override
                public float getOversample() {
                    return 1.0f / this.this$0.scale;
                }

                @Override
                public int getPixelWidth() {
                    return this.this$0.width;
                }

                @Override
                public int getPixelHeight() {
                    return this.this$0.height;
                }

                @Override
                public float getBearingTop() {
                    return this.this$0.ascent;
                }

                @Override
                public void upload(int x, int y, GpuTexture texture) {
                    RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(this.this$0.imageData.gpuData().slice(), this.this$0.offsetX, this.this$0.offsetY, this.this$0.imageData.image.getWidth(), this.this$0.imageData.image.getHeight(), texture, x, y, this.this$0.width, this.this$0.height, 0, 0);
                }

                @Override
                public boolean isColored() {
                    return this.this$0.imageData.image.format().components() > 1;
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Definition(Identifier file, int height, int ascent, int[][] codepointGrid) implements GlyphProviderDefinition
    {
        private static final Codec<int[][]> CODEPOINT_GRID_CODEC = Codec.STRING.listOf().xmap(input -> {
            int lineCount = input.size();
            int[][] result = new int[lineCount][];
            for (int i = 0; i < lineCount; ++i) {
                result[i] = ((String)input.get(i)).codePoints().toArray();
            }
            return result;
        }, grid -> {
            ArrayList<String> result = new ArrayList<String>(((int[][])grid).length);
            for (int[] line : grid) {
                result.add(new String(line, 0, line.length));
            }
            return result;
        }).validate(Definition::validateDimensions);
        public static final MapCodec<Definition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("file").forGetter(Definition::file), (App)Codec.INT.optionalFieldOf("height", (Object)8).forGetter(Definition::height), (App)Codec.INT.fieldOf("ascent").forGetter(Definition::ascent), (App)CODEPOINT_GRID_CODEC.fieldOf("chars").forGetter(Definition::codepointGrid)).apply((Applicative)i, Definition::new)).validate(Definition::validate);

        private static DataResult<int[][]> validateDimensions(int[][] grid) {
            int lineCount = grid.length;
            if (lineCount == 0) {
                return DataResult.error(() -> "Expected to find data in codepoint grid");
            }
            int[] firstLine = grid[0];
            int lineWidth = firstLine.length;
            if (lineWidth == 0) {
                return DataResult.error(() -> "Expected to find data in codepoint grid");
            }
            for (int i = 1; i < lineCount; ++i) {
                int[] line = grid[i];
                if (line.length == lineWidth) continue;
                return DataResult.error(() -> "Lines in codepoint grid have to be the same length (found: " + line.length + " codepoints, expected: " + lineWidth + "), pad with \\u0000");
            }
            return DataResult.success((Object)grid);
        }

        private static DataResult<Definition> validate(Definition builder) {
            if (builder.ascent > builder.height) {
                return DataResult.error(() -> "Ascent " + builder.ascent + " higher than height " + builder.height);
            }
            return DataResult.success((Object)builder);
        }

        @Override
        public GlyphProviderType type() {
            return GlyphProviderType.BITMAP;
        }

        @Override
        public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
            return Either.left(this::load);
        }

        private GlyphProvider load(ResourceManager resourceManager) throws IOException {
            Identifier texture = this.file.withPrefix("textures/");
            try (InputStream resource = resourceManager.open(texture);){
                NativeImage image = NativeImage.read(NativeImage.Format.RGBA, resource);
                int w = image.getWidth();
                int h = image.getHeight();
                int glyphWidth = w / this.codepointGrid[0].length;
                int glyphHeight = h / this.codepointGrid.length;
                float pixelScale = (float)this.height / (float)glyphHeight;
                CodepointMap<Glyph> charMap = new CodepointMap<Glyph>(Glyph[]::new, x$0 -> new Glyph[x$0][]);
                ImageDataHolder imageDataHolder = new ImageDataHolder(texture, image);
                for (int slotY = 0; slotY < this.codepointGrid.length; ++slotY) {
                    int linePos = 0;
                    for (int c : this.codepointGrid[slotY]) {
                        int actualGlyphWidth;
                        Glyph prev;
                        int slotX = linePos++;
                        if (c == 0 || (prev = charMap.put(c, new Glyph(pixelScale, imageDataHolder, slotX * glyphWidth, slotY * glyphHeight, glyphWidth, glyphHeight, (int)(0.5 + (double)((float)(actualGlyphWidth = this.getActualGlyphWidth(image, glyphWidth, glyphHeight, slotX, slotY)) * pixelScale)) + 1, this.ascent))) == null) continue;
                        LOGGER.warn("Codepoint '{}' declared multiple times in {}", (Object)Integer.toHexString(c), (Object)texture);
                    }
                }
                BitmapProvider bitmapProvider = new BitmapProvider(imageDataHolder, charMap);
                return bitmapProvider;
            }
        }

        private int getActualGlyphWidth(NativeImage image, int glyphWidth, int glyphHeight, int xGlyph, int yGlyph) {
            int width;
            for (width = glyphWidth - 1; width >= 0; --width) {
                int xPixel = xGlyph * glyphWidth + width;
                for (int y = 0; y < glyphHeight; ++y) {
                    int yPixel = yGlyph * glyphHeight + y;
                    if (image.getLuminanceOrAlpha(xPixel, yPixel) == 0) continue;
                    return width + 1;
                }
            }
            return width + 1;
        }
    }
}

