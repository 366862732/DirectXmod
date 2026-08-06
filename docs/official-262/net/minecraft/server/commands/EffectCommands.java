/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class EffectCommands {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.effect.give.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.effect.clear.everything.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.effect.clear.specific.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("effect").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((LiteralArgumentBuilder)Commands.literal("clear").executes(c -> EffectCommands.clearEffects((CommandSourceStack)c.getSource(), (Collection<? extends Entity>)ImmutableList.of((Object)((CommandSourceStack)c.getSource()).getEntityOrException())))).then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities()).executes(c -> EffectCommands.clearEffects((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets")))).then(Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT)).executes(c -> EffectCommands.clearEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"))))))).then(Commands.literal("give").then(Commands.argument("targets", EntityArgument.entities()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT)).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), null, 0, true))).then(((RequiredArgumentBuilder)Commands.argument("seconds", IntegerArgumentType.integer((int)1, (int)1000000)).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seconds"), 0, true))).then(((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer((int)0, (int)255)).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seconds"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amplifier"), true))).then(Commands.argument("hideParticles", BoolArgumentType.bool()).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seconds"), IntegerArgumentType.getInteger((CommandContext)c, (String)"amplifier"), !BoolArgumentType.getBool((CommandContext)c, (String)"hideParticles"))))))).then(((LiteralArgumentBuilder)Commands.literal("infinite").executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), -1, 0, true))).then(((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer((int)0, (int)255)).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), -1, IntegerArgumentType.getInteger((CommandContext)c, (String)"amplifier"), true))).then(Commands.argument("hideParticles", BoolArgumentType.bool()).executes(c -> EffectCommands.giveEffect((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ResourceArgument.getMobEffect((CommandContext<CommandSourceStack>)c, "effect"), -1, IntegerArgumentType.getInteger((CommandContext)c, (String)"amplifier"), !BoolArgumentType.getBool((CommandContext)c, (String)"hideParticles"))))))))));
    }

    private static int giveEffect(CommandSourceStack source, Collection<? extends Entity> entities, Holder<MobEffect> effectHolder, @Nullable Integer seconds, int amplifier, boolean particles) throws CommandSyntaxException {
        MobEffect effect = effectHolder.value();
        int count = 0;
        int duration = seconds != null ? (effect.isInstantaneous() ? seconds : (seconds == -1 ? -1 : seconds * 20)) : (effect.isInstantaneous() ? 1 : 600);
        for (Entity entity : entities) {
            MobEffectInstance instance;
            LivingEntity livingEntity;
            if (!(entity instanceof LivingEntity) || !(livingEntity = (LivingEntity)entity).addEffect(instance = new MobEffectInstance(effectHolder, duration, amplifier, false, particles), source.getEntity())) continue;
            ++count;
        }
        if (count == 0) {
            throw ERROR_GIVE_FAILED.create();
        }
        if (entities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.effect.give.success.single", effect.getDisplayName(), ((Entity)entities.iterator().next()).getDisplayName(), duration / 20), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.effect.give.success.multiple", effect.getDisplayName(), entities.size(), duration / 20), true);
        }
        return count;
    }

    private static int clearEffects(CommandSourceStack source, Collection<? extends Entity> entities) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : entities) {
            LivingEntity livingEntity;
            if (!(entity instanceof LivingEntity) || !(livingEntity = (LivingEntity)entity).removeAllEffects()) continue;
            ++count;
        }
        if (count == 0) {
            throw ERROR_CLEAR_EVERYTHING_FAILED.create();
        }
        if (entities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.everything.success.single", ((Entity)entities.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.everything.success.multiple", entities.size()), true);
        }
        return count;
    }

    private static int clearEffect(CommandSourceStack source, Collection<? extends Entity> entities, Holder<MobEffect> effectHolder) throws CommandSyntaxException {
        MobEffect effect = effectHolder.value();
        int count = 0;
        for (Entity entity : entities) {
            LivingEntity livingEntity;
            if (!(entity instanceof LivingEntity) || !(livingEntity = (LivingEntity)entity).removeEffect(effectHolder)) continue;
            ++count;
        }
        if (count == 0) {
            throw ERROR_CLEAR_SPECIFIC_FAILED.create();
        }
        if (entities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.specific.success.single", effect.getDisplayName(), ((Entity)entities.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.specific.success.multiple", effect.getDisplayName(), entities.size()), true);
        }
        return count;
    }
}

