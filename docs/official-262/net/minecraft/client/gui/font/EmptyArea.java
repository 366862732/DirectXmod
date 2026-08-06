/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.network.chat.Style;

@Environment(value=EnvType.CLIENT)
public record EmptyArea(float x, float y, float advance, float ascent, float height, Style style) implements ActiveArea
{
    public static final float DEFAULT_HEIGHT = 9.0f;
    public static final float DEFAULT_ASCENT = 7.0f;

    @Override
    public float activeLeft() {
        return this.x;
    }

    @Override
    public float activeTop() {
        return this.y + 7.0f - this.ascent;
    }

    @Override
    public float activeRight() {
        return this.x + this.advance;
    }

    @Override
    public float activeBottom() {
        return this.activeTop() + this.height;
    }
}

