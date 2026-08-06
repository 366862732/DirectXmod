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

public record WorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) implements Coordinates
{
    public static final WorldCoordinates ZERO_ROTATION = WorldCoordinates.absolute(new Vec2(0.0f, 0.0f));

    @Override
    public Vec3 getPosition(CommandSourceStack sender) {
        Vec3 pos = sender.getPosition();
        return new Vec3(this.x.get(pos.x), this.y.get(pos.y), this.z.get(pos.z));
    }

    @Override
    public Vec2 getRotation(CommandSourceStack sender) {
        Vec2 rot = sender.getRotation();
        return new Vec2((float)this.x.get(rot.x), (float)this.y.get(rot.y));
    }

    @Override
    public boolean isXRelative() {
        return this.x.isRelative();
    }

    @Override
    public boolean isYRelative() {
        return this.y.isRelative();
    }

    @Override
    public boolean isZRelative() {
        return this.z.isRelative();
    }

    public static WorldCoordinates parseInt(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        WorldCoordinate x = WorldCoordinate.parseInt(reader);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        WorldCoordinate y = WorldCoordinate.parseInt(reader);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        WorldCoordinate z = WorldCoordinate.parseInt(reader);
        return new WorldCoordinates(x, y, z);
    }

    public static WorldCoordinates parseDouble(StringReader reader, boolean centerCorrect) throws CommandSyntaxException {
        int start = reader.getCursor();
        WorldCoordinate x = WorldCoordinate.parseDouble(reader, centerCorrect);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        WorldCoordinate y = WorldCoordinate.parseDouble(reader, false);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext((ImmutableStringReader)reader);
        }
        reader.skip();
        WorldCoordinate z = WorldCoordinate.parseDouble(reader, centerCorrect);
        return new WorldCoordinates(x, y, z);
    }

    public static WorldCoordinates absolute(double x, double y, double z) {
        return new WorldCoordinates(new WorldCoordinate(false, x), new WorldCoordinate(false, y), new WorldCoordinate(false, z));
    }

    public static WorldCoordinates absolute(Vec2 rotation) {
        return new WorldCoordinates(new WorldCoordinate(false, rotation.x), new WorldCoordinate(false, rotation.y), new WorldCoordinate(true, 0.0));
    }
}

