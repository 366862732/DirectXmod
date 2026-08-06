/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.advancements.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

public record StatePropertiesPredicate(List<PropertyMatcher> properties) {
    private static final Codec<List<PropertyMatcher>> PROPERTIES_CODEC = Codec.unboundedMap((Codec)Codec.STRING, ValueMatcher.CODEC).xmap(map -> map.entrySet().stream().map(entry -> new PropertyMatcher((String)entry.getKey(), (ValueMatcher)entry.getValue())).toList(), properties -> properties.stream().collect(Collectors.toMap(PropertyMatcher::name, PropertyMatcher::valueMatcher)));
    public static final Codec<StatePropertiesPredicate> CODEC = PROPERTIES_CODEC.xmap(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);
    public static final StreamCodec<ByteBuf, StatePropertiesPredicate> STREAM_CODEC = PropertyMatcher.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);

    public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> definition, S state) {
        for (PropertyMatcher matcher : this.properties) {
            if (matcher.match(definition, state)) continue;
            return false;
        }
        return true;
    }

    public boolean matches(BlockState state) {
        return this.matches(state.getBlock().getStateDefinition(), state);
    }

    public boolean matches(FluidState state) {
        return this.matches(state.getType().getStateDefinition(), state);
    }

    public Optional<String> checkState(StateDefinition<?, ?> states) {
        for (PropertyMatcher property : this.properties) {
            Optional<String> unknownProperty = property.checkState(states);
            if (!unknownProperty.isPresent()) continue;
            return unknownProperty;
        }
        return Optional.empty();
    }

    private record PropertyMatcher(String name, ValueMatcher valueMatcher) {
        public static final StreamCodec<ByteBuf, PropertyMatcher> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PropertyMatcher::name, ValueMatcher.STREAM_CODEC, PropertyMatcher::valueMatcher, PropertyMatcher::new);

        public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> definition, S state) {
            Property<?> property = definition.getProperty(this.name);
            return property != null && this.valueMatcher.match(state, property);
        }

        public Optional<String> checkState(StateDefinition<?, ?> states) {
            Property<?> property = states.getProperty(this.name);
            return property != null ? Optional.empty() : Optional.of(this.name);
        }
    }

    private static interface ValueMatcher {
        public static final Codec<ValueMatcher> CODEC = Codec.either(ExactMatcher.CODEC, RangedMatcher.CODEC).xmap(Either::unwrap, matcher -> {
            if (matcher instanceof ExactMatcher) {
                ExactMatcher exact = (ExactMatcher)matcher;
                return Either.left((Object)exact);
            }
            if (matcher instanceof RangedMatcher) {
                RangedMatcher ranged = (RangedMatcher)matcher;
                return Either.right((Object)ranged);
            }
            throw new UnsupportedOperationException();
        });
        public static final StreamCodec<ByteBuf, ValueMatcher> STREAM_CODEC = ByteBufCodecs.either(ExactMatcher.STREAM_CODEC, RangedMatcher.STREAM_CODEC).map(Either::unwrap, matcher -> {
            if (matcher instanceof ExactMatcher) {
                ExactMatcher exact = (ExactMatcher)matcher;
                return Either.left((Object)exact);
            }
            if (matcher instanceof RangedMatcher) {
                RangedMatcher ranged = (RangedMatcher)matcher;
                return Either.right((Object)ranged);
            }
            throw new UnsupportedOperationException();
        });

        public <T extends Comparable<T>> boolean match(StateHolder<?, ?> var1, Property<T> var2);
    }

    public static class Builder {
        private final ImmutableList.Builder<PropertyMatcher> matchers = ImmutableList.builder();

        private Builder() {
        }

        public static Builder properties() {
            return new Builder();
        }

        public Builder hasProperty(Property<?> property, String value) {
            this.matchers.add((Object)new PropertyMatcher(property.getName(), new ExactMatcher(value)));
            return this;
        }

        public Builder hasProperty(Property<Integer> property, int value) {
            return this.hasProperty((Property)property, (Comparable<T> & StringRepresentable)Integer.toString(value));
        }

        public Builder hasProperty(Property<Boolean> property, boolean value) {
            return this.hasProperty((Property)property, (Comparable<T> & StringRepresentable)Boolean.toString(value));
        }

        public <T extends Comparable<T> & StringRepresentable> Builder hasProperty(Property<T> property, T value) {
            return this.hasProperty(property, (T)((StringRepresentable)value).getSerializedName());
        }

        public Optional<StatePropertiesPredicate> build() {
            return Optional.of(new StatePropertiesPredicate((List<PropertyMatcher>)this.matchers.build()));
        }
    }

    private record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements ValueMatcher
    {
        public static final Codec<RangedMatcher> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.optionalFieldOf("min").forGetter(RangedMatcher::minValue), (App)Codec.STRING.optionalFieldOf("max").forGetter(RangedMatcher::maxValue)).apply((Applicative)i, RangedMatcher::new));
        public static final StreamCodec<ByteBuf, RangedMatcher> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), RangedMatcher::minValue, ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), RangedMatcher::maxValue, RangedMatcher::new);

        @Override
        public <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property) {
            Optional<T> typedMaxValue;
            Optional<T> typedMinValue;
            Comparable value = state.getValue(property);
            if (this.minValue.isPresent() && ((typedMinValue = property.getValue(this.minValue.get())).isEmpty() || value.compareTo((Comparable)((Comparable)typedMinValue.get())) < 0)) {
                return false;
            }
            return !this.maxValue.isPresent() || !(typedMaxValue = property.getValue(this.maxValue.get())).isEmpty() && value.compareTo((Comparable)((Comparable)typedMaxValue.get())) <= 0;
        }
    }

    private record ExactMatcher(String value) implements ValueMatcher
    {
        public static final Codec<ExactMatcher> CODEC = Codec.STRING.xmap(ExactMatcher::new, ExactMatcher::value);
        public static final StreamCodec<ByteBuf, ExactMatcher> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(ExactMatcher::new, ExactMatcher::value);

        @Override
        public <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property) {
            Comparable actualValue = state.getValue(property);
            Optional<T> typedExpected = property.getValue(this.value);
            return typedExpected.isPresent() && actualValue.compareTo((Comparable)((Comparable)typedExpected.get())) == 0;
        }
    }
}

