/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 */
package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;

@FunctionalInterface
public interface FormattedCharSequence {
    public static final FormattedCharSequence EMPTY = output -> true;

    public boolean accept(FormattedCharSink var1);

    public static FormattedCharSequence codepoint(int codepoint, Style style) {
        return output -> output.accept(0, style, codepoint);
    }

    public static FormattedCharSequence forward(String plainText, Style style) {
        if (plainText.isEmpty()) {
            return EMPTY;
        }
        return output -> StringDecomposer.iterate(plainText, style, output);
    }

    public static FormattedCharSequence forward(String plainText, Style style, Int2IntFunction modifier) {
        if (plainText.isEmpty()) {
            return EMPTY;
        }
        return output -> StringDecomposer.iterate(plainText, style, FormattedCharSequence.decorateOutput(output, modifier));
    }

    public static FormattedCharSequence backward(String plainText, Style style) {
        if (plainText.isEmpty()) {
            return EMPTY;
        }
        return output -> StringDecomposer.iterateBackwards(plainText, style, output);
    }

    public static FormattedCharSequence backward(String plainText, Style style, Int2IntFunction modifier) {
        if (plainText.isEmpty()) {
            return EMPTY;
        }
        return output -> StringDecomposer.iterateBackwards(plainText, style, FormattedCharSequence.decorateOutput(output, modifier));
    }

    public static FormattedCharSink decorateOutput(FormattedCharSink output, Int2IntFunction modifier) {
        return (p, s, ch) -> output.accept(p, s, (Integer)modifier.apply((Object)ch));
    }

    public static FormattedCharSequence composite() {
        return EMPTY;
    }

    public static FormattedCharSequence composite(FormattedCharSequence part) {
        return part;
    }

    public static FormattedCharSequence composite(FormattedCharSequence first, FormattedCharSequence second) {
        return FormattedCharSequence.fromPair(first, second);
    }

    public static FormattedCharSequence composite(FormattedCharSequence ... parts) {
        return FormattedCharSequence.fromList((List<FormattedCharSequence>)ImmutableList.copyOf((Object[])parts));
    }

    public static FormattedCharSequence composite(List<FormattedCharSequence> parts) {
        int size = parts.size();
        return switch (size) {
            case 0 -> EMPTY;
            case 1 -> parts.get(0);
            case 2 -> FormattedCharSequence.fromPair(parts.get(0), parts.get(1));
            default -> FormattedCharSequence.fromList((List<FormattedCharSequence>)ImmutableList.copyOf(parts));
        };
    }

    public static FormattedCharSequence fromPair(FormattedCharSequence first, FormattedCharSequence second) {
        return output -> first.accept(output) && second.accept(output);
    }

    public static FormattedCharSequence fromList(List<FormattedCharSequence> partCopy) {
        return output -> {
            for (FormattedCharSequence part : partCopy) {
                if (part.accept(output)) continue;
                return false;
            }
            return true;
        };
    }
}

