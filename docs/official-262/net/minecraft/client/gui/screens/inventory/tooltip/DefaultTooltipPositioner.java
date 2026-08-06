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
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@Environment(value=EnvType.CLIENT)
public class DefaultTooltipPositioner
implements ClientTooltipPositioner {
    public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

    private DefaultTooltipPositioner() {
    }

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int tooltipWidth, int tooltipHeight) {
        Vector2i result = new Vector2i(x, y).add(12, -12);
        this.positionTooltip(screenWidth, screenHeight, result, tooltipWidth, tooltipHeight);
        return result;
    }

    private void positionTooltip(int screenWidth, int screenHeight, Vector2i result, int tooltipWidth, int tooltipHeight) {
        int paddedHeight;
        if (result.x + tooltipWidth > screenWidth) {
            result.x = Math.max(result.x - 24 - tooltipWidth, 4);
        }
        if (result.y + (paddedHeight = tooltipHeight + 3) > screenHeight) {
            result.y = screenHeight - paddedHeight;
        }
    }
}

