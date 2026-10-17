package com.xgdt.dx12.mixin;

import com.xgdt.dx12.shaderpack.ShaderPack;
import com.xgdt.dx12.shaderpack.ShaderPackManager;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 光影包用程序化体积云替换原版天空时，屏蔽原版方块云。
 *
 * <p>原版云会写深度缓冲，深度不为 0 的像素会被光影链当成"地形"透传，
 * 于是方块状的原版云会叠在体积云上。是否屏蔽由 pipeline.json 顶层
 * {@code "replaceVanillaSky": true} 决定。
 */
@Mixin(CloudRenderer.class)
public class CloudRendererShaderPackMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void dx12_suppressVanillaClouds(CallbackInfo ci) {
        ShaderPack pack = ShaderPackManager.getInstance().activeIfEnabled();
        if (pack != null && pack.manifest().replacesVanillaSky()) {
            ci.cancel();
        }
    }
}
