package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.sign.SignTextCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops cached sign pixels when the font set is replaced (F3+T, language or
 * resource-pack change).  The atlas stores rasterised glyphs, so keeping it
 * across a font reload would show text in the old font.
 */
@Environment(EnvType.CLIENT)
@Mixin(FontManager.class)
public abstract class FontManagerSignCacheMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void dx11$clearSignAtlas(CallbackInfo ci) {
        SignTextCache.onFontReload();
    }
}
