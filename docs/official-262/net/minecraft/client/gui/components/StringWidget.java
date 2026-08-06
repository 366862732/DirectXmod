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
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class StringWidget
extends AbstractStringWidget {
    private static final int TEXT_MARGIN = 2;
    private int maxWidth = 0;
    private int cachedWidth = 0;
    private boolean cachedWidthDirty = true;
    private TextOverflow textOverflow = TextOverflow.CLAMPED;

    public StringWidget(Component message, Font font) {
        this(0, 0, font.width(message.getVisualOrderText()), font.lineHeight, message, font);
    }

    public StringWidget(int width, int height, Component message, Font font) {
        this(0, 0, width, height, message, font);
    }

    public StringWidget(int x, int y, int width, int height, Component message, Font font) {
        super(x, y, width, height, message, font);
        this.active = false;
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.cachedWidthDirty = true;
    }

    public StringWidget setMaxWidth(int maxWidth) {
        return this.setMaxWidth(maxWidth, TextOverflow.CLAMPED);
    }

    public StringWidget setMaxWidth(int maxWidth, TextOverflow textOverflow) {
        this.maxWidth = maxWidth;
        this.textOverflow = textOverflow;
        return this;
    }

    @Override
    public int getWidth() {
        if (this.maxWidth > 0) {
            if (this.cachedWidthDirty) {
                this.cachedWidth = Math.min(this.maxWidth, this.getFont().width(this.getMessage().getVisualOrderText()));
                this.cachedWidthDirty = false;
            }
            return this.cachedWidth;
        }
        return super.getWidth();
    }

    @Override
    public void visitLines(ActiveTextCollector output) {
        boolean textOverflow;
        Component message = this.getMessage();
        Font font = this.getFont();
        int maxWidth = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
        int textWidth = font.width(message);
        int x = this.getX();
        int y = this.getY() + (this.getHeight() - font.lineHeight) / 2;
        boolean bl = textOverflow = textWidth > maxWidth;
        if (textOverflow) {
            switch (this.textOverflow.ordinal()) {
                case 0: {
                    output.accept(x, y, ComponentRenderUtils.clipText(message, font, maxWidth));
                    break;
                }
                case 1: {
                    this.extractScrollingStringOverContents(output, message, 2);
                }
            }
        } else {
            output.accept(x, y, message.getVisualOrderText());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum TextOverflow {
        CLAMPED,
        SCROLLING;

    }
}

