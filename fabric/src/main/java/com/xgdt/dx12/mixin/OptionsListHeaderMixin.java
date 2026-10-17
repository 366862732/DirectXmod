package com.xgdt.dx12.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 与 {@link OptionsListRowMixin} 同理，把分类标题也改为跟随列表行左边界，
 * 否则左半屏列表里的"显示 / 品质与性能 / 偏好"标题仍会停在整屏中心。
 */
@Mixin(targets = "net.minecraft.client.gui.components.OptionsList$HeaderEntry")
public abstract class OptionsListHeaderMixin {
    @Shadow
    @Final
    private int paddingTop;

    @Shadow
    @Final
    private StringWidget widget;

    @Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
    private void gl4dx12$layoutHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                                      float a, CallbackInfo ci) {
        LayoutElement self = (LayoutElement) (Object) this;
        this.widget.setPosition(self.getX(), self.getY() + 2 + this.paddingTop);
        this.widget.extractRenderState(graphics, mouseX, mouseY, a);
        ci.cancel();
    }
}
