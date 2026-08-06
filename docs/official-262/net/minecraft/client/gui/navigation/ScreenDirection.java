/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntComparator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenAxis;

@Environment(value=EnvType.CLIENT)
public enum ScreenDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    private final IntComparator coordinateValueComparator = (k1, k2) -> k1 == k2 ? 0 : (this.isBefore(k1, k2) ? -1 : 1);

    public ScreenAxis getAxis() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0, 1 -> ScreenAxis.VERTICAL;
            case 2, 3 -> ScreenAxis.HORIZONTAL;
        };
    }

    public ScreenDirection getOpposite() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> DOWN;
            case 1 -> UP;
            case 2 -> RIGHT;
            case 3 -> LEFT;
        };
    }

    public boolean isPositive() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0, 2 -> false;
            case 1, 3 -> true;
        };
    }

    public boolean isAfter(int a, int b) {
        if (this.isPositive()) {
            return a > b;
        }
        return b > a;
    }

    public boolean isBefore(int a, int b) {
        if (this.isPositive()) {
            return a < b;
        }
        return b < a;
    }

    public IntComparator coordinateValueComparator() {
        return this.coordinateValueComparator;
    }
}

