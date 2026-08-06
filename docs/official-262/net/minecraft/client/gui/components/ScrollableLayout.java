/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ScrollableLayout
implements Layout {
    private static final int DEFAULT_SCROLLBAR_SPACING = 4;
    private final Layout content;
    private final Container container;
    private final ReserveStrategy reserveStrategy;
    private int scrollbarSpacing = 4;
    private int minWidth;
    private int minHeight;
    private int maxHeight;

    public ScrollableLayout(Minecraft minecraft, Layout content, int maxHeight) {
        this(minecraft, content, maxHeight, ReserveStrategy.BOTH);
    }

    public ScrollableLayout(Minecraft minecraft, Layout content, int maxHeight, ReserveStrategy reserveStrategy) {
        this.content = content;
        this.maxHeight = maxHeight;
        this.reserveStrategy = reserveStrategy;
        this.container = new Container(this, minecraft, 0, maxHeight, AbstractScrollArea.defaultSettings(10));
    }

    public void setScrollbarSpacing(int scrollbarSpacing) {
        this.scrollbarSpacing = scrollbarSpacing;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.container.setWidth(Math.max(this.content.getWidth(), minWidth));
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
        this.container.setHeight(Math.max(this.content.getHeight(), minHeight));
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        this.container.setHeight(Math.min(this.content.getHeight(), maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void arrangeElements() {
        this.content.arrangeElements();
        int contentWidth = this.content.getWidth();
        int scrollbarReserve = switch (this.reserveStrategy.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.container.scrollbarReserve();
            case 1 -> 2 * this.container.scrollbarReserve();
        };
        this.container.setWidth(Math.max(contentWidth, this.minWidth) + scrollbarReserve);
        this.container.setHeight(Math.clamp((long)this.container.getHeight(), this.minHeight, this.maxHeight));
        this.container.refreshChildren();
        this.container.refreshScrollAmount();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        layoutElementVisitor.accept(this.container);
    }

    @Override
    public void removeChildren() {
        this.container.children().clear();
        this.content.removeChildren();
    }

    @Override
    public void setX(int x) {
        this.container.setX(x);
    }

    @Override
    public void setY(int y) {
        this.container.setY(y);
    }

    @Override
    public int getX() {
        return this.container.getX();
    }

    @Override
    public int getY() {
        return this.container.getY();
    }

    @Override
    public int getWidth() {
        return this.container.getWidth();
    }

    @Override
    public int getHeight() {
        return this.container.getHeight();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ReserveStrategy {
        RIGHT,
        BOTH;

    }

    @Environment(value=EnvType.CLIENT)
    private class Container
    extends AbstractContainerWidget {
        private final Minecraft minecraft;
        private final List<AbstractWidget> children;
        final /* synthetic */ ScrollableLayout this$0;

        public Container(ScrollableLayout scrollableLayout, Minecraft minecraft, int width, int height, AbstractScrollArea.ScrollbarSettings scrollbarSettings) {
            ScrollableLayout scrollableLayout2 = scrollableLayout;
            Objects.requireNonNull(scrollableLayout2);
            this.this$0 = scrollableLayout2;
            super(0, 0, width, height, CommonComponents.EMPTY, scrollbarSettings);
            this.children = new ArrayList<AbstractWidget>();
            this.minecraft = minecraft;
            this.refreshChildren();
        }

        public void refreshChildren() {
            this.children.clear();
            this.this$0.content.visitWidgets(this.children::add);
        }

        @Override
        protected int contentHeight() {
            return this.this$0.content.getHeight();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
            for (AbstractWidget child : this.children) {
                child.extractRenderState(graphics, mouseX, mouseY, a);
            }
            graphics.disableScissor();
            this.extractScrollbar(graphics, mouseX, mouseY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        @Override
        public ScreenRectangle getBorderForArrowNavigation(ScreenDirection opposite) {
            GuiEventListener focused = this.getFocused();
            return focused != null ? focused.getBorderForArrowNavigation(opposite) : new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight()).getBorder(opposite);
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            super.setFocused(focused);
            if (focused == null || !this.minecraft.getLastInputType().isKeyboard()) {
                return;
            }
            ScreenRectangle area = this.getRectangle();
            ScreenRectangle focusedRect = focused.getRectangle();
            int topDelta = focusedRect.top() - area.top();
            int bottomDelta = focusedRect.bottom() - area.bottom();
            double scrollRate = this.scrollRate();
            if (topDelta < 0) {
                this.setScrollAmount(this.scrollAmount() + (double)topDelta - scrollRate);
            } else if (bottomDelta > 0) {
                this.setScrollAmount(this.scrollAmount() + (double)bottomDelta + scrollRate);
            }
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            this.this$0.content.setX(x + (this.this$0.reserveStrategy == ReserveStrategy.BOTH ? this.scrollbarReserve() : 0));
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            this.this$0.content.setY(y - (int)this.scrollAmount());
        }

        private int scrollbarReserve() {
            return this.this$0.scrollbarSpacing + this.scrollbarWidth();
        }

        @Override
        public void setScrollAmount(double scrollAmount) {
            super.setScrollAmount(scrollAmount);
            this.this$0.content.setY(this.getRectangle().top() - (int)this.scrollAmount());
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public Collection<? extends NarratableEntry> getNarratables() {
            return this.children;
        }
    }
}

