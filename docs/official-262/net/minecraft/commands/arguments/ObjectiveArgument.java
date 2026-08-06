/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;

public class ObjectiveArgument
implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_NOT_FOUND = new DynamicCommandExceptionType(name -> Component.translatableEscape("arguments.objective.notFound", name));
    private static final DynamicCommandExceptionType ERROR_OBJECTIVE_READ_ONLY = new DynamicCommandExceptionType(name -> Component.translatableEscape("arguments.objective.readonly", name));

    public static ObjectiveArgument objective() {
        return new ObjectiveArgument();
    }

    public static Objective getObjective(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String id = (String)context.getArgument(name, String.class);
        ServerScoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(id);
        if (objective == null) {
            throw ERROR_OBJECTIVE_NOT_FOUND.create((Object)id);
        }
        return objective;
    }

    public static Objective getWritableObjective(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Objective objective = ObjectiveArgument.getObjective(context, name);
        if (objective.getCriteria().isReadOnly()) {
            throw ERROR_OBJECTIVE_READ_ONLY.create((Object)objective.getName());
        }
        return objective;
    }

    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Object rawSource = context.getSource();
        if (rawSource instanceof CommandSourceStack) {
            CommandSourceStack source = (CommandSourceStack)rawSource;
            return SharedSuggestionProvider.suggest(source.getServer().getScoreboard().getObjectiveNames(), builder);
        }
        if (rawSource instanceof SharedSuggestionProvider) {
            SharedSuggestionProvider source = (SharedSuggestionProvider)rawSource;
            return source.customSuggestion(context);
        }
        return Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

