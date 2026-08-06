/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Decoder
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument
extends ParserBasedArgument<Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(id -> Component.translatableEscape("argument.item.id.invalid", id));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(tag -> Component.translatableEscape("arguments.item.tag.unknown", tag));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(id -> Component.translatableEscape("arguments.item.component.unknown", id));
    private static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType((type, message) -> Component.translatableEscape("arguments.item.component.malformed", type, message));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType(id -> Component.translatableEscape("arguments.item.predicate.unknown", id));
    private static final Dynamic2CommandExceptionType ERROR_MALFORMED_PREDICATE = new Dynamic2CommandExceptionType((type, message) -> Component.translatableEscape("arguments.item.predicate.malformed", type, message));
    private static final Identifier COUNT_ID = Identifier.withDefaultNamespace("count");
    private static final Map<Identifier, ComponentWrapper> PSEUDO_COMPONENTS = Stream.of(new ComponentWrapper(COUNT_ID, itemStack -> true, (Decoder<? extends Predicate<ItemStack>>)MinMaxBounds.Ints.CODEC.map(range -> itemStack -> range.matches(itemStack.getCount())))).collect(Collectors.toUnmodifiableMap(ComponentWrapper::id, e -> e));
    private static final Map<Identifier, PredicateWrapper> PSEUDO_PREDICATES = Stream.of(new PredicateWrapper(COUNT_ID, (Decoder<? extends Predicate<ItemStack>>)MinMaxBounds.Ints.CODEC.map(range -> itemStack -> range.matches(itemStack.getCount())))).collect(Collectors.toUnmodifiableMap(PredicateWrapper::id, e -> e));

    private static PredicateWrapper createComponentExistencePredicate(Holder.Reference<DataComponentType<?>> componentId) {
        Predicate<ItemStack> componentExists = itemStack -> itemStack.has((DataComponentType)componentId.value());
        return new PredicateWrapper(componentId.key().identifier(), (Decoder<? extends Predicate<ItemStack>>)Unit.CODEC.map(unit -> componentExists));
    }

    public ItemPredicateArgument(CommandBuildContext registries) {
        super(ComponentPredicateParser.createGrammar(new Context(registries)).mapResult(predicates -> Util.allOf(predicates)::test));
    }

    public static ItemPredicateArgument itemPredicate(CommandBuildContext context) {
        return new ItemPredicateArgument(context);
    }

    public static Result getItemPredicate(CommandContext<CommandSourceStack> context, String name) {
        return (Result)context.getArgument(name, Result.class);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private record PredicateWrapper(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
        public PredicateWrapper(Holder.Reference<DataComponentPredicate.Type<?>> holder) {
            this(holder.key().identifier(), (Decoder<? extends Predicate<ItemStack>>)holder.value().codec().map(v -> v::matches));
        }

        public Predicate<ItemStack> decode(ImmutableStringReader reader, Dynamic<?> value) throws CommandSyntaxException {
            DataResult result = this.type.parse(value);
            return (Predicate)result.getOrThrow(message -> ERROR_MALFORMED_PREDICATE.createWithContext(reader, (Object)this.id.toString(), message));
        }
    }

    private static class Context
    implements ComponentPredicateParser.Context<Predicate<ItemStack>, ComponentWrapper, PredicateWrapper> {
        private final HolderLookup.Provider registries;
        private final HolderLookup.RegistryLookup<Item> items;
        private final HolderLookup.RegistryLookup<DataComponentType<?>> components;
        private final HolderLookup.RegistryLookup<DataComponentPredicate.Type<?>> predicates;

        private Context(HolderLookup.Provider registries) {
            this.registries = registries;
            this.items = registries.lookupOrThrow(Registries.ITEM);
            this.components = registries.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
            this.predicates = registries.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
        }

        @Override
        public Predicate<ItemStack> forElementType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException {
            Holder.Reference<Item> item = this.items.get(ResourceKey.create(Registries.ITEM, id)).orElseThrow(() -> ERROR_UNKNOWN_ITEM.createWithContext(reader, (Object)id));
            return itemStack -> itemStack.is(item);
        }

        @Override
        public Predicate<ItemStack> forTagType(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException {
            HolderSet tag = this.items.get(TagKey.create(Registries.ITEM, id)).orElseThrow(() -> ERROR_UNKNOWN_TAG.createWithContext(reader, (Object)id));
            return itemStack -> itemStack.is(tag);
        }

        @Override
        public ComponentWrapper lookupComponentType(ImmutableStringReader reader, Identifier componentId) throws CommandSyntaxException {
            ComponentWrapper wrapper = PSEUDO_COMPONENTS.get(componentId);
            if (wrapper != null) {
                return wrapper;
            }
            DataComponentType componentType = this.components.get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, componentId)).map(Holder::value).orElseThrow(() -> ERROR_UNKNOWN_COMPONENT.createWithContext(reader, (Object)componentId));
            return ComponentWrapper.create(reader, componentId, componentType);
        }

        @Override
        public Predicate<ItemStack> createComponentTest(ImmutableStringReader reader, ComponentWrapper componentType, Dynamic<?> value) throws CommandSyntaxException {
            return componentType.decode(reader, RegistryOps.injectRegistryContext(value, this.registries));
        }

        @Override
        public Predicate<ItemStack> createComponentTest(ImmutableStringReader reader, ComponentWrapper componentType) {
            return componentType.presenceChecker;
        }

        @Override
        public PredicateWrapper lookupPredicateType(ImmutableStringReader reader, Identifier componentId) throws CommandSyntaxException {
            PredicateWrapper wrapper = PSEUDO_PREDICATES.get(componentId);
            if (wrapper != null) {
                return wrapper;
            }
            return this.predicates.get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, componentId)).map(PredicateWrapper::new).or(() -> this.components.get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, componentId)).map(ItemPredicateArgument::createComponentExistencePredicate)).orElseThrow(() -> ERROR_UNKNOWN_PREDICATE.createWithContext(reader, (Object)componentId));
        }

        @Override
        public Predicate<ItemStack> createPredicateTest(ImmutableStringReader reader, PredicateWrapper predicateType, Dynamic<?> value) throws CommandSyntaxException {
            return predicateType.decode(reader, RegistryOps.injectRegistryContext(value, this.registries));
        }

        @Override
        public Stream<Identifier> listElementTypes() {
            return this.items.listElementIds().map(ResourceKey::identifier);
        }

        @Override
        public Stream<Identifier> listTagTypes() {
            return this.items.listTagIds().map(TagKey::location);
        }

        @Override
        public Stream<Identifier> listComponentTypes() {
            return Stream.concat(PSEUDO_COMPONENTS.keySet().stream(), this.components.listElements().filter(e -> !((DataComponentType)e.value()).isTransient()).map(e -> e.key().identifier()));
        }

        @Override
        public Stream<Identifier> listPredicateTypes() {
            return Stream.concat(PSEUDO_PREDICATES.keySet().stream(), this.predicates.listElementIds().map(ResourceKey::identifier));
        }

        @Override
        public Predicate<ItemStack> negate(Predicate<ItemStack> value) {
            return value.negate();
        }

        @Override
        public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> alternatives) {
            return Util.anyOf(alternatives);
        }
    }

    public static interface Result
    extends Predicate<ItemStack> {
    }

    private record ComponentWrapper(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
        public static <T> ComponentWrapper create(ImmutableStringReader reader, Identifier id, DataComponentType<T> type) throws CommandSyntaxException {
            Codec<T> codec = type.codec();
            if (codec == null) {
                throw ERROR_UNKNOWN_COMPONENT.createWithContext(reader, (Object)id);
            }
            return new ComponentWrapper(id, itemStack -> itemStack.has(type), (Decoder<? extends Predicate<ItemStack>>)codec.map(expected -> itemStack -> {
                Object actual = itemStack.get(type);
                return Objects.equals(expected, actual);
            }));
        }

        public Predicate<ItemStack> decode(ImmutableStringReader reader, Dynamic<?> value) throws CommandSyntaxException {
            DataResult result = this.valueChecker.parse(value);
            return (Predicate)result.getOrThrow(message -> ERROR_MALFORMED_COMPONENT.createWithContext(reader, (Object)this.id.toString(), message));
        }
    }
}

