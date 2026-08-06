/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class DamageCommand {
    private static final SimpleCommandExceptionType ERROR_INVULNERABLE = new SimpleCommandExceptionType((Message)Component.translatable("commands.damage.invulnerable"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("damage").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("target", EntityArgument.entity()).then(((RequiredArgumentBuilder)Commands.argument("amount", FloatArgumentType.floatArg((float)0.0f)).executes(c -> DamageCommand.damage((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), FloatArgumentType.getFloat((CommandContext)c, (String)"amount"), ((CommandSourceStack)c.getSource()).getLevel().damageSources().generic()))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("damageType", ResourceArgument.resource(context, Registries.DAMAGE_TYPE)).executes(c -> DamageCommand.damage((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), FloatArgumentType.getFloat((CommandContext)c, (String)"amount"), new DamageSource(ResourceArgument.getResource((CommandContext<CommandSourceStack>)c, "damageType", Registries.DAMAGE_TYPE))))).then(Commands.literal("at").then(Commands.argument("location", Vec3Argument.vec3()).executes(c -> DamageCommand.damage((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), FloatArgumentType.getFloat((CommandContext)c, (String)"amount"), new DamageSource(ResourceArgument.getResource((CommandContext<CommandSourceStack>)c, "damageType", Registries.DAMAGE_TYPE), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "location"))))))).then(Commands.literal("by").then(((RequiredArgumentBuilder)Commands.argument("entity", EntityArgument.entity()).executes(c -> DamageCommand.damage((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), FloatArgumentType.getFloat((CommandContext)c, (String)"amount"), new DamageSource(ResourceArgument.getResource((CommandContext<CommandSourceStack>)c, "damageType", Registries.DAMAGE_TYPE), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "entity"))))).then(Commands.literal("from").then(Commands.argument("cause", EntityArgument.entity()).executes(c -> DamageCommand.damage((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), FloatArgumentType.getFloat((CommandContext)c, (String)"amount"), new DamageSource(ResourceArgument.getResource((CommandContext<CommandSourceStack>)c, "damageType", Registries.DAMAGE_TYPE), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "entity"), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "cause"))))))))))));
    }

    private static int damage(CommandSourceStack stack, Entity target, float amount, DamageSource source) throws CommandSyntaxException {
        if (target.hurtServer(stack.getLevel(), source, amount)) {
            stack.sendSuccess(() -> Component.translatable("commands.damage.success", Float.valueOf(amount), target.getDisplayName()), true);
            return 1;
        }
        throw ERROR_INVULNERABLE.create();
    }
}

