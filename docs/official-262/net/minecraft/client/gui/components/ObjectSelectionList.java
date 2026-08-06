/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class ObjectSelectionList<E extends Entry<E>>
extends AbstractSelectionList<E> {
    private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

    public ObjectSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (this.getItemCount() == 0) {
            return null;
        }
        if (this.isFocused() && navigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
            FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)navigationEvent;
            Entry entry = (Entry)this.nextEntry(arrowNavigation.direction());
            if (entry != null) {
                return ComponentPath.path(this, ComponentPath.leaf(entry));
            }
            this.setFocused(null);
            this.setSelected(null);
            return null;
        }
        if (!this.isFocused()) {
            Entry entry = (Entry)this.getSelected();
            if (entry == null) {
                entry = (Entry)this.nextEntry(navigationEvent.getVerticalDirectionForInitialFocus());
            }
            if (entry == null) {
                return null;
            }
            return ComponentPath.path(this, ComponentPath.leaf(entry));
        }
        return null;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        Entry hovered = (Entry)this.getHovered();
        if (hovered != null) {
            this.narrateListElementPosition(output.nest(), hovered);
            hovered.updateNarration(output);
        } else {
            Entry selected = (Entry)this.getSelected();
            if (selected != null) {
                this.narrateListElementPosition(output.nest(), selected);
                selected.updateNarration(output);
            }
        }
        if (this.isFocused()) {
            output.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry<E extends Entry<E>>
    extends AbstractSelectionList.Entry<E>
    implements NarrationSupplier {
        public abstract Component getNarration();

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return true;
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.getNarration());
        }
    }
}

