/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import org.jspecify.annotations.Nullable;

public class RandomCommand {
    private static final SimpleCommandExceptionType ERROR_RANGE_TOO_LARGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.random.error.range_too_large"));
    private static final SimpleCommandExceptionType ERROR_RANGE_TOO_SMALL = new SimpleCommandExceptionType((Message)Component.translatable("commands.random.error.range_too_small"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("random").then(RandomCommand.drawRandomValueTree("value", false))).then(RandomCommand.drawRandomValueTree("roll", true))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((LiteralArgumentBuilder)Commands.literal("*").executes(c -> RandomCommand.resetAllSequences((CommandSourceStack)c.getSource()))).then(((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer()).executes(c -> RandomCommand.resetAllSequencesAndSetNewDefaults((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), true, true))).then(((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool()).executes(c -> RandomCommand.resetAllSequencesAndSetNewDefaults((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeWorldSeed"), true))).then(Commands.argument("includeSequenceId", BoolArgumentType.bool()).executes(c -> RandomCommand.resetAllSequencesAndSetNewDefaults((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeWorldSeed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeSequenceId")))))))).then(((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id()).suggests(RandomCommand::suggestRandomSequence).executes(c -> RandomCommand.resetSequence((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sequence")))).then(((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer()).executes(c -> RandomCommand.resetSequence((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sequence"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), true, true))).then(((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool()).executes(c -> RandomCommand.resetSequence((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sequence"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeWorldSeed"), true))).then(Commands.argument("includeSequenceId", BoolArgumentType.bool()).executes(c -> RandomCommand.resetSequence((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sequence"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeWorldSeed"), BoolArgumentType.getBool((CommandContext)c, (String)"includeSequenceId")))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> drawRandomValueTree(String name, boolean announce) {
        return (LiteralArgumentBuilder)Commands.literal(name).then(((RequiredArgumentBuilder)Commands.argument("range", RangeArgument.intRange()).executes(c -> RandomCommand.randomSample((CommandSourceStack)c.getSource(), RangeArgument.Ints.getRange((CommandContext<CommandSourceStack>)c, "range"), null, announce))).then(((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id()).suggests(RandomCommand::suggestRandomSequence).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> RandomCommand.randomSample((CommandSourceStack)c.getSource(), RangeArgument.Ints.getRange((CommandContext<CommandSourceStack>)c, "range"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sequence"), announce))));
    }

    private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ArrayList result = Lists.newArrayList();
        ((CommandSourceStack)context.getSource()).getServer().getRandomSequences().forAllSequences((key, sequence) -> result.add(key.toString()));
        return SharedSuggestionProvider.suggest(result, builder);
    }

    private static int randomSample(CommandSourceStack source, MinMaxBounds.Ints range, @Nullable Identifier sequence, boolean announce) throws CommandSyntaxException {
        RandomSource random = sequence != null ? source.getServer().getRandomSequence(sequence) : source.getLevel().getRandom();
        int min = range.min().orElse(Integer.MIN_VALUE);
        int max = range.max().orElse(Integer.MAX_VALUE);
        long span = (long)max - (long)min;
        if (span == 0L) {
            throw ERROR_RANGE_TOO_SMALL.create();
        }
        if (span >= Integer.MAX_VALUE) {
            throw ERROR_RANGE_TOO_LARGE.create();
        }
        int value = Mth.randomBetweenInclusive(random, min, max);
        if (announce) {
            source.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("commands.random.roll", source.getDisplayName(), value, min, max), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.random.sample.success", value), false);
        }
        return value;
    }

    private static int resetSequence(CommandSourceStack source, Identifier sequence) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        source.getServer().getRandomSequences().reset(sequence, level.getSeed());
        source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
        return 1;
    }

    private static int resetSequence(CommandSourceStack source, Identifier sequence, int salt, boolean includeWorldSeed, boolean includeSequenceId) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        source.getServer().getRandomSequences().reset(sequence, level.getSeed(), salt, includeWorldSeed, includeSequenceId);
        source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
        return 1;
    }

    private static int resetAllSequences(CommandSourceStack source) {
        int count = source.getServer().getRandomSequences().clear();
        source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", count), false);
        return count;
    }

    private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack source, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        RandomSequences randomSequences = source.getServer().getRandomSequences();
        randomSequences.setSeedDefaults(salt, includeWorldSeed, includeSequenceId);
        int count = randomSequences.clear();
        source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", count), false);
        return count;
    }
}

