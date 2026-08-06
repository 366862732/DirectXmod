/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.AbstractIterator
 *  com.google.common.collect.Streams
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.DataResult$Success
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.lang.runtime.SwitchBootstraps;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueInputContextHelper;
import org.jspecify.annotations.Nullable;

public class TagValueInput
implements ValueInput {
    private final ProblemReporter problemReporter;
    private final ValueInputContextHelper context;
    private final CompoundTag input;

    private TagValueInput(ProblemReporter problemReporter, ValueInputContextHelper context, CompoundTag input) {
        this.problemReporter = problemReporter;
        this.context = context;
        this.input = input;
    }

    public static ValueInput create(ProblemReporter problemReporter, HolderLookup.Provider holders, CompoundTag tag) {
        return new TagValueInput(problemReporter, new ValueInputContextHelper(holders, NbtOps.INSTANCE), tag);
    }

    public static ValueInput.ValueInputList create(ProblemReporter problemReporter, HolderLookup.Provider holders, List<CompoundTag> tags) {
        return new CompoundListWrapper(problemReporter, new ValueInputContextHelper(holders, NbtOps.INSTANCE), tags);
    }

    @Override
    public <T> Optional<T> read(String name, Codec<T> codec) {
        Tag tag = this.input.get(name);
        if (tag == null) {
            return Optional.empty();
        }
        DataResult dataResult = codec.parse(this.context.ops(), (Object)tag);
        Objects.requireNonNull(dataResult);
        DataResult dataResult2 = dataResult;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DataResult.Success.class, DataResult.Error.class}, (DataResult)dataResult2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                DataResult.Success success = (DataResult.Success)dataResult2;
                yield Optional.of(success.value());
            }
            case 1 -> {
                DataResult.Error error = (DataResult.Error)dataResult2;
                this.problemReporter.report(new DecodeFromFieldFailedProblem(name, tag, error));
                yield error.partialValue();
            }
        };
    }

    @Override
    public <T> Optional<T> read(MapCodec<T> codec) {
        DynamicOps<Tag> ops = this.context.ops();
        DataResult dataResult = ops.getMap((Object)this.input).flatMap(map -> codec.decode(ops, map));
        Objects.requireNonNull(dataResult);
        DataResult dataResult2 = dataResult;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DataResult.Success.class, DataResult.Error.class}, (DataResult)dataResult2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                DataResult.Success success = (DataResult.Success)dataResult2;
                yield Optional.of(success.value());
            }
            case 1 -> {
                DataResult.Error error = (DataResult.Error)dataResult2;
                this.problemReporter.report(new DecodeFromMapFailedProblem(error));
                yield error.partialValue();
            }
        };
    }

    private <T extends Tag> @Nullable T getOptionalTypedTag(String name, TagType<T> expectedType) {
        Tag tag = this.input.get(name);
        if (tag == null) {
            return null;
        }
        TagType<?> actualType = tag.getType();
        if (actualType != expectedType) {
            this.problemReporter.report(new UnexpectedTypeProblem(name, expectedType, actualType));
            return null;
        }
        return (T)tag;
    }

    private @Nullable NumericTag getNumericTag(String name) {
        Tag tag = this.input.get(name);
        if (tag == null) {
            return null;
        }
        if (tag instanceof NumericTag) {
            NumericTag numericTag = (NumericTag)tag;
            return numericTag;
        }
        this.problemReporter.report(new UnexpectedNonNumberProblem(name, tag.getType()));
        return null;
    }

    @Override
    public Optional<ValueInput> child(String name) {
        CompoundTag compound = this.getOptionalTypedTag(name, CompoundTag.TYPE);
        return compound != null ? Optional.of(this.wrapChild(name, compound)) : Optional.empty();
    }

    @Override
    public ValueInput childOrEmpty(String name) {
        CompoundTag compound = this.getOptionalTypedTag(name, CompoundTag.TYPE);
        return compound != null ? this.wrapChild(name, compound) : this.context.empty();
    }

    @Override
    public Optional<ValueInput.ValueInputList> childrenList(String name) {
        ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
        return list != null ? Optional.of(this.wrapList(name, this.context, list)) : Optional.empty();
    }

    @Override
    public ValueInput.ValueInputList childrenListOrEmpty(String name) {
        ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
        return list != null ? this.wrapList(name, this.context, list) : this.context.emptyList();
    }

    @Override
    public <T> Optional<ValueInput.TypedInputList<T>> list(String name, Codec<T> codec) {
        ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
        return list != null ? Optional.of(this.wrapTypedList(name, list, codec)) : Optional.empty();
    }

    @Override
    public <T> ValueInput.TypedInputList<T> listOrEmpty(String name, Codec<T> codec) {
        ListTag list = this.getOptionalTypedTag(name, ListTag.TYPE);
        return list != null ? this.wrapTypedList(name, list, codec) : this.context.emptyTypedList();
    }

    @Override
    public boolean getBooleanOr(String name, boolean defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.byteValue() != 0 : defaultValue;
    }

    @Override
    public byte getByteOr(String name, byte defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.byteValue() : defaultValue;
    }

    @Override
    public int getShortOr(String name, short defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.shortValue() : defaultValue;
    }

    @Override
    public Optional<Integer> getInt(String name) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? Optional.of(numericTag.intValue()) : Optional.empty();
    }

    @Override
    public int getIntOr(String name, int defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.intValue() : defaultValue;
    }

    @Override
    public long getLongOr(String name, long defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.longValue() : defaultValue;
    }

    @Override
    public Optional<Long> getLong(String name) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? Optional.of(numericTag.longValue()) : Optional.empty();
    }

    @Override
    public float getFloatOr(String name, float defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.floatValue() : defaultValue;
    }

    @Override
    public double getDoubleOr(String name, double defaultValue) {
        NumericTag numericTag = this.getNumericTag(name);
        return numericTag != null ? numericTag.doubleValue() : defaultValue;
    }

    @Override
    public Optional<String> getString(String name) {
        StringTag tag = this.getOptionalTypedTag(name, StringTag.TYPE);
        return tag != null ? Optional.of(tag.value()) : Optional.empty();
    }

    @Override
    public String getStringOr(String name, String defaultValue) {
        StringTag tag = this.getOptionalTypedTag(name, StringTag.TYPE);
        return tag != null ? tag.value() : defaultValue;
    }

    @Override
    public Optional<int[]> getIntArray(String name) {
        IntArrayTag tag = this.getOptionalTypedTag(name, IntArrayTag.TYPE);
        return tag != null ? Optional.of(tag.getAsIntArray()) : Optional.empty();
    }

    @Override
    public HolderLookup.Provider lookup() {
        return this.context.lookup();
    }

    private ValueInput wrapChild(String name, CompoundTag compoundTag) {
        return compoundTag.isEmpty() ? this.context.empty() : new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(name)), this.context, compoundTag);
    }

    private static ValueInput wrapChild(ProblemReporter problemReporter, ValueInputContextHelper context, CompoundTag compoundTag) {
        return compoundTag.isEmpty() ? context.empty() : new TagValueInput(problemReporter, context, compoundTag);
    }

    private ValueInput.ValueInputList wrapList(String name, ValueInputContextHelper context, ListTag list) {
        return list.isEmpty() ? context.emptyList() : new ListWrapper(this.problemReporter, name, context, list);
    }

    private <T> ValueInput.TypedInputList<T> wrapTypedList(String name, ListTag list, Codec<T> codec) {
        return list.isEmpty() ? this.context.emptyTypedList() : new TypedListWrapper<T>(this.problemReporter, name, this.context, codec, list);
    }

    private static class CompoundListWrapper
    implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final ValueInputContextHelper context;
        private final List<CompoundTag> list;

        public CompoundListWrapper(ProblemReporter problemReporter, ValueInputContextHelper context, List<CompoundTag> list) {
            this.problemReporter = problemReporter;
            this.context = context;
            this.list = list;
        }

        private ValueInput wrapChild(int index, CompoundTag compoundTag) {
            return TagValueInput.wrapChild(this.problemReporter.forChild(new ProblemReporter.IndexedPathElement(index)), this.context, compoundTag);
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.mapWithIndex(this.list.stream(), (value, index) -> this.wrapChild((int)index, (CompoundTag)value));
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final ListIterator<CompoundTag> iterator = this.list.listIterator();
            return new AbstractIterator<ValueInput>(this){
                final /* synthetic */ CompoundListWrapper this$0;
                {
                    CompoundListWrapper compoundListWrapper = this$0;
                    Objects.requireNonNull(compoundListWrapper);
                    this.this$0 = compoundListWrapper;
                }

                protected @Nullable ValueInput computeNext() {
                    if (iterator.hasNext()) {
                        int index = iterator.nextIndex();
                        CompoundTag value = (CompoundTag)iterator.next();
                        return this.this$0.wrapChild(index, value);
                    }
                    return (ValueInput)this.endOfData();
                }
            };
        }
    }

    public record DecodeFromFieldFailedProblem(String name, Tag tag, DataResult.Error<?> error) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Failed to decode value '" + String.valueOf(this.tag) + "' from field '" + this.name + "': " + this.error.message();
        }
    }

    public record DecodeFromMapFailedProblem(DataResult.Error<?> error) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Failed to decode from map: " + this.error.message();
        }
    }

    public record UnexpectedTypeProblem(String name, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain value of type " + this.expected.getName() + ", but got " + this.actual.getName();
        }
    }

    public record UnexpectedNonNumberProblem(String name, TagType<?> actual) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getName();
        }
    }

    private static class ListWrapper
    implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final String name;
        private final ValueInputContextHelper context;
        private final ListTag list;

        private ListWrapper(ProblemReporter problemReporter, String name, ValueInputContextHelper context, ListTag list) {
            this.problemReporter = problemReporter;
            this.name = name;
            this.context = context;
            this.list = list;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        private ProblemReporter reporterForChild(int index) {
            return this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.name, index));
        }

        private void reportIndexUnwrapProblem(int index, Tag value) {
            this.problemReporter.report(new UnexpectedListElementTypeProblem(this.name, index, CompoundTag.TYPE, value.getType()));
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.mapWithIndex(this.list.stream(), (value, index) -> {
                if (value instanceof CompoundTag) {
                    CompoundTag compoundTag = (CompoundTag)value;
                    return TagValueInput.wrapChild(this.reporterForChild((int)index), this.context, compoundTag);
                }
                this.reportIndexUnwrapProblem((int)index, (Tag)value);
                return null;
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final Iterator iterator = this.list.iterator();
            return new AbstractIterator<ValueInput>(this){
                private int index;
                final /* synthetic */ ListWrapper this$0;
                {
                    ListWrapper listWrapper = this$0;
                    Objects.requireNonNull(listWrapper);
                    this.this$0 = listWrapper;
                }

                protected @Nullable ValueInput computeNext() {
                    while (iterator.hasNext()) {
                        int currentIndex;
                        Tag value = (Tag)iterator.next();
                        ++this.index;
                        if (value instanceof CompoundTag) {
                            CompoundTag compoundTag = (CompoundTag)value;
                            return TagValueInput.wrapChild(this.this$0.reporterForChild(currentIndex), this.this$0.context, compoundTag);
                        }
                        this.this$0.reportIndexUnwrapProblem(currentIndex, value);
                    }
                    return (ValueInput)this.endOfData();
                }
            };
        }
    }

    private static class TypedListWrapper<T>
    implements ValueInput.TypedInputList<T> {
        private final ProblemReporter problemReporter;
        private final String name;
        private final ValueInputContextHelper context;
        private final Codec<T> codec;
        private final ListTag list;

        private TypedListWrapper(ProblemReporter problemReporter, String name, ValueInputContextHelper context, Codec<T> codec, ListTag list) {
            this.problemReporter = problemReporter;
            this.name = name;
            this.context = context;
            this.codec = codec;
            this.list = list;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        private void reportIndexUnwrapProblem(int index, Tag value, DataResult.Error<?> error) {
            this.problemReporter.report(new DecodeFromListFailedProblem(this.name, index, value, error));
        }

        @Override
        public Stream<T> stream() {
            return Streams.mapWithIndex(this.list.stream(), (value, index) -> {
                DataResult dataResult = this.codec.parse(this.context.ops(), value);
                Objects.requireNonNull(dataResult);
                DataResult selector0$temp = dataResult;
                int index$1 = 0;
                return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DataResult.Success.class, DataResult.Error.class}, (DataResult)selector0$temp, index$1)) {
                    default -> throw new MatchException(null, null);
                    case 0 -> {
                        DataResult.Success success = (DataResult.Success)selector0$temp;
                        yield success.value();
                    }
                    case 1 -> {
                        DataResult.Error error = (DataResult.Error)selector0$temp;
                        this.reportIndexUnwrapProblem((int)index, (Tag)value, (DataResult.Error<?>)error);
                        yield error.partialValue().orElse(null);
                    }
                };
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<T> iterator() {
            final ListIterator iterator = this.list.listIterator();
            return new AbstractIterator<T>(this){
                final /* synthetic */ TypedListWrapper this$0;
                {
                    TypedListWrapper typedListWrapper = this$0;
                    Objects.requireNonNull(typedListWrapper);
                    this.this$0 = typedListWrapper;
                }

                protected @Nullable T computeNext() {
                    while (iterator.hasNext()) {
                        DataResult dataResult;
                        int index = iterator.nextIndex();
                        Tag value = (Tag)iterator.next();
                        Objects.requireNonNull(this.this$0.codec.parse(this.this$0.context.ops(), (Object)value));
                        int n = 0;
                        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DataResult.Success.class, DataResult.Error.class}, (DataResult)dataResult, n)) {
                            default: {
                                throw new MatchException(null, null);
                            }
                            case 0: {
                                DataResult.Success success = (DataResult.Success)dataResult;
                                return success.value();
                            }
                            case 1: 
                        }
                        DataResult.Error error = (DataResult.Error)dataResult;
                        this.this$0.reportIndexUnwrapProblem(index, value, error);
                        if (!error.partialValue().isPresent()) continue;
                        return error.partialValue().get();
                    }
                    return this.endOfData();
                }
            };
        }
    }

    public record UnexpectedListElementTypeProblem(String name, int index, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Expected list '" + this.name + "' to contain at index " + this.index + " value of type " + this.expected.getName() + ", but got " + this.actual.getName();
        }
    }

    public record DecodeFromListFailedProblem(String name, int index, Tag tag, DataResult.Error<?> error) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Failed to decode value '" + String.valueOf(this.tag) + "' from field '" + this.name + "' at index " + this.index + "': " + this.error.message();
        }
    }
}

