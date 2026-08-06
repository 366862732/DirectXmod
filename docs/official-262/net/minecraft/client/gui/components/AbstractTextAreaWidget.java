/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractTextAreaWidget
extends AbstractScrollArea {
    private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted"));
    private static final int INNER_PADDING = 4;
    public static final int DEFAULT_TOTAL_PADDING = 8;
    private boolean showBackground = true;
    private boolean showDecorations = true;

    public AbstractTextAreaWidget(int x, int y, int width, int height, Component narration, AbstractScrollArea.ScrollbarSettings scrollbarSettings) {
        super(x, y, width, height, narration, scrollbarSettings);
    }

    public AbstractTextAreaWidget(int x, int y, int width, int height, Component narration, AbstractScrollArea.ScrollbarSettings scrollbarSettings, boolean showBackground, boolean showDecorations) {
        this(x, y, width, height, narration, scrollbarSettings);
        this.showBackground = showBackground;
        this.showDecorations = showDecorations;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean scrolling = this.updateScrolling(event);
        return super.mouseClicked(event, doubleClick) || scrolling;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean isUp = event.isUp();
        boolean isDown = event.isDown();
        if (isUp || isDown) {
            double previousScrollAmount = this.scrollAmount();
            this.setScrollAmount(this.scrollAmount() + (double)(isUp ? -1 : 1) * this.scrollRate());
            if (previousScrollAmount != this.scrollAmount()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!this.visible) {
            return;
        }
        if (this.showBackground) {
            this.extractBackground(graphics);
        }
        graphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0f, (float)(-this.scrollAmount()));
        this.extractContents(graphics, mouseX, mouseY, a);
        graphics.pose().popMatrix();
        graphics.disableScissor();
        this.extractScrollbar(graphics, mouseX, mouseY);
        if (this.showDecorations) {
            this.extractDecorations(graphics);
        }
    }

    protected void extractDecorations(GuiGraphicsExtractor graphics) {
    }

    protected int innerPadding() {
        return 4;
    }

    protected int totalInnerPadding() {
        return this.innerPadding() * 2;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && mouseX < (double)(this.getRight() + this.scrollbarWidth()) && mouseY < (double)this.getBottom();
    }

    @Override
    protected int scrollBarX() {
        return this.getRight();
    }

    @Override
    protected int contentHeight() {
        return this.getInnerHeight() + this.totalInnerPadding();
    }

    protected void extractBackground(GuiGraphicsExtractor graphics) {
        this.extractBorder(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected void extractBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        Identifier sprite = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    protected boolean withinContentAreaTopBottom(int top, int bottom) {
        return (double)bottom - this.scrollAmount() >= (double)this.getY() && (double)top - this.scrollAmount() <= (double)(this.getY() + this.height);
    }

    protected abstract int getInnerHeight();

    protected abstract void extractContents(GuiGraphicsExtractor var1, int var2, int var3, float var4);

    protected int getInnerLeft() {
        return this.getX() + this.innerPadding();
    }

    protected int getInnerTop() {
        return this.getY() + this.innerPadding();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}

