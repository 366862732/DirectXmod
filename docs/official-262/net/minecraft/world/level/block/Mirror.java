/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;

public enum Mirror implements StringRepresentable
{
    NONE("none", OctahedralGroup.IDENTITY),
    LEFT_RIGHT("left_right", OctahedralGroup.INVERT_Z),
    FRONT_BACK("front_back", OctahedralGroup.INVERT_X);

    public static final Codec<Mirror> CODEC;
    @Deprecated
    public static final Codec<Mirror> LEGACY_CODEC;
    private final String id;
    private final Component symbol;
    private final OctahedralGroup rotation;

    private Mirror(String id, OctahedralGroup rotation) {
        this.id = id;
        this.symbol = Component.translatable("mirror." + id);
        this.rotation = rotation;
    }

    public int mirror(int rotation, int steps) {
        int halfSteps = steps / 2;
        int correctedRotation = rotation > halfSteps ? rotation - steps : rotation;
        return switch (this.ordinal()) {
            case 2 -> (steps - correctedRotation) % steps;
            case 1 -> (halfSteps - correctedRotation + steps) % steps;
            default -> rotation;
        };
    }

    public Rotation getRotation(Direction value) {
        Direction.Axis axis = value.getAxis();
        return this == LEFT_RIGHT && axis == Direction.Axis.Z || this == FRONT_BACK && axis == Direction.Axis.X ? Rotation.CLOCKWISE_180 : Rotation.NONE;
    }

    public Direction mirror(Direction direction) {
        if (this == FRONT_BACK && direction.getAxis() == Direction.Axis.X) {
            return direction.getOpposite();
        }
        if (this == LEFT_RIGHT && direction.getAxis() == Direction.Axis.Z) {
            return direction.getOpposite();
        }
        return direction;
    }

    public OctahedralGroup rotation() {
        return this.rotation;
    }

    public Component symbol() {
        return this.symbol;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    static {
        CODEC = StringRepresentable.fromEnum(Mirror::values);
        LEGACY_CODEC = ExtraCodecs.legacyEnum(Mirror::valueOf);
    }
}

