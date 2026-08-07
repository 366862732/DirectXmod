package com.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuTexture}.
 *
 * Native counterpart: {@code dx12CreateTexture} in dx12_mc.dll, which creates a
 * committed D3D12 texture resource and returns a handle ({@code Dx12Object*}).
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuTexture extends GpuTexture {
    private final long handle;
    private boolean closed;

    public Dx12GpuTexture(@Usage int usage, String label, GpuFormat format, int width,
        int height, int depthOrLayers, int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.handle = Dx12Native.dx12CreateTexture(usage, format.ordinal(), width, height,
            depthOrLayers, mipLevels);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateTexture returned a null handle");
        }
    }

    /** Native handle ({@code Dx12Object*} as long). */
    public long handle() {
        return this.handle;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Dx12Native.dx12DestroyResource(this.handle);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
