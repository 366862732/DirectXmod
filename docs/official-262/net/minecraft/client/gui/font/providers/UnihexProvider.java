/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.bytes.ByteArrayList
 *  it.unimi.dsi.fastutil.bytes.ByteList
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.font.providers;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
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
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastBufferedInputStream;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class UnihexProvider
implements GlyphProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GLYPH_HEIGHT = 16;
    private static final int DIGITS_PER_BYTE = 2;
    private static final int DIGITS_FOR_WIDTH_8 = 32;
    private static final int DIGITS_FOR_WIDTH_16 = 64;
    private static final int DIGITS_FOR_WIDTH_24 = 96;
    private static final int DIGITS_FOR_WIDTH_32 = 128;
    private final CodepointMap<Glyph> glyphs;

    private UnihexProvider(CodepointMap<Glyph> glyphs) {
        this.glyphs = glyphs;
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        return this.glyphs.get(codepoint);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    @VisibleForTesting
    static void unpackBitsToBytes(IntBuffer output, int value, int left, int right) {
        int startBit = 32 - left - 1;
        int endBit = 32 - right - 1;
        for (int i = startBit; i >= endBit; --i) {
            if (i >= 32 || i < 0) {
                output.put(0);
                continue;
            }
            boolean isSet = (value >> i & 1) != 0;
            output.put(isSet ? -1 : 0);
        }
    }

    private static void unpackBitsToBytes(IntBuffer output, LineData data, int left, int right) {
        for (int i = 0; i < 16; ++i) {
            int line = data.line(i);
            UnihexProvider.unpackBitsToBytes(output, line, left, right);
        }
    }

    @VisibleForTesting
    static void readFromStream(InputStream input, ReaderOutput output) throws IOException {
        int line = 0;
        ByteArrayList buffer = new ByteArrayList(128);
        while (true) {
            boolean foundColon = UnihexProvider.copyUntil(input, (ByteList)buffer, 58);
            int codepointDigitCount = buffer.size();
            if (codepointDigitCount == 0 && !foundColon) break;
            if (!foundColon || codepointDigitCount != 4 && codepointDigitCount != 5 && codepointDigitCount != 6) {
                throw new IllegalArgumentException("Invalid entry at line " + line + ": expected 4, 5 or 6 hex digits followed by a colon");
            }
            int codepoint = 0;
            for (int i = 0; i < codepointDigitCount; ++i) {
                codepoint = codepoint << 4 | UnihexProvider.decodeHex(line, buffer.getByte(i));
            }
            buffer.clear();
            UnihexProvider.copyUntil(input, (ByteList)buffer, 10);
            int dataDigitCount = buffer.size();
            LineData contents = switch (dataDigitCount) {
                case 32 -> ByteContents.read(line, (ByteList)buffer);
                case 64 -> ShortContents.read(line, (ByteList)buffer);
                case 96 -> IntContents.read24(line, (ByteList)buffer);
                case 128 -> IntContents.read32(line, (ByteList)buffer);
                default -> throw new IllegalArgumentException("Invalid entry at line " + line + ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line");
            };
            output.accept(codepoint, contents);
            ++line;
            buffer.clear();
        }
    }

    private static int decodeHex(int line, ByteList input, int index) {
        return UnihexProvider.decodeHex(line, input.getByte(index));
    }

    private static int decodeHex(int line, byte b) {
        return switch (b) {
            case 48 -> 0;
            case 49 -> 1;
            case 50 -> 2;
            case 51 -> 3;
            case 52 -> 4;
            case 53 -> 5;
            case 54 -> 6;
            case 55 -> 7;
            case 56 -> 8;
            case 57 -> 9;
            case 65 -> 10;
            case 66 -> 11;
            case 67 -> 12;
            case 68 -> 13;
            case 69 -> 14;
            case 70 -> 15;
            default -> throw new IllegalArgumentException("Invalid entry at line " + line + ": expected hex digit, got " + (char)b);
        };
    }

    private static boolean copyUntil(InputStream input, ByteList output, int delimiter) throws IOException {
        int b;
        while ((b = input.read()) != -1) {
            if (b == delimiter) {
                return true;
            }
            output.add((byte)b);
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface LineData {
        public int line(int var1);

        public int bitWidth();

        default public int mask() {
            int mask = 0;
            for (int i = 0; i < 16; ++i) {
                mask |= this.line(i);
            }
            return mask;
        }

        default public int calculateWidth() {
            int right;
            int left;
            int mask = this.mask();
            int bitWidth = this.bitWidth();
            if (mask == 0) {
                left = 0;
                right = bitWidth;
            } else {
                left = Integer.numberOfLeadingZeros(mask);
                right = 32 - Integer.numberOfTrailingZeros(mask) - 1;
            }
            return Dimensions.pack(left, right);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ByteContents(byte[] contents) implements LineData
    {
        @Override
        public int line(int index) {
            return this.contents[index] << 24;
        }

        private static LineData read(int line, ByteList input) {
            byte[] content = new byte[16];
            int pos = 0;
            for (int i = 0; i < 16; ++i) {
                byte v;
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                content[i] = v = (byte)(n1 << 4 | n0);
            }
            return new ByteContents(content);
        }

        @Override
        public int bitWidth() {
            return 8;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ShortContents(short[] contents) implements LineData
    {
        @Override
        public int line(int index) {
            return this.contents[index] << 16;
        }

        private static LineData read(int line, ByteList input) {
            short[] content = new short[16];
            int pos = 0;
            for (int i = 0; i < 16; ++i) {
                short v;
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                content[i] = v = (short)(n3 << 12 | n2 << 8 | n1 << 4 | n0);
            }
            return new ShortContents(content);
        }

        @Override
        public int bitWidth() {
            return 16;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record IntContents(int[] contents, int bitWidth) implements LineData
    {
        private static final int SIZE_24 = 24;

        @Override
        public int line(int index) {
            return this.contents[index];
        }

        private static LineData read24(int line, ByteList input) {
            int[] content = new int[16];
            int mask = 0;
            int pos = 0;
            for (int i = 0; i < 16; ++i) {
                int n5 = UnihexProvider.decodeHex(line, input, pos++);
                int n4 = UnihexProvider.decodeHex(line, input, pos++);
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                int v = n5 << 20 | n4 << 16 | n3 << 12 | n2 << 8 | n1 << 4 | n0;
                content[i] = v << 8;
                mask |= v;
            }
            return new IntContents(content, 24);
        }

        public static LineData read32(int line, ByteList input) {
            int[] content = new int[16];
            int mask = 0;
            int pos = 0;
            for (int i = 0; i < 16; ++i) {
                int v;
                int n7 = UnihexProvider.decodeHex(line, input, pos++);
                int n6 = UnihexProvider.decodeHex(line, input, pos++);
                int n5 = UnihexProvider.decodeHex(line, input, pos++);
                int n4 = UnihexProvider.decodeHex(line, input, pos++);
                int n3 = UnihexProvider.decodeHex(line, input, pos++);
                int n2 = UnihexProvider.decodeHex(line, input, pos++);
                int n1 = UnihexProvider.decodeHex(line, input, pos++);
                int n0 = UnihexProvider.decodeHex(line, input, pos++);
                content[i] = v = n7 << 28 | n6 << 24 | n5 << 20 | n4 << 16 | n3 << 12 | n2 << 8 | n1 << 4 | n0;
                mask |= v;
            }
            return new IntContents(content, 32);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface ReaderOutput {
        public void accept(int var1, LineData var2);
    }

    @Environment(value=EnvType.CLIENT)
    private record Glyph(LineData contents, int left, int right) implements UnbakedGlyph
    {
        public int width() {
            return this.right - this.left + 1;
        }

        @Override
        public GlyphInfo info() {
            return new GlyphInfo(this){
                final /* synthetic */ Glyph this$0;
                {
                    Glyph glyph = this$0;
                    Objects.requireNonNull(glyph);
                    this.this$0 = glyph;
                }

                @Override
                public float getAdvance() {
                    return (float)this.this$0.width() / 2.0f + 1.0f;
                }

                @Override
                public float getShadowOffset() {
                    return 0.5f;
                }

                @Override
                public float getBoldOffset() {
                    return 0.5f;
                }
            };
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
                    return 2.0f;
                }

                @Override
                public int getPixelWidth() {
                    return this.this$0.width();
                }

                @Override
                public int getPixelHeight() {
                    return 16;
                }

                @Override
                public void upload(int x, int y, GpuTexture texture) {
                    IntBuffer targetBuffer = MemoryUtil.memAllocInt((int)(this.this$0.width() * 16));
                    UnihexProvider.unpackBitsToBytes(targetBuffer, this.this$0.contents, this.this$0.left, this.this$0.right);
                    targetBuffer.rewind();
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, MemoryUtil.memByteBuffer((IntBuffer)targetBuffer), 0, 0, x, y, this.this$0.width(), 16);
                    MemoryUtil.memFree((IntBuffer)targetBuffer);
                }

                @Override
                public boolean isColored() {
                    return true;
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Definition
    implements GlyphProviderDefinition {
        public static final MapCodec<Definition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("hex_file").forGetter(o -> o.hexFile), (App)OverrideRange.CODEC.listOf().optionalFieldOf("size_overrides", List.of()).forGetter(o -> o.sizeOverrides)).apply((Applicative)i, Definition::new));
        private final Identifier hexFile;
        private final List<OverrideRange> sizeOverrides;

        private Definition(Identifier hexFile, List<OverrideRange> sizeOverrides) {
            this.hexFile = hexFile;
            this.sizeOverrides = sizeOverrides;
        }

        @Override
        public GlyphProviderType type() {
            return GlyphProviderType.UNIHEX;
        }

        @Override
        public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
            return Either.left(this::load);
        }

        private GlyphProvider load(ResourceManager resourceManager) throws IOException {
            try (InputStream raw = resourceManager.open(this.hexFile);){
                UnihexProvider unihexProvider = this.loadData(raw);
                return unihexProvider;
            }
        }

        private UnihexProvider loadData(InputStream zipFile) throws IOException {
            CodepointMap<LineData> bits = new CodepointMap<LineData>(LineData[]::new, x$0 -> new LineData[x$0][]);
            ReaderOutput output = bits::put;
            try (ZipInputStream zis = new ZipInputStream(zipFile);){
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!name.endsWith(".hex")) continue;
                    LOGGER.info("Found {}, loading", (Object)name);
                    UnihexProvider.readFromStream(new FastBufferedInputStream(zis), output);
                }
                CodepointMap<Glyph> glyphs = new CodepointMap<Glyph>(Glyph[]::new, x$0 -> new Glyph[x$0][]);
                for (OverrideRange sizeOverride : this.sizeOverrides) {
                    int from = sizeOverride.from;
                    int to = sizeOverride.to;
                    Dimensions size = sizeOverride.dimensions;
                    for (int c = from; c <= to; ++c) {
                        LineData codepointBits = (LineData)bits.remove(c);
                        if (codepointBits == null) continue;
                        glyphs.put(c, new Glyph(codepointBits, size.left, size.right));
                    }
                }
                bits.forEach((codepoint, glyphBits) -> {
                    int packedSize = glyphBits.calculateWidth();
                    int left = Dimensions.left(packedSize);
                    int right = Dimensions.right(packedSize);
                    glyphs.put(codepoint, new Glyph((LineData)glyphBits, left, right));
                });
                UnihexProvider unihexProvider = new UnihexProvider(glyphs);
                return unihexProvider;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Dimensions(int left, int right) {
        public static final MapCodec<Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("left").forGetter(Dimensions::left), (App)Codec.INT.fieldOf("right").forGetter(Dimensions::right)).apply((Applicative)i, Dimensions::new));
        public static final Codec<Dimensions> CODEC = MAP_CODEC.codec();

        public int pack() {
            return Dimensions.pack(this.left, this.right);
        }

        public static int pack(int left, int right) {
            return (left & 0xFF) << 8 | right & 0xFF;
        }

        public static int left(int packed) {
            return (byte)(packed >> 8);
        }

        public static int right(int packed) {
            return (byte)packed;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record OverrideRange(int from, int to, Dimensions dimensions) {
        private static final Codec<OverrideRange> RAW_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.CODEPOINT.fieldOf("from").forGetter(OverrideRange::from), (App)ExtraCodecs.CODEPOINT.fieldOf("to").forGetter(OverrideRange::to), (App)Dimensions.MAP_CODEC.forGetter(OverrideRange::dimensions)).apply((Applicative)i, OverrideRange::new));
        public static final Codec<OverrideRange> CODEC = RAW_CODEC.validate(o -> {
            if (o.from >= o.to) {
                return DataResult.error(() -> "Invalid range: [" + o.from + ";" + o.to + "]");
            }
            return DataResult.success((Object)o);
        });
    }
}

