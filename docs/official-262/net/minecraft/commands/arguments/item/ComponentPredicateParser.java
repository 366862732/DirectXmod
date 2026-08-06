/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.commands.arguments.item;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.IdentifierParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.TagParseRule;

public class ComponentPredicateParser {
    public static <T, C, P> Grammar<List<T>> createGrammar(Context<T, C, P> context) {
        Atom top = Atom.of("top");
        Atom type = Atom.of("type");
        Atom anyType = Atom.of("any_type");
        Atom elementType = Atom.of("element_type");
        Atom tagType = Atom.of("tag_type");
        Atom conditions = Atom.of("conditions");
        Atom alternatives = Atom.of("alternatives");
        Atom term = Atom.of("term");
        Atom negation = Atom.of("negation");
        Atom test = Atom.of("test");
        Atom componentType = Atom.of("component_type");
        Atom predicateType = Atom.of("predicate_type");
        Atom id = Atom.of("id");
        Atom tag = Atom.of("tag");
        Dictionary<StringReader> rules = new Dictionary<StringReader>();
        NamedRule<StringReader, Identifier> idRule = rules.put(id, IdentifierParseRule.INSTANCE);
        NamedRule topRule = rules.put(top, Term.alternative(Term.sequence(rules.named(type), StringReaderTerms.character('['), Term.cut(), Term.optional(rules.named(conditions)), StringReaderTerms.character(']')), rules.named(type)), scope -> {
            ImmutableList.Builder builder = ImmutableList.builder();
            ((Optional)scope.getOrThrow(type)).ifPresent(arg_0 -> ((ImmutableList.Builder)builder).add(arg_0));
            List parsedConditions = (List)scope.get(conditions);
            if (parsedConditions != null) {
                builder.addAll((Iterable)parsedConditions);
            }
            return builder.build();
        });
        rules.put(type, Term.alternative(rules.named(elementType), Term.sequence(StringReaderTerms.character('#'), Term.cut(), rules.named(tagType)), rules.named(anyType)), scope -> Optional.ofNullable(scope.getAny(elementType, tagType)));
        rules.put(anyType, StringReaderTerms.character('*'), s -> Unit.INSTANCE);
        rules.put(elementType, new ElementLookupRule<T, C, P>(idRule, context));
        rules.put(tagType, new TagLookupRule<T, C, P>(idRule, context));
        rules.put(conditions, Term.sequence(rules.named(alternatives), Term.optional(Term.sequence(StringReaderTerms.character(','), rules.named(conditions)))), scope -> {
            Object parsedCondition = context.anyOf((List)scope.getOrThrow(alternatives));
            return Optional.ofNullable((List)scope.get(conditions)).map(rest -> Util.copyAndAdd(parsedCondition, rest)).orElse(List.of(parsedCondition));
        });
        rules.put(alternatives, Term.sequence(rules.named(term), Term.optional(Term.sequence(StringReaderTerms.character('|'), rules.named(alternatives)))), scope -> {
            Object alternative = scope.getOrThrow(term);
            return Optional.ofNullable((List)scope.get(alternatives)).map(rest -> Util.copyAndAdd(alternative, rest)).orElse(List.of(alternative));
        });
        rules.put(term, Term.alternative(rules.named(test), Term.sequence(StringReaderTerms.character('!'), rules.named(negation))), scope -> scope.getAnyOrThrow(test, negation));
        rules.put(negation, rules.named(test), scope -> context.negate(scope.getOrThrow(test)));
        rules.putComplex(test, Term.alternative(Term.sequence(rules.named(componentType), StringReaderTerms.character('='), Term.cut(), rules.named(tag)), Term.sequence(rules.named(predicateType), StringReaderTerms.character('~'), Term.cut(), rules.named(tag)), rules.named(componentType)), state -> {
            Scope scope = state.scope();
            Object predicate = scope.get(predicateType);
            try {
                if (predicate != null) {
                    Dynamic value = (Dynamic)scope.getOrThrow(tag);
                    return context.createPredicateTest((ImmutableStringReader)state.input(), predicate, value);
                }
                Object component = scope.getOrThrow(componentType);
                Dynamic value = (Dynamic)scope.get(tag);
                return value != null ? context.createComponentTest((ImmutableStringReader)state.input(), component, value) : context.createComponentTest((ImmutableStringReader)state.input(), component);
            }
            catch (CommandSyntaxException e) {
                state.errorCollector().store(state.mark(), (Object)e);
                return null;
            }
        });
        rules.put(componentType, new ComponentLookupRule<T, C, P>(idRule, context));
        rules.put(predicateType, new PredicateLookupRule<T, C, P>(idRule, context));
        rules.put(tag, new TagParseRule<Tag>(NbtOps.INSTANCE));
        return new Grammar<List<T>>(rules, topRule);
    }

