/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.MemoryPool
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.IOUtils
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.stb.STBIWriteCallback
 *  org.lwjgl.stb.STBImage
 *  org.lwjgl.stb.STBImageResize
 *  org.lwjgl.stb.STBImageWrite
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.freetype.FT_Bitmap
 *  org.lwjgl.util.freetype.FT_Face
 *  org.lwjgl.util.freetype.FT_GlyphSlot
 *  org.lwjgl.util.freetype.FreeType
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.ARGB;
import net.minecraft.util.PngInfo;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public final class NativeImage
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool((String)"NativeImage");
    private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    private final Format format;
    private final int width;
    private final int height;
    private final boolean useStbFree;
    private long pixels;
    private final long size;

    public NativeImage(int width, int height, boolean zero) {
        this(Format.RGBA, width, height, zero);
    }

    public NativeImage(Format format, int width, int height, boolean zero) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
        }
        this.format = format;
        this.width = width;
        this.height = height;
        this.size = (long)width * (long)height * (long)format.components();
        this.useStbFree = false;
        this.pixels = zero ? MemoryUtil.nmemCalloc((long)1L, (long)this.size) : MemoryUtil.nmemAlloc((long)this.size);
        MEMORY_POOL.malloc(this.pixels, (int)this.size);
        if (this.pixels == 0L) {
            throw new IllegalStateException("Unable to allocate texture of size " + width + "x" + height + " (" + format.components() + " channels)");
        }
    }

    public NativeImage(Format format, int width, int height, boolean useStbFree, long pixels) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
        }
        this.format = format;
        this.width = width;
        this.height = height;
        this.useStbFree = useStbFree;
        this.pixels = pixels;
        this.size = (long)width * (long)height * (long)format.components();
    }

    public String toString() {
        return "NativeImage[" + String.valueOf((Object)this.format) + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
    }

    private boolean isOutsideBounds(int x, int y) {
        return x < 0 || x >= this.width || y < 0 || y >= this.height;
    }

    public static NativeImage read(InputStream inputStream) throws IOException {
        return NativeImage.read(Format.RGBA, inputStream);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static NativeImage read(@Nullable Format format, InputStream inputStream) throws IOException {
        ByteBuffer file = null;
        try {
            file = TextureUtil.readResource(inputStream);
            NativeImage nativeImage = NativeImage.read(format, file);
            return nativeImage;
        }
        finally {
            MemoryUtil.memFree((ByteBuffer)file);
            IOUtils.closeQuietly((InputStream)inputStream);
        }
    }

    public static NativeImage read(ByteBuffer bytes) throws IOException {
        return NativeImage.read(Format.RGBA, bytes);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static NativeImage read(byte[] bytes) throws IOException {
        MemoryStack memoryStack = MemoryStack.stackGet();
        int bytesAvailable = memoryStack.getPointer();
        if (bytesAvailable < bytes.length) {
            ByteBuffer buffer = MemoryUtil.memAlloc((int)bytes.length);
            try {
                NativeImage nativeImage = NativeImage.putAndRead(buffer, bytes);
                return nativeImage;
            }
            finally {
                MemoryUtil.memFree((ByteBuffer)buffer);
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush();){
            ByteBuffer buffer = stack.malloc(bytes.length);
            NativeImage nativeImage = NativeImage.putAndRead(buffer, bytes);
            return nativeImage;
        }
    }

    private static NativeImage putAndRead(ByteBuffer nativeBuffer, byte[] bytes) throws IOException {
        nativeBuffer.put(bytes);
        nativeBuffer.rewind();
        return NativeImage.read(nativeBuffer);
    }

    public static NativeImage read(@Nullable Format format, ByteBuffer bytes) throws IOException {
        if (format != null && !format.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to read format " + String.valueOf((Object)format));
        }
        if (MemoryUtil.memAddress((ByteBuffer)bytes) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        }
        PngInfo.validateHeader(bytes);
        try (MemoryStack stack = MemoryStack.stackPush();){
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory((ByteBuffer)bytes, (IntBuffer)w, (IntBuffer)h, (IntBuffer)comp, (int)(format == null ? 0 : format.components));
            if (pixels == null) {
                throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }
            long address = MemoryUtil.memAddress((ByteBuffer)pixels);
            MEMORY_POOL.malloc(address, pixels.limit());
            NativeImage nativeImage = new NativeImage(format == null ? Format.getStbFormat(comp.get(0)) : format, w.get(0), h.get(0), true, address);
            return nativeImage;
        }
    }

    private void checkAllocated() {
        if (this.pixels == 0L) {
            throw new IllegalStateException("Image is not allocated.");
        }
    }

    @Override
    public void close() {
        if (this.pixels != 0L) {
            if (this.useStbFree) {
                STBImage.nstbi_image_free((long)this.pixels);
            } else {
                MemoryUtil.nmemFree((long)this.pixels);
            }
            MEMORY_POOL.free(this.pixels);
        }
        this.pixels = 0L;
    }

    public boolean isClosed() {
        return this.pixels == 0L;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Format format() {
        return this.format;
    }

    private int getPixelABGR(int x, int y) {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", new Object[]{this.format}));
        }
        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }
        this.checkAllocated();
        long offset = ((long)x + (long)y * (long)this.width) * 4L;
        return MemoryUtil.memGetInt((long)(this.pixels + offset));
    }

    public int getPixel(int x, int y) {
        return ARGB.fromABGR(this.getPixelABGR(x, y));
    }

    public void setPixelABGR(int x, int y, int pixel) {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", new Object[]{this.format}));
        }
        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }
        this.checkAllocated();
        long offset = ((long)x + (long)y * (long)this.width) * 4L;
        MemoryUtil.memPutInt((long)(this.pixels + offset), (int)pixel);
    }

    public void setPixel(int x, int y, int pixel) {
        this.setPixelABGR(x, y, ARGB.toABGR(pixel));
    }

    public NativeImage mappedCopy(IntUnaryOperator function) {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", new Object[]{this.format}));
        }
        this.checkAllocated();
        NativeImage result = new NativeImage(this.width, this.height, false);
        int pixelCount = this.width * this.height;
        IntBuffer sourceBuffer = MemoryUtil.memIntBuffer((long)this.pixels, (int)pixelCount);
        IntBuffer targetBuffer = MemoryUtil.memIntBuffer((long)result.pixels, (int)pixelCount);
        for (int i = 0; i < pixelCount; ++i) {
            int pixel = ARGB.fromABGR(sourceBuffer.get(i));
            int modified = function.applyAsInt(pixel);
            targetBuffer.put(i, ARGB.toABGR(modified));
        }
        return result;
    }

    public int[] getPixelsABGR() {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixels only works on RGBA images; have %s", new Object[]{this.format}));
        }
        this.checkAllocated();
        int[] result = new int[this.width * this.height];
        MemoryUtil.memIntBuffer((long)this.pixels, (int)(this.width * this.height)).get(result);
        return result;
    }

    public int[] getPixels() {
        int[] result = this.getPixelsABGR();
        for (int i = 0; i < result.length; ++i) {
            result[i] = ARGB.fromABGR(result[i]);
        }
        return result;
    }

    public byte getLuminanceOrAlpha(int x, int y) {
        if (!this.format.hasLuminanceOrAlpha()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", new Object[]{this.format}));
        }
        if (this.isOutsideBounds(x, y)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }
        int offset = (x + y * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
        return MemoryUtil.memGetByte((long)(this.pixels + (long)offset));
    }

    @Deprecated
    public int[] makePixelArray() {
        if (this.format != Format.RGBA) {
            throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
        }
        this.checkAllocated();
        int[] pixels = new int[this.getWidth() * this.getHeight()];
        for (int y = 0; y < this.getHeight(); ++y) {
            for (int x = 0; x < this.getWidth(); ++x) {
                pixels[x + y * this.getWidth()] = this.getPixel(x, y);
            }
        }
        return pixels;
    }

    public void writeToFile(File file) throws IOException {
        this.writeToFile(file.toPath());
    }

    public boolean copyFromFont(FT_Face face, int index) {
        if (this.format.components() != 1) {
            throw new IllegalArgumentException("Can only write fonts into 1-component images.");
        }
        if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph((FT_Face)face, (int)index, (int)4), "Loading glyph")) {
            return false;
        }
        FT_GlyphSlot glyph = Objects.requireNonNull(face.glyph(), "Glyph not initialized");
        FT_Bitmap bitmap = glyph.bitmap();
        if (bitmap.pixel_mode() != 2) {
            throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
        }
        if (bitmap.width() != this.getWidth() || bitmap.rows() != this.getHeight()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Glyph bitmap of size %sx%s does not match image of size: %sx%s", bitmap.width(), bitmap.rows(), this.getWidth(), this.getHeight()));
        }
        int bufferSize = bitmap.width() * bitmap.rows();
        ByteBuffer buffer = Objects.requireNonNull(bitmap.buffer(bufferSize), "Glyph has no bitmap");
        MemoryUtil.memCopy((long)MemoryUtil.memAddress((ByteBuffer)buffer), (long)this.pixels, (long)bufferSize);
        return true;
    }

    public void writeToFile(Path file) throws IOException {
        if (!this.format.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to write format " + String.valueOf((Object)this.format));
        }
        this.checkAllocated();
        try (SeekableByteChannel out = Files.newByteChannel(file, OPEN_OPTIONS, new FileAttribute[0]);){
            if (!this.writeToChannel(out)) {
                throw new IOException("Could not write image to the PNG file \"" + String.valueOf(file.toAbsolutePath()) + "\": " + STBImage.stbi_failure_reason());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean writeToChannel(WritableByteChannel output) throws IOException {
        WriteCallback writer = new WriteCallback(output);
        try {
            int height = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
            if (height < this.getHeight()) {
                LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", (Object)this.getHeight(), (Object)height);
            }
            if (STBImageWrite.nstbi_write_png_to_func((long)writer.address(), (long)0L, (int)this.getWidth(), (int)height, (int)this.format.components(), (long)this.pixels, (int)0) == 0) {
                boolean bl = false;
                return bl;
            }
            writer.throwIfException();
            boolean bl = true;
            return bl;
        }
        finally {
            writer.free();
        }
    }

    public void copyFrom(NativeImage from) {
        if (from.format() != this.format) {
            throw new UnsupportedOperationException("Image formats don't match.");
        }
        int components = this.format.components();
        this.checkAllocated();
        from.checkAllocated();
        if (this.width == from.width) {
            MemoryUtil.memCopy((long)from.pixels, (long)this.pixels, (long)Math.min(this.size, from.size));
        } else {
            int minWidth = Math.min(this.getWidth(), from.getWidth());
            int minHeight = Math.min(this.getHeight(), from.getHeight());
            for (int y = 0; y < minHeight; ++y) {
                int fromOffset = y * from.getWidth() * components;
                int toOffset = y * this.getWidth() * components;
                MemoryUtil.memCopy((long)(from.pixels + (long)fromOffset), (long)(this.pixels + (long)toOffset), (long)minWidth);
            }
        }
    }

    public void fillRect(int xs, int ys, int width, int height, int pixel) {
        for (int y = ys; y < ys + height; ++y) {
            for (int x = xs; x < xs + width; ++x) {
                this.setPixel(x, y, pixel);
            }
        }
    }

    public void copyRect(int startX, int startY, int offsetX, int offsetY, int sizeX, int sizeY, boolean swapX, boolean swapY) {
        this.copyRect(this, startX, startY, startX + offsetX, startY + offsetY, sizeX, sizeY, swapX, swapY);
    }

    public void copyRect(NativeImage target, int sourceX, int sourceY, int targetX, int targetY, int sizeX, int sizeY, boolean swapX, boolean swapY) {
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                int dx = swapX ? sizeX - 1 - x : x;
                int dy = swapY ? sizeY - 1 - y : y;
                int source = this.getPixelABGR(sourceX + x, sourceY + y);
                target.setPixelABGR(targetX + dx, targetY + dy, source);
            }
        }
    }

    public void resizeSubRectTo(int sourceX, int sourceY, int sizeX, int sizeY, NativeImage to) {
        this.checkAllocated();
        if (to.format() != this.format) {
            throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
        }
        int components = this.format.components();
        STBImageResize.nstbir_resize_uint8_linear((long)(this.pixels + (long)((sourceX + sourceY * this.getWidth()) * components)), (int)sizeX, (int)sizeY, (int)(this.getWidth() * components), (long)to.pixels, (int)to.getWidth(), (int)to.getHeight(), (int)0, (int)components);
    }

    public void untrack() {
        DebugMemoryUntracker.untrack(this.pixels);
    }

    public long getPointer() {
        return this.pixels;
    }

    public ByteBuffer getPixelBytes() {
        return MemoryUtil.memByteBuffer((long)this.pixels, (int)Math.toIntExact(this.size));
    }

    public Transparency computeTransparency(int x0, int y0, int x1, int y1) {
        this.checkAllocated();
        if (this.format != Format.RGBA) {
            return Transparency.NONE;
        }
        if (x0 < 0 || y0 < 0 || x1 > this.width || y1 > this.height) {
            throw new IllegalArgumentException("Cannot compute translucency out of bounds: [" + x0 + ", " + y0 + ", " + x1 + ", " + y1 + "] in " + this.width + "x" + this.height + " image");
        }
        if ((long)this.width * (long)this.height * 4L > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image of size " + this.width + "x" + this.height + " is too large to compute translucency");
        }
        boolean hasTransparentPixel = false;
        boolean hasTranslucentPixel = false;
        IntBuffer buffer = MemoryUtil.memIntBuffer((long)this.pixels, (int)(this.width * this.height));
        for (int y = y0; y < y1; ++y) {
            for (int x = x0; x < x1; ++x) {
                int alpha = ARGB.alpha(buffer.get(x + y * this.width));
                if (alpha == 0) {
                    hasTransparentPixel = true;
                    continue;
                }
                if (alpha == 255) continue;
                hasTranslucentPixel = true;
            }
        }
        return Transparency.of(hasTransparentPixel, hasTranslucentPixel);
    }

    public Transparency computeTransparency() {
        return this.computeTransparency(0, 0, this.width, this.height);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Format {
        RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
        RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
        LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
        LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

        private final int components;
        private final boolean hasRed;
        private final boolean hasGreen;
        private final boolean hasBlue;
        private final boolean hasLuminance;
        private final boolean hasAlpha;
        private final int redOffset;
        private final int greenOffset;
        private final int blueOffset;
        private final int luminanceOffset;
        private final int alphaOffset;
        private final boolean supportedByStb;

        private Format(int components, boolean hasRed, boolean hasGreen, boolean hasBlue, boolean hasLuminance, boolean hasAlpha, int redOffset, int greenOffset, int blueOffset, int luminanceOffset, int alphaOffset, boolean supportedByStb) {
            this.components = components;
            this.hasRed = hasRed;
            this.hasGreen = hasGreen;
            this.hasBlue = hasBlue;
            this.hasLuminance = hasLuminance;
            this.hasAlpha = hasAlpha;
            this.redOffset = redOffset;
            this.greenOffset = greenOffset;
            this.blueOffset = blueOffset;
            this.luminanceOffset = luminanceOffset;
            this.alphaOffset = alphaOffset;
            this.supportedByStb = supportedByStb;
        }

        public int components() {
            return this.components;
        }

        public boolean hasRed() {
            return this.hasRed;
        }

        public boolean hasGreen() {
            return this.hasGreen;
        }

        public boolean hasBlue() {
            return this.hasBlue;
        }

        public boolean hasLuminance() {
            return this.hasLuminance;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }

        public int redOffset() {
            return this.redOffset;
        }

        public int greenOffset() {
            return this.greenOffset;
        }

        public int blueOffset() {
            return this.blueOffset;
        }

        public int luminanceOffset() {
            return this.luminanceOffset;
        }

        public int alphaOffset() {
            return this.alphaOffset;
        }

        public boolean hasLuminanceOrRed() {
            return this.hasLuminance || this.hasRed;
        }

        public boolean hasLuminanceOrGreen() {
            return this.hasLuminance || this.hasGreen;
        }

        public boolean hasLuminanceOrBlue() {
            return this.hasLuminance || this.hasBlue;
        }

        public boolean hasLuminanceOrAlpha() {
            return this.hasLuminance || this.hasAlpha;
        }

        public int luminanceOrRedOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.redOffset;
        }

        public int luminanceOrGreenOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
        }

        public int luminanceOrBlueOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
        }

        public int luminanceOrAlphaOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
        }

        public boolean supportedByStb() {
            return this.supportedByStb;
        }

        private static Format getStbFormat(int i) {
            switch (i) {
                case 1: {
                    return LUMINANCE;
                }
                case 2: {
                    return LUMINANCE_ALPHA;
                }
                case 3: {
                    return RGB;
                }
            }
            return RGBA;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class WriteCallback
    extends STBIWriteCallback {
        private final WritableByteChannel output;
        private @Nullable IOException exception;

        private WriteCallback(WritableByteChannel output) {
            this.output = output;
        }

        public void invoke(long context, long data, int size) {
            ByteBuffer dataBuf = WriteCallback.getData((long)data, (int)size);
            try {
                this.output.write(dataBuf);
            }
            catch (IOException e) {
                this.exception = e;
            }
        }

        public void throwIfException() throws IOException {
            if (this.exception != null) {
                throw this.exception;
            }
        }
    }
}

