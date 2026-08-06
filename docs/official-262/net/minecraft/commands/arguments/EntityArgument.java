/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.gson.JsonObject
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

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;

public class EntityArgument
implements ArgumentType<EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.toomany"));
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType((Message)Component.translatable("argument.player.toomany"));
    public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType((Message)Component.translatable("argument.player.entities"));
    public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.notfound.entity"));
    public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.notfound.player"));
    public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.selector.not_allowed"));
    private final boolean single;
    private final boolean playersOnly;

    protected EntityArgument(boolean single, boolean playersOnly) {
        this.single = single;
        this.playersOnly = playersOnly;
    }

    public static EntityArgument entity() {
        return new EntityArgument(true, false);
    }

    public static Entity getEntity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findSingleEntity((CommandSourceStack)context.getSource());
    }

    public static EntityArgument entities() {
        return new EntityArgument(false, false);
    }

    public static Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Collection<? extends Entity> result = EntityArgument.getOptionalEntities(context, name);
        if (result.isEmpty()) {
            throw NO_ENTITIES_FOUND.create();
        }
        return result;
    }

    public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findEntities((CommandSourceStack)context.getSource());
    }

    public static Collection<ServerPlayer> getOptionalPlayers(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findPlayers((CommandSourceStack)context.getSource());
    }

    public static EntityArgument player() {
        return new EntityArgument(true, true);
    }

    public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ((EntitySelector)context.getArgument(name, EntitySelector.class)).findSinglePlayer((CommandSourceStack)context.getSource());
    }

    public static EntityArgument players() {
        return new EntityArgument(false, true);
    }

    public static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        List<ServerPlayer> players = ((EntitySelector)context.getArgument(name, EntitySelector.class)).findPlayers((CommandSourceStack)context.getSource());
        if (players.isEmpty()) {
            throw NO_PLAYERS_FOUND.create();
        }
        return players;
    }

    public EntitySelector parse(StringReader reader) throws CommandSyntaxException {
        return this.parse(reader, true);
    }

    public <S> EntitySelector parse(StringReader reader, S source) throws CommandSyntaxException {
        return this.parse(reader, EntitySelectorParser.allowSelectors(source));
    }

    private EntitySelector parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
        boolean start = false;
        EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
        EntitySelector selector = parser.parse();
        if (selector.getMaxResults() > 1 && this.single) {
            if (this.playersOnly) {
                reader.setCursor(0);
                throw ERROR_NOT_SINGLE_PLAYER.createWithContext((ImmutableStringReader)reader);
            }
            reader.setCursor(0);
            throw ERROR_NOT_SINGLE_ENTITY.createWithContext((ImmutableStringReader)reader);
        }
        if (selector.includesEntities() && this.playersOnly && !selector.isSelfSelector()) {
            reader.setCursor(0);
            throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext((ImmutableStringReader)reader);
        }
        return selector;
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
            return parser.fillSuggestions(builder, suggestions -> {
                Collection<String> onlinePlayerNames = source.getOnlinePlayerNames();
                Collection<String> suggestedNames = this.playersOnly ? onlinePlayerNames : Iterables.concat(onlinePlayerNames, source.getSelectedEntities());
                SharedSuggestionProvider.suggest(suggestedNames, suggestions);
            });
        }
        return Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info
    implements ArgumentTypeInfo<EntityArgument, Template> {
        private static final byte FLAG_SINGLE = 1;
        private static final byte FLAG_PLAYERS_ONLY = 2;

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf out) {
            int flags = 0;
            if (template.single) {
                flags |= 1;
            }
            if (template.playersOnly) {
                flags |= 2;
            }
            out.writeByte(flags);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf in) {
            byte flags = in.readByte();
            return new Template(this, (flags & 1) != 0, (flags & 2) != 0);
        }

        @Override
        public void serializeToJson(Template template, JsonObject out) {
            out.addProperty("amount", template.single ? "single" : "multiple");
            out.addProperty("type", template.playersOnly ? "players" : "entities");
        }

        @Override
        public Template unpack(EntityArgument argument) {
            return new Template(this, argument.single, argument.playersOnly);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<EntityArgument> {
            private final boolean single;
            private final boolean playersOnly;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, boolean single, boolean playersOnly) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.single = single;
                this.playersOnly = playersOnly;
            }

            @Override
            public EntityArgument instantiate(CommandBuildContext context) {
                return new EntityArgument(this.single, this.playersOnly);
            }

            @Override
            public ArgumentTypeInfo<EntityArgument, ?> type() {
                return this.this$0;
            }
        }
    }
}

