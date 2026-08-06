/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class ContainerObjectSelectionList<E extends Entry<E>>
extends AbstractSelectionList<E> {
    public ContainerObjectSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (this.getItemCount() == 0) {
            return null;
        }
        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
            ComponentPath componentPath;
            FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)navigationEvent;
            Entry focused = (Entry)this.getFocused();
            if (arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL && focused != null) {
                return ComponentPath.path(this, focused.nextFocusPath(navigationEvent));
            }
            int index = -1;
            ScreenDirection direction = arrowNavigation.direction();
            if (focused != null) {
                index = focused.children().indexOf(focused.getFocused());
            }
            if (index == -1) {
                switch (direction) {
                    case LEFT: {
                        index = Integer.MAX_VALUE;
                        direction = ScreenDirection.DOWN;
                        break;
                    }
                    case RIGHT: {
                        index = 0;
                        direction = ScreenDirection.DOWN;
                        break;
                    }
                    default: {
                        index = 0;
                    }
                }
            }
            Entry entry = focused;
            do {
                if ((entry = this.nextEntry(direction, e -> !e.children().isEmpty(), entry)) != null) continue;
                return null;
            } while ((componentPath = entry.focusPathAtIndex(arrowNavigation, index)) == null);
            return ComponentPath.path(this, componentPath);
        }
        return super.nextFocusPath(navigationEvent);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.getFocused() == focused) {
            return;
        }
        super.setFocused(focused);
        if (focused == null) {
            this.setSelected(null);
        }
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        return super.narrationPriority();
    }

    @Override
    protected boolean entriesCanBeSelected() {
        return false;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        Entry hovered = (Entry)this.getHovered();
        if (hovered != null) {
            hovered.updateNarration(output.nest());
            this.narrateListElementPosition(output, hovered);
        } else {
            Entry focused = (Entry)this.getFocused();
            if (focused != null) {
                focused.updateNarration(output.nest());
                this.narrateListElementPosition(output, focused);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry<E extends Entry<E>>
    extends AbstractSelectionList.Entry<E>
    implements ContainerEventHandler {
        private @Nullable GuiEventListener focused;
        private @Nullable NarratableEntry lastNarratable;
        private boolean dragging;

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return ContainerEventHandler.super.mouseClicked(event, doubleClick);
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }
            if (focused != null) {
                focused.setFocused(true);
            }
            this.focused = focused;
        }

        @Override
        public @Nullable GuiEventListener getFocused() {
            return this.focused;
        }

        public @Nullable ComponentPath focusPathAtIndex(FocusNavigationEvent navigationEvent, int currentIndex) {
            if (this.children().isEmpty()) {
                return null;
            }
            ComponentPath componentPath = this.children().get(Math.min(currentIndex, this.children().size() - 1)).nextFocusPath(navigationEvent);
            return ComponentPath.path(this, componentPath);
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
                int index;
                int delta;
                FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)navigationEvent;
                switch (arrowNavigation.direction()) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case UP: 
                    case DOWN: {
                        int n = 0;
                        break;
                    }
                    case LEFT: {
                        int n = -1;
                        break;
                    }
                    case RIGHT: {
                        int n = delta = 1;
                    }
                }
                if (delta == 0) {
                    return null;
                }
                for (int i = index = Mth.clamp(delta + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1); i >= 0 && i < this.children().size(); i += delta) {
                    GuiEventListener child = this.children().get(i);
                    ComponentPath componentPath = child.nextFocusPath(navigationEvent);
                    if (componentPath == null) continue;
                    return ComponentPath.path(this, componentPath);
                }
            }
            return ContainerEventHandler.super.nextFocusPath(navigationEvent);
        }

        public abstract List<? extends NarratableEntry> narratables();

        void updateNarration(NarrationElementOutput output) {
            List<NarratableEntry> narratables = this.narratables();
            Screen.NarratableSearchResult result = Screen.findNarratableWidget(narratables, this.lastNarratable);
            if (result != null) {
                if (result.priority().isTerminal()) {
                    this.lastNarratable = result.entry();
                }
                if (narratables.size() > 1) {
                    output.add(NarratedElementType.POSITION, (Component)Component.translatable("narrator.position.object_list", result.index() + 1, narratables.size()));
                }
                result.entry().updateNarration(output.nest());
            }
        }
    }
}

