/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableFloat
 *  org.apache.commons.lang3.mutable.MutableInt
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ComponentCollector;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class StringSplitter {
    private final WidthProvider widthProvider;

    public StringSplitter(WidthProvider widthProvider) {
        this.widthProvider = widthProvider;
    }

    public float stringWidth(@Nullable String str) {
        if (str == null) {
            return 0.0f;
        }
        MutableFloat result = new MutableFloat();
        StringDecomposer.iterateFormatted(str, Style.EMPTY, (position, style, codepoint) -> {
            result.add(this.widthProvider.getWidth(codepoint, style));
            return true;
        });
        return result.floatValue();
    }

    public float stringWidth(FormattedText text) {
        MutableFloat result = new MutableFloat();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (position, style, codepoint) -> {
            result.add(this.widthProvider.getWidth(codepoint, style));
            return true;
        });
        return result.floatValue();
    }

    public float stringWidth(FormattedCharSequence text) {
        MutableFloat result = new MutableFloat();
        text.accept((position, style, codepoint) -> {
            result.add(this.widthProvider.getWidth(codepoint, style));
            return true;
        });
        return result.floatValue();
    }

    public int plainIndexAtWidth(String str, int maxWidth, Style style) {
        WidthLimitedCharSink output = new WidthLimitedCharSink(this, maxWidth);
        StringDecomposer.iterate(str, style, output);
        return output.getPosition();
    }

    public String plainHeadByWidth(String str, int maxWidth, Style style) {
        return str.substring(0, this.plainIndexAtWidth(str, maxWidth, style));
    }

    public String plainTailByWidth(String str, int maxWidth, Style style) {
        MutableFloat currentWidth = new MutableFloat();
        MutableInt result = new MutableInt(str.length());
        StringDecomposer.iterateBackwards(str, style, (position, s, codepoint) -> {
            float w = currentWidth.addAndGet(this.widthProvider.getWidth(codepoint, s));
            if (w > (float)maxWidth) {
                return false;
            }
            result.setValue(position);
            return true;
        });
        return str.substring(result.intValue());
    }

    public FormattedText headByWidth(FormattedText text, int width, Style initialStyle) {
        final WidthLimitedCharSink output = new WidthLimitedCharSink(this, width);
        return text.visit(new FormattedText.StyledContentConsumer<FormattedText>(){
            private final ComponentCollector collector;
            {
                Objects.requireNonNull(this$0);
                this.collector = new ComponentCollector();
            }

            @Override
            public Optional<FormattedText> accept(Style style, String contents) {
                output.resetPosition();
                if (!StringDecomposer.iterateFormatted(contents, style, (FormattedCharSink)output)) {
                    String partial = contents.substring(0, output.getPosition());
                    if (!partial.isEmpty()) {
                        this.collector.append(FormattedText.of(partial, style));
                    }
                    return Optional.of(this.collector.getResultOrEmpty());
                }
                if (!contents.isEmpty()) {
                    this.collector.append(FormattedText.of(contents, style));
                }
                return Optional.empty();
            }
        }, initialStyle).orElse(text);
    }

    public int findLineBreak(String input, int max, Style initialStyle) {
        LineBreakFinder finder = new LineBreakFinder(this, max);
        StringDecomposer.iterateFormatted(input, initialStyle, (FormattedCharSink)finder);
        return finder.getSplitPosition();
    }

    public static int getWordPosition(String text, int dir, int from, boolean stripSpaces) {
        int result = from;
        boolean reverse = dir < 0;
        int abs = Math.abs(dir);
        for (int i = 0; i < abs; ++i) {
            if (reverse) {
                while (stripSpaces && result > 0 && (text.charAt(result - 1) == ' ' || text.charAt(result - 1) == '\n')) {
                    --result;
                }
                while (result > 0 && text.charAt(result - 1) != ' ' && text.charAt(result - 1) != '\n') {
                    --result;
                }
                continue;
            }
            int length = text.length();
            int index1 = text.indexOf(32, result);
            int index2 = text.indexOf(10, result);
            result = index1 == -1 && index2 == -1 ? -1 : (index1 != -1 && index2 != -1 ? Math.min(index1, index2) : (index1 != -1 ? index1 : index2));
            if (result == -1) {
                result = length;
                continue;
            }
            while (stripSpaces && result < length && (text.charAt(result) == ' ' || text.charAt(result) == '\n')) {
                ++result;
            }
        }
        return result;
    }

    public void splitLines(String input, int maxWidth, Style initialStyle, boolean includeAll, LinePosConsumer output) {
        int start = 0;
        int size = input.length();
        Style workStyle = initialStyle;
        while (start < size) {
            LineBreakFinder finder = new LineBreakFinder(this, maxWidth);
            boolean endOfText = StringDecomposer.iterateFormatted(input, start, workStyle, initialStyle, finder);
            if (endOfText) {
                output.accept(workStyle, start, size);
                break;
            }
            int lineBreak = finder.getSplitPosition();
            char firstTailChar = input.charAt(lineBreak);
            int adjustedBreak = firstTailChar == '\n' || firstTailChar == ' ' ? lineBreak + 1 : lineBreak;
            output.accept(workStyle, start, includeAll ? adjustedBreak : lineBreak);
            start = adjustedBreak;
            workStyle = finder.getSplitStyle();
        }
    }

    public List<FormattedText> splitLines(String input, int maxWidth, Style initialStyle) {
        ArrayList result = Lists.newArrayList();
        this.splitLines(input, maxWidth, initialStyle, false, (style, start, end) -> result.add(FormattedText.of(input.substring(start, end), style)));
        return result;
    }

    public List<FormattedText> splitLines(FormattedText input, int maxWidth, Style initialStyle) {
        ArrayList result = Lists.newArrayList();
        this.splitLines(input, maxWidth, initialStyle, (text, wrapped) -> result.add(text));
        return result;
    }

    public void splitLines(FormattedText input, int maxWidth, Style initialStyle, BiConsumer<FormattedText, Boolean> output) {
        ArrayList partList = Lists.newArrayList();
        input.visit((style, contents) -> {
            if (!contents.isEmpty()) {
                partList.add(new LineComponent(contents, style));
            }
            return Optional.empty();
        }, initialStyle);
        FlatComponents parts = new FlatComponents(partList);
        boolean shouldRestart = true;
        boolean forceNewLine = false;
        boolean isWrapped = false;
        block0: while (shouldRestart) {
            shouldRestart = false;
            LineBreakFinder finder = new LineBreakFinder(this, maxWidth);
            for (LineComponent part : parts.parts) {
                boolean endOfText = StringDecomposer.iterateFormatted(part.contents, 0, part.style, initialStyle, finder);
                if (!endOfText) {
                    int lineBreak = finder.getSplitPosition();
                    Style lineBreakStyle = finder.getSplitStyle();
                    char firstTailChar = parts.charAt(lineBreak);
                    boolean isNewLine = firstTailChar == '\n';
                    boolean skipNextChar = isNewLine || firstTailChar == ' ';
                    forceNewLine = isNewLine;
                    FormattedText result = parts.splitAt(lineBreak, skipNextChar ? 1 : 0, lineBreakStyle);
                    output.accept(result, isWrapped);
                    isWrapped = !isNewLine;
                    shouldRestart = true;
                    continue block0;
                }
                finder.addToOffset(part.contents.length());
            }
        }
        FormattedText lastLine = parts.getRemainder();
        if (lastLine != null) {
            output.accept(lastLine, isWrapped);
        } else if (forceNewLine) {
            output.accept(FormattedText.EMPTY, false);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface WidthProvider {
        public float getWidth(int var1, Style var2);
    }

    @Environment(value=EnvType.CLIENT)
    private class WidthLimitedCharSink
    implements FormattedCharSink {
        private float maxWidth;
        private int position;
        final /* synthetic */ StringSplitter this$0;

        public WidthLimitedCharSink(StringSplitter stringSplitter, float maxWidth) {
            StringSplitter stringSplitter2 = stringSplitter;
            Objects.requireNonNull(stringSplitter2);
            this.this$0 = stringSplitter2;
            this.maxWidth = maxWidth;
        }

        @Override
        public boolean accept(int position, Style style, int codepoint) {
            this.maxWidth -= this.this$0.widthProvider.getWidth(codepoint, style);
            if (this.maxWidth >= 0.0f) {
                this.position = position + Character.charCount(codepoint);
                return true;
            }
            return false;
        }

        public int getPosition() {
            return this.position;
        }

        public void resetPosition() {
            this.position = 0;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class LineBreakFinder
    implements FormattedCharSink {
        private final float maxWidth;
        private int lineBreak;
        private Style lineBreakStyle;
        private boolean hadNonZeroWidthChar;
        private float width;
        private int lastSpace;
        private Style lastSpaceStyle;
        private int nextChar;
        private int offset;
        final /* synthetic */ StringSplitter this$0;

        public LineBreakFinder(StringSplitter stringSplitter, float maxWidth) {
            StringSplitter stringSplitter2 = stringSplitter;
            Objects.requireNonNull(stringSplitter2);
            this.this$0 = stringSplitter2;
            this.lineBreak = -1;
            this.lineBreakStyle = Style.EMPTY;
            this.lastSpace = -1;
            this.lastSpaceStyle = Style.EMPTY;
            this.maxWidth = Math.max(maxWidth, 1.0f);
        }

        @Override
        public boolean accept(int position, Style style, int codepoint) {
            int adjustedPosition = position + this.offset;
            switch (codepoint) {
                case 10: {
                    return this.finishIteration(adjustedPosition, style);
                }
                case 32: {
                    this.lastSpace = adjustedPosition;
                    this.lastSpaceStyle = style;
                }
            }
            float charWidth = this.this$0.widthProvider.getWidth(codepoint, style);
            this.width += charWidth;
            if (this.hadNonZeroWidthChar && this.width > this.maxWidth) {
                if (this.lastSpace != -1) {
                    return this.finishIteration(this.lastSpace, this.lastSpaceStyle);
                }
                return this.finishIteration(adjustedPosition, style);
            }
            this.hadNonZeroWidthChar |= charWidth != 0.0f;
            this.nextChar = adjustedPosition + Character.charCount(codepoint);
            return true;
        }

        private boolean finishIteration(int lineBreak, Style style) {
            this.lineBreak = lineBreak;
            this.lineBreakStyle = style;
            return false;
        }

        private boolean lineBreakFound() {
            return this.lineBreak != -1;
        }

        public int getSplitPosition() {
            return this.lineBreakFound() ? this.lineBreak : this.nextChar;
        }

        public Style getSplitStyle() {
            return this.lineBreakStyle;
        }

        public void addToOffset(int delta) {
            this.offset += delta;
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface LinePosConsumer {
        public void accept(Style var1, int var2, int var3);
    }

    @Environment(value=EnvType.CLIENT)
    private static class FlatComponents {
        private final List<LineComponent> parts;
        private String flatParts;

        public FlatComponents(List<LineComponent> parts) {
            this.parts = parts;
            this.flatParts = parts.stream().map(p -> p.contents).collect(Collectors.joining());
        }

        public char charAt(int position) {
            return this.flatParts.charAt(position);
        }

        public FormattedText splitAt(int skipPosition, int skipSize, Style splitStyle) {
            ComponentCollector result = new ComponentCollector();
            ListIterator<LineComponent> it = this.parts.listIterator();
            int position = skipPosition;
            boolean inSkip = false;
            while (it.hasNext()) {
                LineComponent element = it.next();
                String contents = element.contents;
                int contentsSize = contents.length();
                if (!inSkip) {
                    if (position > contentsSize) {
                        result.append(element);
                        it.remove();
                        position -= contentsSize;
                    } else {
                        String beforeSplit = contents.substring(0, position);
                        if (!beforeSplit.isEmpty()) {
                            result.append(FormattedText.of(beforeSplit, element.style));
                        }
                        position += skipSize;
                        inSkip = true;
                    }
                }
                if (!inSkip) continue;
                if (position > contentsSize) {
                    it.remove();
                    position -= contentsSize;
                    continue;
                }
                String afterSplit = contents.substring(position);
                if (afterSplit.isEmpty()) {
                    it.remove();
                    break;
                }
                it.set(new LineComponent(afterSplit, splitStyle));
                break;
            }
            this.flatParts = this.flatParts.substring(skipPosition + skipSize);
            return result.getResultOrEmpty();
        }

        public @Nullable FormattedText getRemainder() {
            ComponentCollector result = new ComponentCollector();
            this.parts.forEach(result::append);
            this.parts.clear();
            return result.getResult();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class LineComponent
    implements FormattedText {
        private final String contents;
        private final Style style;

        public LineComponent(String contents, Style style) {
            this.contents = contents;
            this.style = style;
        }

        @Override
        public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
            return output.accept(this.contents);
        }

        @Override
        public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style parentStyle) {
            return output.accept(this.style.applyTo(parentStyle), this.contents);
        }
    }
}

