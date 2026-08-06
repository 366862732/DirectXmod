/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.slf4j.Logger;

public interface ListOperation {
    public static final MapCodec<ListOperation> UNLIMITED_CODEC = ListOperation.codec(Integer.MAX_VALUE);

    public static MapCodec<ListOperation> codec(int maxSize) {
        return Type.CODEC.dispatchMap("mode", ListOperation::mode, e -> e.mapCodec).validate(op -> {
            int size;
            ReplaceSection section;
            if (op instanceof ReplaceSection && (section = (ReplaceSection)op).size().isPresent() && (size = section.size().get().intValue()) > maxSize) {
                return DataResult.error(() -> "Size value too large: " + size + ", max size is " + maxSize);
            }
            return DataResult.success((Object)op);
        });
    }

    public Type mode();

    default public <T> List<T> apply(List<T> original, List<T> replacement) {
        return this.apply(original, replacement, Integer.MAX_VALUE);
    }

    public <T> List<T> apply(List<T> var1, List<T> var2, int var3);

    public static enum Type implements StringRepresentable
    {
        REPLACE_ALL("replace_all", ReplaceAll.MAP_CODEC),
        REPLACE_SECTION("replace_section", ReplaceSection.MAP_CODEC),
        INSERT("insert", Insert.MAP_CODEC),
        APPEND("append", Append.MAP_CODEC);

        public static final Codec<Type> CODEC;
        private final String id;
        private final MapCodec<? extends ListOperation> mapCodec;

        private Type(String id, MapCodec<? extends ListOperation> mapCodec) {
            this.id = id;
            this.mapCodec = mapCodec;
        }

        public MapCodec<? extends ListOperation> mapCodec() {
            return this.mapCodec;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }

    public record ReplaceSection(int offset, Optional<Integer> size) implements ListOperation
    {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final MapCodec<ReplaceSection> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", (Object)0).forGetter(ReplaceSection::offset), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("size").forGetter(ReplaceSection::size)).apply((Applicative)i, ReplaceSection::new));

        public ReplaceSection(int offset) {
            this(offset, Optional.empty());
        }

        @Override
        public Type mode() {
            return Type.REPLACE_SECTION;
        }

        @Override
        public <T> List<T> apply(List<T> original, List<T> replacement, int maxSize) {
            ImmutableList result;
            int originalSize = original.size();
            if (this.offset > originalSize) {
                LOGGER.error("Cannot replace when offset is out of bounds");
                return original;
            }
            ImmutableList.Builder newList = ImmutableList.builder();
            newList.addAll(original.subList(0, this.offset));
            newList.addAll(replacement);
            int resumeIndex = this.offset + this.size.orElse(replacement.size());
            if (resumeIndex < originalSize) {
                newList.addAll(original.subList(resumeIndex, originalSize));
            }
            if ((result = newList.build()).size() > maxSize) {
                LOGGER.error("Contents overflow in section replacement");
                return original;
            }
            return result;
        }
    }

    public record StandAlone<T>(List<T> value, ListOperation operation) {
        public static <T> Codec<StandAlone<T>> codec(Codec<T> valueCodec, int maxSize) {
            return RecordCodecBuilder.create(i -> i.group((App)valueCodec.sizeLimitedListOf(maxSize).fieldOf("values").forGetter(f -> f.value), (App)ListOperation.codec(maxSize).forGetter(f -> f.operation)).apply((Applicative)i, StandAlone::new));
        }

        public List<T> apply(List<T> input) {
            return this.operation.apply(input, this.value);
        }
    }

    public static class Append
    implements ListOperation {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final Append INSTANCE = new Append();
        public static final MapCodec<Append> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

        private Append() {
        }

        @Override
        public Type mode() {
            return Type.APPEND;
        }

        @Override
        public <T> List<T> apply(List<T> original, List<T> replacement, int maxSize) {
            if (original.size() + replacement.size() > maxSize) {
                LOGGER.error("Contents overflow in section append");
                return original;
            }
            return Stream.concat(original.stream(), replacement.stream()).toList();
        }
    }

    public record Insert(int offset) implements ListOperation
    {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final MapCodec<Insert> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", (Object)0).forGetter(Insert::offset)).apply((Applicative)i, Insert::new));

        @Override
        public Type mode() {
            return Type.INSERT;
        }

        @Override
        public <T> List<T> apply(List<T> original, List<T> replacement, int maxSize) {
            int originalSize = original.size();
            if (this.offset > originalSize) {
                LOGGER.error("Cannot insert when offset is out of bounds");
                return original;
            }
            if (originalSize + replacement.size() > maxSize) {
                LOGGER.error("Contents overflow in section insertion");
                return original;
            }
            ImmutableList.Builder newList = ImmutableList.builder();
            newList.addAll(original.subList(0, this.offset));
            newList.addAll(replacement);
            newList.addAll(original.subList(this.offset, originalSize));
            return newList.build();
        }
    }

    public static class ReplaceAll
    implements ListOperation {
        public static final ReplaceAll INSTANCE = new ReplaceAll();
        public static final MapCodec<ReplaceAll> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

        private ReplaceAll() {
        }

        @Override
        public Type mode() {
            return Type.REPLACE_ALL;
        }

        @Override
        public <T> List<T> apply(List<T> original, List<T> replacement, int maxSize) {
            return replacement;
        }
    }
}

