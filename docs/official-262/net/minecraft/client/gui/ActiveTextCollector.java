/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.joml.Vector2f
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface ActiveTextCollector {
    public static final double PERIOD_PER_SCROLLED_PIXEL = 0.5;
    public static final double MIN_SCROLL_PERIOD = 3.0;

    public Parameters defaultParameters();

    public void defaultParameters(Parameters var1);

    default public void accept(int x, int y, FormattedCharSequence text) {
        this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text);
    }

    default public void accept(int x, int y, Component text) {
        this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text.getVisualOrderText());
    }

    default public void accept(TextAlignment alignment, int anchorX, int y, Parameters parameters, Component text) {
        this.accept(alignment, anchorX, y, parameters, text.getVisualOrderText());
    }

    public void accept(TextAlignment var1, int var2, int var3, Parameters var4, FormattedCharSequence var5);

    default public void accept(TextAlignment alignment, int anchorX, int y, Component text) {
        this.accept(alignment, anchorX, y, text.getVisualOrderText());
    }

    default public void accept(TextAlignment alignment, int anchorX, int y, FormattedCharSequence text) {
        this.accept(alignment, anchorX, y, this.defaultParameters(), text);
    }

    public void acceptScrolling(Component var1, int var2, int var3, int var4, int var5, int var6, Parameters var7);

    default public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom) {
        this.acceptScrolling(message, centerX, left, right, top, bottom, this.defaultParameters());
    }

    default public void acceptScrollingWithDefaultCenter(Component message, int left, int right, int top, int bottom) {
        this.acceptScrolling(message, (left + right) / 2, left, right, top, bottom);
    }

    default public void defaultScrollingHelper(Component message, int centerX, int left, int right, int top, int bottom, int lineWidth, int lineHeight, Parameters parameters) {
        int textTop = (top + bottom - lineHeight) / 2 + 1;
        int availableMessageWidth = right - left;
        if (lineWidth > availableMessageWidth) {
            int maxPosition = lineWidth - availableMessageWidth;
            double time = (double)Util.getMillis() / 1000.0;
            double period = Math.max((double)maxPosition * 0.5, 3.0);
            double alpha = Math.sin(1.5707963267948966 * Math.cos(Math.PI * 2 * time / period)) / 2.0 + 0.5;
            double pos = Mth.lerp(alpha, 0.0, (double)maxPosition);
            Parameters localParameters = parameters.withScissor(left, right, top, bottom);
            this.accept(TextAlignment.LEFT, left - (int)pos, textTop, localParameters, message.getVisualOrderText());
        } else {
            int textX = Mth.clamp(centerX, left + lineWidth / 2, right - lineWidth / 2);
            this.accept(TextAlignment.CENTER, textX, textTop, message);
        }
    }

    public static void findElementUnderCursor(GuiTextRenderState text, float testX, float testY, final Consumer<Style> output) {
        ScreenRectangle bounds = text.bounds();
        if (bounds == null || !bounds.containsPoint((int)testX, (int)testY)) {
            return;
        }
        Vector2f localMousePos = text.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(testX, testY));
        final float localMouseX = localMousePos.x();
        final float localMouseY = localMousePos.y();
        text.ensurePrepared().visit(new Font.GlyphVisitor(){

            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                this.acceptActiveArea(glyph);
            }

            @Override
            public void acceptEmptyArea(EmptyArea empty) {
                this.acceptActiveArea(empty);
            }

            private void acceptActiveArea(ActiveArea glyph) {
                if (ActiveTextCollector.isPointInRectangle(localMouseX, localMouseY, glyph.activeLeft(), glyph.activeTop(), glyph.activeRight(), glyph.activeBottom())) {
                    output.accept(glyph.style());
                }
            }
        });
    }

    public static boolean isPointInRectangle(float x, float y, float left, float top, float right, float bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    @Environment(value=EnvType.CLIENT)
    public record Parameters(Matrix3x2fc pose, float opacity, @Nullable ScreenRectangle scissor) {
        public Parameters(Matrix3x2fc pose) {
            this(pose, 1.0f, null);
        }

        public Parameters withPose(Matrix3x2fc pose) {
            return new Parameters(pose, this.opacity, this.scissor);
        }

        public Parameters withScale(float scale) {
            return this.withPose((Matrix3x2fc)this.pose.scale(scale, scale, new Matrix3x2f()));
        }

        public Parameters withOpacity(float opacity) {
            if (this.opacity == opacity) {
                return this;
            }
            return new Parameters(this.pose, opacity, this.scissor);
        }

        public Parameters withScissor(ScreenRectangle scissor) {
            if (scissor.equals(this.scissor)) {
                return this;
            }
            return new Parameters(this.pose, this.opacity, scissor);
        }

        public Parameters withScissor(int left, int right, int top, int bottom) {
            ScreenRectangle newScissor = new ScreenRectangle(left, top, right - left, bottom - top).transformAxisAligned(this.pose);
            if (this.scissor != null) {
                newScissor = Objects.requireNonNullElse(this.scissor.intersection(newScissor), ScreenRectangle.empty());
            }
            return this.withScissor(newScissor);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ClickableStyleFinder
    implements ActiveTextCollector {
        private static final Parameters INITIAL = new Parameters((Matrix3x2fc)new Matrix3x2f());
        private final Font font;
        private final int testX;
        private final int testY;
        private Parameters defaultParameters = INITIAL;
        private boolean includeInsertions;
        private @Nullable Style result;
        private final Consumer<Style> styleScanner = style -> {
            if (style.getClickEvent() != null || this.includeInsertions && style.getInsertion() != null) {
                this.result = style;
            }
        };

        public ClickableStyleFinder(Font font, int testX, int testY) {
            this.font = font;
            this.testX = testX;
            this.testY = testY;
        }

        @Override
        public Parameters defaultParameters() {
            return this.defaultParameters;
        }

        @Override
        public void defaultParameters(Parameters newParameters) {
            this.defaultParameters = newParameters;
        }

        @Override
        public void accept(TextAlignment alignment, int anchorX, int y, Parameters parameters, FormattedCharSequence text) {
            int leftX = alignment.calculateLeft(anchorX, this.font, text);
            GuiTextRenderState renderState = new GuiTextRenderState(this.font, text, parameters.pose(), leftX, y, ARGB.white(parameters.opacity()), 0, true, true, parameters.scissor());
            ActiveTextCollector.findElementUnderCursor(renderState, this.testX, this.testY, this.styleScanner);
        }

        @Override
        public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom, Parameters parameters) {
            int lineWidth = this.font.width(message);
            int lineHeight = this.font.lineHeight;
            this.defaultScrollingHelper(message, centerX, left, right, top, bottom, lineWidth, lineHeight, parameters);
        }

        public ClickableStyleFinder includeInsertions(boolean flag) {
            this.includeInsertions = flag;
            return this;
        }

        public @Nullable Style result() {
            return this.result;
        }
    }
}

