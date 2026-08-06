/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class SwizzleArgument
implements ArgumentType<EnumSet<Direction.Axis>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("xyz", "x");
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType((Message)Component.translatable("arguments.swizzle.invalid"));

    public static SwizzleArgument swizzle() {
        return new SwizzleArgument();
    }

    public static EnumSet<Direction.Axis> getSwizzle(CommandContext<CommandSourceStack> context, String name) {
        return (EnumSet)context.getArgument(name, EnumSet.class);
    }

    public EnumSet<Direction.Axis> parse(StringReader reader) throws CommandSyntaxException {
        EnumSet<Direction.Axis> result = EnumSet.noneOf(Direction.Axis.class);
        while (reader.canRead() && reader.peek() != ' ') {
            char c = reader.read();
            Direction.Axis axis = switch (c) {
                case 'x' -> Direction.Axis.X;
                case 'y' -> Direction.Axis.Y;
                case 'z' -> Direction.Axis.Z;
                default -> throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader);
            };
            if (result.contains(axis)) {
                throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader);
            }
            result.add(axis);
        }
        return result;
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

