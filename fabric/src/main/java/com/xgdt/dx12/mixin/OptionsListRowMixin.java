package com.xgdt.dx12.mixin;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让原版 {@link OptionsList} 的行内容跟随列表本身的位置与宽度，而不是始终以整屏中心定位。
 *
 * <p>原版 {@code Entry.extractContent} 用 {@code screen.width / 2 - 155} 计算起点，
 * 因此列表再窄，控件仍然画在屏幕正中。这里改用条目自身的
 * {@link LayoutElement#getX()} / {@link LayoutElement#getWidth()}
 * （由 {@code AbstractSelectionList.repositionEntries()} 设置为行左边界与行宽）：
 * <ul>
 *   <li>对原版全屏列表：行宽仍为 310，位置与改动前完全一致；</li>
 *   <li>对左半屏列表：行宽随列表收窄，两列控件自动平分宽度。</li>
 * </ul>
 */
@Mixin(targets = "net.minecraft.client.gui.components.OptionsList$Entry")
public abstract class OptionsListRowMixin {
    @Shadow
    @Final
    private List<OptionsList.OptionInstanceWidget> children;

    @Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
    private void gl4dx12$layoutRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                                   float a, CallbackInfo ci) {
        LayoutElement self = (LayoutElement) (Object) this;
        int rowLeft = self.getX();
        int rowWidth = self.getWidth();
        int contentY = self.getY() + 2;
        int count = this.children.size();
        int gap = 10;
        int twoColumnWidth = Math.max(20, (rowWidth - gap) / 2);
        int offset = 0;
        for (OptionsList.OptionInstanceWidget optionWidget : this.children) {
            AbstractWidget widget = optionWidget.widget();
            // 单控件行保留原版宽度（big=310 / small=150），只在行变窄时收缩；双控件行平分行宽。
            int widgetWidth = count > 1 ? twoColumnWidth : Math.min(rowWidth, widget.getWidth());
            widget.setWidth(widgetWidth);
            widget.setPosition(rowLeft + offset, contentY);
            widget.extractRenderState(graphics, mouseX, mouseY, a);
            offset += widgetWidth + gap;
        }
        ci.cancel();
    }
}
