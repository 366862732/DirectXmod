package com.xgdt.dx11.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the fog renderer so nested passes (the sign atlas) can bind its
 * prebuilt no-fog uniform buffer instead of the world's fog settings.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("fogRenderer")
    FogRenderer dx11$fogRenderer();
}
