/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Range
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.BuiltInExceptionProvider
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.advancements.predicates;

import com.google.common.collect.Range;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public interface MinMaxBounds<T extends Number> {
    public static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType((Message)Component.translatable("argument.range.empty"));
    public static final SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType((Message)Component.translatable("argument.range.swapped"));

    public Bounds<T> bounds();

    default public Optional<T> min() {
        return this.bounds().min;
    }

    default public Optional<T> max() {
        return this.bounds().max;
    }

    default public boolean isAny() {
        return this.bounds().isAny();
    }

    public static <V extends Number, B extends MinMaxBounds<V>> Function<B, DataResult<B>> validateContainedInRange(MinMaxBounds<V> allowed) {
        Range allowedRange = allowed.bounds().asRange();
        return target -> {
            Range selfAsRange = target.bounds().asRange();
            if (!allowedRange.encloses(selfAsRange)) {
                return DataResult.error(() -> "Range must be within " + String.valueOf(allowedRange) + ", but was " + String.valueOf(selfAsRange));
            }
            return DataResult.success((Object)target);
        };
    }

    public record Bounds<T extends Number>(Optional<T> min, Optional<T> max) {
        public boolean isAny() {
            return this.min().isEmpty() && this.max().isEmpty();
        }

        public DataResult<Bounds<T>> validateSwappedBoundsInCodec() {
            if (this.areSwapped()) {
                return DataResult.error(() -> "Swapped bounds in range: " + String.valueOf(this.min()) + " is higher than " + String.valueOf(this.max()));
            }
            return DataResult.success((Object)this);
        }

        public boolean areSwapped() {
            return this.min.isPresent() && this.max.isPresent() && ((Comparable)((Object)((Number)this.min.get()))).compareTo((Number)this.max.get()) > 0;
        }

        public Range<T> asRange() {
            if (this.min.isPresent()) {
                if (this.max.isPresent()) {
                    return Range.closed((Comparable)((Object)((Number)this.min.get())), (Comparable)((Object)((Number)this.max.get())));
                }
                return Range.atLeast((Comparable)((Object)((Number)this.min.get())));
            }
            if (this.max.isPresent()) {
                return Range.atMost((Comparable)((Object)((Number)this.max.get())));
            }
            return Range.all();
        }

        public Optional<T> asPoint() {
            Optional<T> max;
            Optional<T> min = this.min();
            return min.equals(max = this.max()) ? min : Optional.empty();
        }

        public static <T extends Number> Bounds<T> any() {
            return new Bounds(Optional.empty(), Optional.empty());
        }

        public static <T extends Number> Bounds<T> exactly(T value) {
            Optional<T> wrapped = Optional.of(value);
            return new Bounds<T>(wrapped, wrapped);
        }

        public static <T extends Number> Bounds<T> between(T min, T max) {
            return new Bounds<T>(Optional.of(min), Optional.of(max));
        }

        public static <T extends Number> Bounds<T> atLeast(T value) {
            return new Bounds<T>(Optional.of(value), Optional.empty());
        }

        public static <T extends Number> Bounds<T> atMost(T value) {
            return new Bounds(Optional.empty(), Optional.of(value));
        }

        public <U extends Number> Bounds<U> map(Function<T, U> mapper) {
            return new Bounds<U>(this.min.map(mapper), this.max.map(mapper));
        }

        public static <T extends Number> Codec<Bounds<T>> createCodec(Codec<T> numberCodec) {
            Codec rangeCodec = RecordCodecBuilder.create(i -> i.group((App)numberCodec.optionalFieldOf("min").forGetter(Bounds::min), (App)numberCodec.optionalFieldOf("max").forGetter(Bounds::max)).apply((Applicative)i, Bounds::new));
            return Codec.either((Codec)rangeCodec, numberCodec).xmap(either -> (Bounds)either.map(v -> v, x$0 -> Bounds.exactly(x$0)), bounds -> {
                Optional point = bounds.asPoint();
                return point.isPresent() ? Either.right((Object)((Number)point.get())) : Either.left((Object)bounds);
            });
        }

        public static <B extends ByteBuf, T extends Number> StreamCodec<B, Bounds<T>> createStreamCodec(final StreamCodec<B, T> numberCodec) {
            return new StreamCodec<B, Bounds<T>>(){
                private static final int MIN_FLAG = 1;
                private static final int MAX_FLAG = 2;

                @Override
                public Bounds<T> decode(B input) {
                    byte flags = input.readByte();
                    Optional min = (flags & 1) != 0 ? Optional.of((Number)numberCodec.decode(input)) : Optional.empty();
                    Optional max = (flags & 2) != 0 ? Optional.of((Number)numberCodec.decode(input)) : Optional.empty();
                    return new Bounds(min, max);
                }

                @Override
                public void encode(B output, Bounds<T> value) {
                    Optional<Number> min = value.min();
                    Optional<Number> max = value.max();
                    output.writeByte((min.isPresent() ? 1 : 0) | (max.isPresent() ? 2 : 0));
                    min.ifPresent(v -> numberCodec.encode(output, v));
                    max.ifPresent(v -> numberCodec.encode(output, v));
                }
            };
        }

        public static <T extends Number> Bounds<T> fromReader(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> parseExc) throws CommandSyntaxException {
            if (!reader.canRead()) {
                throw ERROR_EMPTY.createWithContext((ImmutableStringReader)reader);
            }
            int start = reader.getCursor();
            try {
                Optional<T> max;
                Optional<T> min = Bounds.readNumber(reader, converter, parseExc);
                if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
                    reader.skip();
                    reader.skip();
                    max = Bounds.readNumber(reader, converter, parseExc);
                } else {
                    max = min;
                }
                if (min.isEmpty() && max.isEmpty()) {
                    throw ERROR_EMPTY.createWithContext((ImmutableStringReader)reader);
                }
                return new Bounds<T>(min, max);
            }
            catch (CommandSyntaxException e) {
                reader.setCursor(start);
                throw new CommandSyntaxException(e.getType(), e.getRawMessage(), e.getInput(), start);
            }
        }

        private static <T extends Number> Optional<T> readNumber(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> parseExc) throws CommandSyntaxException {
            int start = reader.getCursor();
            while (reader.canRead() && Bounds.isAllowedInputChar(reader)) {
                reader.skip();
            }
            String number = reader.getString().substring(start, reader.getCursor());
            if (number.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of((Number)converter.apply(number));
            }
            catch (NumberFormatException ex) {
                throw parseExc.get().createWithContext((ImmutableStringReader)reader, (Object)number);
            }
        }

        private static boolean isAllowedInputChar(StringReader reader) {
            char c = reader.peek();
            if (c >= '0' && c <= '9' || c == '-') {
                return true;
            }
            if (c == '.') {
                return !reader.canRead(2) || reader.peek(1) != '.';
            }
            return false;
        }
    }

    public record FloatDegrees(Bounds<Float> bounds) implements MinMaxBounds<Float>
    {
        public static final FloatDegrees ANY = new FloatDegrees(Bounds.any());
        public static final Codec<FloatDegrees> CODEC = Bounds.createCodec(Codec.FLOAT).xmap(FloatDegrees::new, FloatDegrees::bounds);
        public static final StreamCodec<ByteBuf, FloatDegrees> STREAM_CODEC = Bounds.createStreamCodec(ByteBufCodecs.FLOAT).map(FloatDegrees::new, FloatDegrees::bounds);

        public static FloatDegrees fromReader(StringReader reader) throws CommandSyntaxException {
            Bounds<Float> bounds = Bounds.fromReader(reader, Float::parseFloat, () -> ((BuiltInExceptionProvider)CommandSyntaxException.BUILT_IN_EXCEPTIONS).readerInvalidFloat());
            return new FloatDegrees(bounds);
        }
    }

    public record Doubles(Bounds<Double> bounds, Bounds<Double> boundsSqr) implements MinMaxBounds<Double>
    {
        public static final Doubles ANY = new Doubles(Bounds.any());
        public static final Codec<Doubles> CODEC = Bounds.createCodec(Codec.DOUBLE).validate(Bounds::validateSwappedBoundsInCodec).xmap(Doubles::new, Doubles::bounds);
        public static final StreamCodec<ByteBuf, Doubles> STREAM_CODEC = Bounds.createStreamCodec(ByteBufCodecs.DOUBLE).map(Doubles::new, Doubles::bounds);

        private Doubles(Bounds<Double> bounds) {
            this(bounds, bounds.map(Mth::square));
        }

        public static Doubles exactly(double value) {
            return new Doubles(Bounds.exactly(value));
        }

        public static Doubles between(double min, double max) {
            return new Doubles(Bounds.between(min, max));
        }

        public static Doubles atLeast(double value) {
            return new Doubles(Bounds.atLeast(value));
        }

        public static Doubles atMost(double value) {
            return new Doubles(Bounds.atMost(value));
        }

        public boolean matches(double value) {
            if (this.bounds.min.isPresent() && (Double)this.bounds.min.get() > value) {
                return false;
            }
            return this.bounds.max.isEmpty() || !((Double)this.bounds.max.get() < value);
        }

        public boolean matchesSqr(double valueSqr) {
            if (this.boundsSqr.min.isPresent() && (Double)this.boundsSqr.min.get() > valueSqr) {
                return false;
            }
            return this.boundsSqr.max.isEmpty() || !((Double)this.boundsSqr.max.get() < valueSqr);
        }

        public static Doubles fromReader(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            Bounds<Double> bounds = Bounds.fromReader(reader, Double::parseDouble, () -> ((BuiltInExceptionProvider)CommandSyntaxException.BUILT_IN_EXCEPTIONS).readerInvalidDouble());
            if (bounds.areSwapped()) {
                reader.setCursor(start);
                throw ERROR_SWAPPED.createWithContext((ImmutableStringReader)reader);
            }
            return new Doubles(bounds);
        }
    }

    public record Ints(Bounds<Integer> bounds, Bounds<Long> boundsSqr) implements MinMaxBounds<Integer>
    {
        public static final Ints ANY = new Ints(Bounds.any());
        public static final Codec<Ints> CODEC = Bounds.createCodec(Codec.INT).validate(Bounds::validateSwappedBoundsInCodec).xmap(Ints::new, Ints::bounds);
        public static final StreamCodec<ByteBuf, Ints> STREAM_CODEC = Bounds.createStreamCodec(ByteBufCodecs.INT).map(Ints::new, Ints::bounds);

        private Ints(Bounds<Integer> bounds) {
            this(bounds, bounds.map(i -> Mth.square(i.longValue())));
        }

        public static Ints exactly(int value) {
            return new Ints(Bounds.exactly(value));
        }

        public static Ints between(int min, int max) {
            return new Ints(Bounds.between(min, max));
        }

        public static Ints atLeast(int value) {
            return new Ints(Bounds.atLeast(value));
        }

        public static Ints atMost(int value) {
            return new Ints(Bounds.atMost(value));
        }

        public boolean matches(int value) {
            if (this.bounds.min.isPresent() && (Integer)this.bounds.min.get() > value) {
                return false;
            }
            return this.bounds.max.isEmpty() || (Integer)this.bounds.max.get() >= value;
        }

        public boolean matchesSqr(long valueSqr) {
            if (this.boundsSqr.min.isPresent() && (Long)this.boundsSqr.min.get() > valueSqr) {
                return false;
            }
            return this.boundsSqr.max.isEmpty() || (Long)this.boundsSqr.max.get() >= valueSqr;
        }

        public static Ints fromReader(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            Bounds<Integer> bounds = Bounds.fromReader(reader, Integer::parseInt, () -> ((BuiltInExceptionProvider)CommandSyntaxException.BUILT_IN_EXCEPTIONS).readerInvalidInt());
            if (bounds.areSwapped()) {
                reader.setCursor(start);
                throw ERROR_SWAPPED.createWithContext((ImmutableStringReader)reader);
            }
            return new Ints(bounds);
        }
    }
}

