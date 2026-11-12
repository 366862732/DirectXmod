package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.textures.GpuTextureView;

/** {@link GpuTextureView} backed by a native {@code ID3D11ShaderResourceView}. */
final class Dx11TextureView extends GpuTextureView implements Dx11Resource {
    private final Dx11Device device;
    private final long handle;
    private boolean closed;

    Dx11TextureView(Dx11Device device, Dx11Texture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        this.device = device;
        this.handle = Dx11Native.nativeCreateTextureView(
            device.nativeHandle(), texture.handle(), baseMipLevel, mipLevels);
        if (this.handle == 0L) {
            throw new IllegalStateException("Could not create D3D11 texture view");
        }
        device.register(this);
    }

    long handle() {
        return this.handle;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            Dx11Native.nativeDestroyTextureView(this.handle);
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
