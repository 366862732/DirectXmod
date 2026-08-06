/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class MultiLineTextWidget
extends AbstractStringWidget {
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final SingleKeyCache<CacheKey, MultiLineLabel> cache = Util.singleKeyCache(key -> {
        if (key.maxRows.isPresent()) {
            return MultiLineLabel.create(font, key.maxWidth, key.maxRows.getAsInt(), key.message);
        }
        return MultiLineLabel.create(font, key.message, key.maxWidth);
    });
    private boolean centered = false;

    public MultiLineTextWidget(Component message, Font font) {
        this(0, 0, message, font);
    }

    public MultiLineTextWidget(int x, int y, Component message, Font font) {
        super(x, y, 0, 0, message, font);
        this.active = false;
    }

    public MultiLineTextWidget setMaxWidth(int maxWidth) {
        this.maxWidth = OptionalInt.of(maxWidth);
        return this;
    }

    public MultiLineTextWidget setMaxRows(int maxRows) {
        this.maxRows = OptionalInt.of(maxRows);
        return this;
    }

    public MultiLineTextWidget setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    @Override
    public int getWidth() {
        return this.cache.getValue(this.getFreshCacheKey()).getWidth();
    }

    @Override
    public int getHeight() {
        return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * this.getFont().lineHeight;
    }

    @Override
    public void visitLines(ActiveTextCollector output) {
        MultiLineLabel multilineLabel = this.cache.getValue(this.getFreshCacheKey());
        int x = this.getTextX();
        int y = this.getTextY();
        int lineHeight = this.getFont().lineHeight;
        if (this.centered) {
            int midX = this.getX() + this.getWidth() / 2;
            multilineLabel.visitLines(TextAlignment.CENTER, midX, y, lineHeight, output);
        } else {
            multilineLabel.visitLines(TextAlignment.LEFT, x, y, lineHeight, output);
        }
    }

    protected int getTextX() {
        return this.getX();
    }

    protected int getTextY() {
        return this.getY();
    }

    private CacheKey getFreshCacheKey() {
        return new CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    @Environment(value=EnvType.CLIENT)
    private record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
    }
}

