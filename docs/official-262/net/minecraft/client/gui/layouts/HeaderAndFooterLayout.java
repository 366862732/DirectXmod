/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class HeaderAndFooterLayout
implements Layout {
    public static final int MAGIC_PADDING = 13;
    public static final int DEFAULT_HEADER_AND_FOOTER_HEIGHT = 33;
    private static final int CONTENT_MARGIN_TOP = 30;
    private final FrameLayout headerFrame = new FrameLayout();
    private final FrameLayout footerFrame = new FrameLayout();
    private final FrameLayout contentsFrame = new FrameLayout();
    private final Screen screen;
    private int headerHeight;
    private int footerHeight;

    public HeaderAndFooterLayout(Screen screen) {
        this(screen, 33);
    }

    public HeaderAndFooterLayout(Screen screen, int headerAndFooterHeight) {
        this(screen, headerAndFooterHeight, headerAndFooterHeight);
    }

    public HeaderAndFooterLayout(Screen screen, int headerHeight, int footerHeight) {
        this.screen = screen;
        this.headerHeight = headerHeight;
        this.footerHeight = footerHeight;
        this.headerFrame.defaultChildLayoutSetting().align(0.5f, 0.5f);
        this.footerFrame.defaultChildLayoutSetting().align(0.5f, 0.5f);
    }

    @Override
    public void setX(int x) {
    }

    @Override
    public void setY(int y) {
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return this.screen.width;
    }

    @Override
    public int getHeight() {
        return this.screen.height;
    }

    public int getFooterHeight() {
        return this.footerHeight;
    }

    public void setFooterHeight(int footerHeight) {
        this.footerHeight = footerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    public int getHeaderHeight() {
        return this.headerHeight;
    }

    public int getContentHeight() {
        return this.screen.height - this.getHeaderHeight() - this.getFooterHeight();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        this.headerFrame.visitChildren(layoutElementVisitor);
        this.contentsFrame.visitChildren(layoutElementVisitor);
        this.footerFrame.visitChildren(layoutElementVisitor);
    }

    @Override
    public void removeChildren() {
        this.headerFrame.removeChildren();
        this.contentsFrame.removeChildren();
        this.footerFrame.removeChildren();
    }

    @Override
    public void arrangeElements() {
        int headerHeight = this.getHeaderHeight();
        int footerHeight = this.getFooterHeight();
        this.headerFrame.setMinWidth(this.screen.width);
        this.headerFrame.setMinHeight(headerHeight);
        this.headerFrame.setPosition(0, 0);
        this.headerFrame.arrangeElements();
        this.footerFrame.setMinWidth(this.screen.width);
        this.footerFrame.setMinHeight(footerHeight);
        this.footerFrame.arrangeElements();
        this.footerFrame.setY(this.screen.height - footerHeight);
        this.contentsFrame.setMinWidth(this.screen.width);
        this.contentsFrame.arrangeElements();
        int preferredContentY = headerHeight + 30;
        int maxContentY = this.screen.height - footerHeight - this.contentsFrame.getHeight();
        this.contentsFrame.setPosition(0, Math.min(preferredContentY, maxContentY));
    }

    public <T extends LayoutElement> T addToHeader(T child) {
        return this.headerFrame.addChild(child);
    }

    public <T extends LayoutElement> T addToHeader(T child, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.headerFrame.addChild(child, layoutSettingsAdjustments);
    }

    public void addTitleHeader(Component component, Font font) {
        this.headerFrame.addChild(new StringWidget(component, font));
    }

    public <T extends LayoutElement> T addToFooter(T child) {
        return this.footerFrame.addChild(child);
    }

    public <T extends LayoutElement> T addToFooter(T child, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.footerFrame.addChild(child, layoutSettingsAdjustments);
    }

    public <T extends LayoutElement> T addToContents(T child) {
        return this.contentsFrame.addChild(child);
    }

    public <T extends LayoutElement> T addToContents(T child, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.contentsFrame.addChild(child, layoutSettingsAdjustments);
    }
}

