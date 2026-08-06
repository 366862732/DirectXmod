/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Unit;

public class StringDecomposer {
    private static final char REPLACEMENT_CHAR = '\ufffd';
    private static final Optional<Object> STOP_ITERATION = Optional.of(Unit.INSTANCE);

    private static boolean feedChar(Style style, FormattedCharSink output, int pos, char ch) {
        if (Character.isSurrogate(ch)) {
            return output.accept(pos, style, 65533);
        }
        return output.accept(pos, style, ch);
    }

    public static boolean iterate(String string, Style style, FormattedCharSink output) {
        int size = string.length();
        for (int i = 0; i < size; ++i) {
            char ch = string.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= size) {
                    if (output.accept(i, style, 65533)) break;
                    return false;
                }
                char low = string.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    if (!output.accept(i, style, Character.toCodePoint(ch, low))) {
                        return false;
                    }
                    ++i;
                    continue;
                }
                if (output.accept(i, style, 65533)) continue;
                return false;
            }
            if (StringDecomposer.feedChar(style, output, i, ch)) continue;
            return false;
        }
        return true;
    }

    public static boolean iterateBackwards(String string, Style style, FormattedCharSink output) {
        int size = string.length();
        for (int i = size - 1; i >= 0; --i) {
            char ch = string.charAt(i);
            if (Character.isLowSurrogate(ch)) {
                if (i - 1 < 0) {
                    if (output.accept(0, style, 65533)) break;
                    return false;
                }
                char high = string.charAt(i - 1);
                if (!(Character.isHighSurrogate(high) ? !output.accept(--i, style, Character.toCodePoint(high, ch)) : !output.accept(i, style, 65533))) continue;
                return false;
            }
            if (StringDecomposer.feedChar(style, output, i, ch)) continue;
            return false;
        }
        return true;
    }

    public static boolean iterateFormatted(String string, Style style, FormattedCharSink output) {
        return StringDecomposer.iterateFormatted(string, 0, style, output);
    }

    public static boolean iterateFormatted(String string, int offset, Style style, FormattedCharSink output) {
        return StringDecomposer.iterateFormatted(string, offset, style, style, output);
    }

    public static boolean iterateFormatted(String string, int offset, Style currentStyle, Style resetStyle, FormattedCharSink output) {
        int size = string.length();
        Style style = currentStyle;
        for (int i = offset; i < size; ++i) {
            char ch = string.charAt(i);
            if (ch == '\u00a7') {
                if (i + 1 >= size) break;
                char code = string.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    style = formatting == ChatFormatting.RESET ? resetStyle : style.applyLegacyFormat(formatting);
                }
                ++i;
                continue;
            }
            if (Character.isHighSurrogate(ch)) {
                if (i + 1 >= size) {
                    if (output.accept(i, style, 65533)) break;
                    return false;
                }
                char low = string.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    if (!output.accept(i, style, Character.toCodePoint(ch, low))) {
                        return false;
                    }
                    ++i;
                    continue;
                }
                if (output.accept(i, style, 65533)) continue;
                return false;
            }
            if (StringDecomposer.feedChar(style, output, i, ch)) continue;
            return false;
        }
        return true;
    }

    public static boolean iterateFormatted(FormattedText component, Style rootStyle, FormattedCharSink output) {
        return component.visit((style, contents) -> StringDecomposer.iterateFormatted(contents, 0, style, output) ? Optional.empty() : STOP_ITERATION, rootStyle).isEmpty();
    }

    public static String filterBrokenSurrogates(String input) {
        StringBuilder builder = new StringBuilder();
        StringDecomposer.iterate(input, Style.EMPTY, (position, style, codepoint) -> {
            builder.appendCodePoint(codepoint);
            return true;
        });
        return builder.toString();
    }

    public static String getPlainText(FormattedText input) {
        StringBuilder builder = new StringBuilder();
        StringDecomposer.iterateFormatted(input, Style.EMPTY, (int position, Style style, int codepoint) -> {
            builder.appendCodePoint(codepoint);
            return true;
        });
        return builder.toString();
    }
}

