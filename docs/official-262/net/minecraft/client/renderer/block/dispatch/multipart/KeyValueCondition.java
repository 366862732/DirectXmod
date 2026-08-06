/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.google.common.base.Splitter
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.block.dispatch.multipart;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record KeyValueCondition(Map<String, Terms> tests) implements Condition
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<KeyValueCondition> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap((Codec)Codec.STRING, Terms.CODEC)).xmap(KeyValueCondition::new, KeyValueCondition::tests);

    @Override
    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> definition) {
        ArrayList predicates = new ArrayList(this.tests.size());
        this.tests.forEach((key, valueTest) -> predicates.add(KeyValueCondition.instantiate(definition, key, valueTest)));
        return Util.allOf(predicates);
    }

    private static <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> definition, String key, Terms valueTest) {
        Property<?> property = definition.getProperty(key);
        if (property == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", key, definition.getOwner()));
        }
        return valueTest.instantiate(definition.getOwner(), property);
    }

    @Environment(value=EnvType.CLIENT)
    public record Terms(List<Term> entries) {
        private static final char SEPARATOR = '|';
        private static final Joiner JOINER = Joiner.on((char)'|');
        private static final Splitter SPLITTER = Splitter.on((char)'|');
        private static final Codec<String> LEGACY_REPRESENTATION_CODEC = Codec.either((Codec)Codec.INT, (Codec)Codec.BOOL).flatComapMap(either -> (String)either.map(String::valueOf, String::valueOf), o -> DataResult.error(() -> "This codec can't be used for encoding"));
        public static final Codec<Terms> CODEC = Codec.withAlternative((Codec)Codec.STRING, LEGACY_REPRESENTATION_CODEC).comapFlatMap(Terms::parse, Terms::toString);

        public Terms {
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Empty value for property");
            }
        }

        public static DataResult<Terms> parse(String value) {
            List<Term> terms = SPLITTER.splitToStream((CharSequence)value).map(Term::parse).toList();
            if (terms.isEmpty()) {
                return DataResult.error(() -> "Empty value for property");
            }
            for (Term entry : terms) {
                if (!entry.value.isEmpty()) continue;
                return DataResult.error(() -> "Empty term in value '" + value + "'");
            }
            return DataResult.success((Object)new Terms(terms));
        }

        @Override
        public String toString() {
            return JOINER.join(this.entries);
        }

        public <O, S extends StateHolder<O, S>, T extends Comparable<T>> Predicate<S> instantiate(O owner, Property<T> property) {
            ArrayList valuesToMatch;
            boolean negate;
            Predicate allowedValueTest = Util.anyOf(Lists.transform(this.entries, t -> this.instantiate(owner, property, (Term)t)));
            ArrayList allowedValues = new ArrayList(property.getPossibleValues());
            int allValuesCount = allowedValues.size();
            allowedValues.removeIf(allowedValueTest.negate());
            int allowedValuesCount = allowedValues.size();
            if (allowedValuesCount == 0) {
                LOGGER.warn("Condition {} for property {} on {} is always false", new Object[]{this, property.getName(), owner});
                return blockState -> false;
            }
            int rejectedValuesCount = allValuesCount - allowedValuesCount;
            if (rejectedValuesCount == 0) {
                LOGGER.warn("Condition {} for property {} on {} is always true", new Object[]{this, property.getName(), owner});
                return blockState -> true;
            }
            if (allowedValuesCount <= rejectedValuesCount) {
                negate = false;
                valuesToMatch = allowedValues;
            } else {
                negate = true;
                ArrayList<T> rejectedValues = new ArrayList<T>(property.getPossibleValues());
                rejectedValues.removeIf(allowedValueTest);
                valuesToMatch = rejectedValues;
            }
            if (valuesToMatch.size() == 1) {
                Comparable expectedValue = (Comparable)valuesToMatch.getFirst();
                return state -> {
                    Object value = state.getValue(property);
                    return expectedValue.equals(value) ^ negate;
                };
            }
            return state -> {
                Object value = state.getValue(property);
                return valuesToMatch.contains(value) ^ negate;
            };
        }

        private <T extends Comparable<T>> T getValueOrThrow(Object owner, Property<T> property, String input) {
            Optional<T> value = property.getValue(input);
            if (value.isEmpty()) {
                throw new RuntimeException(String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", input, property, owner, this));
            }
            return (T)((Comparable)value.get());
        }

        private <T extends Comparable<T>> Predicate<T> instantiate(Object owner, Property<T> property, Term term) {
            Object parsedValue = this.getValueOrThrow(owner, property, term.value);
            if (term.negated) {
                return value -> !value.equals(parsedValue);
            }
            return value -> value.equals(parsedValue);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Term(String value, boolean negated) {
        private static final String NEGATE = "!";

        public Term {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Empty term");
            }
        }

        public static Term parse(String value) {
            if (value.startsWith(NEGATE)) {
                return new Term(value.substring(1), true);
            }
            return new Term(value, false);
        }

        @Override
        public String toString() {
            return this.negated ? NEGATE + this.value : this.value;
        }
    }
}

