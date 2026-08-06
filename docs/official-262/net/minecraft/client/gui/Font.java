/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.ibm.icu.text.ArabicShaping
 *  com.ibm.icu.text.ArabicShapingException
 *  com.ibm.icu.text.Bidi
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Font {
    private static final float EFFECT_DEPTH = 0.01f;
    private static final float OVER_EFFECT_DEPTH = 0.01f;
    private static final float UNDER_EFFECT_DEPTH = -0.01f;
    public static final float SHADOW_DEPTH = 0.03f;
    public final int lineHeight = 9;
    private final RandomSource random = RandomSource.create();
    private final Provider provider;
    private final StringSplitter splitter;

    public Font(Provider provider) {
        this.provider = provider;
        this.splitter = new StringSplitter((codepoint, style) -> this.getGlyphSource(style.getFont()).getGlyph(codepoint).info().getAdvance(style.isBold()));
    }

    private GlyphSource getGlyphSource(FontDescription fontLocation) {
        return this.provider.glyphs(fontLocation);
    }

    public String bidirectionalShaping(String text) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        }
        catch (ArabicShapingException arabicShapingException) {
            return text;
        }
    }

    public PreparedText prepare8xTextOutline(FormattedCharSequence str, float x, float y, int outlineColor) {
        PreparedTextBuilder outlineOutput = new PreparedTextBuilder(this, 0.0f, 0.0f, outlineColor, false, false);
        for (int xo = -1; xo <= 1; ++xo) {
            for (int yo = -1; yo <= 1; ++yo) {
                if (xo == 0 && yo == 0) continue;
                float[] startX = new float[]{x};
                int finalXo = xo;
                int finalYo = yo;
                str.accept((position, style, codepoint) -> {
                    boolean bold = style.isBold();
                    BakedGlyph glyph = this.getGlyph(codepoint, style);
                    outlineOutput.x = startX[0] + (float)finalXo * glyph.info().getShadowOffset();
                    outlineOutput.y = y + (float)finalYo * glyph.info().getShadowOffset();
                    startX[0] = startX[0] + glyph.info().getAdvance(bold);
                    return outlineOutput.accept(position, style.withColor(outlineColor), glyph);
                });
            }
        }
        outlineOutput.discardEffects();
        return outlineOutput;
    }

    private BakedGlyph getGlyph(int codepoint, Style style) {
        GlyphSource glyphSource = this.getGlyphSource(style.getFont());
        BakedGlyph glyph = glyphSource.getGlyph(codepoint);
        if (style.isObfuscated() && codepoint != 32) {
            int targetWidth = Mth.ceil(glyph.info().getAdvance(false));
            glyph = glyphSource.getRandomGlyph(this.random, targetWidth);
        }
        return glyph;
    }

    public PreparedText prepareText(String text, float x, float y, int originalColor, boolean drawShadow, int backgroundColor) {
        if (this.isBidirectional()) {
            text = this.bidirectionalShaping(text);
        }
        PreparedTextBuilder output = new PreparedTextBuilder(this, x, y, originalColor, backgroundColor, drawShadow, false);
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (FormattedCharSink)output);
        return output;
    }

    public PreparedText prepareText(FormattedCharSequence text, float x, float y, int originalColor, boolean drawShadow, boolean includeEmpty, int backgroundColor) {
        PreparedTextBuilder builder = new PreparedTextBuilder(this, x, y, originalColor, backgroundColor, drawShadow, includeEmpty);
        text.accept(builder);
        return builder;
    }

    public int width(String str) {
        return Mth.ceil(this.splitter.stringWidth(str));
    }

    public int width(FormattedText text) {
        return Mth.ceil(this.splitter.stringWidth(text));
    }

    public int width(FormattedCharSequence text) {
        return Mth.ceil(this.splitter.stringWidth(text));
    }

    public String plainSubstrByWidth(String str, int width, boolean reverse) {
        return reverse ? this.splitter.plainTailByWidth(str, width, Style.EMPTY) : this.splitter.plainHeadByWidth(str, width, Style.EMPTY);
    }

    public String plainSubstrByWidth(String str, int width) {
        return this.splitter.plainHeadByWidth(str, width, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText text, int width) {
        return this.splitter.headByWidth(text, width, Style.EMPTY);
    }

    public int wordWrapHeight(FormattedText input, int textWidth) {
        return 9 * this.splitter.splitLines(input, textWidth, Style.EMPTY).size();
    }

    public List<FormattedCharSequence> split(FormattedText input, int maxWidth) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(input, maxWidth, Style.EMPTY));
    }

    public List<FormattedText> splitIgnoringLanguage(FormattedText input, int maxWidth) {
        return this.splitter.splitLines(input, maxWidth, Style.EMPTY);
    }

    public boolean isBidirectional() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    public StringSplitter getSplitter() {
        return this.splitter;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Provider {
        public GlyphSource glyphs(FontDescription var1);

        public EffectGlyph effect();
    }

    @Environment(value=EnvType.CLIENT)
    private class PreparedTextBuilder
    implements PreparedText,
    FormattedCharSink {
        private final boolean drawShadow;
        private final int color;
        private final int backgroundColor;
        private final boolean includeEmpty;
        private float x;
        private float y;
        private float left;
        private float top;
        private float right;
        private float bottom;
        private float backgroundLeft;
        private float backgroundTop;
        private float backgroundRight;
        private float backgroundBottom;
        private final List<TextRenderable.Styled> glyphs;
        private @Nullable List<TextRenderable> effects;
        private @Nullable List<EmptyArea> emptyAreas;
        final /* synthetic */ Font this$0;

        public PreparedTextBuilder(Font font, float x, float y, int color, boolean drawShadow, boolean includeEmpty) {
            this(font, x, y, color, 0, drawShadow, includeEmpty);
        }

        public PreparedTextBuilder(Font font, float x, float y, int color, int backgroundColor, boolean drawShadow, boolean includeEmpty) {
            Font font2 = font;
            Objects.requireNonNull(font2);
            this.this$0 = font2;
            this.left = Float.MAX_VALUE;
            this.top = Float.MAX_VALUE;
            this.right = -3.4028235E38f;
            this.bottom = -3.4028235E38f;
            this.backgroundLeft = Float.MAX_VALUE;
            this.backgroundTop = Float.MAX_VALUE;
            this.backgroundRight = -3.4028235E38f;
            this.backgroundBottom = -3.4028235E38f;
            this.glyphs = new ArrayList<TextRenderable.Styled>();
            this.x = x;
            this.y = y;
            this.drawShadow = drawShadow;
            this.color = color;
            this.backgroundColor = backgroundColor;
            this.includeEmpty = includeEmpty;
            this.markBackground(x, y, 0.0f);
        }

        private void markSize(float left, float top, float right, float bottom) {
            this.left = Math.min(this.left, left);
            this.top = Math.min(this.top, top);
            this.right = Math.max(this.right, right);
            this.bottom = Math.max(this.bottom, bottom);
        }

        private void markBackground(float x, float y, float advance) {
            if (ARGB.alpha(this.backgroundColor) == 0) {
                return;
            }
            this.backgroundLeft = Math.min(this.backgroundLeft, x - 1.0f);
            this.backgroundTop = Math.min(this.backgroundTop, y - 1.0f);
            this.backgroundRight = Math.max(this.backgroundRight, x + advance);
            this.backgroundBottom = Math.max(this.backgroundBottom, y + 9.0f);
            this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
        }

        private void addGlyph(TextRenderable.Styled instance) {
            this.glyphs.add(instance);
            this.markSize(instance.left(), instance.top(), instance.right(), instance.bottom());
        }

        private void addEffect(TextRenderable effect) {
            if (this.effects == null) {
                this.effects = new ArrayList<TextRenderable>();
            }
            this.effects.add(effect);
            this.markSize(effect.left(), effect.top(), effect.right(), effect.bottom());
        }

        private void addEmptyGlyph(EmptyArea empty) {
            if (this.emptyAreas == null) {
                this.emptyAreas = new ArrayList<EmptyArea>();
            }
            this.emptyAreas.add(empty);
        }

        @Override
        public boolean accept(int position, Style style, int c) {
            BakedGlyph glyph = this.this$0.getGlyph(c, style);
            return this.accept(position, style, glyph);
        }

        public boolean accept(int position, Style style, BakedGlyph glyph) {
            float shadowOffset;
            GlyphInfo glyphInfo = glyph.info();
            boolean bold = style.isBold();
            TextColor styleColor = style.getColor();
            int textColor = this.getTextColor(styleColor);
            int shadowColor = this.getShadowColor(style, textColor);
            float advance = glyphInfo.getAdvance(bold);
            float effectX0 = position == 0 ? this.x - 1.0f : this.x;
            float boldOffset = bold ? glyphInfo.getBoldOffset() : 0.0f;
            TextRenderable.Styled instance = glyph.createGlyph(this.x, this.y, textColor, shadowColor, style, boldOffset, shadowOffset = glyphInfo.getShadowOffset());
            if (instance != null) {
                this.addGlyph(instance);
            } else if (this.includeEmpty) {
                this.addEmptyGlyph(new EmptyArea(this.x, this.y, advance, 7.0f, 9.0f, style));
            }
            this.markBackground(this.x, this.y, advance);
            if (style.isStrikethrough()) {
                this.addEffect(this.this$0.provider.effect().createEffect(effectX0, this.y + 4.5f - 1.0f, this.x + advance, this.y + 4.5f, 0.01f, textColor, shadowColor, shadowOffset));
            }
            if (style.isUnderlined()) {
                this.addEffect(this.this$0.provider.effect().createEffect(effectX0, this.y + 9.0f - 1.0f, this.x + advance, this.y + 9.0f, 0.01f, textColor, shadowColor, shadowOffset));
            }
            this.x += advance;
            return true;
        }

        @Override
        public void visit(GlyphVisitor visitor) {
            if (ARGB.alpha(this.backgroundColor) != 0) {
                visitor.acceptEffect(this.this$0.provider.effect().createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01f, this.backgroundColor, 0, 0.0f));
            }
            for (TextRenderable.Styled glyph : this.glyphs) {
                visitor.acceptGlyph(glyph);
            }
            if (this.effects != null) {
                for (TextRenderable effect : this.effects) {
                    visitor.acceptEffect(effect);
                }
            }
            if (this.emptyAreas != null) {
                for (EmptyArea emptyArea : this.emptyAreas) {
                    visitor.acceptEmptyArea(emptyArea);
                }
            }
        }

        public void discardEffects() {
            this.effects = null;
        }

        private int getTextColor(@Nullable TextColor textColor) {
            if (textColor != null) {
                int alpha = ARGB.alpha(this.color);
                int rgb = textColor.getValue();
                return ARGB.color(alpha, rgb);
            }
            return this.color;
        }

        private int getShadowColor(Style style, int textColor) {
            Integer shadow = style.getShadowColor();
            if (shadow != null) {
                float textAlpha = ARGB.alphaFloat(textColor);
                float shadowAlpha = ARGB.alphaFloat(shadow);
                if (textAlpha != 1.0f) {
                    return ARGB.color(ARGB.as8BitChannel(textAlpha * shadowAlpha), (int)shadow);
                }
                return shadow;
            }
            if (this.drawShadow) {
                return ARGB.scaleRGB(textColor, 0.25f);
            }
            return 0;
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            if (this.left >= this.right || this.top >= this.bottom) {
                return null;
            }
            int left = Mth.floor(this.left);
            int top = Mth.floor(this.top);
            int right = Mth.ceil(this.right);
            int bottom = Mth.ceil(this.bottom);
            return new ScreenRectangle(left, top, right - left, bottom - top);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface GlyphVisitor {
        default public void acceptGlyph(TextRenderable.Styled glyph) {
            this.acceptRenderable(glyph);
        }

        default public void acceptEffect(TextRenderable effect) {
            this.acceptRenderable(effect);
        }

        default public void acceptRenderable(TextRenderable renderable) {
        }

        default public void acceptEmptyArea(EmptyArea empty) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface PreparedText {
        public void visit(GlyphVisitor var1);

        public @Nullable ScreenRectangle bounds();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum DisplayMode {
        NORMAL,
        SEE_THROUGH,
        POLYGON_OFFSET;

    }
}

