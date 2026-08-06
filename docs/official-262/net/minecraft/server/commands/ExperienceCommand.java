/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ExperienceCommand {
    private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID = new SimpleCommandExceptionType((Message)Component.translatable("commands.experience.set.points.invalid"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode command = dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("experience").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("add").then(Commands.argument("target", EntityArgument.players()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer()).executes(c -> ExperienceCommand.addExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.POINTS))).then(Commands.literal("points").executes(c -> ExperienceCommand.addExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.POINTS)))).then(Commands.literal("levels").executes(c -> ExperienceCommand.addExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.LEVELS))))))).then(Commands.literal("set").then(Commands.argument("target", EntityArgument.players()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer((int)0)).executes(c -> ExperienceCommand.setExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.POINTS))).then(Commands.literal("points").executes(c -> ExperienceCommand.setExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.POINTS)))).then(Commands.literal("levels").executes(c -> ExperienceCommand.setExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amount"), Type.LEVELS))))))).then(Commands.literal("query").then(((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.player()).then(Commands.literal("points").executes(c -> ExperienceCommand.queryExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayer((CommandContext<CommandSourceStack>)c, "target"), Type.POINTS)))).then(Commands.literal("levels").executes(c -> ExperienceCommand.queryExperience((CommandSourceStack)c.getSource(), EntityArgument.getPlayer((CommandContext<CommandSourceStack>)c, "target"), Type.LEVELS))))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).redirect((CommandNode)command));
    }

    private static int queryExperience(CommandSourceStack source, ServerPlayer target, Type type) {
        int result = type.query.applyAsInt(target);
        source.sendSuccess(() -> Component.translatable("commands.experience.query." + type.name, target.getDisplayName(), result), false);
        return result;
    }

    private static int addExperience(CommandSourceStack source, Collection<? extends ServerPlayer> players, int amount, Type type) {
        for (ServerPlayer serverPlayer : players) {
            type.add.accept(serverPlayer, amount);
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.experience.add." + type.name + ".success.single", amount, ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.experience.add." + type.name + ".success.multiple", amount, players.size()), true);
        }
        return players.size();
    }

    private static int setExperience(CommandSourceStack source, Collection<? extends ServerPlayer> players, int amount, Type type) throws CommandSyntaxException {
        int success = 0;
        for (ServerPlayer serverPlayer : players) {
            if (!type.set.test(serverPlayer, amount)) continue;
            ++success;
        }
        if (success == 0) {
            throw ERROR_SET_POINTS_INVALID.create();
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.experience.set." + type.name + ".success.single", amount, ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.experience.set." + type.name + ".success.multiple", amount, players.size()), true);
        }
        return players.size();
    }

    private static enum Type {
        POINTS("points", Player::giveExperiencePoints, (p, a) -> {
            if (a >= p.getXpNeededForNextLevel()) {
                return false;
            }
            p.setExperiencePoints((int)a);
            return true;
        }, p -> Mth.floor(p.experienceProgress * (float)p.getXpNeededForNextLevel())),
        LEVELS("levels", ServerPlayer::giveExperienceLevels, (p, a) -> {
            p.setExperienceLevels((int)a);
            return true;
        }, p -> p.experienceLevel);

        public final BiConsumer<ServerPlayer, Integer> add;
        public final BiPredicate<ServerPlayer, Integer> set;
        public final String name;
        private final ToIntFunction<ServerPlayer> query;

        private Type(String name, BiConsumer<ServerPlayer, Integer> add, BiPredicate<ServerPlayer, Integer> set, ToIntFunction<ServerPlayer> query) {
            this.add = add;
            this.name = name;
            this.set = set;
            this.query = query;
        }
    }
}

