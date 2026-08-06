/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface MultiLineLabel {
    public static final MultiLineLabel EMPTY = new MultiLineLabel(){

        @Override
        public int visitLines(TextAlignment align, int anchorX, int topY, int lineHeight, ActiveTextCollector output) {
            return topY;
        }

        @Override
        public int getLineCount() {
            return 0;
        }

        @Override
        public int getWidth() {
            return 0;
        }
    };

    public static MultiLineLabel create(Font font, Component ... messages) {
        return MultiLineLabel.create(font, Integer.MAX_VALUE, Integer.MAX_VALUE, messages);
    }

    public static MultiLineLabel create(Font font, int maxWidth, Component ... messages) {
        return MultiLineLabel.create(font, maxWidth, Integer.MAX_VALUE, messages);
    }

    public static MultiLineLabel create(Font font, Component message, int maxWidth) {
        return MultiLineLabel.create(font, maxWidth, Integer.MAX_VALUE, message);
    }

    public static MultiLineLabel create(final Font font, final int maxWidth, final int maxLines, final Component ... messages) {
        if (messages.length == 0) {
            return EMPTY;
        }
        return new MultiLineLabel(){
            private @Nullable List<TextAndWidth> cachedTextAndWidth;
            private @Nullable Language splitWithLanguage;

            @Override
            public int visitLines(TextAlignment align, int anchorX, int topY, int lineHeight, ActiveTextCollector output) {
                int y = topY;
                for (TextAndWidth splitLine : this.getSplitMessage()) {
                    int leftX = align.calculateLeft(anchorX, splitLine.width);
                    output.accept(leftX, y, splitLine.text);
                    y += lineHeight;
                }
                return y;
            }

            private List<TextAndWidth> getSplitMessage() {
                Language currentLanguage = Language.getInstance();
                if (this.cachedTextAndWidth != null && currentLanguage == this.splitWithLanguage) {
                    return this.cachedTextAndWidth;
                }
                this.splitWithLanguage = currentLanguage;
                ArrayList<FormattedText> splitMessage = new ArrayList<FormattedText>();
                for (Component message : messages) {
                    splitMessage.addAll(font.splitIgnoringLanguage(message, maxWidth));
                }
                this.cachedTextAndWidth = new ArrayList<TextAndWidth>();
                int actualMaxLines = Math.min(splitMessage.size(), maxLines);
                List linesToAdd = splitMessage.subList(0, actualMaxLines);
                for (int i = 0; i < linesToAdd.size(); ++i) {
                    FormattedText formattedText = (FormattedText)linesToAdd.get(i);
                    FormattedCharSequence formattedCharSequence = Language.getInstance().getVisualOrder(formattedText);
                    if (i == linesToAdd.size() - 1 && actualMaxLines == maxLines && actualMaxLines != splitMessage.size()) {
                        FormattedText clippedText = font.substrByWidth(formattedText, font.width(formattedText) - font.width(CommonComponents.ELLIPSIS));
                        FormattedText withEllipsis = FormattedText.composite(clippedText, CommonComponents.ELLIPSIS.copy().withStyle(messages[messages.length - 1].getStyle()));
                        this.cachedTextAndWidth.add(new TextAndWidth(Language.getInstance().getVisualOrder(withEllipsis), font.width(withEllipsis)));
                        continue;
                    }
                    this.cachedTextAndWidth.add(new TextAndWidth(formattedCharSequence, font.width(formattedCharSequence)));
                }
                return this.cachedTextAndWidth;
            }

            @Override
            public int getLineCount() {
                return this.getSplitMessage().size();
            }

            @Override
            public int getWidth() {
                return Math.min(maxWidth, this.getSplitMessage().stream().mapToInt(TextAndWidth::width).max().orElse(0));
            }
        };
    }

    public int visitLines(TextAlignment var1, int var2, int var3, int var4, ActiveTextCollector var5);

    public int getLineCount();

    public int getWidth();

    @Environment(value=EnvType.CLIENT)
    public record TextAndWidth(FormattedCharSequence text, int width) {
    }
}

