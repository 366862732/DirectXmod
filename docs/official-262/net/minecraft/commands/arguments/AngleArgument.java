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
package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AngleArgument
implements ArgumentType<SingleAngle> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType((Message)Component.translatable("argument.angle.incomplete"));
    public static final SimpleCommandExceptionType ERROR_INVALID_ANGLE = new SimpleCommandExceptionType((Message)Component.translatable("argument.angle.invalid"));

    public static AngleArgument angle() {
        return new AngleArgument();
    }

    public static float getAngle(CommandContext<CommandSourceStack> context, String name) {
        return ((SingleAngle)context.getArgument(name, SingleAngle.class)).getAngle((CommandSourceStack)context.getSource());
    }

    public SingleAngle parse(StringReader reader) throws CommandSyntaxException {
        float value;
        if (!reader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        boolean isRelative = WorldCoordinate.isRelative(reader);
        float f = value = reader.canRead() && reader.peek() != ' ' ? reader.readFloat() : 0.0f;
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw ERROR_INVALID_ANGLE.createWithContext((ImmutableStringReader)reader);
        }
        return new SingleAngle(value, isRelative);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static final class SingleAngle {
        private final float angle;
        private final boolean isRelative;

        private SingleAngle(float angle, boolean isRelative) {
            this.angle = angle;
            this.isRelative = isRelative;
        }

        public float getAngle(CommandSourceStack sender) {
            return Mth.wrapDegrees(this.isRelative ? this.angle + sender.getRotation().y : this.angle);
        }
    }
}

