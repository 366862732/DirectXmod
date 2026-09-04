package com.xgdt.dx12.dx12;

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
    /** View 计数器（镜像 VulkanGpuTexture.views）。close() 时若 view=0 则入延迟销毁。 */
    private int views = 0;

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

    /** 镜像 {@code VulkanGpuTexture.addViews}：每创建一个 TextureView 时调用。 */
    public void addViews() {
        ++this.views;
    }

    /** 镜像 {@code VulkanGpuTexture.removeViews}：关闭 TextureView 时调用；
     * 若 texture 本身也已 close 且无残留 view，则入延迟销毁队列。 */
    public void removeViews() {
        --this.views;
        if (this.views < 0) {
            throw new IllegalStateException("Too many views removed from texture");
        }
        if (this.closed && this.views == 0) {
            Dx12Native.dx12DestroyResource(this.handle);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.views == 0) {
            Dx12Native.dx12DestroyResource(this.handle);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
