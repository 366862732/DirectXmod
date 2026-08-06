/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Keyable
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface StringRepresentable {
    public static final int PRE_BUILT_MAP_THRESHOLD = 16;

    public String getSerializedName();

    public static <E extends Enum<E>> EnumCodec<E> fromEnum(Supplier<E[]> values) {
        return StringRepresentable.fromEnumWithMapping(values, s -> s);
    }

    public static <E extends Enum<E>> EnumCodec<E> fromEnumWithMapping(Supplier<E[]> values, Function<String, String> converter) {
        Enum[] valueArray = (Enum[])values.get();
        Function<String, Enum> lookupFunction = StringRepresentable.createNameLookup(valueArray, e -> (String)converter.apply(((StringRepresentable)((Object)e)).getSerializedName()));
        return new EnumCodec(valueArray, lookupFunction);
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> values) {
        StringRepresentable[] valueArray = (StringRepresentable[])values.get();
        @Nullable Function lookupFunction = StringRepresentable.createNameLookup((StringRepresentable[])valueArray);
        ToIntFunction<StringRepresentable> indexLookup = Util.createIndexLookup(Arrays.asList(valueArray));
        return new StringRepresentableCodec(valueArray, lookupFunction, indexLookup);
    }

    public static <T extends StringRepresentable> Function<String, @Nullable T> createNameLookup(T[] valueArray) {
        return StringRepresentable.createNameLookup(valueArray, StringRepresentable::getSerializedName);
    }

    public static <T> Function<String, @Nullable T> createNameLookup(T[] valueArray, Function<T, String> converter) {
        if (valueArray.length > 16) {
            Map<String, Object> byName = Arrays.stream(valueArray).collect(Collectors.toMap(converter, d -> d));
            return byName::get;
        }
        return id -> {
            for (Object value : valueArray) {
                if (!((String)converter.apply(value)).equals(id)) continue;
                return value;
            }
            return null;
        };
    }

    public static Keyable keys(final StringRepresentable[] values) {
        return new Keyable(){

            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Arrays.stream(values).map(StringRepresentable::getSerializedName).map(arg_0 -> ops.createString(arg_0));
            }
        };
    }

    public static class EnumCodec<E extends Enum<E>>
    extends StringRepresentableCodec<E> {
        private final Function<String, @Nullable E> resolver;

        public EnumCodec(E[] valueArray, Function<String, E> nameResolver) {
            super(valueArray, nameResolver, rec$ -> rec$.ordinal());
            this.resolver = nameResolver;
        }

        public @Nullable E byName(String name) {
            return (E)((Enum)this.resolver.apply(name));
        }

        public E byName(String name, E _default) {
            return (E)((Enum)Objects.requireNonNullElse(this.byName(name), _default));
        }

        public E byName(String name, Supplier<? extends E> defaultSupplier) {
            return (E)((Enum)Objects.requireNonNullElseGet(this.byName(name), defaultSupplier));
        }
    }

    public static class StringRepresentableCodec<S extends StringRepresentable>
    implements Codec<S> {
        private final Codec<S> codec;

        public StringRepresentableCodec(S[] valueArray, Function<String, @Nullable S> nameResolver, ToIntFunction<S> idResolver) {
            this.codec = ExtraCodecs.orCompressed(Codec.stringResolver(StringRepresentable::getSerializedName, nameResolver), ExtraCodecs.idResolverCodec(idResolver, i -> i >= 0 && i < valueArray.length ? valueArray[i] : null, -1));
        }

        public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> ops, T input) {
            return this.codec.decode(ops, input);
        }

        public <T> DataResult<T> encode(S input, DynamicOps<T> ops, T prefix) {
            return this.codec.encode(input, ops, prefix);
        }
    }
}

