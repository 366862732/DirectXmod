package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.xgdt.dx12.config.Dx12Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 渲染缩放（超分辨率的第一步）。
 *
 * <p>{@code GameRenderer.render()} 每帧用窗口尺寸与主渲染目标尺寸比对，不一致就调用
 * {@code resize()}。{@code RenderTarget.resize()} 内部会 destroy + create 全套纹理，
 * 绝不能每帧执行。这里把该调用重定向为「按 render_scale 计算内部尺寸，仅在真正变化时重建」，
 * 于是内部以低分辨率渲染，最终由 blit 一次性放大到窗口（HUD 与主目标同源，一起缩放）。
 *
 * <p>DLSS 质量档位会改变有效渲染比例。原版只在「窗口尺寸 != 主目标尺寸」时调用
 * resize，因此从 100%（DLAA）切到 67%（Quality）时不会触发。这里在 render HEAD
 * 额外做一次尺寸检查，保证切换档位后立即重建主目标。
 */
@Mixin(GameRenderer.class)
public class GameRendererRenderScaleMixin {

    @Shadow @Final private RenderTarget mainRenderTarget;

    @Shadow public void resize(int width, int height) {
        throw new AssertionError();
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void dx12_applyRenderScale(DeltaTracker deltaTracker, boolean advanceGameTime,
                                       CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        int scale = Dx12Config.getInstance().getEffectiveRenderScale();
        int scaledWidth = Math.max(1, (int) ((long) width * scale / 100L));
        int scaledHeight = Math.max(1, (int) ((long) height * scale / 100L));
        if (this.mainRenderTarget.width != scaledWidth
            || this.mainRenderTarget.height != scaledHeight) {
            this.resize(scaledWidth, scaledHeight);
        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;resize(II)V"
        ),
        remap = false
    )
    private void dx12_renderScaleResize(GameRenderer self, int width, int height) {
        // DLSS 超分开启时由质量档位决定实际内部比例（Quality 67% 等）。
        int scale = Dx12Config.getInstance().getEffectiveRenderScale();
        int scaledWidth = Math.max(1, (int) ((long) width * scale / 100L));
        int scaledHeight = Math.max(1, (int) ((long) height * scale / 100L));
        if (this.mainRenderTarget.width != scaledWidth || this.mainRenderTarget.height != scaledHeight) {
            self.resize(scaledWidth, scaledHeight);
        }
    }
}
