/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class ResourceOrIdArgument<T>
implements ArgumentType<Holder<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(error -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", error));
    public static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ELEMENT = new Dynamic2CommandExceptionType((id, registry) -> Component.translatableEscape("argument.resource_or_id.no_such_element", id, registry));
    public static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private final HolderLookup.Provider registryLookup;
    private final Optional<? extends HolderLookup.RegistryLookup<T>> elementLookup;
    private final Codec<T> codec;
    private final Grammar<Result<T, Tag>> grammar;
    private final ResourceKey<? extends Registry<T>> registryKey;

    protected ResourceOrIdArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey, Codec<T> codec) {
        this.registryLookup = context;
        this.elementLookup = context.lookup(registryKey);
        this.registryKey = registryKey;
        this.codec = codec;
        this.grammar = ResourceOrIdArgument.createGrammar(registryKey, OPS);
    }

    public static <T, O> Grammar<Result<T, O>> createGrammar(ResourceKey<? extends Registry<T>> registryKey, DynamicOps<O> ops) {
        Grammar<O> inlineValueGrammar = SnbtGrammar.createParser(ops);
        Dictionary<StringReader> rules = new Dictionary<StringReader>();
        Atom result = Atom.of("result");
        Atom id = Atom.of("id");
        Atom value = Atom.of("value");
        rules.put(id, IdentifierParseRule.INSTANCE);
        rules.put(value, inlineValueGrammar.top().value());
        NamedRule topRule = rules.put(result, Term.alternative(rules.named(id), rules.named(value)), scope -> {
            Identifier parsedId = (Identifier)scope.get(id);
            if (parsedId != null) {
                return new ReferenceResult(ResourceKey.create(registryKey, parsedId));
            }
            Object parsedInline = scope.getOrThrow(value);
            return new InlineResult(parsedInline);
        });
        return new Grammar<Result<T, O>>(rules, topRule);
    }

    public static LootTableArgument lootTable(CommandBuildContext context) {
        return new LootTableArgument(context);
    }

    public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceOrIdArgument.getResource(context, name);
    }

    public static LootModifierArgument lootModifier(CommandBuildContext context) {
        return new LootModifierArgument(context);
    }

    public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> context, String name) {
        return ResourceOrIdArgument.getResource(context, name);
    }

    public static LootPredicateArgument lootPredicate(CommandBuildContext context) {
        return new LootPredicateArgument(context);
    }

    public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> context, String name) {
        return ResourceOrIdArgument.getResource(context, name);
    }

    public static DialogArgument dialog(CommandBuildContext context) {
        return new DialogArgument(context);
    }

    public static Holder<Dialog> getDialog(CommandContext<CommandSourceStack> context, String name) {
        return ResourceOrIdArgument.getResource(context, name);
    }

    private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> context, String name) {
        return (Holder)context.getArgument(name, Holder.class);
    }

    public @Nullable Holder<T> parse(StringReader reader) throws CommandSyntaxException {
        return this.parse(reader, this.grammar, OPS);
    }

    private <O> @Nullable Holder<T> parse(StringReader reader, Grammar<Result<T, O>> grammar, DynamicOps<O> ops) throws CommandSyntaxException {
        Result<T, O> contents = grammar.parseForCommands(reader);
        if (this.elementLookup.isEmpty()) {
            return null;
        }
        return contents.parse((ImmutableStringReader)reader, this.registryLookup, ops, this.codec, this.elementLookup.get());
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class LootTableArgument
    extends ResourceOrIdArgument<LootTable> {
        protected LootTableArgument(CommandBuildContext context) {
            super(context, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC);
        }
    }

    public static class LootModifierArgument
    extends ResourceOrIdArgument<LootItemFunction> {
        protected LootModifierArgument(CommandBuildContext context) {
            super(context, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC);
        }
    }

    public static class LootPredicateArgument
    extends ResourceOrIdArgument<LootItemCondition> {
        protected LootPredicateArgument(CommandBuildContext context) {
            super(context, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC);
        }
    }

    public static class DialogArgument
    extends ResourceOrIdArgument<Dialog> {
        protected DialogArgument(CommandBuildContext context) {
            super(context, Registries.DIALOG, Dialog.DIRECT_CODEC);
        }
    }

    public static sealed interface Result<T, O>
    permits InlineResult, ReferenceResult {
        public Holder<T> parse(ImmutableStringReader var1, HolderLookup.Provider var2, DynamicOps<O> var3, Codec<T> var4, HolderLookup.RegistryLookup<T> var5) throws CommandSyntaxException;
    }

    public record ReferenceResult<T, O>(ResourceKey<T> key) implements Result<T, O>
    {
        @Override
        public Holder<T> parse(ImmutableStringReader reader, HolderLookup.Provider lookup, DynamicOps<O> ops, Codec<T> codec, HolderLookup.RegistryLookup<T> elementLookup) throws CommandSyntaxException {
            return elementLookup.get(this.key).orElseThrow(() -> ERROR_NO_SUCH_ELEMENT.createWithContext(reader, (Object)this.key.identifier(), (Object)this.key.registry()));
        }
    }

    public record InlineResult<T, O>(O value) implements Result<T, O>
    {
        @Override
        public Holder<T> parse(ImmutableStringReader reader, HolderLookup.Provider lookup, DynamicOps<O> ops, Codec<T> codec, HolderLookup.RegistryLookup<T> elementLookup) throws CommandSyntaxException {
            return Holder.direct(codec.parse(lookup.createSerializationContext(ops), this.value).getOrThrow(msg -> ERROR_FAILED_TO_PARSE.createWithContext(reader, msg)));
        }
    }
}

