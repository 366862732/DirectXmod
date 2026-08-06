/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

public class NbtTagArgument
extends ParserBasedArgument<Tag> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0", "0b", "0l", "0.0", "\"foo\"", "{foo=bar}", "[0]");
    private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(NbtOps.INSTANCE);

    private NbtTagArgument() {
        super(TAG_PARSER);
    }

    public static NbtTagArgument nbtTag() {
        return new NbtTagArgument();
    }

    public static <S> Tag getNbtTag(CommandContext<S> context, String name) {
        return (Tag)context.getArgument(name, Tag.class);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

