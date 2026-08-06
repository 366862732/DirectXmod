/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractStringWidget
extends AbstractWidget {
    private @Nullable Consumer<Style> componentClickHandler = null;
    private final Font font;

    public AbstractStringWidget(int x, int y, int width, int height, Component message, Font font) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public abstract void visitLines(ActiveTextCollector var1);

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        GuiGraphicsExtractor.HoveredTextEffects effects = this.isHovered() ? (this.componentClickHandler != null ? GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR : GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY) : GuiGraphicsExtractor.HoveredTextEffects.NONE;
        this.visitLines(graphics.textRendererForWidget(this, effects));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.componentClickHandler != null) {
            ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.getFont(), (int)event.x(), (int)event.y());
            this.visitLines(finder);
            Style clickedStyle = finder.result();
            if (clickedStyle != null) {
                this.componentClickHandler.accept(clickedStyle);
                return;
            }
        }
        super.onClick(event, doubleClick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    protected final Font getFont() {
        return this.font;
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.setWidth(this.getFont().width(message.getVisualOrderText()));
    }

    public AbstractStringWidget setComponentClickHandler(@Nullable Consumer<Style> clickEventConsumer) {
        this.componentClickHandler = clickEventConsumer;
        return this;
    }

    protected boolean handleStyleClick(Style style) {
        if (this.componentClickHandler != null && style.getClickEvent() != null) {
            this.componentClickHandler.accept(style);
            return true;
        }
        return false;
    }
}

