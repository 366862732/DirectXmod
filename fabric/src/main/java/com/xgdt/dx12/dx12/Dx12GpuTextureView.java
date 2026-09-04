package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuTextureView} (an SRV over a {@link Dx12GpuTexture}).
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuTextureView extends GpuTextureView {
    private long handle;
    private boolean closed;

    public Dx12GpuTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        Dx12GpuTexture dx12Texture = (Dx12GpuTexture) texture;
        // 镜像 VulkanGpuTextureView：构造时注册 view 引用，close() 时 removeViews
        dx12Texture.addViews();
        this.handle = Dx12Native.dx12CreateTextureView(dx12Texture.handle(), baseMipLevel, mipLevels);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateTextureView returned a null handle");
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
        // 镜像 VulkanGpuTextureView.close()：先通知 texture 移除本 view 引用，
        // 再销毁 native 层描述符（入 gPendingDeletes 延迟销毁，等 GPU 空闲后释放）。
        ((Dx12GpuTexture) this.texture()).removeViews();
        Dx12Native.dx12DestroyResource(this.handle);
        this.handle = 0L;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
