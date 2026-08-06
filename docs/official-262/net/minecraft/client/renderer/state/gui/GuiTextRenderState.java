/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public final class GuiTextRenderState
implements ScreenArea {
    private final Font font;
    private final FormattedCharSequence text;
    public final Matrix3x2fc pose;
    private final int x;
    private final int y;
    private final int color;
    private final int backgroundColor;
    private final boolean dropShadow;
    private final boolean includeEmpty;
    public final @Nullable ScreenRectangle scissor;
    private @Nullable Font.PreparedText preparedText;
    private @Nullable ScreenRectangle bounds;

    public GuiTextRenderState(Font font, FormattedCharSequence text, Matrix3x2fc pose, int x, int y, int color, int backgroundColor, boolean dropShadow, boolean includeEmpty, @Nullable ScreenRectangle scissor) {
        this.font = font;
        this.text = text;
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.dropShadow = dropShadow;
        this.includeEmpty = includeEmpty;
        this.scissor = scissor;
    }

    public Font.PreparedText ensurePrepared() {
        if (this.preparedText == null) {
            this.preparedText = this.font.prepareText(this.text, this.x, this.y, this.color, this.dropShadow, this.includeEmpty, this.backgroundColor);
            ScreenRectangle bounds = this.preparedText.bounds();
            if (bounds != null) {
                bounds = bounds.transformMaxBounds(this.pose);
                this.bounds = this.scissor != null ? this.scissor.intersection(bounds) : bounds;
            }
        }
        return this.preparedText;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        this.ensurePrepared();
        return this.bounds;
    }
}

