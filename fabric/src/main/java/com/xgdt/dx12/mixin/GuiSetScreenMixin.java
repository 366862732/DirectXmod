package com.xgdt.dx12.mixin;

import com.xgdt.dx12.gui.Dx12VideoSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把原版视频设置界面替换为 gl4dx12 的主题界面。
 *
 * <p>原版入口是 {@code OptionsScreen.init()} 里的
 * {@code () -> new VideoSettingsScreen(...)}，位于 lambda 内且没有稳定的方法名，
 * 因此不在 NEW 指令上做重定向，而是在 {@link Gui#setScreen(Screen)} 的入参处统一替换：
 * <ul>
 *   <li>只匹配 {@code getClass() == VideoSettingsScreen.class}，避免对子类重复包装；</li>
 *   <li>用 {@code minecraft.gui.screen()} 作为 {@code lastScreen}，与 OptionsScreen 原逻辑一致；</li>
 *   <li>其余界面（含世界内暂停菜单）流程不受影响。</li>
 * </ul>
 */
@Mixin(Gui.class)
public abstract class GuiSetScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen gl4dx12$replaceVideoSettings(Screen screen) {
        if (screen != null && screen.getClass() == VideoSettingsScreen.class) {
            Minecraft minecraft = Minecraft.getInstance();
            return new Dx12VideoSettingsScreen(minecraft.gui.screen(), minecraft, minecraft.options);
        }
        return screen;
    }
}
