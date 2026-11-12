package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immediate-mode command encoder.  D3D11 has no command lists on feature level
 * 10.x; every call executes on the device's immediate context.
 */
final class Dx11CommandEncoder implements CommandEncoder {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    private final Dx11Device device;
    private long presentCount;
    private long passCount;
    private long clearCount;

    Dx11CommandEncoder(Dx11Device device) {
        this.device = device;
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label, GpuTextureView colorView, OptionalInt clearColor) {
        return this.createRenderPass(label, colorView, clearColor, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorView,
        OptionalInt clearColor,
        @Nullable GpuTextureView depthView,
        OptionalDouble clearDepth) {
        if (colorView.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        }
        if (depthView != null && depthView.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        }
        final long pass = ++this.passCount;
        if (pass <= 20) {
            LOGGER.info("[dx11] render pass #{} '{}' {}x{} colorClear={} depth={}",
                pass, label.get(), colorView.getWidth(0), colorView.getHeight(0),
                clearColor.isPresent(), depthView != null);
        }
        long handle = Dx11Native.nativeCreateRenderPass(
            this.device.nativeHandle(),
            ((Dx11TextureView) colorView).handle(),
            depthView == null ? 0L : ((Dx11TextureView) depthView).handle(),
            clearColor.isPresent(),
            clearColor.orElse(0),
            depthView != null && clearDepth.isPresent(),
            clearDepth.orElse(0.0));
        return new Dx11RenderPass(this.device, handle);
    }

    @Override
    public void clearColorTexture(GpuTexture texture, int color) {
        this.check(texture, "clearColorTexture");
        this.logClear("clearColorTexture", texture, color);
        Dx11Native.nativeClearColorTexture(
            this.device.nativeHandle(), ((Dx11Texture) texture).handle(), color);
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture, int color, GpuTexture depthTexture, double depth) {
        this.check(colorTexture, "clearColorAndDepthTextures");
        this.check(depthTexture, "clearColorAndDepthTextures");
        this.logClear("clearColorAndDepthTextures", colorTexture, color);
        Dx11Native.nativeClearColorAndDepthTexture(
            this.device.nativeHandle(),
            ((Dx11Texture) colorTexture).handle(),
            color,
            ((Dx11Texture) depthTexture).handle(),
            depth);
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        int color,
        GpuTexture depthTexture,
        double depth,
        int x,
        int y,
        int width,
        int height) {
        this.check(colorTexture, "clearColorAndDepthTextures");
        this.check(depthTexture, "clearColorAndDepthTextures");
        this.logClear("clearColorAndDepthTexturesRegion", colorTexture, color);
        Dx11Native.nativeClearColorAndDepthTextureRegion(
            this.device.nativeHandle(),
            ((Dx11Texture) colorTexture).handle(),
            color,
            ((Dx11Texture) depthTexture).handle(),
            depth,
            x,
            y,
            width,
            height);
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double depth) {
        this.check(texture, "clearDepthTexture");
        this.logClear("clearDepthTexture", texture);
        Dx11Native.nativeClearDepthTexture(
            this.device.nativeHandle(), ((Dx11Texture) texture).handle(), depth);
    }

    @Override
    public void writeToBuffer(GpuBufferSlice slice, ByteBuffer data) {
        Dx11Buffer buffer = (Dx11Buffer) slice.buffer();
        if (data.remaining() > slice.length()) {
            throw new IllegalArgumentException(
                "Cannot write more data than the slice allows");
        }
        Dx11Native.nativeWriteToBuffer(
            buffer.handle(), slice.offset(), MemoryUtil.memAddress(data), data.remaining());
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write) {
        return ((Dx11Buffer) buffer).map(0L, buffer.size(), read, write);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write) {
        Dx11Buffer buffer = (Dx11Buffer) slice.buffer();
        return buffer.map(slice.offset(), slice.length(), read, write);
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice destination) {
        Dx11Native.nativeCopyBufferToBuffer(
            ((Dx11Buffer) source.buffer()).handle(),
            source.offset(),
            ((Dx11Buffer) destination.buffer()).handle(),
            destination.offset(),
            source.length());
    }

    @Override
    public void writeToTexture(GpuTexture texture, NativeImage image) {
        this.writeToTexture(
            texture, image, 0, 0, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        NativeImage image,
        int mipLevel,
        int depthOrLayer,
        int x,
        int y,
        int width,
        int height,
        int sourceX,
        int sourceY) {
        this.check(texture, "writeToTexture");
        int components = image.format().components();
        long address = image.getPointer()
            + ((long) sourceY * image.getWidth() + sourceX) * components;
        boolean ok = Dx11Native.nativeWriteToTexture(
            ((Dx11Texture) texture).handle(),
            mipLevel,
            depthOrLayer,
            x,
            y,
            width,
            height,
            address,
            image.getWidth() * components);
        if (!ok) {
            LOGGER.warn("[dx11] writeToTexture failed for {}", texture.getLabel());
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        ByteBuffer data,
        NativeImage.Format format,
        int mipLevel,
        int depthOrLayer,
        int x,
        int y,
        int width,
        int height) {
        this.check(texture, "writeToTexture");
        int components = format.components();
        if (width * height * components > data.remaining()) {
            throw new IllegalArgumentException("Copy would overrun the source buffer");
        }
        boolean ok = Dx11Native.nativeWriteToTexture(
            ((Dx11Texture) texture).handle(),
            mipLevel,
            depthOrLayer,
            x,
            y,
            width,
            height,
            MemoryUtil.memAddress(data),
            width * components);
        if (!ok) {
            LOGGER.warn("[dx11] writeToTexture(ByteBuffer) failed for {}", texture.getLabel());
        }
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture texture,
        GpuBuffer buffer,
        long bufferOffset,
        Runnable callback,
        int mipLevel) {
        this.copyTextureToBuffer(
            texture,
            buffer,
            bufferOffset,
            callback,
            mipLevel,
            0,
            0,
            texture.getWidth(mipLevel),
            texture.getHeight(mipLevel));
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture texture,
        GpuBuffer buffer,
        long bufferOffset,
        Runnable callback,
        int mipLevel,
        int x,
        int y,
        int width,
        int height) {
        this.check(texture, "copyTextureToBuffer");
        boolean ok = Dx11Native.nativeCopyTextureToBuffer(
            ((Dx11Texture) texture).handle(),
            ((Dx11Buffer) buffer).handle(),
            bufferOffset,
            mipLevel,
            x,
            y,
            width,
            height);
        if (!ok) {
            LOGGER.warn("[dx11] copyTextureToBuffer failed for {}", texture.getLabel());
        }
        callback.run();
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture source,
        GpuTexture destination,
        int mipLevel,
        int sourceX,
        int sourceY,
        int destinationX,
        int destinationY,
        int width,
        int height) {
        this.check(source, "copyTextureToTexture");
        this.check(destination, "copyTextureToTexture");
        boolean ok = Dx11Native.nativeCopyTextureToTexture(
            ((Dx11Texture) source).handle(),
            ((Dx11Texture) destination).handle(),
            mipLevel,
            sourceX,
            sourceY,
            destinationX,
            destinationY,
            width,
            height);
        if (!ok) {
            LOGGER.warn("[dx11] copyTextureToTexture failed");
        }
    }

    @Override
    public void presentTexture(GpuTextureView textureView) {
        if (textureView.isClosed()) {
            throw new IllegalStateException("Cannot present a closed texture view");
        }
        final long present = ++this.presentCount;
        if (present <= 5) {
            LOGGER.info("[dx11] presentTexture #{} '{}' {}x{} mips={}",
                present, textureView.texture().getLabel(),
                textureView.getWidth(0), textureView.getHeight(0), textureView.mipLevels());
        }
        String dumpDir = Dx11Device.debugDumpDir();
        if (present == 1 && dumpDir != null) {
            try {
                Dx11Native.nativeDumpTexture(
                    this.device.nativeHandle(),
                    ((Dx11Texture) textureView.texture()).handle(),
                    dumpDir + "/dx11-main-target.bmp");
                LOGGER.info("[dx11] Dumped main target to {}/dx11-main-target.bmp", dumpDir);
            } catch (RuntimeException e) {
                LOGGER.warn("[dx11] Could not dump the main target", e);
            }
        }
        boolean ok = Dx11Native.nativeBlitToBackBuffer(
            this.device.nativeHandle(), ((Dx11Texture) textureView.texture()).handle());
        if (!ok) {
            LOGGER.warn("[dx11] presentTexture failed");
        }
    }

    @Override
    public GpuFence createFence() {
        return new Dx11Fence(this.device);
    }

    @Override
    public GpuQuery timerQueryBegin() {
        return new Dx11Query(this.device);
    }

    @Override
    public void timerQueryEnd(GpuQuery query) {
        ((Dx11Query) query).end();
    }

    private void check(GpuTexture texture, String operation) {
        if (texture.isClosed()) {
            throw new IllegalStateException(operation + ": texture is closed");
        }
    }

    private void logClear(String operation, GpuTexture texture, int color) {
        final long clear = ++this.clearCount;
        if (clear <= 20) {
            LOGGER.info(
                "[dx11] clear #{} {} '{}' argb=#{}{}",
                clear, operation, texture.getLabel(),
                String.format("%08X", color),
                (color >>> 24) == 0xFF
                    ? String.format(" (r=%d g=%d b=%d)", (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
                    : "");
        }
    }

    private void logClear(String operation, GpuTexture texture) {
        final long clear = ++this.clearCount;
        if (clear <= 20) {
            LOGGER.info("[dx11] clear #{} {} '{}'", clear, operation, texture.getLabel());
        }
    }
}
