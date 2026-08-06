/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;

@Environment(value=EnvType.CLIENT)
public record ScreenPosition(int x, int y) {
    public static ScreenPosition of(ScreenAxis axis, int primaryValue, int secondaryValue) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case ScreenAxis.HORIZONTAL -> new ScreenPosition(primaryValue, secondaryValue);
            case ScreenAxis.VERTICAL -> new ScreenPosition(secondaryValue, primaryValue);
        };
    }

    public ScreenPosition step(ScreenDirection direction) {
        return switch (direction) {
            default -> throw new MatchException(null, null);
            case ScreenDirection.DOWN -> new ScreenPosition(this.x, this.y + 1);
            case ScreenDirection.UP -> new ScreenPosition(this.x, this.y - 1);
            case ScreenDirection.LEFT -> new ScreenPosition(this.x - 1, this.y);
            case ScreenDirection.RIGHT -> new ScreenPosition(this.x + 1, this.y);
        };
    }

    public int getCoordinate(ScreenAxis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case ScreenAxis.HORIZONTAL -> this.x;
            case ScreenAxis.VERTICAL -> this.y;
        };
    }
}

