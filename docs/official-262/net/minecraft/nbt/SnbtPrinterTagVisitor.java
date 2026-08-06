/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 */
package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.util.Util;

public class SnbtPrinterTagVisitor
implements TagVisitor {
    private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), map -> {
        map.put("{}", Lists.newArrayList((Object[])new String[]{"DataVersion", "author", "size", "data", "entities", "palette", "palettes"}));
        map.put("{}.data.[].{}", Lists.newArrayList((Object[])new String[]{"pos", "state", "nbt"}));
        map.put("{}.entities.[].{}", Lists.newArrayList((Object[])new String[]{"blockPos", "pos"}));
    });
    private static final Set<String> NO_INDENTATION = Sets.newHashSet((Object[])new String[]{"{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"});
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
    private static final String ELEMENT_SEPARATOR = String.valueOf(',');
    private static final String LIST_OPEN = "[";
    private static final String LIST_CLOSE = "]";
    private static final String LIST_TYPE_SEPARATOR = ";";
    private static final String ELEMENT_SPACING = " ";
    private static final String STRUCT_OPEN = "{";
    private static final String STRUCT_CLOSE = "}";
    private static final String NEWLINE = "\n";
    private final String indentation;
    private final int depth;
    private final List<String> path;
    private String result = "";

    public SnbtPrinterTagVisitor() {
        this("    ", 0, Lists.newArrayList());
    }

    public SnbtPrinterTagVisitor(String indentation, int depth, List<String> path) {
        this.indentation = indentation;
        this.depth = depth;
        this.path = path;
    }

    public String visit(Tag tag) {
        tag.accept(this);
        return this.result;
    }

    @Override
    public void visitString(StringTag tag) {
        this.result = StringTag.quoteAndEscape(tag.value());
    }

    @Override
    public void visitByte(ByteTag tag) {
        this.result = tag.value() + "b";
    }

    @Override
    public void visitShort(ShortTag tag) {
        this.result = tag.value() + "s";
    }

    @Override
    public void visitInt(IntTag tag) {
        this.result = String.valueOf(tag.value());
    }

    @Override
    public void visitLong(LongTag tag) {
        this.result = tag.value() + "L";
    }

    @Override
    public void visitFloat(FloatTag tag) {
        this.result = tag.value() + "f";
    }

    @Override
    public void visitDouble(DoubleTag tag) {
        this.result = tag.value() + "d";
    }

    @Override
    public void visitByteArray(ByteArrayTag tag) {
        StringBuilder builder = new StringBuilder(LIST_OPEN).append("B").append(LIST_TYPE_SEPARATOR);
        byte[] data = tag.getAsByteArray();
        for (int i = 0; i < data.length; ++i) {
            builder.append(ELEMENT_SPACING).append(data[i]).append("B");
            if (i == data.length - 1) continue;
            builder.append(ELEMENT_SEPARATOR);
        }
        builder.append(LIST_CLOSE);
        this.result = builder.toString();
    }

    @Override
    public void visitIntArray(IntArrayTag tag) {
        StringBuilder builder = new StringBuilder(LIST_OPEN).append("I").append(LIST_TYPE_SEPARATOR);
        int[] data = tag.getAsIntArray();
        for (int i = 0; i < data.length; ++i) {
            builder.append(ELEMENT_SPACING).append(data[i]);
            if (i == data.length - 1) continue;
            builder.append(ELEMENT_SEPARATOR);
        }
        builder.append(LIST_CLOSE);
        this.result = builder.toString();
    }

    @Override
    public void visitLongArray(LongArrayTag tag) {
        String type = "L";
        StringBuilder builder = new StringBuilder(LIST_OPEN).append("L").append(LIST_TYPE_SEPARATOR);
        long[] data = tag.getAsLongArray();
        for (int i = 0; i < data.length; ++i) {
            builder.append(ELEMENT_SPACING).append(data[i]).append("L");
            if (i == data.length - 1) continue;
            builder.append(ELEMENT_SEPARATOR);
        }
        builder.append(LIST_CLOSE);
        this.result = builder.toString();
    }

    @Override
    public void visitList(ListTag tag) {
        String indentation;
        if (tag.isEmpty()) {
            this.result = "[]";
            return;
        }
        StringBuilder builder = new StringBuilder(LIST_OPEN);
        this.pushPath("[]");
        String string = indentation = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
        if (!indentation.isEmpty()) {
            builder.append(NEWLINE);
        }
        for (int i = 0; i < tag.size(); ++i) {
            builder.append(Strings.repeat((String)indentation, (int)(this.depth + 1)));
            builder.append(new SnbtPrinterTagVisitor(indentation, this.depth + 1, this.path).visit(tag.get(i)));
            if (i == tag.size() - 1) continue;
            builder.append(ELEMENT_SEPARATOR).append(indentation.isEmpty() ? ELEMENT_SPACING : NEWLINE);
        }
        if (!indentation.isEmpty()) {
            builder.append(NEWLINE).append(Strings.repeat((String)indentation, (int)this.depth));
        }
        builder.append(LIST_CLOSE);
        this.result = builder.toString();
        this.popPath();
    }

    @Override
    public void visitCompound(CompoundTag tag) {
        String indentation;
        if (tag.isEmpty()) {
            this.result = "{}";
            return;
        }
        StringBuilder builder = new StringBuilder(STRUCT_OPEN);
        this.pushPath("{}");
        String string = indentation = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
        if (!indentation.isEmpty()) {
            builder.append(NEWLINE);
        }
        List<String> keys = this.getKeys(tag);
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = (String)iterator.next();
            Tag value = tag.get(key);
            this.pushPath(key);
            builder.append(Strings.repeat((String)indentation, (int)(this.depth + 1))).append(SnbtPrinterTagVisitor.handleEscapePretty(key)).append(NAME_VALUE_SEPARATOR).append(ELEMENT_SPACING).append(new SnbtPrinterTagVisitor(indentation, this.depth + 1, this.path).visit(value));
            this.popPath();
            if (!iterator.hasNext()) continue;
            builder.append(ELEMENT_SEPARATOR).append(indentation.isEmpty() ? ELEMENT_SPACING : NEWLINE);
        }
        if (!indentation.isEmpty()) {
            builder.append(NEWLINE).append(Strings.repeat((String)indentation, (int)this.depth));
        }
        builder.append(STRUCT_CLOSE);
        this.result = builder.toString();
        this.popPath();
    }

    private void popPath() {
        this.path.remove(this.path.size() - 1);
    }

    private void pushPath(String e) {
        this.path.add(e);
    }

    protected List<String> getKeys(CompoundTag tag) {
        HashSet keys = Sets.newHashSet(tag.keySet());
        ArrayList strings = Lists.newArrayList();
        List<String> order = KEY_ORDER.get(this.pathString());
        if (order != null) {
            for (String key : order) {
                if (!keys.remove(key)) continue;
                strings.add(key);
            }
            if (!keys.isEmpty()) {
                keys.stream().sorted().forEach(strings::add);
            }
        } else {
            strings.addAll(keys);
            Collections.sort(strings);
        }
        return strings;
    }

    public String pathString() {
        return String.join((CharSequence)".", this.path);
    }

    protected static String handleEscapePretty(String input) {
        if (SIMPLE_VALUE.matcher(input).matches()) {
            return input;
        }
        return StringTag.quoteAndEscape(input);
    }

    @Override
    public void visitEnd(EndTag tag) {
    }
}

