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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;

public class GameProfileArgument
implements ArgumentType<Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
    public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType((Message)Component.translatable("argument.player.unknown"));

    public static Collection<NameAndId> getGameProfiles(CommandContext<CommandSourceStack> source, String name) throws CommandSyntaxException {
        return ((Result)source.getArgument(name, Result.class)).getNames((CommandSourceStack)source.getSource());
    }

    public static GameProfileArgument gameProfile() {
        return new GameProfileArgument();
    }

    public <S> Result parse(StringReader reader, S source) throws CommandSyntaxException {
        return GameProfileArgument.parse(reader, EntitySelectorParser.allowSelectors(source));
    }

    public Result parse(StringReader reader) throws CommandSyntaxException {
        return GameProfileArgument.parse(reader, true);
    }

    private static Result parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
            EntitySelector parse = parser.parse();
            if (parse.includesEntities()) {
                throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext((ImmutableStringReader)reader);
            }
            return new SelectorResult(parse);
        }
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String name = reader.getString().substring(start, reader.getCursor());
        return c -> {
            Optional<NameAndId> result = c.getServer().services().nameToIdCache().get(name);
            return Collections.singleton(result.orElseThrow(() -> ((SimpleCommandExceptionType)ERROR_UNKNOWN_PLAYER).create()));
        };
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> contextBuilder, SuggestionsBuilder builder) {
        Object object = contextBuilder.getSource();
        if (object instanceof SharedSuggestionProvider) {
            SharedSuggestionProvider source = (SharedSuggestionProvider)object;
            StringReader reader = new StringReader(builder.getInput());
            reader.setCursor(builder.getStart());
            EntitySelectorParser parser = new EntitySelectorParser(reader, source.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
            try {
                parser.parse();
            }
            catch (CommandSyntaxException commandSyntaxException) {
                // empty catch block
            }
            return parser.fillSuggestions(builder, suggestions -> SharedSuggestionProvider.suggest(source.getOnlinePlayerNames(), suggestions));
        }
        return Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public static interface Result {
        public Collection<NameAndId> getNames(CommandSourceStack var1) throws CommandSyntaxException;
    }

    public static class SelectorResult
    implements Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector selector) {
            this.selector = selector;
        }

        @Override
        public Collection<NameAndId> getNames(CommandSourceStack sender) throws CommandSyntaxException {
            List<ServerPlayer> players = this.selector.findPlayers(sender);
            if (players.isEmpty()) {
                throw EntityArgument.NO_PLAYERS_FOUND.create();
            }
            ArrayList<NameAndId> result = new ArrayList<NameAndId>();
            for (ServerPlayer entity : players) {
                result.add(entity.nameAndId());
            }
            return result;
        }
    }
}

