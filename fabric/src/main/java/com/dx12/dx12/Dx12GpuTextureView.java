package com.dx12.dx12;

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
    /** P3b：本 view 的 base mip（render pass 绑 RTV 时须用，图集逐级上传 mip）。 */
    private final int baseMip;

    public Dx12GpuTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        this.baseMip = baseMipLevel;
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

    /** 该 view 指向的 base mip slice（render pass 附件 RTV/DSV 绑定用）。 */
    public int baseMip() {
        return this.baseMip;
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
