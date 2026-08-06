/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument
implements ArgumentType<Result> {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (context, builder) -> {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        EntitySelectorParser parser = new EntitySelectorParser(reader, ((CommandSourceStack)context.getSource()).permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
        try {
            parser.parse();
        }
        catch (CommandSyntaxException commandSyntaxException) {
            // empty catch block
        }
        return parser.fillSuggestions(builder, suggestions -> SharedSuggestionProvider.suggest(((CommandSourceStack)context.getSource()).getOnlinePlayerNames(), suggestions));
    };
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
    private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType((Message)Component.translatable("argument.scoreHolder.empty"));
    private final boolean multiple;

    public ScoreHolderArgument(boolean multiple) {
        this.multiple = multiple;
    }

    public static ScoreHolder getName(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ScoreHolderArgument.getNames(context, name).iterator().next();
    }

    public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ScoreHolderArgument.getNames(context, name, Collections::emptyList);
    }

    public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ScoreHolderArgument.getNames(context, name, ((CommandSourceStack)context.getSource()).getServer().getScoreboard()::getTrackedPlayers);
    }

    public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> context, String name, Supplier<Collection<ScoreHolder>> wildcard) throws CommandSyntaxException {
        Collection<ScoreHolder> result = ((Result)context.getArgument(name, Result.class)).getNames((CommandSourceStack)context.getSource(), wildcard);
        if (result.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        }
        return result;
    }

    public static ScoreHolderArgument scoreHolder() {
        return new ScoreHolderArgument(false);
    }

    public static ScoreHolderArgument scoreHolders() {
        return new ScoreHolderArgument(true);
    }

    public Result parse(StringReader reader) throws CommandSyntaxException {
        return this.parse(reader, true);
    }

    public <S> Result parse(StringReader reader, S source) throws CommandSyntaxException {
        return this.parse(reader, EntitySelectorParser.allowSelectors(source));
    }

    private Result parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            EntitySelectorParser parser = new EntitySelectorParser(reader, allowSelectors);
            EntitySelector selector = parser.parse();
            if (!this.multiple && selector.getMaxResults() > 1) {
                throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext((ImmutableStringReader)reader);
            }
            return new SelectorResult(selector);
        }
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String text = reader.getString().substring(start, reader.getCursor());
        if (text.equals("*")) {
            return (sender, wildcard) -> {
                Collection results = (Collection)wildcard.get();
                if (results.isEmpty()) {
                    throw ERROR_NO_RESULTS.create();
                }
                return results;
            };
        }
        List<ScoreHolder> nameOnlyHolder = List.of(ScoreHolder.forNameOnly(text));
        if (text.startsWith("#")) {
            return (sender, wildcard) -> nameOnlyHolder;
        }
        try {
            UUID uuid = UUID.fromString(text);
            return (sender, wildcard) -> {
                MinecraftServer server = sender.getServer();
                Entity firstResult = null;
                ArrayList<Entity> moreResults = null;
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(uuid);
                    if (entity == null) continue;
                    if (firstResult == null) {
                        firstResult = entity;
                        continue;
                    }
                    if (moreResults == null) {
                        moreResults = new ArrayList<Entity>();
                        moreResults.add(firstResult);
                    }
                    moreResults.add(entity);
                }
                if (moreResults != null) {
                    return moreResults;
                }
                if (firstResult != null) {
                    return List.of(firstResult);
                }
                return nameOnlyHolder;
            };
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return (sender, wildcard) -> {
                MinecraftServer server = sender.getServer();
                ServerPlayer player = server.getPlayerList().getPlayerByName(text);
                if (player != null) {
                    return List.of(player);
                }
                return nameOnlyHolder;
            };
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public static interface Result {
        public Collection<ScoreHolder> getNames(CommandSourceStack var1, Supplier<Collection<ScoreHolder>> var2) throws CommandSyntaxException;
    }

    public static class SelectorResult
    implements Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector selector) {
            this.selector = selector;
        }

        @Override
        public Collection<ScoreHolder> getNames(CommandSourceStack sender, Supplier<Collection<ScoreHolder>> wildcard) throws CommandSyntaxException {
            List<? extends Entity> entities = this.selector.findEntities(sender);
            if (entities.isEmpty()) {
                throw EntityArgument.NO_ENTITIES_FOUND.create();
            }
            return List.copyOf(entities);
        }
    }

    public static class Info
    implements ArgumentTypeInfo<ScoreHolderArgument, Template> {
        private static final byte FLAG_MULTIPLE = 1;

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf out) {
            int flags = 0;
            if (template.multiple) {
                flags |= 1;
            }
            out.writeByte(flags);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf in) {
            byte flags = in.readByte();
            boolean multiple = (flags & 1) != 0;
            return new Template(this, multiple);
        }

        @Override
        public void serializeToJson(Template template, JsonObject out) {
            out.addProperty("amount", template.multiple ? "multiple" : "single");
        }

        @Override
        public Template unpack(ScoreHolderArgument argument) {
            return new Template(this, argument.multiple);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
            private final boolean multiple;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, boolean multiple) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.multiple = multiple;
            }

            @Override
            public ScoreHolderArgument instantiate(CommandBuildContext context) {
                return new ScoreHolderArgument(this.multiple);
            }

            @Override
            public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
                return this.this$0;
            }
        }
    }
}