    private static class ElementLookupRule<T, C, P>
    extends ResourceLookupRule<Context<T, C, P>, T> {
        private ElementLookupRule(NamedRule<StringReader, Identifier> idParser, Context<T, C, P> context) {
            super(idParser, context);
        }

        @Override
        protected T validateElement(ImmutableStringReader reader, Identifier id) throws Exception {
            return ((Context)this.context).forElementType(reader, id);
        }

        @Override
        public Stream<Identifier> possibleResources() {
            return ((Context)this.context).listElementTypes();
        }
    }

    public static interface Context<T, C, P> {
        public T forElementType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

        public Stream<Identifier> listElementTypes();

        public T forTagType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

        public Stream<Identifier> listTagTypes();

        public C lookupComponentType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

        public Stream<Identifier> listComponentTypes();

        public T createComponentTest(ImmutableStringReader var1, C var2, Dynamic<?> var3) throws CommandSyntaxException;

        public T createComponentTest(ImmutableStringReader var1, C var2);

        public P lookupPredicateType(ImmutableStringReader var1, Identifier var2) throws CommandSyntaxException;

        public Stream<Identifier> listPredicateTypes();

        public T createPredicateTest(ImmutableStringReader var1, P var2, Dynamic<?> var3) throws CommandSyntaxException;

        public T negate(T var1);

        public T anyOf(List<T> var1);
    }

    private static class TagLookupRule<T, C, P>
    extends ResourceLookupRule<Context<T, C, P>, T> {
        private TagLookupRule(NamedRule<StringReader, Identifier> idParser, Context<T, C, P> context) {
            super(idParser, context);
        }

        @Override
        protected T validateElement(ImmutableStringReader reader, Identifier id) throws Exception {
            return ((Context)this.context).forTagType(reader, id);
        }

        @Override
        public Stream<Identifier> possibleResources() {
            return ((Context)this.context).listTagTypes();
        }
    }

    private static class ComponentLookupRule<T, C, P>
    extends ResourceLookupRule<Context<T, C, P>, C> {
        private ComponentLookupRule(NamedRule<StringReader, Identifier> idParser, Context<T, C, P> context) {
            super(idParser, context);
        }

        @Override
        protected C validateElement(ImmutableStringReader reader, Identifier id) throws Exception {
            return ((Context)this.context).lookupComponentType(reader, id);
        }

        @Override
        public Stream<Identifier> possibleResources() {
            return ((Context)this.context).listComponentTypes();
        }
    }

    private static class PredicateLookupRule<T, C, P>
    extends ResourceLookupRule<Context<T, C, P>, P> {
        private PredicateLookupRule(NamedRule<StringReader, Identifier> idParser, Context<T, C, P> context) {
            super(idParser, context);
        }

        @Override
        protected P validateElement(ImmutableStringReader reader, Identifier id) throws Exception {
            return ((Context)this.context).lookupPredicateType(reader, id);
        }

        @Override
        public Stream<Identifier> possibleResources() {
            return ((Context)this.context).listPredicateTypes();
        }
    }
}

