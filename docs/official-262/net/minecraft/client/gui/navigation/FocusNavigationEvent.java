/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface FocusNavigationEvent {
    public ScreenDirection getVerticalDirectionForInitialFocus();

    @Environment(value=EnvType.CLIENT)
    public record ArrowNavigation(ScreenDirection direction, @Nullable ScreenRectangle previousFocus) implements FocusNavigationEvent
    {
        public ArrowNavigation(ScreenDirection direction) {
            this(direction, null);
        }

        @Override
        public ScreenDirection getVerticalDirectionForInitialFocus() {
            return this.direction.getAxis() == ScreenAxis.VERTICAL ? this.direction : ScreenDirection.DOWN;
        }

        public ArrowNavigation with(ScreenRectangle previousFocus) {
            return new ArrowNavigation(this.direction(), previousFocus);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class InitialFocus
    implements FocusNavigationEvent {
        @Override
        public ScreenDirection getVerticalDirectionForInitialFocus() {
            return ScreenDirection.DOWN;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record TabNavigation(boolean forward) implements FocusNavigationEvent
    {
        @Override
        public ScreenDirection getVerticalDirectionForInitialFocus() {
            return this.forward ? ScreenDirection.DOWN : ScreenDirection.UP;
        }
    }
}

