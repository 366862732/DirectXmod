package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.texture.StitcherHolderView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes vanilla's package-private {@code Stitcher$Holder} record.
 *
 * <p>The record is not visible outside its package, so the STB packer cannot
 * reference it in a field or parameter type.  It can still be mixed into by
 * name, and the {@link StitcherHolderView} interface gives the packer typed
 * access to the entry and the atlas-space size.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.renderer.texture.Stitcher$Holder")
public abstract class StitcherHolderMixin implements StitcherHolderView {
    @Shadow
    public abstract Stitcher.Entry entry();

    @Shadow
    public abstract int width();

    @Shadow
    public abstract int height();

    @Override
    public Stitcher.Entry dx11$entry() {
        return this.entry();
    }

    @Override
    public int dx11$width() {
        return this.width();
    }

    @Override
    public int dx11$height() {
        return this.height();
    }
}
