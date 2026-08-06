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
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Checkbox
extends AbstractButton {
    private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final Identifier CHECKBOX_SELECTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_selected");
    private static final Identifier CHECKBOX_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/checkbox_highlighted");
    private static final Identifier CHECKBOX_SPRITE = Identifier.withDefaultNamespace("widget/checkbox");
    private static final int SPACING = 4;
    private static final int ROWS = 2;
    private static final int BOX_PADDING = 8;
    private boolean selected;
    private final OnValueChange onValueChange;
    private final MultiLineTextWidget textWidget;

    private Checkbox(int x, int y, int maxWidth, Component message, Font font, boolean selected, OnValueChange onValueChange) {
        super(x, y, 0, 0, message);
        this.textWidget = new MultiLineTextWidget(message, font);
        this.textWidget.setMaxRows(2);
        this.width = this.adjustWidth(maxWidth, font);
        this.height = this.getAdjustedHeight(font);
        this.selected = selected;
        this.onValueChange = onValueChange;
    }

    public int adjustWidth(int maxWidth, Font font) {
        this.width = this.getAdjustedWidth(maxWidth, this.getMessage(), font);
        this.textWidget.setMaxWidth(this.width);
        return this.width;
    }

    private int getAdjustedWidth(int maxWidth, Component message, Font font) {
        return Math.min(Checkbox.getDefaultWidth(message, font), maxWidth);
    }

    private int getAdjustedHeight(Font font) {
        return Math.max(Checkbox.getBoxSize(font), this.textWidget.getHeight());
    }

    private static int getDefaultWidth(Component message, Font font) {
        return Checkbox.getBoxSize(font) + 4 + font.width(message);
    }

    private boolean overflowsRowLimit(Font font) {
        return font.getSplitter().splitLines(this.textWidget.getMessage(), this.width, Style.EMPTY).size() > 2;
    }

    public static Builder builder(Component message, Font font) {
        return new Builder(message, font);
    }

    public static int getBoxSize(Font font) {
        return font.lineHeight + 8;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.selected = !this.selected;
        this.onValueChange.onValueChange(this, this.selected);
    }

    public boolean selected() {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable(this.selected ? "narration.checkbox.usage.focused.uncheck" : "narration.checkbox.usage.focused.check"));
            } else {
                output.add(NarratedElementType.USAGE, (Component)Component.translatable(this.selected ? "narration.checkbox.usage.hovered.uncheck" : "narration.checkbox.usage.hovered.check"));
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Identifier sprite = this.selected ? (this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE) : (this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE);
        int boxSize = Checkbox.getBoxSize(font);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), boxSize, boxSize, ARGB.white(this.alpha));
        int textX = this.getX() + boxSize + 4;
        int textY = this.getY() + boxSize / 2 - this.textWidget.getHeight() / 2;
        this.textWidget.setPosition(textX, textY);
        this.textWidget.visitLines(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.notClickable(this.isHovered())));
    }

    @Environment(value=EnvType.CLIENT)
    public static interface OnValueChange {
        public static final OnValueChange NOP = (checkbox, value) -> {};

        public void onValueChange(Checkbox var1, boolean var2);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Component message;
        private final Font font;
        private int maxWidth;
        private int x = 0;
        private int y = 0;
        private OnValueChange onValueChange = OnValueChange.NOP;
        private boolean selected = false;
        private @Nullable OptionInstance<Boolean> option = null;
        private @Nullable Tooltip tooltip = null;

        private Builder(Component message, Font font) {
            this.message = message;
            this.font = font;
            this.maxWidth = Checkbox.getDefaultWidth(message, font);
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder onValueChange(OnValueChange onValueChange) {
            this.onValueChange = onValueChange;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            this.option = null;
            return this;
        }

        public Builder selected(OptionInstance<Boolean> option) {
            this.option = option;
            this.selected = option.get();
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Checkbox build() {
            OnValueChange onChange = this.option == null ? this.onValueChange : (checkbox, value) -> {
                this.option.set(value);
                this.onValueChange.onValueChange(checkbox, value);
            };
            Checkbox box = new Checkbox(this.x, this.y, this.maxWidth, this.message, this.font, this.selected, onChange);
            if (box.overflowsRowLimit(this.font)) {
                box.setTooltip(this.tooltip);
            }
            return box;
        }
    }
}

