package com.xgdt.dx11.perf.immediate.sign;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Texture handle for a render target's colour attachment.
 *
 * <p>{@code RenderTypes.text*} resolves its sampler through the texture
 * manager, so the render target needs a registered texture entry.  The target
 * owns the actual GPU texture, therefore {@link #close()} must not free it.
 * Nearest filtering and edge clamping match the reference implementation; the
 * atlas is sampled 1:1 and must not bleed neighbouring signs.</p>
 */
public final class RenderTargetTexture extends AbstractTexture {
    public RenderTargetTexture(RenderTarget renderTarget) {
        this.texture = renderTarget.getColorTexture();
        this.textureView = renderTarget.getColorTextureView();
        this.sampler = RenderSystem.getSamplerCache().getSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.NEAREST,
            FilterMode.NEAREST,
            false);
    }

    @Override
    public void close() {
        // The owning render target destroys the texture.
    }
}
