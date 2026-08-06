/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LoadingDotsWidget
extends AbstractWidget {
    private static final int Y_PADDING = 2;
    private final Font font;

    public LoadingDotsWidget(Font font, Component message) {
        super(0, 0, font.width(message), 2 + font.lineHeight + 6 + font.lineHeight + 2, message);
        this.font = font;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int centerX = this.getX() + this.getWidth() / 2;
        Component message = this.getMessage();
        graphics.text(this.font, message, centerX - this.font.width(message) / 2, this.getY() + 2, -1);
        String dots = LoadingDotsText.get(Util.getMillis());
        graphics.text(this.font, dots, centerX - this.font.width(dots) / 2, this.getBottom() - this.font.lineHeight - 2, -8355712);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }
}

