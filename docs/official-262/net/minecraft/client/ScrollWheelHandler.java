/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector2i
 */
package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2i;

@Environment(value=EnvType.CLIENT)
public class ScrollWheelHandler {
    private double accumulatedScrollX;
    private double accumulatedScrollY;

    public Vector2i onMouseScroll(double scaledXScrollOffset, double scaledYScrollOffset) {
        if (this.accumulatedScrollX != 0.0 && Math.signum(scaledXScrollOffset) != Math.signum(this.accumulatedScrollX)) {
            this.accumulatedScrollX = 0.0;
        }
        if (this.accumulatedScrollY != 0.0 && Math.signum(scaledYScrollOffset) != Math.signum(this.accumulatedScrollY)) {
            this.accumulatedScrollY = 0.0;
        }
        this.accumulatedScrollX += scaledXScrollOffset;
        this.accumulatedScrollY += scaledYScrollOffset;
        int wheelX = (int)this.accumulatedScrollX;
        int wheelY = (int)this.accumulatedScrollY;
        if (wheelX == 0 && wheelY == 0) {
            return new Vector2i(0, 0);
        }
        this.accumulatedScrollX -= (double)wheelX;
        this.accumulatedScrollY -= (double)wheelY;
        return new Vector2i(wheelX, wheelY);
    }

    public static int getNextScrollWheelSelection(double wheel, int currentSelected, int limit) {
        int step = (int)Math.signum(wheel);
        currentSelected -= step;
        for (currentSelected = Math.max(-1, currentSelected); currentSelected < 0; currentSelected += limit) {
        }
        while (currentSelected >= limit) {
            currentSelected -= limit;
        }
        return currentSelected;
    }
}

