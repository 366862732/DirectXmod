/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 */
package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
    private final String plainText;
    private final List<Style> charStyles;
    private final Int2IntFunction reverseCharModifier;

    private SubStringSource(String plainText, List<Style> charStyles, Int2IntFunction reverseCharModifier) {
        this.plainText = plainText;
        this.charStyles = ImmutableList.copyOf(charStyles);
        this.reverseCharModifier = reverseCharModifier;
    }

    public String getPlainText() {
        return this.plainText;
    }

    public List<FormattedCharSequence> substring(int start, int length, boolean reverse) {
        if (length == 0) {
            return ImmutableList.of();
        }
        ArrayList parts = Lists.newArrayList();
        Style currentRunStyle = this.charStyles.get(start);
        int currentRunStart = start;
        for (int i = 1; i < length; ++i) {
            int actualIndex = start + i;
            Style charStyle = this.charStyles.get(actualIndex);
            if (charStyle.equals(currentRunStyle)) continue;
            String currentRunText = this.plainText.substring(currentRunStart, actualIndex);
            parts.add(reverse ? FormattedCharSequence.backward(currentRunText, currentRunStyle, this.reverseCharModifier) : FormattedCharSequence.forward(currentRunText, currentRunStyle));
            currentRunStyle = charStyle;
            currentRunStart = actualIndex;
        }
        if (currentRunStart < start + length) {
            String lastRunText = this.plainText.substring(currentRunStart, start + length);
            parts.add(reverse ? FormattedCharSequence.backward(lastRunText, currentRunStyle, this.reverseCharModifier) : FormattedCharSequence.forward(lastRunText, currentRunStyle));
        }
        return reverse ? Lists.reverse((List)parts) : parts;
    }

    public static SubStringSource create(FormattedText text) {
        return SubStringSource.create(text, ch -> ch, s -> s);
    }

    public static SubStringSource create(FormattedText text, Int2IntFunction reverseCharModifier, UnaryOperator<String> shaper) {
        StringBuilder plainText = new StringBuilder();
        ArrayList charStyles = Lists.newArrayList();
        text.visit((style, contents) -> {
            StringDecomposer.iterateFormatted(contents, style, (position, charStyle, codepoint) -> {
                plainText.appendCodePoint(codepoint);
                int charCount = Character.charCount(codepoint);
                for (int i = 0; i < charCount; ++i) {
                    charStyles.add(charStyle);
                }
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        return new SubStringSource((String)shaper.apply(plainText.toString()), charStyles, reverseCharModifier);
    }
}

