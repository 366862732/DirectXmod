package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jspecify.annotations.Nullable;

/**
 * {@link GpuTexture} backed by a native {@code ID3D11Texture2D}.
 *
 * <p>The native format ordinal matches {@link TextureFormat#ordinal()}: 0 =
 * RGBA8, 1 = RED8, 2 = RED8I, 3 = DEPTH32.</p>
 */
final class Dx11Texture extends GpuTexture implements Dx11Resource {
    private final Dx11Device device;
    private final long handle;
    private boolean closed;

    Dx11Texture(
        Dx11Device device,
        @Nullable String label,
        int usage,
        TextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        // Vanilla asks animated sprites for one mip level more than the frame
        // size can hold (the extra level is a zero-sized level on GL). D3D11
        // rejects MipLevels above floor(log2(max(width, height))) + 1, so clamp
        // the chain and keep the GpuTexture metadata in sync with the native
        // resource. Nothing reads past the last real level.
        super(usage, label == null ? "Unnamed" : label, format, width, height, depthOrLayers,
            clampMipLevels(width, height, mipLevels));
        this.device = device;
        final int effectiveMipLevels = getMipLevels();
        final String textureLabel = label == null ? "Unnamed" : label;
        try {
            this.handle = Dx11Native.nativeCreateTexture(
                device.nativeHandle(), usage, format.ordinal(), width, height, depthOrLayers,
                effectiveMipLevels);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Could not create D3D11 texture '" + textureLabel + "' "
                    + width + "x" + height + "x" + depthOrLayers
                    + " mips=" + effectiveMipLevels + " usage=" + usage + " format=" + format,
                e);
        }
        if (this.handle == 0L) {
            throw new IllegalStateException(
                "Could not create D3D11 texture '" + textureLabel + "' "
                    + width + "x" + height + "x" + depthOrLayers
                    + " mips=" + effectiveMipLevels + " usage=" + usage + " format=" + format);
        }
        device.register(this);
    }

    private static int clampMipLevels(int width, int height, int mipLevels) {
        if (width <= 0 || height <= 0 || mipLevels <= 0) {
            return mipLevels;
        }
        final int maxMipLevels =
            Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(width, height));
        return Math.min(mipLevels, maxMipLevels);
    }

    long handle() {
        return this.handle;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            Dx11Native.nativeDestroyTexture(this.handle);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public void destroyNative() {
        close();
    }
}
