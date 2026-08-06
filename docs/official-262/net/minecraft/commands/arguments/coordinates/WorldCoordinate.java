/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;

public record WorldCoordinate(boolean relative, double value) {
    private static final char PREFIX_RELATIVE = '~';
    public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType((Message)Component.translatable("argument.pos.missing.double"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType((Message)Component.translatable("argument.pos.missing.int"));

    public double get(double original) {
        if (this.relative) {
            return this.value + original;
        }
        return this.value;
    }

    public static WorldCoordinate parseDouble(StringReader reader, boolean center) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext((ImmutableStringReader)reader);
        }
        if (!reader.canRead()) {
            throw ERROR_EXPECTED_DOUBLE.createWithContext((ImmutableStringReader)reader);
        }
        boolean relative = WorldCoordinate.isRelative(reader);
        int start = reader.getCursor();
        double value = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
        String number = reader.getString().substring(start, reader.getCursor());
        if (relative && number.isEmpty()) {
            return new WorldCoordinate(true, 0.0);
        }
        if (!number.contains(".") && !relative && center) {
            value += 0.5;
        }
        return new WorldCoordinate(relative, value);
    }

    public static WorldCoordinate parseInt(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext((ImmutableStringReader)reader);
        }
        if (!reader.canRead()) {
            throw ERROR_EXPECTED_INT.createWithContext((ImmutableStringReader)reader);
        }
        boolean relative = WorldCoordinate.isRelative(reader);
        double value = reader.canRead() && reader.peek() != ' ' ? (relative ? reader.readDouble() : (double)reader.readInt()) : 0.0;
        return new WorldCoordinate(relative, value);
    }

    public static boolean isRelative(StringReader reader) {
        boolean relative;
        if (reader.peek() == '~') {
            relative = true;
            reader.skip();
        } else {
            relative = false;
        }
        return relative;
    }

    public boolean isRelative() {
        return this.relative;
    }
}

