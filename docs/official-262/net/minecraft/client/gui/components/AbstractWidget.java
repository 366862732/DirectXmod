/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.time.Duration;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractWidget
implements LayoutElement,
Renderable,
GuiEventListener,
NarratableEntry {
    protected int width;
    protected int height;
    private int x;
    private int y;
    protected Component message;
    protected boolean isHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0f;
    private int tabOrderGroup;
    private boolean focused;
    private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

    public AbstractWidget(int x, int y, int width, int height, Component message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) {
            return;
        }
        this.isHovered = graphics.containsPointInScissor(mouseX, mouseY) && this.areCoordinatesInRectangle(mouseX, mouseY);
        this.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        this.extractTooltipForNextRenderPass(graphics, mouseX, mouseY);
    }

    protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.tooltip.refreshTooltipForNextRenderPass(graphics, mouseX, mouseY, this.isHovered(), this.isFocused(), this.getRectangle());
    }

    protected void handleCursor(GuiGraphicsExtractor graphics) {
        if (this.isHovered()) {
            graphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    public void setTooltip(@Nullable Tooltip tooltip) {
        this.tooltip.set(tooltip);
    }

    public void setTooltipDelay(Duration delay) {
        this.tooltip.setDelay(delay);
    }

    protected MutableComponent createNarrationMessage() {
        return AbstractWidget.wrapDefaultNarrationMessage(this.getMessage());
    }

    public static MutableComponent wrapDefaultNarrationMessage(Component message) {
        return Component.translatable("gui.narrate.button", message);
    }

    protected abstract void extractWidgetRenderState(GuiGraphicsExtractor var1, int var2, int var3, float var4);

    protected void extractScrollingStringOverContents(ActiveTextCollector output, Component message, int margin) {
        int left = this.getX() + margin;
        int right = this.getX() + this.getWidth() - margin;
        int top = this.getY();
        int bottom = this.getY() + this.getHeight();
        output.acceptScrollingWithDefaultCenter(message, left, right, top, bottom);
    }

    public void onClick(MouseButtonEvent event, boolean doubleClick) {
    }

    public void onRelease(MouseButtonEvent event) {
    }

    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean isMouseOver;
        if (!this.isActive()) {
            return false;
        }
        if (this.isValidClickButton(event.buttonInfo()) && (isMouseOver = this.isMouseOver(event.x(), event.y()))) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.isValidClickButton(event.buttonInfo())) {
            this.onRelease(event);
            return true;
        }
        return false;
    }

    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isValidClickButton(event.buttonInfo())) {
            this.onDrag(event, dx, dy);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!this.isActive()) {
            return null;
        }
        if (!this.isFocused()) {
            return ComponentPath.leaf(this);
        }
        return null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isActive() && this.areCoordinatesInRectangle(mouseX, mouseY);
    }

    public void playDownSound(SoundManager soundManager) {
        AbstractWidget.playButtonClickSound(soundManager);
    }

    public static void playButtonClickSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public Component getMessage() {
        return this.message;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isHoveredOrFocused() {
        return this.isHovered() || this.isFocused();
    }

    @Override
    public boolean isActive() {
        return this.visible && this.active;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        if (this.isHovered) {
            return NarratableEntry.NarrationPriority.HOVERED;
        }
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public final void updateNarration(NarrationElementOutput output) {
        this.updateWidgetNarration(output);
        this.tooltip.updateNarration(output);
    }

    protected abstract void updateWidgetNarration(NarrationElementOutput var1);

    protected void defaultButtonNarrationText(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.button.usage.focused"));
            } else {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.button.usage.hovered"));
            }
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    public int getRight() {
        return this.getX() + this.getWidth();
    }

    public int getBottom() {
        return this.getY() + this.getHeight();
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        widgetVisitor.accept(this);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }

    private boolean areCoordinatesInRectangle(double x, double y) {
        return x >= (double)this.getX() && y >= (double)this.getY() && x < (double)this.getRight() && y < (double)this.getBottom();
    }

    public void setRectangle(int width, int height, int x, int y) {
        this.setSize(width, height);
        this.setPosition(x, y);
    }

    @Override
    public int getTabOrderGroup() {
        return this.tabOrderGroup;
    }

    public void setTabOrderGroup(int tabOrderGroup) {
        this.tabOrderGroup = tabOrderGroup;
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class WithInactiveMessage
    extends AbstractWidget {
        private Component inactiveMessage;

        public static Component defaultInactiveMessage(Component activeMessage) {
            return ComponentUtils.mergeStyles(activeMessage, Style.EMPTY.withColor(-6250336));
        }

        public WithInactiveMessage(int x, int y, int width, int height, Component message) {
            super(x, y, width, height, message);
            this.inactiveMessage = WithInactiveMessage.defaultInactiveMessage(message);
        }

        @Override
        public Component getMessage() {
            return this.active ? super.getMessage() : this.inactiveMessage;
        }

        @Override
        public void setMessage(Component message) {
            super.setMessage(message);
            this.inactiveMessage = WithInactiveMessage.defaultInactiveMessage(message);
        }
    }
}

