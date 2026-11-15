package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.FontTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Font atlas resizing: replaces the hard-coded 256x256 sheet with a larger
 * power of two.
 *
 * <p>High-resolution resource packs can fill a 256x256 sheet with only a
 * handful of glyphs, so a line of text starts dozens of render-type buffers
 * that each hold just a few quads.  A bigger sheet keeps a whole line in one
 * buffer.</p>
 *
 * <p>The constructor constants are read from {@link ImmediateFlags} directly,
 * so no handler runs before {@code super()} (Mixin rejects instance handlers
 * there).  The glyph UV divisor instead reads the real texture width, which
 * keeps existing glyph UVs valid even if the flag or the configured size is
 * changed after the sheet was built; a new size still only takes effect on
 * {@code F3+T}.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(FontTexture.class)
public abstract class FontTextureMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 256))
    private int dx11$sheetDimension(int original) {
        return ImmediateFlags.fontAtlasResizing() ? ImmediateFlags.fontAtlasSize() : original;
    }

    @ModifyConstant(method = "add", constant = @Constant(floatValue = 256.0F))
    private float dx11$sheetDimensionFloat(float original) {
        // The texture already exists with the size chosen at construction.
        return ((FontTexture) (Object) this).getTexture().getWidth(0);
    }
}
