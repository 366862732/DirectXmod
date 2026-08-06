/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import net.minecraft.nbt.TagVisitor;
import org.jspecify.annotations.Nullable;

public final class ListTag
extends AbstractList<Tag>
implements CollectionTag {
    private static final String WRAPPER_MARKER = "";
    private static final int SELF_SIZE_IN_BYTES = 36;
    public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>(){

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public ListTag load(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                ListTag listTag = 1.loadList(input, accounter);
                return listTag;
            }
            finally {
                accounter.popDepth();
            }
        }

        private static ListTag loadList(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(36L);
            byte typeId = input.readByte();
            int count = 1.readListCount(input);
            if (typeId == 0 && count > 0) {
                throw new NbtFormatException("Missing type on ListTag");
            }
            accounter.accountBytes(4L, count);
            TagType<?> type = TagTypes.getType(typeId);
            ListTag list = new ListTag(new ArrayList<Tag>(count));
            for (int i = 0; i < count; ++i) {
                list.addAndUnwrap((Tag)type.load(input, accounter));
            }
            return list;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                StreamTagVisitor.ValueResult valueResult = 1.parseList(input, output, accounter);
                return valueResult;
            }
            finally {
                accounter.popDepth();
            }
        }

        /*
         * Exception decompiling
         */
        private static StreamTagVisitor.ValueResult parseList(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            /*
             * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
             * 
             * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [8[CASE], 4[SWITCH]], but top level block is 9[SWITCH]
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
             *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
             *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseInnerClassesPass1(ClassFile.java:923)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1035)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
             *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
             *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
             *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
             *     at org.benf.cfr.reader.Main.main(Main.java:54)
             */
            throw new IllegalStateException("Decompilation failed");
        }

        private static int readListCount(DataInput input) throws IOException {
            int count = input.readInt();
            if (count < 0) {
                throw new NbtFormatException("ListTag length cannot be negative: " + count);
            }
            return count;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                TagType<?> type = TagTypes.getType(input.readByte());
                int count = input.readInt();
                type.skip(input, count, accounter);
            }
            finally {
                accounter.popDepth();
            }
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public String getPrettyName() {
            return "TAG_List";
        }
    };
    private final List<Tag> list;

    public ListTag() {
        this(new ArrayList<Tag>());
    }

    ListTag(List<Tag> list) {
        this.list = list;
    }

    private static Tag tryUnwrap(CompoundTag tag) {
        Tag value;
        if (tag.size() == 1 && (value = tag.get(WRAPPER_MARKER)) != null) {
            return value;
        }
        return tag;
    }

    private static boolean isWrapper(CompoundTag tag) {
        return tag.size() == 1 && tag.contains(WRAPPER_MARKER);
    }

    private static Tag wrapIfNeeded(byte elementType, Tag tag) {
        CompoundTag compoundTag;
        if (elementType != 10) {
            return tag;
        }
        if (tag instanceof CompoundTag && !ListTag.isWrapper(compoundTag = (CompoundTag)tag)) {
            return compoundTag;
        }
        return ListTag.wrapElement(tag);
    }

    private static CompoundTag wrapElement(Tag tag) {
        return new CompoundTag(Map.of(WRAPPER_MARKER, tag));
    }

    @Override
    public void write(DataOutput output) throws IOException {
        byte elementType = this.identifyRawElementType();
        output.writeByte(elementType);
        output.writeInt(this.list.size());
        for (Tag element : this.list) {
            ListTag.wrapIfNeeded(elementType, element).write(output);
        }
    }

    @VisibleForTesting
    byte identifyRawElementType() {
        byte homogenousType = 0;
        for (Tag element : this.list) {
            byte elementType = element.getId();
            if (homogenousType == 0) {
                homogenousType = elementType;
                continue;
            }
            if (homogenousType == elementType) continue;
            return 10;
        }
        return homogenousType;
    }

    public void addAndUnwrap(Tag tag) {
        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag)tag;
            this.add(ListTag.tryUnwrap(compound));
        } else {
            this.add(tag);
        }
    }

    @Override
    public int sizeInBytes() {
        int size = 36;
        size += 4 * this.list.size();
        for (Tag child : this.list) {
            size += child.sizeInBytes();
        }
        return size;
    }

    @Override
    public byte getId() {
        return 9;
    }

    public TagType<ListTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor visitor = new StringTagVisitor();
        visitor.visitList(this);
        return visitor.build();
    }

    @Override
    public Tag remove(int index) {
        return this.list.remove(index);
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Optional<CompoundTag> getCompound(int index) {
        Tag tag = this.getNullable(index);
        if (tag instanceof CompoundTag) {
            CompoundTag tag2 = (CompoundTag)tag;
            return Optional.of(tag2);
        }
        return Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(int index) {
        return this.getCompound(index).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(int index) {
        Tag tag = this.getNullable(index);
        if (tag instanceof ListTag) {
            ListTag tag2 = (ListTag)tag;
            return Optional.of(tag2);
        }
        return Optional.empty();
    }

    public ListTag getListOrEmpty(int index) {
        return this.getList(index).orElseGet(ListTag::new);
    }

    public Optional<Short> getShort(int index) {
        return this.getOptional(index).flatMap(Tag::asShort);
    }

    public short getShortOr(int index, short defaultValue) {
        Tag tag = this.getNullable(index);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.shortValue();
        }
        return defaultValue;
    }

    public Optional<Integer> getInt(int index) {
        return this.getOptional(index).flatMap(Tag::asInt);
    }

    public int getIntOr(int index, int defaultValue) {
        Tag tag = this.getNullable(index);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.intValue();
        }
        return defaultValue;
    }

    public Optional<int[]> getIntArray(int index) {
        Tag tag = this.getNullable(index);
        if (tag instanceof IntArrayTag) {
            IntArrayTag tag2 = (IntArrayTag)tag;
            return Optional.of(tag2.getAsIntArray());
        }
        return Optional.empty();
    }

    public Optional<long[]> getLongArray(int index) {
        Tag tag = this.getNullable(index);
        if (tag instanceof LongArrayTag) {
            LongArrayTag tag2 = (LongArrayTag)tag;
            return Optional.of(tag2.getAsLongArray());
        }
        return Optional.empty();
    }

    public Optional<Double> getDouble(int index) {
        return this.getOptional(index).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(int index, double defaultValue) {
        Tag tag = this.getNullable(index);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.doubleValue();
        }
        return defaultValue;
    }

    public Optional<Float> getFloat(int index) {
        return this.getOptional(index).flatMap(Tag::asFloat);
    }

    public float getFloatOr(int index, float defaultValue) {
        Tag tag = this.getNullable(index);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.floatValue();
        }
        return defaultValue;
    }

    public Optional<String> getString(int index) {
        return this.getOptional(index).flatMap(Tag::asString);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public String getStringOr(int index, String defaultValue) {
        Tag tag = this.getNullable(index);
        if (!(tag instanceof StringTag)) return defaultValue;
        StringTag stringTag = (StringTag)tag;
        try {
            String string = stringTag.value();
            return string;
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
    }

    private @Nullable Tag getNullable(int index) {
        return index >= 0 && index < this.list.size() ? this.list.get(index) : null;
    }

    private Optional<Tag> getOptional(int index) {
        return Optional.ofNullable(this.getNullable(index));
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Tag get(int index) {
        return this.list.get(index);
    }

    @Override
    public Tag set(int index, Tag tag) {
        return this.list.set(index, tag);
    }

    @Override
    public void add(int index, Tag tag) {
        this.list.add(index, tag);
    }

    @Override
    public boolean setTag(int index, Tag tag) {
        this.list.set(index, tag);
        return true;
    }

    @Override
    public boolean addTag(int index, Tag tag) {
        this.list.add(index, tag);
        return true;
    }

    @Override
    public ListTag copy() {
        ArrayList<Tag> copy = new ArrayList<Tag>(this.list.size());
        for (Tag tag : this.list) {
            copy.add(tag.copy());
        }
        return new ListTag(copy);
    }

    @Override
    public Optional<ListTag> asList() {
        return Optional.of(this);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ListTag)) return false;
        ListTag listTag = (ListTag)obj;
        if (!Objects.equals(this.list, listTag.list)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public Stream<Tag> stream() {
        return super.stream();
    }

    public Stream<CompoundTag> compoundStream() {
        return this.stream().mapMulti((tag, output) -> {
            if (tag instanceof CompoundTag) {
                CompoundTag compound = (CompoundTag)tag;
                output.accept(compound);
            }
        });
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitList(this);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        byte elementType = this.identifyRawElementType();
        switch (visitor.visitList(TagTypes.getType(elementType), this.list.size())) {
            case HALT: {
                return StreamTagVisitor.ValueResult.HALT;
            }
            case BREAK: {
                return visitor.visitContainerEnd();
            }
        }
        block13: for (int i = 0; i < this.list.size(); ++i) {
            Tag tag = ListTag.wrapIfNeeded(elementType, this.list.get(i));
            switch (visitor.visitElement(tag.getType(), i)) {
                case HALT: {
                    return StreamTagVisitor.ValueResult.HALT;
                }
                case SKIP: {
                    continue block13;
                }
                case BREAK: {
                    return visitor.visitContainerEnd();
                }
                default: {
                    switch (tag.accept(visitor)) {
                        case HALT: {
                            return StreamTagVisitor.ValueResult.HALT;
                        }
                        case BREAK: {
                            return visitor.visitContainerEnd();
                        }
                    }
                }
            }
        }
        return visitor.visitContainerEnd();
    }
}

