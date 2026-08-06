/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.time.Duration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class WidgetTooltipHolder {
    private @Nullable Tooltip tooltip;
    private Duration delay = Duration.ZERO;
    private long displayStartTime;
    private boolean wasDisplayed;

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public void set(@Nullable Tooltip tooltip) {
        this.tooltip = tooltip;
    }

    public @Nullable Tooltip get() {
        return this.tooltip;
    }

    public void refreshTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovered, boolean isFocused, ScreenRectangle screenRectangle) {
        boolean shouldDisplay;
        if (this.tooltip == null) {
            this.wasDisplayed = false;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean bl = shouldDisplay = isHovered || isFocused && minecraft.getLastInputType().isKeyboard();
        if (shouldDisplay != this.wasDisplayed) {
            if (shouldDisplay) {
                this.displayStartTime = Util.getMillis();
            }
            this.wasDisplayed = shouldDisplay;
        }
        if (shouldDisplay && Util.getMillis() - this.displayStartTime > this.delay.toMillis()) {
            graphics.setTooltipForNextFrame(minecraft.font, this.tooltip.toCharSequence(minecraft), this.tooltip.component(), this.createTooltipPositioner(screenRectangle, isHovered, isFocused), mouseX, mouseY, isFocused, this.tooltip.style());
        }
    }

    private ClientTooltipPositioner createTooltipPositioner(ScreenRectangle screenRectangle, boolean isHovered, boolean isFocused) {
        if (!isHovered && isFocused && Minecraft.getInstance().getLastInputType().isKeyboard()) {
            return new BelowOrAboveWidgetTooltipPositioner(screenRectangle);
        }
        return new MenuTooltipPositioner(screenRectangle);
    }

    public void updateNarration(NarrationElementOutput output) {
        if (this.tooltip != null) {
            this.tooltip.updateNarration(output);
        }
    }
}

