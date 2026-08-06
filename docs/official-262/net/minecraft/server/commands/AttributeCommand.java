/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.DoubleArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(target -> Component.translatableEscape("commands.attribute.failed.entity", target));
    private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType((target, attribute) -> Component.translatableEscape("commands.attribute.failed.no_attribute", target, attribute));
    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType((target, attribute, modifier) -> Component.translatableEscape("commands.attribute.failed.no_modifier", attribute, target, modifier));
    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType((target, attribute, modifier) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", modifier, attribute, target));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("attribute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("target", EntityArgument.entity()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("attribute", ResourceArgument.resource(context, Registries.ATTRIBUTE)).then(((LiteralArgumentBuilder)Commands.literal("get").executes(c -> AttributeCommand.getAttributeValue((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), 1.0))).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes(c -> AttributeCommand.getAttributeValue((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), DoubleArgumentType.getDouble((CommandContext)c, (String)"scale")))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("base").then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes(c -> AttributeCommand.setAttributeBase((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), DoubleArgumentType.getDouble((CommandContext)c, (String)"value")))))).then(((LiteralArgumentBuilder)Commands.literal("get").executes(c -> AttributeCommand.getAttributeBase((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), 1.0))).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes(c -> AttributeCommand.getAttributeBase((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), DoubleArgumentType.getDouble((CommandContext)c, (String)"scale")))))).then(Commands.literal("reset").executes(c -> AttributeCommand.resetAttributeBase((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute")))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("modifier").then(Commands.literal("add").then(Commands.argument("id", IdentifierArgument.id()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("value", DoubleArgumentType.doubleArg()).then(Commands.literal("add_value").executes(c -> AttributeCommand.addModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), DoubleArgumentType.getDouble((CommandContext)c, (String)"value"), AttributeModifier.Operation.ADD_VALUE)))).then(Commands.literal("add_multiplied_base").executes(c -> AttributeCommand.addModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), DoubleArgumentType.getDouble((CommandContext)c, (String)"value"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)))).then(Commands.literal("add_multiplied_total").executes(c -> AttributeCommand.addModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), DoubleArgumentType.getDouble((CommandContext)c, (String)"value"), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))))))).then(Commands.literal("remove").then(Commands.argument("id", IdentifierArgument.id()).suggests((c, p) -> SharedSuggestionProvider.suggestResource(AttributeCommand.getAttributeModifiers(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute")), p)).executes(c -> AttributeCommand.removeModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id")))))).then(Commands.literal("value").then(Commands.literal("get").then(((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id()).suggests((c, p) -> SharedSuggestionProvider.suggestResource(AttributeCommand.getAttributeModifiers(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute")), p)).executes(c -> AttributeCommand.getAttributeModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), 1.0))).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes(c -> AttributeCommand.getAttributeModifier((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), ResourceArgument.getAttribute((CommandContext<CommandSourceStack>)c, "attribute"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), DoubleArgumentType.getDouble((CommandContext)c, (String)"scale")))))))))));
    }

    private static AttributeInstance getAttributeInstance(Entity target, Holder<Attribute> attribute) throws CommandSyntaxException {
        AttributeInstance attributeInstance = AttributeCommand.getLivingEntity(target).getAttributes().getInstance(attribute);
        if (attributeInstance == null) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute));
        }
        return attributeInstance;
    }

    private static LivingEntity getLivingEntity(Entity target) throws CommandSyntaxException {
        if (!(target instanceof LivingEntity)) {
            throw ERROR_NOT_LIVING_ENTITY.create((Object)target.getName());
        }
        LivingEntity livingEntity = (LivingEntity)target;
        return livingEntity;
    }

    private static LivingEntity getEntityWithAttribute(Entity target, Holder<Attribute> attribute) throws CommandSyntaxException {
        LivingEntity livingEntity = AttributeCommand.getLivingEntity(target);
        if (!livingEntity.getAttributes().hasAttribute(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute));
        }
        return livingEntity;
    }

    private static int getAttributeValue(CommandSourceStack source, Entity target, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
        LivingEntity livingEntity = AttributeCommand.getEntityWithAttribute(target, attribute);
        double result = livingEntity.getAttributeValue(attribute);
        source.sendSuccess(() -> Component.translatable("commands.attribute.value.get.success", AttributeCommand.getAttributeDescription(attribute), target.getName(), result), false);
        return (int)(result * scale);
    }

    private static int getAttributeBase(CommandSourceStack source, Entity target, Holder<Attribute> attribute, double scale) throws CommandSyntaxException {
        LivingEntity livingEntity = AttributeCommand.getEntityWithAttribute(target, attribute);
        double result = livingEntity.getAttributeBaseValue(attribute);
        source.sendSuccess(() -> Component.translatable("commands.attribute.base_value.get.success", AttributeCommand.getAttributeDescription(attribute), target.getName(), result), false);
        return (int)(result * scale);
    }

    private static int getAttributeModifier(CommandSourceStack source, Entity target, Holder<Attribute> attribute, Identifier id, double scale) throws CommandSyntaxException {
        LivingEntity livingEntity = AttributeCommand.getEntityWithAttribute(target, attribute);
        AttributeMap attributes = livingEntity.getAttributes();
        if (!attributes.hasModifier(attribute, id)) {
            throw ERROR_NO_SUCH_MODIFIER.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute), (Object)id);
        }
        double result = attributes.getModifierValue(attribute, id);
        source.sendSuccess(() -> Component.translatable("commands.attribute.modifier.value.get.success", Component.translationArg(id), AttributeCommand.getAttributeDescription(attribute), target.getName(), result), false);
        return (int)(result * scale);
    }

    private static Stream<Identifier> getAttributeModifiers(Entity target, Holder<Attribute> attribute) throws CommandSyntaxException {
        AttributeInstance attributeInstance = AttributeCommand.getAttributeInstance(target, attribute);
        return attributeInstance.getModifiers().stream().map(AttributeModifier::id);
    }

    private static int setAttributeBase(CommandSourceStack source, Entity target, Holder<Attribute> attribute, double value) throws CommandSyntaxException {
        AttributeCommand.getAttributeInstance(target, attribute).setBaseValue(value);
        source.sendSuccess(() -> Component.translatable("commands.attribute.base_value.set.success", AttributeCommand.getAttributeDescription(attribute), target.getName(), value), false);
        return 1;
    }

    private static int resetAttributeBase(CommandSourceStack source, Entity target, Holder<Attribute> attribute) throws CommandSyntaxException {
        LivingEntity livingTarget = AttributeCommand.getLivingEntity(target);
        if (!livingTarget.getAttributes().resetBaseValue(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute));
        }
        double value = livingTarget.getAttributeBaseValue(attribute);
        source.sendSuccess(() -> Component.translatable("commands.attribute.base_value.reset.success", AttributeCommand.getAttributeDescription(attribute), target.getName(), value), false);
        return 1;
    }

    private static int addModifier(CommandSourceStack source, Entity target, Holder<Attribute> attribute, Identifier id, double value, AttributeModifier.Operation operation) throws CommandSyntaxException {
        AttributeInstance attributeInstance = AttributeCommand.getAttributeInstance(target, attribute);
        AttributeModifier modifier = new AttributeModifier(id, value, operation);
        if (attributeInstance.hasModifier(id)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute), (Object)id);
        }
        attributeInstance.addPermanentModifier(modifier);
        source.sendSuccess(() -> Component.translatable("commands.attribute.modifier.add.success", Component.translationArg(id), AttributeCommand.getAttributeDescription(attribute), target.getName()), false);
        return 1;
    }

    private static int removeModifier(CommandSourceStack source, Entity target, Holder<Attribute> attribute, Identifier id) throws CommandSyntaxException {
        AttributeInstance attributeInstance = AttributeCommand.getAttributeInstance(target, attribute);
        if (attributeInstance.removeModifier(id)) {
            source.sendSuccess(() -> Component.translatable("commands.attribute.modifier.remove.success", Component.translationArg(id), AttributeCommand.getAttributeDescription(attribute), target.getName()), false);
            return 1;
        }
        throw ERROR_NO_SUCH_MODIFIER.create((Object)target.getName(), (Object)AttributeCommand.getAttributeDescription(attribute), (Object)id);
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.translatable(attribute.value().getDescriptionId());
    }
}

