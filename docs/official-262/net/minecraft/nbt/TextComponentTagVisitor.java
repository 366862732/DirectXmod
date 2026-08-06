/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.nbt;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;

public class TextComponentTagVisitor
implements TagVisitor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INLINE_LIST_THRESHOLD = 8;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_LENGTH = 128;
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private static final Component NEWLINE = Component.literal("\n");
    private static final Component ELEMENT_SPACING = Component.literal(" ");
    private final String indentation;
    private final Styling styling;
    private final boolean sortKeys;
    private int indentDepth;
    private int depth;
    private final MutableComponent result = Component.empty();

    public TextComponentTagVisitor(String indentation) {
        this(indentation, RichStyling.INSTANCE);
    }

    public TextComponentTagVisitor(String indentation, Styling styling) {
        this(indentation, styling, LOGGER.isDebugEnabled());
    }

    public TextComponentTagVisitor(String indentation, Styling styling, boolean sortKeys) {
        this.indentation = indentation;
        this.styling = styling;
        this.sortKeys = sortKeys;
    }

    public Component visit(Tag tag) {
        tag.accept(this);
        return this.result;
    }

    private TextComponentTagVisitor append(String string, Style style) {
        this.result.append(Component.literal(string).withStyle(style));
        return this;
    }

    private TextComponentTagVisitor append(Component component) {
        this.result.append(component);
        return this;
    }

    private TextComponentTagVisitor append(Token token) {
        this.result.append(this.styling.token(token));
        return this;
    }

    @Override
    public void visitString(StringTag tag) {
        String quoted = StringTag.quoteAndEscape(tag.value());
        MutableComponent quote = Component.literal(quoted.substring(0, 1));
        this.append(quote).append(quoted.substring(1, quoted.length() - 1), this.styling.stringStyle()).append(quote);
    }

    @Override
    public void visitByte(ByteTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle()).append(Token.BYTE_SUFFIX);
    }

    @Override
    public void visitShort(ShortTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle()).append(Token.SHORT_SUFFIX);
    }

    @Override
    public void visitInt(IntTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle());
    }

    @Override
    public void visitLong(LongTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle()).append(Token.LONG_SUFFIX);
    }

    @Override
    public void visitFloat(FloatTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle()).append(Token.FLOAT_SUFFIX);
    }

    @Override
    public void visitDouble(DoubleTag tag) {
        this.append(String.valueOf(tag.value()), this.styling.numberStyle()).append(Token.DOUBLE_SUFFIX);
    }

    @Override
    public void visitByteArray(ByteArrayTag tag) {
        this.append(Token.LIST_OPEN).append(Token.BYTE_ARRAY_PREFIX).append(Token.LIST_TYPE_SEPARATOR);
        byte[] data = tag.getAsByteArray();
        for (int i = 0; i < data.length && i < 128; ++i) {
            this.append(ELEMENT_SPACING).append(String.valueOf(data[i]), this.styling.numberStyle()).append(Token.BYTE_SUFFIX);
            if (i == data.length - 1) continue;
            this.append(Token.ELEMENT_SEPARATOR);
        }
        if (data.length > 128) {
            this.append(Token.FOLDED);
        }
        this.append(Token.LIST_CLOSE);
    }

    @Override
    public void visitIntArray(IntArrayTag tag) {
        this.append(Token.LIST_OPEN).append(Token.INT_ARRAY_PREFIX).append(Token.LIST_TYPE_SEPARATOR);
        int[] data = tag.getAsIntArray();
        for (int i = 0; i < data.length && i < 128; ++i) {
            this.append(ELEMENT_SPACING).append(String.valueOf(data[i]), this.styling.numberStyle());
            if (i == data.length - 1) continue;
            this.append(Token.ELEMENT_SEPARATOR);
        }
        if (data.length > 128) {
            this.append(Token.FOLDED);
        }
        this.append(Token.LIST_CLOSE);
    }

    @Override
    public void visitLongArray(LongArrayTag tag) {
        this.append(Token.LIST_OPEN).append(Token.LONG_ARRAY_PREFIX).append(Token.LIST_TYPE_SEPARATOR);
        long[] data = tag.getAsLongArray();
        for (int i = 0; i < data.length && i < 128; ++i) {
            this.append(ELEMENT_SPACING).append(String.valueOf(data[i]), this.styling.numberStyle()).append(Token.LONG_SUFFIX);
            if (i == data.length - 1) continue;
            this.append(Token.ELEMENT_SEPARATOR);
        }
        if (data.length > 128) {
            this.append(Token.FOLDED);
        }
        this.append(Token.LIST_CLOSE);
    }

    private static boolean shouldWrapListElements(ListTag list) {
        if (list.size() >= 8) {
            return false;
        }
        for (Tag element : list) {
            if (element instanceof NumericTag) continue;
            return true;
        }
        return false;
    }

    @Override
    public void visitList(ListTag tag) {
        if (tag.isEmpty()) {
            this.append(Token.LIST_OPEN).append(Token.LIST_CLOSE);
            return;
        }
        if (this.depth >= 64) {
            this.append(Token.LIST_OPEN).append(Token.FOLDED).append(Token.LIST_CLOSE);
            return;
        }
        if (!TextComponentTagVisitor.shouldWrapListElements(tag)) {
            this.append(Token.LIST_OPEN);
            for (int i = 0; i < tag.size(); ++i) {
                if (i != 0) {
                    this.append(Token.ELEMENT_SEPARATOR).append(ELEMENT_SPACING);
                }
                this.appendSubTag(tag.get(i), false);
            }
            this.append(Token.LIST_CLOSE);
            return;
        }
        this.append(Token.LIST_OPEN);
        if (!this.indentation.isEmpty()) {
            this.append(NEWLINE);
        }
        MutableComponent entryIndent = Component.literal(this.indentation.repeat(this.indentDepth + 1));
        Component elementSpacing = this.indentation.isEmpty() ? ELEMENT_SPACING : NEWLINE;
        for (int i = 0; i < tag.size() && i < 128; ++i) {
            this.append(entryIndent);
            this.appendSubTag(tag.get(i), true);
            if (i == tag.size() - 1) continue;
            this.append(Token.ELEMENT_SEPARATOR).append(elementSpacing);
        }
        if (tag.size() > 128) {
            this.append(entryIndent).append(Token.FOLDED);
        }
        if (!this.indentation.isEmpty()) {
            this.append(NEWLINE).append(Component.literal(this.indentation.repeat(this.indentDepth)));
        }
        this.append(Token.LIST_CLOSE);
    }

    @Override
    public void visitCompound(CompoundTag tag) {
        Collection<String> keys;
        if (tag.isEmpty()) {
            this.append(Token.STRUCT_OPEN).append(Token.STRUCT_CLOSE);
            return;
        }
        if (this.depth >= 64) {
            this.append(Token.STRUCT_OPEN).append(Token.FOLDED).append(Token.STRUCT_CLOSE);
            return;
        }
        this.append(Token.STRUCT_OPEN);
        if (this.sortKeys) {
            ArrayList<String> keyCopy = new ArrayList<String>(tag.keySet());
            Collections.sort(keyCopy);
            keys = keyCopy;
        } else {
            keys = tag.keySet();
        }
        if (!this.indentation.isEmpty()) {
            this.append(NEWLINE);
        }
        MutableComponent entryIndent = Component.literal(this.indentation.repeat(this.indentDepth + 1));
        Component elementSpacing = this.indentation.isEmpty() ? ELEMENT_SPACING : NEWLINE;
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            this.append(entryIndent).append(this.handleEscapePretty(key)).append(Token.NAME_VALUE_SEPARATOR).append(ELEMENT_SPACING);
            this.appendSubTag(tag.get(key), true);
            if (!iterator.hasNext()) continue;
            this.append(Token.ELEMENT_SEPARATOR).append(elementSpacing);
        }
        if (!this.indentation.isEmpty()) {
            this.append(NEWLINE).append(Component.literal(this.indentation.repeat(this.indentDepth)));
        }
        this.append(Token.STRUCT_CLOSE);
    }

    private void appendSubTag(Tag tag, boolean indent) {
        if (indent) {
            ++this.indentDepth;
        }
        ++this.depth;
        try {
            tag.accept(this);
        }
        finally {
            if (indent) {
                --this.indentDepth;
            }
            --this.depth;
        }
    }

    private Component handleEscapePretty(String input) {
        if (SIMPLE_VALUE.matcher(input).matches()) {
            return Component.literal(input).withStyle(this.styling.keyStyle());
        }
        String quoted = StringTag.quoteAndEscape(input);
        String quote = quoted.substring(0, 1);
        MutableComponent inner = Component.literal(quoted.substring(1, quoted.length() - 1)).withStyle(this.styling.keyStyle());
        return Component.literal(quote).append(inner).append(quote);
    }

    @Override
    public void visitEnd(EndTag tag) {
    }

    public static class RichStyling
    implements Styling {
        private static final Style SYNTAX_HIGHLIGHTING_NUMBER_TYPE = Style.EMPTY.withColor(ChatFormatting.RED);
        public static final Styling INSTANCE = new RichStyling();
        private final Map<Token, Component> tokens = new HashMap<Token, Component>();
        private static final Style SYNTAX_HIGHLIGHTING_KEY = Style.EMPTY.withColor(ChatFormatting.AQUA);
        private static final Style SYNTAX_HIGHLIGHTING_STRING = Style.EMPTY.withColor(ChatFormatting.GREEN);
        private static final Style SYNTAX_HIGHLIGHTING_NUMBER = Style.EMPTY.withColor(ChatFormatting.GOLD);

        private RichStyling() {
            this.overrideToken(Token.FOLDED, Style.EMPTY.withColor(ChatFormatting.GRAY));
            this.overrideToken(Token.BYTE_SUFFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.BYTE_ARRAY_PREFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.SHORT_SUFFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.INT_ARRAY_PREFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.LONG_SUFFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.LONG_ARRAY_PREFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.FLOAT_SUFFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            this.overrideToken(Token.DOUBLE_SUFFIX, SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
            for (Token value : Token.values()) {
                this.tokens.putIfAbsent(value, Component.literal(value.text));
            }
        }

        private void overrideToken(Token token, Style style) {
            this.tokens.put(token, Component.literal(token.text).withStyle(style));
        }

        @Override
        public Style keyStyle() {
            return SYNTAX_HIGHLIGHTING_KEY;
        }

        @Override
        public Style stringStyle() {
            return SYNTAX_HIGHLIGHTING_STRING;
        }

        @Override
        public Style numberStyle() {
            return SYNTAX_HIGHLIGHTING_NUMBER;
        }

        @Override
        public Component token(Token token) {
            return Objects.requireNonNull(this.tokens.get((Object)token));
        }
    }

    public static interface Styling {
        public Style keyStyle();

        public Style stringStyle();

        public Style numberStyle();

        public Component token(Token var1);
    }

    public static enum Token {
        FOLDED("<...>"),
        ELEMENT_SEPARATOR(","),
        LIST_CLOSE("]"),
        LIST_OPEN("["),
        LIST_TYPE_SEPARATOR(";"),
        STRUCT_CLOSE("}"),
        STRUCT_OPEN("{"),
        NAME_VALUE_SEPARATOR(":"),
        BYTE_SUFFIX("b"),
        BYTE_ARRAY_PREFIX("B"),
        SHORT_SUFFIX("s"),
        INT_ARRAY_PREFIX("I"),
        LONG_SUFFIX("L"),
        LONG_ARRAY_PREFIX("L"),
        FLOAT_SUFFIX("f"),
        DOUBLE_SUFFIX("d");

        public final String text;

        private Token(String text) {
            this.text = text;
        }
    }

    public static class PlainStyling
    implements Styling {
        public static final Styling INSTANCE = new PlainStyling();
        private final Map<Token, Component> tokens = new HashMap<Token, Component>();

        private PlainStyling() {
            for (Token value : Token.values()) {
                this.tokens.put(value, Component.literal(value.text));
            }
        }

        @Override
        public Style keyStyle() {
            return Style.EMPTY;
        }

        @Override
        public Style stringStyle() {
            return Style.EMPTY;
        }

        @Override
        public Style numberStyle() {
            return Style.EMPTY;
        }

        @Override
        public Component token(Token token) {
            return Objects.requireNonNull(this.tokens.get((Object)token));
        }
    }
}

