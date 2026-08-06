/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.MapLike
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import net.minecraft.nbt.TagVisitor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class CompoundTag
implements Tag {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH.comapFlatMap(t -> {
        Tag tag = (Tag)t.convert((DynamicOps)NbtOps.INSTANCE).getValue();
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)tag;
            return DataResult.success((Object)(compoundTag == t.getValue() ? compoundTag.copy() : compoundTag));
        }
        return DataResult.error(() -> "Not a compound tag: " + String.valueOf(tag));
    }, t -> new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)t.copy()));
    private static final int SELF_SIZE_IN_BYTES = 48;
    private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>(){

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public CompoundTag load(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                CompoundTag compoundTag = 1.loadCompound(input, accounter);
                return compoundTag;
            }
            finally {
                accounter.popDepth();
            }
        }

        private static CompoundTag loadCompound(DataInput input, NbtAccounter accounter) throws IOException {
            byte tagType;
            accounter.accountBytes(48L);
            HashMap values = Maps.newHashMap();
            while ((tagType = input.readByte()) != 0) {
                Tag tag;
                String key = 1.readString(input, accounter);
                if (values.put(key, tag = CompoundTag.readNamedTagData(TagTypes.getType(tagType), key, input, accounter)) != null) continue;
                accounter.accountBytes(36L);
            }
            return new CompoundTag(values);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                StreamTagVisitor.ValueResult valueResult = 1.parseCompound(input, output, accounter);
                return valueResult;
            }
            finally {
                accounter.popDepth();
            }
        }

        private static StreamTagVisitor.ValueResult parseCompound(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
            byte tagTypeId;
            accounter.accountBytes(48L);
            block13: while ((tagTypeId = input.readByte()) != 0) {
                TagType<?> tagType = TagTypes.getType(tagTypeId);
                switch (output.visitEntry(tagType)) {
                    case HALT: {
                        return StreamTagVisitor.ValueResult.HALT;
                    }
                    case BREAK: {
                        StringTag.skipString(input);
                        tagType.skip(input, accounter);
                        break block13;
                    }
                    case SKIP: {
                        StringTag.skipString(input);
                        tagType.skip(input, accounter);
                        continue block13;
                    }
                    default: {
                        String key = 1.readString(input, accounter);
                        switch (output.visitEntry(tagType, key)) {
                            case HALT: {
                                return StreamTagVisitor.ValueResult.HALT;
                            }
                            case BREAK: {
                                tagType.skip(input, accounter);
                                break block13;
                            }
                            case SKIP: {
                                tagType.skip(input, accounter);
                                continue block13;
                            }
                        }
                        accounter.accountBytes(36L);
                        switch (tagType.parse(input, output, accounter)) {
                            case HALT: {
                                return StreamTagVisitor.ValueResult.HALT;
                            }
                        }
                        continue block13;
                    }
                }
            }
            if (tagTypeId != 0) {
                while ((tagTypeId = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(tagTypeId).skip(input, accounter);
                }
            }
            return output.visitContainerEnd();
        }

        private static String readString(DataInput input, NbtAccounter accounter) throws IOException {
            String key = input.readUTF();
            accounter.accountBytes(28L);
            accounter.accountBytes(2L, key.length());
            return key;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();
            try {
                byte tagTypeId;
                while ((tagTypeId = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(tagTypeId).skip(input, accounter);
                }
            }
            finally {
                accounter.popDepth();
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    CompoundTag(Map<String, Tag> tags) {
        this.tags = tags;
    }

    public CompoundTag() {
        this(new HashMap<String, Tag>());
    }

    @Override
    public void write(DataOutput output) throws IOException {
        for (String key : this.tags.keySet()) {
            Tag tag = this.tags.get(key);
            CompoundTag.writeNamedTag(key, tag, output);
        }
        output.writeByte(0);
    }

    @Override
    public int sizeInBytes() {
        int size = 48;
        for (Map.Entry<String, Tag> entry : this.tags.entrySet()) {
            size += 28 + 2 * entry.getKey().length();
            size += 36;
            size += entry.getValue().sizeInBytes();
        }
        return size;
    }

    public Set<String> keySet() {
        return this.tags.keySet();
    }

    public Set<Map.Entry<String, Tag>> entrySet() {
        return this.tags.entrySet();
    }

    public Collection<Tag> values() {
        return this.tags.values();
    }

    public void forEach(BiConsumer<String, Tag> consumer) {
        this.tags.forEach(consumer);
    }

    @Override
    public byte getId() {
        return 10;
    }

    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    public @Nullable Tag put(String name, Tag tag) {
        return this.tags.put(name, tag);
    }

    public void putByte(String name, byte value) {
        this.tags.put(name, ByteTag.valueOf(value));
    }

    public void putShort(String name, short value) {
        this.tags.put(name, ShortTag.valueOf(value));
    }

    public void putInt(String name, int value) {
        this.tags.put(name, IntTag.valueOf(value));
    }

    public void putLong(String name, long value) {
        this.tags.put(name, LongTag.valueOf(value));
    }

    public void putFloat(String name, float value) {
        this.tags.put(name, FloatTag.valueOf(value));
    }

    public void putDouble(String name, double value) {
        this.tags.put(name, DoubleTag.valueOf(value));
    }

    public void putString(String name, String value) {
        this.tags.put(name, StringTag.valueOf(value));
    }

    public void putByteArray(String name, byte[] value) {
        this.tags.put(name, new ByteArrayTag(value));
    }

    public void putIntArray(String name, int[] value) {
        this.tags.put(name, new IntArrayTag(value));
    }

    public void putLongArray(String name, long[] value) {
        this.tags.put(name, new LongArrayTag(value));
    }

    public void putBoolean(String name, boolean value) {
        this.tags.put(name, ByteTag.valueOf(value));
    }

    public @Nullable Tag get(String name) {
        return this.tags.get(name);
    }

    public boolean contains(String name) {
        return this.tags.containsKey(name);
    }

    private Optional<Tag> getOptional(String name) {
        return Optional.ofNullable(this.tags.get(name));
    }

    public Optional<Byte> getByte(String name) {
        return this.getOptional(name).flatMap(Tag::asByte);
    }

    public byte getByteOr(String name, byte defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.byteValue();
        }
        return defaultValue;
    }

    public Optional<Short> getShort(String name) {
        return this.getOptional(name).flatMap(Tag::asShort);
    }

    public short getShortOr(String name, short defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.shortValue();
        }
        return defaultValue;
    }

    public Optional<Integer> getInt(String name) {
        return this.getOptional(name).flatMap(Tag::asInt);
    }

    public int getIntOr(String name, int defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.intValue();
        }
        return defaultValue;
    }

    public Optional<Long> getLong(String name) {
        return this.getOptional(name).flatMap(Tag::asLong);
    }

    public long getLongOr(String name, long defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.longValue();
        }
        return defaultValue;
    }

    public Optional<Float> getFloat(String name) {
        return this.getOptional(name).flatMap(Tag::asFloat);
    }

    public float getFloatOr(String name, float defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.floatValue();
        }
        return defaultValue;
    }

    public Optional<Double> getDouble(String name) {
        return this.getOptional(name).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(String name, double defaultValue) {
        Tag tag = this.tags.get(name);
        if (tag instanceof NumericTag) {
            NumericTag tag2 = (NumericTag)tag;
            return tag2.doubleValue();
        }
        return defaultValue;
    }

    public Optional<String> getString(String name) {
        return this.getOptional(name).flatMap(Tag::asString);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public String getStringOr(String name, String defaultValue) {
        Tag tag = this.tags.get(name);
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

    public Optional<byte[]> getByteArray(String name) {
        Tag tag = this.tags.get(name);
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag tag2 = (ByteArrayTag)tag;
            return Optional.of(tag2.getAsByteArray());
        }
        return Optional.empty();
    }

    public Optional<int[]> getIntArray(String name) {
        Tag tag = this.tags.get(name);
        if (tag instanceof IntArrayTag) {
            IntArrayTag tag2 = (IntArrayTag)tag;
            return Optional.of(tag2.getAsIntArray());
        }
        return Optional.empty();
    }

    public Optional<long[]> getLongArray(String name) {
        Tag tag = this.tags.get(name);
        if (tag instanceof LongArrayTag) {
            LongArrayTag tag2 = (LongArrayTag)tag;
            return Optional.of(tag2.getAsLongArray());
        }
        return Optional.empty();
    }

    public Optional<CompoundTag> getCompound(String name) {
        Tag tag = this.tags.get(name);
        if (tag instanceof CompoundTag) {
            CompoundTag tag2 = (CompoundTag)tag;
            return Optional.of(tag2);
        }
        return Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(String name) {
        return this.getCompound(name).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(String name) {
        Tag tag = this.tags.get(name);
        if (tag instanceof ListTag) {
            ListTag tag2 = (ListTag)tag;
            return Optional.of(tag2);
        }
        return Optional.empty();
    }

    public ListTag getListOrEmpty(String name) {
        return this.getList(name).orElseGet(ListTag::new);
    }

    public Optional<Boolean> getBoolean(String name) {
        return this.getOptional(name).flatMap(Tag::asBoolean);
    }

    public boolean getBooleanOr(String string, boolean defaultValue) {
        return this.getByteOr(string, defaultValue ? (byte)1 : 0) != 0;
    }

    public @Nullable Tag remove(String name) {
        return this.tags.remove(name);
    }

    @Override
    public String toString() {
        StringTagVisitor visitor = new StringTagVisitor();
        visitor.visitCompound(this);
        return visitor.build();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    CompoundTag shallowCopy() {
        return new CompoundTag(new HashMap<String, Tag>(this.tags));
    }

    @Override
    public CompoundTag copy() {
        HashMap<String, Tag> newTags = new HashMap<String, Tag>();
        this.tags.forEach((? super K key, ? super V tag) -> newTags.put((String)key, tag.copy()));
        return new CompoundTag(newTags);
    }

    @Override
    public Optional<CompoundTag> asCompound() {
        return Optional.of(this);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompoundTag)) return false;
        CompoundTag compoundTag = (CompoundTag)obj;
        if (!Objects.equals(this.tags, compoundTag.tags)) return false;
        return true;
    }

    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String name, Tag tag, DataOutput output) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() == 0) {
            return;
        }
        output.writeUTF(name);
        tag.write(output);
    }

    private static Tag readNamedTagData(TagType<?> type, String name, DataInput input, NbtAccounter accounter) {
        try {
            return type.load(input, accounter);
        }
        catch (IOException e) {
            CrashReport report = CrashReport.forThrowable(e, "Loading NBT data");
            CrashReportCategory category = report.addCategory("NBT Tag");
            category.setDetail("Tag name", name);
            category.setDetail("Tag type", type.getName());
            throw new ReportedNbtException(report);
        }
    }

    public CompoundTag merge(CompoundTag other) {
        for (String tagName : other.tags.keySet()) {
            Tag otherTag = other.tags.get(tagName);
            if (otherTag instanceof CompoundTag) {
                CompoundTag otherCompound = (CompoundTag)otherTag;
                Tag tag = this.tags.get(tagName);
                if (tag instanceof CompoundTag) {
                    CompoundTag selfCompound = (CompoundTag)tag;
                    selfCompound.merge(otherCompound);
                    continue;
                }
            }
            this.put(tagName, otherTag.copy());
        }
        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitCompound(this);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        block14: for (Map.Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag value = entry.getValue();
            TagType<?> type = value.getType();
            StreamTagVisitor.EntryResult entryParseResult = visitor.visitEntry(type);
            switch (entryParseResult) {
                case HALT: {
                    return StreamTagVisitor.ValueResult.HALT;
                }
                case BREAK: {
                    return visitor.visitContainerEnd();
                }
                case SKIP: {
                    continue block14;
                }
            }
            entryParseResult = visitor.visitEntry(type, entry.getKey());
            switch (entryParseResult) {
                case HALT: {
                    return StreamTagVisitor.ValueResult.HALT;
                }
                case BREAK: {
                    return visitor.visitContainerEnd();
                }
                case SKIP: {
                    continue block14;
                }
            }
            StreamTagVisitor.ValueResult valueResult = value.accept(visitor);
            switch (valueResult) {
                case HALT: {
                    return StreamTagVisitor.ValueResult.HALT;
                }
                case BREAK: {
                    return visitor.visitContainerEnd();
                }
            }
        }
        return visitor.visitContainerEnd();
    }

    public <T> void store(String name, Codec<T> codec, T value) {
        this.store(name, codec, NbtOps.INSTANCE, value);
    }

    public <T> void storeNullable(String name, Codec<T> codec, @Nullable T value) {
        if (value != null) {
            this.store(name, codec, value);
        }
    }

    public <T> void store(String name, Codec<T> codec, DynamicOps<Tag> ops, T value) {
        this.put(name, (Tag)codec.encodeStart(ops, value).getOrThrow());
    }

    public <T> void storeNullable(String name, Codec<T> codec, DynamicOps<Tag> ops, @Nullable T value) {
        if (value != null) {
            this.store(name, codec, ops, value);
        }
    }

    public <T> void store(MapCodec<T> codec, T value) {
        this.store(codec, NbtOps.INSTANCE, value);
    }

    public <T> void store(MapCodec<T> codec, DynamicOps<Tag> ops, T value) {
        this.merge((CompoundTag)codec.encoder().encodeStart(ops, value).getOrThrow());
    }

    public <T> Optional<T> read(String name, Codec<T> codec) {
        return this.read(name, codec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(String name, Codec<T> codec, DynamicOps<Tag> ops) {
        Tag tag = this.get(name);
        if (tag == null) {
            return Optional.empty();
        }
        return codec.parse(ops, (Object)tag).resultOrPartial(error -> LOGGER.error("Failed to read field ({}={}): {}", new Object[]{name, tag, error}));
    }

    public <T> Optional<T> read(MapCodec<T> codec) {
        return this.read(codec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(MapCodec<T> codec, DynamicOps<Tag> ops) {
        return codec.decode(ops, (MapLike)ops.getMap((Object)this).getOrThrow()).resultOrPartial(error -> LOGGER.error("Failed to read value ({}): {}", (Object)this, error));
    }
}

