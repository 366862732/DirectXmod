/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
    private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(name -> Component.translatableEscape("clear.failed.single", name));
    private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(count -> Component.translatableEscape("clear.failed.multiple", count));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> ClearInventoryCommands.clearUnlimited((CommandSourceStack)c.getSource(), Collections.singleton(((CommandSourceStack)c.getSource()).getPlayerOrException()), i -> true))).then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).executes(c -> ClearInventoryCommands.clearUnlimited((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), i -> true))).then(((RequiredArgumentBuilder)Commands.argument("item", ItemPredicateArgument.itemPredicate(context)).executes(c -> ClearInventoryCommands.clearUnlimited((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item")))).then(Commands.argument("maxCount", IntegerArgumentType.integer((int)0)).executes(c -> ClearInventoryCommands.clearInventory((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item"), IntegerArgumentType.getInteger((CommandContext)c, (String)"maxCount")))))));
    }

    private static int clearUnlimited(CommandSourceStack source, Collection<ServerPlayer> players, Predicate<ItemStack> predicate) throws CommandSyntaxException {
        return ClearInventoryCommands.clearInventory(source, players, predicate, -1);
    }

    private static int clearInventory(CommandSourceStack source, Collection<ServerPlayer> players, Predicate<ItemStack> predicate, int maxCount) throws CommandSyntaxException {
        int count = 0;
        for (ServerPlayer player : players) {
            count += player.getInventory().clearOrCountMatchingItems(predicate, maxCount, player.inventoryMenu.getCraftSlots());
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
        }
        if (count == 0) {
            if (players.size() == 1) {
                throw ERROR_SINGLE.create((Object)players.iterator().next().getName());
            }
            throw ERROR_MULTIPLE.create((Object)players.size());
        }
        int finalCount = count;
        if (maxCount == 0) {
            if (players.size() == 1) {
                source.sendSuccess(() -> Component.translatable("commands.clear.test.single", finalCount, ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.clear.test.multiple", finalCount, players.size()), true);
            }
        } else if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.clear.success.single", finalCount, ((ServerPlayer)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.clear.success.multiple", finalCount, players.size()), true);
        }
        return count;
    }
}

