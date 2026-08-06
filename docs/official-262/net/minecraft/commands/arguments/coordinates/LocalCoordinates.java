/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 */
package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record LocalCoordinates(double left, double up, double forwards) implements Coordinates
{
    public static final char PREFIX_LOCAL_COORDINATE = '^';

    @Override
    public Vec3 getPosition(CommandSourceStack sender) {
        Vec3 source = sender.getAnchor().apply(sender);
        return Vec3.applyLocalCoordinatesToRotation(sender.getRotation(), new Vec3(this.left, this.up, this.forwards)).add(source.x, source.y, source.z);
    }

    @Override
    public Vec2 getRotation(CommandSourceStack sender) {
        return Vec2.ZERO;
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static LocalCoordinates parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        double left = LocalCoordinates.readDouble(reader, start);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        double up = LocalCoordinates.readDouble(reader, start);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        double forwards = LocalCoordinates.readDouble(reader, start);
        return new LocalCoordinates(left, up, forwards);
    }

    private static double readDouble(StringReader reader, int start) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext((ImmutableStringReader)reader);
        }
        if (reader.peek() != '^') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
    }
}

