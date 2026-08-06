/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands.arguments.coordinates;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface Coordinates {
    public Vec3 getPosition(CommandSourceStack var1);

    public Vec2 getRotation(CommandSourceStack var1);

    default public BlockPos getBlockPos(CommandSourceStack sender) {
        return BlockPos.containing(this.getPosition(sender));
    }

    public boolean isXRelative();

    public boolean isYRelative();

    public boolean isZRelative();
}

