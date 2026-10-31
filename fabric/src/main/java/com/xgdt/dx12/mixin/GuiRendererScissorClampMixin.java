package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 渲染缩放下的 GUI 裁剪区域修正。
 *
 * <p>{@code GameRendererRenderScaleMixin} 会把主渲染目标缩到 render_scale%（DLSS Quality
 * 档为 67%），此时 {@code GuiRenderer} 的 render pass 区域是缩放后的尺寸（如 572x321），
 * 但它计算 scissor 时仍按窗口尺寸（854x480）换算，于是下发的裁剪矩形会超出 pass 区域，
 * 触发 {@code RenderPass.enableScissor} 的越界断言并崩溃。
 *
 * <p>这里把 scissor 按「主目标尺寸 / 窗口尺寸」等比缩放到渲染目标坐标系，再夹紧到目标
 * 范围内；缩放后为空区域的画一个 1x1 裁剪（等价于不绘制，且不会触发 size > 0 断言）。
 * 未开启缩放（目标尺寸 == 窗口尺寸）时原样透传，不改变原版行为。
 */
@Mixin(GuiRenderer.class)
public class GuiRendererScissorClampMixin {

    @Redirect(
        method = "enableScissor",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;enableScissor(IIII)V"
        ),
        remap = false
    )
    private void dx12_scaleAndClampScissor(RenderPass pass, int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            pass.enableScissor(x, y, width, height);
            return;
        }
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        WindowRenderState window = minecraft.gameRenderer.gameRenderState().windowRenderState;
        if (window.width <= 0 || window.height <= 0
            || (target.width == window.width && target.height == window.height)) {
            pass.enableScissor(x, y, width, height);
            return;
        }
        double scaleX = (double) target.width / (double) window.width;
        double scaleY = (double) target.height / (double) window.height;
        // y 沿用原版约定（自下而上），与 RenderPass 的 GL 风格坐标系一致。
        int x0 = Math.max(0, (int) Math.floor(x * scaleX));
        int y0 = Math.max(0, (int) Math.floor(y * scaleY));
        int x1 = Math.min(target.width, (int) Math.ceil((x + width) * scaleX));
        int y1 = Math.min(target.height, (int) Math.ceil((y + height) * scaleY));
        if (x1 <= x0 || y1 <= y0) {
            int clampX = Math.max(0, Math.min(x0, target.width - 1));
            int clampY = Math.max(0, Math.min(y0, target.height - 1));
            pass.enableScissor(clampX, clampY, 1, 1);
            return;
        }
        pass.enableScissor(x0, y0, x1 - x0, y1 - y0);
    }
}
