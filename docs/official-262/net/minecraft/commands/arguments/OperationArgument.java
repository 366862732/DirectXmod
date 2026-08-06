/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.ScoreAccess;

public class OperationArgument
implements ArgumentType<Operation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
    private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType((Message)Component.translatable("arguments.operation.invalid"));
    private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType((Message)Component.translatable("arguments.operation.div0"));

    public static OperationArgument operation() {
        return new OperationArgument();
    }

    public static Operation getOperation(CommandContext<CommandSourceStack> context, String name) {
        return (Operation)context.getArgument(name, Operation.class);
    }

    public Operation parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead()) {
            int start = reader.getCursor();
            while (reader.canRead() && reader.peek() != ' ') {
                reader.skip();
            }
            return OperationArgument.getOperation(reader.getString().substring(start, reader.getCursor()));
        }
        throw ERROR_INVALID_OPERATION.createWithContext((ImmutableStringReader)reader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static Operation getOperation(String op) throws CommandSyntaxException {
        if (op.equals("><")) {
            return (a, b) -> {
                int swap = a.get();
                a.set(b.get());
                b.set(swap);
            };
        }
        return OperationArgument.getSimpleOperation(op);
    }

    private static SimpleOperation getSimpleOperation(String op) throws CommandSyntaxException {
        return switch (op) {
            case "=" -> (a, b) -> b;
            case "+=" -> Integer::sum;
            case "-=" -> (a, b) -> a - b;
            case "*=" -> (a, b) -> a * b;
            case "/=" -> (a, b) -> {
                if (b == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                }
                return Mth.floorDiv(a, b);
            };
            case "%=" -> (a, b) -> {
                if (b == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                }
                return Mth.positiveModulo(a, b);
            };
            case "<" -> Math::min;
            case ">" -> Math::max;
            default -> throw ERROR_INVALID_OPERATION.create();
        };
    }

    @FunctionalInterface
    public static interface Operation {
        public void apply(ScoreAccess var1, ScoreAccess var2) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private static interface SimpleOperation
    extends Operation {
        public int apply(int var1, int var2) throws CommandSyntaxException;

        @Override
        default public void apply(ScoreAccess a, ScoreAccess b) throws CommandSyntaxException {
            a.set(this.apply(a.get(), b.get()));
        }
    }
}

