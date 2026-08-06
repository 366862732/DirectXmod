/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector2i
 *  org.joml.Vector2ic
 */
package net.minecraft.client.gui.screens.inventory.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@Environment(value=EnvType.CLIENT)
public class BelowOrAboveWidgetTooltipPositioner
implements ClientTooltipPositioner {
    private final ScreenRectangle screenRectangle;

    public BelowOrAboveWidgetTooltipPositioner(ScreenRectangle screenRectangle) {
        this.screenRectangle = screenRectangle;
    }

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int tooltipWidth, int tooltipHeight) {
        Vector2i result = new Vector2i();
        result.x = this.screenRectangle.left() + 3;
        result.y = this.screenRectangle.bottom() + 3 + 1;
        if (result.y + tooltipHeight + 3 > screenHeight) {
            result.y = this.screenRectangle.top() - tooltipHeight - 3 - 1;
        }
        if (result.x + tooltipWidth > screenWidth) {
            result.x = Math.max(this.screenRectangle.right() - tooltipWidth - 3, 4);
        }
        return result;
    }
}

