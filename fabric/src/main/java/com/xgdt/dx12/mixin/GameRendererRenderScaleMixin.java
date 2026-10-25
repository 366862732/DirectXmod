package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.xgdt.dx12.config.Dx12Config;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 渲染缩放（超分辨率的第一步）。
 *
 * <p>{@code GameRenderer.render()} 每帧用窗口尺寸与主渲染目标尺寸比对，不一致就调用
 * {@code resize()}。{@code RenderTarget.resize()} 内部会 destroy + create 全套纹理，
 * 绝不能每帧执行。这里把该调用重定向为「按 render_scale 计算内部尺寸，仅在真正变化时重建」，
 * 于是内部以低分辨率渲染，最终由 blit 一次性放大到窗口（HUD 与主目标同源，一起缩放）。
 */
@Mixin(GameRenderer.class)
public class GameRendererRenderScaleMixin {

    @Shadow @Final private RenderTarget mainRenderTarget;

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;resize(II)V"
        ),
        remap = false
    )
    private void dx12_renderScaleResize(GameRenderer self, int width, int height) {
        int scale = Dx12Config.getInstance().getRenderScale();
        int scaledWidth = Math.max(1, (int) ((long) width * scale / 100L));
        int scaledHeight = Math.max(1, (int) ((long) height * scale / 100L));
        if (this.mainRenderTarget.width != scaledWidth || this.mainRenderTarget.height != scaledHeight) {
            self.resize(scaledWidth, scaledHeight);
        }
    }
}
