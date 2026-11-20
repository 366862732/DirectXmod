package com.xgdt.dx11.perf.texture;

import net.minecraft.client.renderer.texture.Stitcher;

/**
 * Public read access to vanilla's package-private {@code Stitcher$Holder}.
 *
 * <p>{@code Stitcher.Holder} is the record the stitcher sorts and packs: one
 * entry plus the size the sprite occupies in the atlas, already including the
 * mip-level alignment and the inter-sprite padding.  It is not visible outside
 * {@code net.minecraft.client.renderer.texture}, so the holder mixin
 * implements this interface and the packer reads holders through it.</p>
 */
public interface StitcherHolderView {
    /** The sprite this holder positions. */
    Stitcher.Entry dx11$entry();

    /** Atlas-space width in texels, padding and mip alignment included. */
    int dx11$width();

    /** Atlas-space height in texels, padding and mip alignment included. */
    int dx11$height();
}
