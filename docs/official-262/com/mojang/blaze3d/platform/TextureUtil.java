/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TextureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MIN_MIPMAP_LEVEL = 0;
    private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;
    private static final int[][] DIRECTIONS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static ByteBuffer readResource(InputStream inputStream) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        if (channel instanceof SeekableByteChannel) {
            SeekableByteChannel seekableChannel = (SeekableByteChannel)channel;
            return TextureUtil.readResource(channel, (int)seekableChannel.size() + 1);
        }
        return TextureUtil.readResource(channel, 8192);
    }

    private static ByteBuffer readResource(ReadableByteChannel channel, int expectedSize) throws IOException {
        ByteBuffer buffer = MemoryUtil.memAlloc((int)expectedSize);
        try {
            while (channel.read(buffer) != -1) {
                if (buffer.hasRemaining()) continue;
                buffer = MemoryUtil.memRealloc((ByteBuffer)buffer, (int)(buffer.capacity() * 2));
            }
            buffer.flip();
            return buffer;
        }
        catch (IOException e) {
            MemoryUtil.memFree((ByteBuffer)buffer);
            throw e;
        }
    }

    public static void writeAsPNG(Path dir, String prefix, GpuTexture texture, int maxMipLevel, IntUnaryOperator pixelModifier) {
        RenderSystem.assertOnRenderThread();
        long bufferLength = 0L;
        for (int i = 0; i <= maxMipLevel; ++i) {
            bufferLength += (long)texture.getFormat().blockSize() * (long)texture.getWidth(i) * (long)texture.getHeight(i);
        }
        if (bufferLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Exporting textures larger than 2GB is not supported");
        }
        if (texture.getFormat() != GpuFormat.RGBA8_UNORM) {
            throw new IllegalArgumentException("Exporting textures other than RGBA8_UNORM is not supported");
        }
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, bufferLength);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        Runnable onCopyComplete = () -> {
            try (GpuBufferSlice.MappedView read = buffer.map(true, false);){
                ByteBuffer data = read.data();
                IntUnaryOperator decodeTexel = data::getInt;
                int offset = 0;
                for (int i = 0; i <= maxMipLevel; ++i) {
                    int mipWidth = texture.getWidth(i);
                    int mipHeight = texture.getHeight(i);
                    try (NativeImage image = new NativeImage(mipWidth, mipHeight, false);){
                        for (int y = 0; y < mipHeight; ++y) {
                            for (int x = 0; x < mipWidth; ++x) {
                                int argb = decodeTexel.applyAsInt(offset + (x + y * mipWidth) * texture.getFormat().blockSize());
                                image.setPixelABGR(x, y, pixelModifier.applyAsInt(argb));
                            }
                        }
                        Path target = dir.resolve(prefix + "_" + i + ".png");
                        image.writeToFile(target);
                        LOGGER.debug("Exported png to: {}", (Object)target.toAbsolutePath());
                    }
                    catch (IOException e) {
                        LOGGER.debug("Unable to write: ", (Throwable)e);
                    }
                    offset += texture.getFormat().blockSize() * mipWidth * mipHeight;
                }
            }
            buffer.close();
        };
        AtomicInteger completedCopies = new AtomicInteger();
        int offset = 0;
        for (int i = 0; i <= maxMipLevel; ++i) {
            commandEncoder.copyTextureToBuffer(texture, buffer, offset, () -> {
                if (completedCopies.getAndIncrement() == maxMipLevel) {
                    onCopyComplete.run();
                }
            }, i);
            offset += texture.getFormat().blockSize() * texture.getWidth(i) * texture.getHeight(i);
        }
    }

    public static Path getDebugTexturePath(Path root) {
        return root.resolve("screenshots").resolve("debug");
    }

    public static Path getDebugTexturePath() {
        return TextureUtil.getDebugTexturePath(Path.of(".", new String[0]));
    }

    public static void solidify(NativeImage image) {
        int color;
        int y;
        int x;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] nearestColor = new int[width * height];
        int[] distances = new int[width * height];
        Arrays.fill(distances, Integer.MAX_VALUE);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        for (x = 0; x < width; ++x) {
            for (y = 0; y < height; ++y) {
                color = image.getPixel(x, y);
                if (ARGB.alpha(color) == 0) continue;
                int packedCoordinates = TextureUtil.pack(x, y, width);
                distances[packedCoordinates] = 0;
                nearestColor[packedCoordinates] = color;
                queue.enqueue(packedCoordinates);
            }
        }
        while (!queue.isEmpty()) {
            int packedCoordinates = queue.dequeueInt();
            int x2 = TextureUtil.x(packedCoordinates, width);
            int y2 = TextureUtil.y(packedCoordinates, width);
            for (int[] direction : DIRECTIONS) {
                int neighborX = x2 + direction[0];
                int neighborY = y2 + direction[1];
                int packedNeighborCoordinates = TextureUtil.pack(neighborX, neighborY, width);
                if (neighborX < 0 || neighborY < 0 || neighborX >= width || neighborY >= height || distances[packedNeighborCoordinates] <= distances[packedCoordinates] + 1) continue;
                distances[packedNeighborCoordinates] = distances[packedCoordinates] + 1;
                nearestColor[packedNeighborCoordinates] = nearestColor[packedCoordinates];
                queue.enqueue(packedNeighborCoordinates);
            }
        }
        for (x = 0; x < width; ++x) {
            for (y = 0; y < height; ++y) {
                color = image.getPixel(x, y);
                if (ARGB.alpha(color) == 0) {
                    image.setPixel(x, y, ARGB.color(0, nearestColor[TextureUtil.pack(x, y, width)]));
                    continue;
                }
                image.setPixel(x, y, color);
            }
        }
    }

    public static void fillEmptyAreasWithDarkColor(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int darkestColor = -1;
        int minBrightness = Integer.MAX_VALUE;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int blue;
                int green;
                int red;
                int brightness;
                int color = image.getPixel(x, y);
                int alpha = ARGB.alpha(color);
                if (alpha == 0 || (brightness = (red = ARGB.red(color)) + (green = ARGB.green(color)) + (blue = ARGB.blue(color))) >= minBrightness) continue;
                minBrightness = brightness;
                darkestColor = color;
            }
        }
        int darkRed = 3 * ARGB.red(darkestColor) / 4;
        int darkGreen = 3 * ARGB.green(darkestColor) / 4;
        int darkBlue = 3 * ARGB.blue(darkestColor) / 4;
        int darkenedColor = ARGB.color(0, darkRed, darkGreen, darkBlue);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int color = image.getPixel(x, y);
                if (ARGB.alpha(color) != 0) continue;
                image.setPixel(x, y, darkenedColor);
            }
        }
    }

    private static int pack(int x, int y, int width) {
        return x + y * width;
    }

    private static int x(int packed, int width) {
        return packed % width;
    }

    private static int y(int packed, int width) {
        return packed / width;
    }
}

