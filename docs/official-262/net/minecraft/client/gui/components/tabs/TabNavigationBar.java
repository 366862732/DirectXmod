/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TabNavigationBar
extends AbstractContainerWidget
implements NarratableEntry,
Renderable {
    private static final int NO_TAB = -1;
    private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");
    protected final FrameLayout layout;
    private final TabManager tabManager;
    protected final ImmutableList<Tab> tabs;
    protected final ImmutableList<TabButton> tabButtons;

    protected TabNavigationBar(int x, int y, int width, int height, TabManager tabManager, ImmutableList<TabButton> tabButtons, ImmutableList<Tab> tabs) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.tabManager = tabManager;
        this.tabButtons = tabButtons;
        this.tabs = ImmutableList.copyOf(tabs);
        this.layout = new FrameLayout();
        this.layout.setPosition(x, y);
        LinearLayout linearLayout = this.layout.addChild(LinearLayout.horizontal());
        for (TabButton tabButton : tabButtons) {
            linearLayout.addChild(tabButton);
        }
        this.layout.arrangeElements();
    }

    @Override
    protected int contentHeight() {
        return this.height;
    }

    public static Builder builder(TabManager tabManager, int x, int y, int width, int height) {
        return new Builder(tabManager, x, y, width, height);
    }

    public void arrangeElements(int width) {
        this.layout.setPosition(this.getX(), this.getY());
        this.layout.arrangeElements();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        AtomicBoolean mouseOver = new AtomicBoolean();
        this.layout.visitChildren(child -> {
            if (child.getRectangle().containsPoint((int)mouseX, (int)mouseY)) {
                mouseOver.set(true);
            }
        });
        return mouseOver.get();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (this.getFocused() != null) {
            this.setFocused(null);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        TabButton button;
        super.setFocused(focused);
        if (focused instanceof TabButton && (button = (TabButton)focused).isActive()) {
            this.tabManager.setCurrentTab(button.tab(), true);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        TabButton button;
        if (!this.isFocused() && (button = this.currentTabButton()) != null) {
            return ComponentPath.path(this, ComponentPath.leaf(button));
        }
        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
            return null;
        }
        return super.nextFocusPath(navigationEvent);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.tabButtons;
    }

    public List<Tab> getTabs() {
        return this.tabs;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        Optional<TabButton> selected = this.tabButtons.stream().filter(AbstractWidget::isHovered).findFirst().or(() -> Optional.ofNullable(this.currentTabButton()));
        selected.ifPresent(button -> {
            this.narrateListElementPosition(output.nest(), (TabButton)button);
            button.updateNarration(output);
        });
        if (this.isFocused()) {
            output.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }
    }

    protected void narrateListElementPosition(NarrationElementOutput output, TabButton widget) {
        int index;
        if (this.tabs.size() > 1 && (index = this.tabButtons.indexOf((Object)widget)) != -1) {
            output.add(NarratedElementType.POSITION, (Component)Component.translatable("narrator.position.tab", index + 1, this.tabs.size()));
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        for (TabButton value : this.tabButtons) {
            value.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        return this.layout.getRectangle();
    }

    public void selectTab(int index, boolean playSound) {
        if (this.isFocused()) {
            this.setFocused((GuiEventListener)this.tabButtons.get(index));
        } else if (((TabButton)this.tabButtons.get(index)).isActive()) {
            this.tabManager.setCurrentTab((Tab)this.tabs.get(index), playSound);
        }
    }

    public void setTabActiveState(int index, boolean active) {
        if (index >= 0 && index < this.tabButtons.size()) {
            ((TabButton)this.tabButtons.get((int)index)).active = active;
        }
    }

    public void setTabTooltip(int index, @Nullable Tooltip hint) {
        if (index >= 0 && index < this.tabButtons.size()) {
            ((TabButton)this.tabButtons.get(index)).setTooltip(hint);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int tabIndex;
        if (event.hasControlDownWithQuirk() && (tabIndex = this.getNextTabIndex(event)) != -1) {
            this.selectTab(Mth.clamp(tabIndex, 0, this.tabs.size() - 1), true);
            return true;
        }
        return false;
    }

    private int getNextTabIndex(KeyEvent event) {
        return this.getNextTabIndex(this.currentTabIndex(), event);
    }

    private int getNextTabIndex(int currentTab, KeyEvent event) {
        int digit = event.getDigit();
        if (digit != -1) {
            return Math.floorMod(digit - 1, 10);
        }
        if (event.isCycleFocus() && currentTab != -1) {
            int nextTabIndex = event.hasShiftDown() ? currentTab - 1 : currentTab + 1;
            int index = Math.floorMod(nextTabIndex, this.tabs.size());
            if (((TabButton)this.tabButtons.get((int)index)).active) {
                return index;
            }
            return this.getNextTabIndex(index, event);
        }
        return -1;
    }

    private int currentTabIndex() {
        Tab currentTab = this.tabManager.getCurrentTab();
        int index = this.tabs.indexOf((Object)currentTab);
        return index != -1 ? index : -1;
    }

    private @Nullable TabButton currentTabButton() {
        int index = this.currentTabIndex();
        return index != -1 ? (TabButton)this.tabButtons.get(index) : null;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        protected final int x;
        protected final int y;
        protected final int width;
        protected final int height;
        protected final TabManager tabManager;
        protected final List<TabButton> tabButtons = new ArrayList<TabButton>();
        protected final List<Tab> tabs = new ArrayList<Tab>();

        protected Builder(TabManager tabManager, int x, int y, int width, int height) {
            this.tabManager = tabManager;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Builder addTab(TabButton button, Tab tab) {
            this.tabButtons.add(button);
            this.tabs.add(tab);
            return this;
        }

        public TabNavigationBar build() {
            return new TabNavigationBar(this.x, this.y, this.width, this.height, this.tabManager, (ImmutableList<TabButton>)ImmutableList.copyOf(this.tabButtons), (ImmutableList<Tab>)ImmutableList.copyOf(this.tabs));
        }
    }
}

