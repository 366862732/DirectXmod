/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FunctionArgument
implements ArgumentType<Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(tag -> Component.translatableEscape("arguments.function.tag.unknown", tag));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType(value -> Component.translatableEscape("arguments.function.unknown", value));

    public static FunctionArgument functions() {
        return new FunctionArgument();
    }

    public Result parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '#') {
            reader.skip();
            final Identifier id = Identifier.read(reader);
            return new Result(){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
                    return FunctionArgument.getFunctionTag(c, id);
                }

                @Override
                public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return Pair.of((Object)id, (Object)Either.right(FunctionArgument.getFunctionTag(context, id)));
                }

                @Override
                public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return Pair.of((Object)id, FunctionArgument.getFunctionTag(context, id));
                }
            };
        }
        final Identifier id = Identifier.read(reader);
        return new Result(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
                return Collections.singleton(FunctionArgument.getFunction(c, id));
            }

            @Override
            public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                return Pair.of((Object)id, (Object)Either.left(FunctionArgument.getFunction(context, id)));
            }

            @Override
            public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                return Pair.of((Object)id, Collections.singleton(FunctionArgument.getFunction(context, id)));
            }
        };
    }

    private static CommandFunction<CommandSourceStack> getFunction(CommandContext<CommandSourceStack> c, Identifier id) throws CommandSyntaxException {
        return ((CommandSourceStack)c.getSource()).getServer().getFunctions().get(id).orElseThrow(() -> ERROR_UNKNOWN_FUNCTION.create((Object)id.toString()));
    }

    private static Collection<CommandFunction<CommandSourceStack>> getFunctionTag(CommandContext<CommandSourceStack> c, Identifier id) throws CommandSyntaxException {
        List<CommandFunction<CommandSourceStack>> tag = ((CommandSourceStack)c.getSource()).getServer().getFunctions().getTag(id);
        if (tag == null) {
            throw ERROR_UNKNOWN_TAG.create((Object)id.toString());
        }
        return tag;
    }

    public static Collection<CommandFunction<CommandSourceStack>> getFunctions(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((Result)context.getArgument(name, Result.class)).create(context);
    }

    public static Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> getFunctionOrTag(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((Result)context.getArgument(name, Result.class)).unwrap(context);
    }

    public static Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> getFunctionCollection(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((Result)context.getArgument(name, Result.class)).unwrapToCollection(context);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static interface Result {
        public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

        public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

        public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
    }
}

