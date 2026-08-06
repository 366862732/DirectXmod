/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;

public class BossBarCommands {
    private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.bossbar.create.failed", id));
    private static final DynamicCommandExceptionType ERROR_DOESNT_EXIST = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.bossbar.unknown", id));
    private static final SimpleCommandExceptionType ERROR_NO_PLAYER_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.players.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_NAME_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.name.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_COLOR_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.color.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_STYLE_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.style.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_VALUE_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.value.unchanged"));
    private static final SimpleCommandExceptionType ERROR_NO_MAX_CHANGE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.max.unchanged"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_HIDDEN = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.visibility.unchanged.hidden"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_VISIBLE = new SimpleCommandExceptionType((Message)Component.translatable("commands.bossbar.set.visibility.unchanged.visible"));
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_BOSS_BAR = (c, b) -> SharedSuggestionProvider.suggestResource(((CommandSourceStack)c.getSource()).getServer().getCustomBossEvents().getIds(), b);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("bossbar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("add").then(Commands.argument("id", IdentifierArgument.id()).then(Commands.argument("name", ComponentArgument.textComponent(context)).executes(c -> BossBarCommands.createBar((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), ComponentArgument.getResolvedComponent((CommandContext<CommandSourceStack>)c, "name"))))))).then(Commands.literal("remove").then(Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_BOSS_BAR).executes(c -> BossBarCommands.removeBar((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c)))))).then(Commands.literal("list").executes(c -> BossBarCommands.listBars((CommandSourceStack)c.getSource())))).then(Commands.literal("set").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_BOSS_BAR).then(Commands.literal("name").then(Commands.argument("name", ComponentArgument.textComponent(context)).executes(c -> BossBarCommands.setName((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), ComponentArgument.getResolvedComponent((CommandContext<CommandSourceStack>)c, "name")))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("color").then(Commands.literal("pink").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.PINK)))).then(Commands.literal("blue").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.BLUE)))).then(Commands.literal("red").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.RED)))).then(Commands.literal("green").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.GREEN)))).then(Commands.literal("yellow").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.YELLOW)))).then(Commands.literal("purple").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.PURPLE)))).then(Commands.literal("white").executes(c -> BossBarCommands.setColor((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarColor.WHITE))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("style").then(Commands.literal("progress").executes(c -> BossBarCommands.setStyle((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarOverlay.PROGRESS)))).then(Commands.literal("notched_6").executes(c -> BossBarCommands.setStyle((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarOverlay.NOTCHED_6)))).then(Commands.literal("notched_10").executes(c -> BossBarCommands.setStyle((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarOverlay.NOTCHED_10)))).then(Commands.literal("notched_12").executes(c -> BossBarCommands.setStyle((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarOverlay.NOTCHED_12)))).then(Commands.literal("notched_20").executes(c -> BossBarCommands.setStyle((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BossEvent.BossBarOverlay.NOTCHED_20))))).then(Commands.literal("value").then(Commands.argument("value", IntegerArgumentType.integer((int)0)).executes(c -> BossBarCommands.setValue((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), IntegerArgumentType.getInteger((CommandContext)c, (String)"value")))))).then(Commands.literal("max").then(Commands.argument("max", IntegerArgumentType.integer((int)1)).executes(c -> BossBarCommands.setMax((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), IntegerArgumentType.getInteger((CommandContext)c, (String)"max")))))).then(Commands.literal("visible").then(Commands.argument("visible", BoolArgumentType.bool()).executes(c -> BossBarCommands.setVisible((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), BoolArgumentType.getBool((CommandContext)c, (String)"visible")))))).then(((LiteralArgumentBuilder)Commands.literal("players").executes(c -> BossBarCommands.setPlayers((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), Collections.emptyList()))).then(Commands.argument("targets", EntityArgument.players()).executes(c -> BossBarCommands.setPlayers((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), EntityArgument.getOptionalPlayers((CommandContext<CommandSourceStack>)c, "targets")))))))).then(Commands.literal("get").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_BOSS_BAR).then(Commands.literal("value").executes(c -> BossBarCommands.getValue((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c))))).then(Commands.literal("max").executes(c -> BossBarCommands.getMax((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c))))).then(Commands.literal("visible").executes(c -> BossBarCommands.getVisible((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c))))).then(Commands.literal("players").executes(c -> BossBarCommands.getPlayers((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c)))))));
    }

    private static int getValue(CommandSourceStack source, CustomBossEvent bossBar) {
        source.sendSuccess(() -> Component.translatable("commands.bossbar.get.value", bossBar.getDisplayName(), bossBar.value()), true);
        return bossBar.value();
    }

    private static int getMax(CommandSourceStack source, CustomBossEvent bossBar) {
        source.sendSuccess(() -> Component.translatable("commands.bossbar.get.max", bossBar.getDisplayName(), bossBar.max()), true);
        return bossBar.max();
    }

