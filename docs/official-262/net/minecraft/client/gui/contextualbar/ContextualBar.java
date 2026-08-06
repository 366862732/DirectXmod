/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Environment(value=EnvType.CLIENT)
public interface ContextualBar {
    public static final int WIDTH = 182;
    public static final int HEIGHT = 5;
    public static final int MARGIN_BOTTOM = 24;
    public static final ContextualBar EMPTY = new ContextualBar(){

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        }
    };

    default public int left(Window window) {
        return (window.getGuiScaledWidth() - 182) / 2;
    }

    default public int top(Window window) {
        return window.getGuiScaledHeight() - 24 - 5;
    }

    public void extractBackground(GuiGraphicsExtractor var1, DeltaTracker var2);

    public void extractRenderState(GuiGraphicsExtractor var1, DeltaTracker var2);

    public static void extractExperienceLevel(GuiGraphicsExtractor graphics, Font font, int experienceLevel) {
        MutableComponent str = Component.translatable("gui.experience.level", experienceLevel);
        int x = (graphics.guiWidth() - font.width(str)) / 2;
        int y = graphics.guiHeight() - 24 - font.lineHeight - 2;
        graphics.text(font, str, x + 1, y, -16777216, false);
        graphics.text(font, str, x - 1, y, -16777216, false);
        graphics.text(font, str, x, y + 1, -16777216, false);
        graphics.text(font, str, x, y - 1, -16777216, false);
        graphics.text(font, str, x, y, -8323296, false);
    }
}

