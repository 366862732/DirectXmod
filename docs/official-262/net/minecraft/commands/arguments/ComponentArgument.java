/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.entity.Entity;

public class ComponentArgument
extends ParserBasedArgument<Component> {
    private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
    public static final DynamicCommandExceptionType ERROR_INVALID_COMPONENT = new DynamicCommandExceptionType(message -> Component.translatableEscape("argument.component.invalid", message));
    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

    private ComponentArgument(HolderLookup.Provider registries) {
        super(TAG_PARSER.withCodec(registries.createSerializationContext(OPS), TAG_PARSER, ComponentSerialization.CODEC, ERROR_INVALID_COMPONENT));
    }

    public static Component getRawComponent(CommandContext<CommandSourceStack> context, String name) {
        return (Component)context.getArgument(name, Component.class);
    }

    public static Component getResolvedComponent(CommandContext<CommandSourceStack> context, String name, Entity contentEntity) throws CommandSyntaxException {
        return ComponentUtils.resolve(ResolutionContext.builder().withSource((CommandSourceStack)context.getSource()).withEntityOverride(contentEntity).build(), ComponentArgument.getRawComponent(context, name));
    }

    public static Component getResolvedComponent(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ComponentUtils.resolve(ResolutionContext.create((CommandSourceStack)context.getSource()), ComponentArgument.getRawComponent(context, name));
    }

    public static ComponentArgument textComponent(CommandBuildContext context) {
        return new ComponentArgument(context);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

