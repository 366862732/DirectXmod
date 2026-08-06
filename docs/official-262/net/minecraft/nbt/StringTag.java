/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagVisitor;

public record StringTag(String value) implements PrimitiveTag
{
    private static final int SELF_SIZE_IN_BYTES = 36;
    public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>(){

        @Override
        public StringTag load(DataInput input, NbtAccounter accounter) throws IOException {
            return StringTag.valueOf(1.readAccounted(input, accounter));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            return output.visit(1.readAccounted(input, accounter));
        }

        private static String readAccounted(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(36L);
            String data = input.readUTF();
            accounter.accountBytes(2L, data.length());
            return data;
        }

        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            StringTag.skipString(input);
        }

        @Override
        public String getName() {
            return "STRING";
        }

        @Override
        public String getPrettyName() {
            return "TAG_String";
        }
    };
    private static final StringTag EMPTY = new StringTag("");
    private static final char DOUBLE_QUOTE = '\"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char ESCAPE = '\\';
    private static final char NOT_SET = '\u0000';

    public static void skipString(DataInput input) throws IOException {
        input.skipBytes(input.readUnsignedShort());
    }

    public static StringTag valueOf(String data) {
        if (data.isEmpty()) {
            return EMPTY;
        }
        return new StringTag(data);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeUTF(this.value);
    }

    @Override
    public int sizeInBytes() {
        return 36 + 2 * this.value.length();
    }

    @Override
    public byte getId() {
        return 8;
    }

    public TagType<StringTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor visitor = new StringTagVisitor();
        visitor.visitString(this);
        return visitor.build();
    }

    @Override
    public StringTag copy() {
        return this;
    }

    @Override
    public Optional<String> asString() {
        return Optional.of(this.value);
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitString(this);
    }

    public static String quoteAndEscape(String input) {
        StringBuilder result = new StringBuilder();
        StringTag.quoteAndEscape(input, result);
        return result.toString();
    }

    public static void quoteAndEscape(String input, StringBuilder result) {
        int quoteMarkIndex = result.length();
        result.append(' ');
        int quote = 0;
        for (int i = 0; i < input.length(); ++i) {
            int c = input.charAt(i);
            if (c == 92) {
                result.append("\\\\");
                continue;
            }
            if (c == 34 || c == 39) {
                if (quote == 0) {
                    int n = quote = c == 34 ? 39 : 34;
                }
                if (quote == c) {
                    result.append('\\');
                }
                result.append((char)c);
                continue;
            }
            String escaped = SnbtGrammar.escapeControlCharacters((char)c);
            if (escaped != null) {
                result.append('\\');
                result.append(escaped);
                continue;
            }
            result.append((char)c);
        }
        if (quote == 0) {
            quote = 34;
        }
        result.setCharAt(quoteMarkIndex, (char)quote);
        result.append((char)quote);
    }

    public static String escapeWithoutQuotes(String input) {
        StringBuilder result = new StringBuilder();
        StringTag.escapeWithoutQuotes(input, result);
        return result.toString();
    }

    public static void escapeWithoutQuotes(String input, StringBuilder result) {
        block3: for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            switch (c) {
                case '\"': 
                case '\'': 
                case '\\': {
                    result.append('\\');
                    result.append(c);
                    continue block3;
                }
                default: {
                    String escaped = SnbtGrammar.escapeControlCharacters(c);
                    if (escaped != null) {
                        result.append('\\');
                        result.append(escaped);
                        continue block3;
                    }
                    result.append(c);
                }
            }
        }
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.value);
    }
}

