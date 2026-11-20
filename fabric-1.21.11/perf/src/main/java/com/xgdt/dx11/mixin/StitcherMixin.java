package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.PerfState;
import com.xgdt.dx11.perf.texture.StbStitcher;
import com.xgdt.dx11.perf.texture.StbStitcher.PackedAtlas;
import com.xgdt.dx11.perf.texture.StitcherStats;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.Stitcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packs large atlases with stb_rect_pack instead of vanilla's growing tree.
 *
 * <p>Vanilla {@code stitch} walks a region tree for every holder and expands the
 * atlas one power-of-two step at a time, which is why building the block atlas
 * is a visible part of resource loading.  This mixin packs the same sorted
 * holder list with {@link StbStitcher} and then feeds the resulting positions
 * straight to the {@code SpriteLoader} that vanilla would have called.</p>
 *
 * <p>It only takes over when the perf layer is live, the flag is on, and there
 * are at least {@link StbStitcher#MIN_SPRITES} sprites; small atlases keep the
 * exact vanilla layout that mods occasionally depend on.  Any packing failure
 * leaves the stitcher untouched, so vanilla still runs and reports its own
 * errors.  Positions stay mip-aligned, because the packer works in units of
 * {@code 1 << mipLevel}.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(Stitcher.class)
public abstract class StitcherMixin {
    @Shadow
    @Final
    private List<?> texturesToBeStitched;

    @Shadow
    private int storageX;

    @Shadow
    private int storageY;

    @Shadow
    @Final
    private int maxWidth;

    @Shadow
    @Final
    private int maxHeight;

    @Shadow
    @Final
    private int mipLevel;

    @Shadow
    @Final
    private static Comparator<Object> HOLDER_COMPARATOR;

    /** Positions written by {@link #dx11$packWithStb}; null means "use vanilla". */
    @Unique
    private @Nullable PackedAtlas dx11$packed;

    @Inject(method = "stitch", at = @At("HEAD"), cancellable = true)
    private void dx11$packWithStb(CallbackInfo ci) {
        this.dx11$packed = null;
        if (!PerfState.layerActive() || !PerfState.config().fastStitcher) {
            return;
        }
        if (!StbStitcher.isWorthPacking(this.texturesToBeStitched.size())) {
            StitcherStats.skipped();
            return;
        }

        List<Object> sorted = new ArrayList<>(this.texturesToBeStitched);
        sorted.sort(HOLDER_COMPARATOR);
        PackedAtlas packed = StbStitcher.pack(sorted, this.maxWidth, this.maxHeight, this.mipLevel);
        if (packed == null) {
            StitcherStats.fellBack();
            return;
        }

        this.storageX = packed.width();
        this.storageY = packed.height();
        this.dx11$packed = packed;
        StitcherStats.packed(packed.sprites().size(), packed.width(), packed.height());
        ci.cancel();
    }

    @Inject(method = "gatherSprites", at = @At("HEAD"), cancellable = true)
    private void dx11$gatherPackedSprites(
        Stitcher.SpriteLoader<Stitcher.Entry> spriteLoader, CallbackInfo ci) {
        StbStitcher.PackedAtlas packed = this.dx11$packed;
        if (packed == null) {
            return;
        }
        ci.cancel();
        for (StbStitcher.PackedSprite sprite : packed.sprites()) {
            spriteLoader.load(sprite.entry(), sprite.x(), sprite.y(), this.mipLevel);
        }
    }
}
