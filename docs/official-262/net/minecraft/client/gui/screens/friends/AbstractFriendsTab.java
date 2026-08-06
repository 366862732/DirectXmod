/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.friends;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.friends.AbstractFriendsEntryContainerWidget;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractFriendsTab
implements Tab {
    static final int SPACING = 8;
    static final int SCROLLBAR_SPACING = 2;
    static final int LIST_MARGIN = 8;
    protected final int width;
    protected int height;

    public AbstractFriendsTab(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected int getListContentWidth() {
        return this.width - 16;
    }

    abstract void rearrangeElements();

    protected abstract Layout entriesContainer();

    final void disable() {
        this.entriesContainer().visitWidgets(widget -> {
            if (widget instanceof AbstractFriendsEntryContainerWidget) {
                AbstractFriendsEntryContainerWidget entry = (AbstractFriendsEntryContainerWidget)widget;
                entry.disable();
            }
        });
    }

    void setHeight(int height) {
        this.height = height;
        this.rearrangeElements();
    }

    FrameLayout createCenteredFrame(LayoutElement child, int frameWidth, int frameHeight) {
        FrameLayout frameLayout = new FrameLayout(frameWidth, frameHeight);
        frameLayout.defaultChildLayoutSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        frameLayout.addChild(child);
        return frameLayout;
    }

    FocusableTextWidget createText(Component message, Font font, int maxWidth) {
        return FocusableTextWidget.builder(message, font).maxWidth(maxWidth).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build();
    }

    MultiLineTextWidget createCenteredText(Component message, Font font, int maxWidth) {
        return this.createText(message, font, maxWidth).setCentered(true);
    }
}

