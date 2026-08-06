/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractButton
extends AbstractWidget.WithInactiveMessage {
    protected static final int TEXT_MARGIN = 2;
    private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("widget/button"), Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted"));
    private @Nullable Supplier<Boolean> overrideRenderHighlightedSprite;

    public AbstractButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public abstract void onPress(InputWithModifiers var1);

    @Override
    protected final void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractContents(graphics, mouseX, mouseY, a);
        this.handleCursor(graphics);
    }

    protected abstract void extractContents(GuiGraphicsExtractor var1, int var2, int var3, float var4);

    protected void extractDefaultLabel(ActiveTextCollector output) {
        this.extractScrollingStringOverContents(output, this.getMessage(), 2);
    }

    protected final void extractDefaultSprite(GuiGraphicsExtractor graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.overrideRenderHighlightedSprite != null ? this.overrideRenderHighlightedSprite.get().booleanValue() : this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onPress(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isActive()) {
            return false;
        }
        if (event.isSelection()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress(event);
            return true;
        }
        return false;
    }

    public void setOverrideRenderHighlightedSprite(Supplier<Boolean> overrideRenderHighlightedSprite) {
        this.overrideRenderHighlightedSprite = overrideRenderHighlightedSprite;
    }
}

