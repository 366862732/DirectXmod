/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.HashSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;

public class TagCommand {
    private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.tag.add.failed"));
    private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.tag.remove.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tag").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities()).then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).executes(c -> TagCommand.addTag((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), StringArgumentType.getString((CommandContext)c, (String)"name")))))).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests((c, p) -> SharedSuggestionProvider.suggest(TagCommand.getTags(EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets")), p)).executes(c -> TagCommand.removeTag((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), StringArgumentType.getString((CommandContext)c, (String)"name")))))).then(Commands.literal("list").executes(c -> TagCommand.listTags((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"))))));
    }

    private static Collection<String> getTags(Collection<? extends Entity> entities) {
        HashSet result = Sets.newHashSet();
        for (Entity entity : entities) {
            result.addAll(entity.entityTags());
        }
        return result;
    }

    private static int addTag(CommandSourceStack source, Collection<? extends Entity> targets, String name) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : targets) {
            if (!entity.addTag(name)) continue;
            ++count;
        }
        if (count == 0) {
            throw ERROR_ADD_FAILED.create();
        }
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.tag.add.success.single", name, ((Entity)targets.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tag.add.success.multiple", name, targets.size()), true);
        }
        return count;
    }

    private static int removeTag(CommandSourceStack source, Collection<? extends Entity> targets, String name) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : targets) {
            if (!entity.removeTag(name)) continue;
            ++count;
        }
        if (count == 0) {
            throw ERROR_REMOVE_FAILED.create();
        }
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.tag.remove.success.single", name, ((Entity)targets.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tag.remove.success.multiple", name, targets.size()), true);
        }
        return count;
    }

    private static int listTags(CommandSourceStack source, Collection<? extends Entity> targets) {
        HashSet tags = Sets.newHashSet();
        for (Entity entity : targets) {
            tags.addAll(entity.entityTags());
        }
        if (targets.size() == 1) {
            Entity entity = targets.iterator().next();
            if (tags.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.tag.list.single.empty", entity.getDisplayName()), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.tag.list.single.success", entity.getDisplayName(), tags.size(), ComponentUtils.formatList(tags)), false);
            }
        } else if (tags.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.empty", targets.size()), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tag.list.multiple.success", targets.size(), tags.size(), ComponentUtils.formatList(tags)), false);
        }
        return tags.size();
    }
}

