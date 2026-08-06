/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeCommand {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.recipe.give.failed"));
    private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.recipe.take.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("recipe").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("give").then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).then(Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE)).executes(c -> RecipeCommand.giveRecipes((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), Collections.singleton(ResourceKeyArgument.getRecipe((CommandContext<CommandSourceStack>)c, "recipe")))))).then(Commands.literal("*").executes(c -> RecipeCommand.giveRecipes((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getServer().getRecipeManager().getRecipes())))))).then(Commands.literal("take").then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).then(Commands.argument("recipe", ResourceKeyArgument.key(Registries.RECIPE)).executes(c -> RecipeCommand.takeRecipes((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), Collections.singleton(ResourceKeyArgument.getRecipe((CommandContext<CommandSourceStack>)c, "recipe")))))).then(Commands.literal("*").executes(c -> RecipeCommand.takeRecipes((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getServer().getRecipeManager().getRecipes()))))));
    }

    private static int giveRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException {
        int success = 0;
        for (ServerPlayer player : players) {
            success += player.awardRecipes(recipes);
        }
        if (success == 0) {
            throw ERROR_GIVE_FAILED.create();
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.recipe.give.success.single", recipes.size(), ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.recipe.give.success.multiple", recipes.size(), players.size()), true);
        }
        return success;
    }

    private static int takeRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException {
        int success = 0;
        for (ServerPlayer player : players) {
            success += player.resetRecipes(recipes);
        }
        if (success == 0) {
            throw ERROR_TAKE_FAILED.create();
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.recipe.take.success.single", recipes.size(), ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.recipe.take.success.multiple", recipes.size(), players.size()), true);
        }
        return success;
    }
}