    private static int getVisible(CommandSourceStack source, CustomBossEvent bossBar) {
        if (bossBar.isVisible()) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.get.visible.visible", bossBar.getDisplayName()), true);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("commands.bossbar.get.visible.hidden", bossBar.getDisplayName()), true);
        return 0;
    }

    private static int getPlayers(CommandSourceStack source, CustomBossEvent bossBar) {
        if (bossBar.getPlayers().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.get.players.none", bossBar.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.get.players.some", bossBar.getDisplayName(), bossBar.getPlayers().size(), ComponentUtils.formatList(bossBar.getPlayers(), Player::getDisplayName)), true);
        }
        return bossBar.getPlayers().size();
    }

    private static int setVisible(CommandSourceStack source, CustomBossEvent bossBar, boolean visible) throws CommandSyntaxException {
        if (bossBar.isVisible() == visible) {
            if (visible) {
                throw ERROR_ALREADY_VISIBLE.create();
            }
            throw ERROR_ALREADY_HIDDEN.create();
        }
        bossBar.setVisible(visible);
        if (visible) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.visible.success.visible", bossBar.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.visible.success.hidden", bossBar.getDisplayName()), true);
        }
        return 0;
    }

    private static int setValue(CommandSourceStack source, CustomBossEvent bossBar, int value) throws CommandSyntaxException {
        if (bossBar.value() == value) {
            throw ERROR_NO_VALUE_CHANGE.create();
        }
        bossBar.setValue(value);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.set.value.success", bossBar.getDisplayName(), value), true);
        return value;
    }

    private static int setMax(CommandSourceStack source, CustomBossEvent bossBar, int value) throws CommandSyntaxException {
        if (bossBar.max() == value) {
            throw ERROR_NO_MAX_CHANGE.create();
        }
        bossBar.setMax(value);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.set.max.success", bossBar.getDisplayName(), value), true);
        return value;
    }

    private static int setColor(CommandSourceStack source, CustomBossEvent bossBar, BossEvent.BossBarColor color) throws CommandSyntaxException {
        if (bossBar.getColor().equals(color)) {
            throw ERROR_NO_COLOR_CHANGE.create();
        }
        bossBar.setColor(color);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.set.color.success", bossBar.getDisplayName()), true);
        return 0;
    }

    private static int setStyle(CommandSourceStack source, CustomBossEvent bossBar, BossEvent.BossBarOverlay style) throws CommandSyntaxException {
        if (bossBar.getOverlay().equals(style)) {
            throw ERROR_NO_STYLE_CHANGE.create();
        }
        bossBar.setOverlay(style);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.set.style.success", bossBar.getDisplayName()), true);
        return 0;
    }

    private static int setName(CommandSourceStack source, CustomBossEvent bossBar, Component name) throws CommandSyntaxException {
        MutableComponent replaced = ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(null).build(), name);
        if (bossBar.getName().equals(replaced)) {
            throw ERROR_NO_NAME_CHANGE.create();
        }
        bossBar.setName(replaced);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.set.name.success", bossBar.getDisplayName()), true);
        return 0;
    }

    private static int setPlayers(CommandSourceStack source, CustomBossEvent bossBar, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        boolean changed = bossBar.setPlayers(targets);
        if (!changed) {
            throw ERROR_NO_PLAYER_CHANGE.create();
        }
        if (bossBar.getPlayers().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.players.success.none", bossBar.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.players.success.some", bossBar.getDisplayName(), targets.size(), ComponentUtils.formatList(targets, Player::getDisplayName)), true);
        }
        return bossBar.getPlayers().size();
    }

    private static int listBars(CommandSourceStack source) {
        Collection<CustomBossEvent> events = source.getServer().getCustomBossEvents().getEvents();
        if (events.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.list.bars.none"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.list.bars.some", events.size(), ComponentUtils.formatList(events, CustomBossEvent::getDisplayName)), false);
        }
        return events.size();
    }

    private static int createBar(CommandSourceStack source, Identifier id, Component name) throws CommandSyntaxException {
        CustomBossEvents events = source.getServer().getCustomBossEvents();
        if (events.get(id) != null) {
            throw ERROR_ALREADY_EXISTS.create((Object)id.toString());
        }
        CustomBossEvent event = events.create(source.getLevel().getRandom(), id, ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(null).build(), name));
        source.sendSuccess(() -> Component.translatable("commands.bossbar.create.success", event.getDisplayName()), true);
        return events.getEvents().size();
    }

    private static int removeBar(CommandSourceStack source, CustomBossEvent bossBar) {
        CustomBossEvents events = source.getServer().getCustomBossEvents();
        bossBar.removeAllPlayers();
        events.remove(bossBar);
        source.sendSuccess(() -> Component.translatable("commands.bossbar.remove.success", bossBar.getDisplayName()), true);
        return events.getEvents().size();
    }

    public static CustomBossEvent getBossBar(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");
        CustomBossEvent event = ((CommandSourceStack)context.getSource()).getServer().getCustomBossEvents().get(id);
        if (event == null) {
            throw ERROR_DOESNT_EXIST.create((Object)id.toString());
        }
        return event;
    }
}

