/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
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
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagVisitor;

public class StringTagVisitor
implements TagVisitor {
    private static final Pattern UNQUOTED_KEY_MATCH = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
    private final StringBuilder builder = new StringBuilder();

    public String build() {
        return this.builder.toString();
    }

    @Override
    public void visitString(StringTag tag) {
        this.builder.append(StringTag.quoteAndEscape(tag.value()));
    }

    @Override
    public void visitByte(ByteTag tag) {
        this.builder.append(tag.value()).append('b');
    }

    @Override
    public void visitShort(ShortTag tag) {
        this.builder.append(tag.value()).append('s');
    }

    @Override
    public void visitInt(IntTag tag) {
        this.builder.append(tag.value());
    }

    @Override
    public void visitLong(LongTag tag) {
        this.builder.append(tag.value()).append('L');
    }

    @Override
    public void visitFloat(FloatTag tag) {
        this.builder.append(tag.value()).append('f');
    }

    @Override
    public void visitDouble(DoubleTag tag) {
        this.builder.append(tag.value()).append('d');
    }

    @Override
    public void visitByteArray(ByteArrayTag tag) {
        this.builder.append("[B;");
        byte[] data = tag.getAsByteArray();
        for (int i = 0; i < data.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }
            this.builder.append(data[i]).append('B');
        }
        this.builder.append(']');
    }

    @Override
    public void visitIntArray(IntArrayTag tag) {
        this.builder.append("[I;");
        int[] data = tag.getAsIntArray();
        for (int i = 0; i < data.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }
            this.builder.append(data[i]);
        }
        this.builder.append(']');
    }

    @Override
    public void visitLongArray(LongArrayTag tag) {
        this.builder.append("[L;");
        long[] data = tag.getAsLongArray();
        for (int i = 0; i < data.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }
            this.builder.append(data[i]).append('L');
        }
        this.builder.append(']');
    }

    @Override
    public void visitList(ListTag tag) {
        this.builder.append('[');
        for (int i = 0; i < tag.size(); ++i) {
            if (i != 0) {
                this.builder.append(',');
            }
            tag.get(i).accept(this);
        }
        this.builder.append(']');
    }

    @Override
    public void visitCompound(CompoundTag tag) {
        this.builder.append('{');
        ArrayList<Map.Entry<String, Tag>> entries = new ArrayList<Map.Entry<String, Tag>>(tag.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (int i = 0; i < entries.size(); ++i) {
            Map.Entry entry = (Map.Entry)entries.get(i);
            if (i != 0) {
                this.builder.append(',');
            }
            this.handleKeyEscape((String)entry.getKey());
            this.builder.append(':');
            ((Tag)entry.getValue()).accept(this);
        }
        this.builder.append('}');
    }

    private void handleKeyEscape(String input) {
        if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false") && UNQUOTED_KEY_MATCH.matcher(input).matches()) {
            this.builder.append(input);
        } else {
            StringTag.quoteAndEscape(input, this.builder);
        }
    }

    @Override
    public void visitEnd(EndTag tag) {
        this.builder.append("END");
    }
}

