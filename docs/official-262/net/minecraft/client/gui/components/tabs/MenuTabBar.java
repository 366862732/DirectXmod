/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class MenuTabBar
extends TabNavigationBar {
    private static final int HEIGHT = 24;
    private static final int MAX_WIDTH = 400;
    private static final int MARGIN = 14;

    public MenuTabBar(int x, int y, int width, int height, TabManager tabManager, ImmutableList<TabButton> tabButtons, ImmutableList<Tab> tabs) {
        super(x, y, width, height, tabManager, tabButtons, tabs);
    }

    public static Builder builder(TabManager tabManager, int width) {
        return new Builder(tabManager, width);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, 0, this.layout.getY() + this.layout.getHeight() - 2, 0.0f, 0.0f, ((TabButton)this.tabButtons.getFirst()).getX(), 2, 32, 2);
        int afterLastTab = ((TabButton)this.tabButtons.get(this.tabButtons.size() - 1)).getRight();
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, afterLastTab, this.layout.getY() + this.layout.getHeight() - 2, 0.0f, 0.0f, this.width, 2, 32, 2);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void arrangeElements(int width) {
        this.width = width;
        int tabsWidth = Math.min(400, width) - 28;
        int tabWidth = Mth.roundToward(tabsWidth / this.tabs.size(), 2);
        for (TabButton button : this.tabButtons) {
            button.setWidth(tabWidth);
        }
        this.layout.arrangeElements();
        this.layout.setX(Mth.roundToward((width - tabsWidth) / 2, 2));
        this.layout.setY(0);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder
    extends TabNavigationBar.Builder {
        private Builder(TabManager tabManager, int width) {
            super(tabManager, 0, 0, width, 24);
        }

        public Builder addTab(Tab tab) {
            super.addTab(new MenuTabButton(this.tabManager, tab, 0, this.height), tab);
            return this;
        }

        public Builder addTabs(Tab ... tabs) {
            for (Tab tab : tabs) {
                this.addTab(tab);
            }
            return this;
        }

        @Override
        public MenuTabBar build() {
            return new MenuTabBar(this.x, this.y, this.width, this.height, this.tabManager, (ImmutableList<TabButton>)ImmutableList.copyOf((Collection)this.tabButtons), (ImmutableList<Tab>)ImmutableList.copyOf((Collection)this.tabs));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class MenuTabButton
    extends TabButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("widget/tab_selected"), Identifier.withDefaultNamespace("widget/tab"), Identifier.withDefaultNamespace("widget/tab_selected_highlighted"), Identifier.withDefaultNamespace("widget/tab_highlighted"));
        private static final int SELECTED_OFFSET = 3;
        private static final int TEXT_MARGIN = 1;
        private static final int UNDERLINE_HEIGHT = 1;
        private static final int UNDERLINE_MARGIN_X = 4;
        private static final int UNDERLINE_MARGIN_BOTTOM = 2;

        public MenuTabButton(TabManager tabManager, Tab tab, int width, int height) {
            super(tabManager, tab, width, height);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            int underlineColor;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.isSelected(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
            Font font = Minecraft.getInstance().font;
            int n = underlineColor = this.active ? -1 : -6250336;
            if (this.isSelected()) {
                this.renderMenuBackground(graphics, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
                this.renderFocusUnderline(graphics, font, underlineColor);
            }
            this.renderLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
            this.handleCursor(graphics);
        }

        protected void renderMenuBackground(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
            Screen.extractMenuBackgroundTexture(graphics, Screen.MENU_BACKGROUND, x0, y0, 0.0f, 0.0f, x1 - x0, y1 - y0);
        }

        private void renderLabel(ActiveTextCollector output) {
            int left = this.getX() + 1;
            int top = this.getY() + (this.isSelected() ? 0 : 3);
            int right = this.getX() + this.getWidth() - 1;
            int bottom = this.getY() + this.getHeight();
            output.acceptScrollingWithDefaultCenter(this.getMessage(), left, right, top, bottom);
        }

        private void renderFocusUnderline(GuiGraphicsExtractor graphics, Font font, int color) {
            int width = Math.min(font.width(this.getMessage()), this.getWidth() - 4);
            int left = this.getX() + (this.getWidth() - width) / 2;
            int top = this.getY() + this.getHeight() - 2;
            graphics.fill(left, top, left + width, top + 1, color);
        }
    }
}

