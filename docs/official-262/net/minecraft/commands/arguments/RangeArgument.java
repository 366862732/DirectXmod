/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 */
package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;

public interface RangeArgument<T extends MinMaxBounds<?>>
extends ArgumentType<T> {
    public static Ints intRange() {
        return new Ints();
    }

    public static Floats floatRange() {
        return new Floats();
    }

    public static class Ints
    implements RangeArgument<MinMaxBounds.Ints> {
        private static final Collection<String> EXAMPLES = Arrays.asList("0..5", "0", "-5", "-100..", "..100");

        public static MinMaxBounds.Ints getRange(CommandContext<CommandSourceStack> context, String name) {
            return (MinMaxBounds.Ints)context.getArgument(name, MinMaxBounds.Ints.class);
        }

        public MinMaxBounds.Ints parse(StringReader reader) throws CommandSyntaxException {
            return MinMaxBounds.Ints.fromReader(reader);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }

    public static class Floats
    implements RangeArgument<MinMaxBounds.Doubles> {
        private static final Collection<String> EXAMPLES = Arrays.asList("0..5.2", "0", "-5.4", "-100.76..", "..100");

        public static MinMaxBounds.Doubles getRange(CommandContext<CommandSourceStack> context, String name) {
            return (MinMaxBounds.Doubles)context.getArgument(name, MinMaxBounds.Doubles.class);
        }

        public MinMaxBounds.Doubles parse(StringReader reader) throws CommandSyntaxException {
            return MinMaxBounds.Doubles.fromReader(reader);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}

