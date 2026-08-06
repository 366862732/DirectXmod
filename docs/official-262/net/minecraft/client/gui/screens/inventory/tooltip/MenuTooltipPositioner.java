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
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@Environment(value=EnvType.CLIENT)
public class MenuTooltipPositioner
implements ClientTooltipPositioner {
    private static final int MARGIN = 5;
    private static final int MOUSE_OFFSET_X = 12;
    public static final int MAX_OVERLAP_WITH_WIDGET = 3;
    public static final int MAX_DISTANCE_TO_WIDGET = 5;
    private final ScreenRectangle screenRectangle;

    public MenuTooltipPositioner(ScreenRectangle screenRectangle) {
        this.screenRectangle = screenRectangle;
    }

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int tooltipWidth, int tooltipHeight) {
        int maxY;
        Vector2i result = new Vector2i(x + 12, y);
        if (result.x + tooltipWidth > screenWidth - 5) {
            result.x = Math.max(x - 12 - tooltipWidth, 9);
        }
        result.y += 3;
        int paddedHeight = tooltipHeight + 3 + 3;
        int lowestPossibleY = this.screenRectangle.bottom() + 3 + MenuTooltipPositioner.getOffset(0, 0, this.screenRectangle.height());
        result.y = lowestPossibleY + paddedHeight <= (maxY = screenHeight - 5) ? (result.y += MenuTooltipPositioner.getOffset(result.y, this.screenRectangle.top(), this.screenRectangle.height())) : (result.y -= paddedHeight + MenuTooltipPositioner.getOffset(result.y, this.screenRectangle.bottom(), this.screenRectangle.height()));
        return result;
    }

    private static int getOffset(int mouseY, int widgetY, int widgetHeight) {
        int distance = Math.min(Math.abs(mouseY - widgetY), widgetHeight);
        return Math.round(Mth.lerp((float)distance / (float)widgetHeight, widgetHeight - 3, 5.0f));
    }
}

