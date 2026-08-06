/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public record CardinalLighting(float down, float up, float north, float south, float west, float east) {
    public static final CardinalLighting DEFAULT = new CardinalLighting(0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f);
    public static final CardinalLighting NETHER = new CardinalLighting(0.9f, 0.9f, 0.8f, 0.8f, 0.6f, 0.6f);

    public float byFace(Direction direction) {
        return switch (direction) {
            default -> throw new MatchException(null, null);
            case Direction.DOWN -> this.down;
            case Direction.UP -> this.up;
            case Direction.NORTH -> this.north;
            case Direction.SOUTH -> this.south;
            case Direction.WEST -> this.west;
            case Direction.EAST -> this.east;
        };
    }

    public static enum Type implements StringRepresentable
    {
        DEFAULT("default", DEFAULT),
        NETHER("nether", NETHER);

        public static final Codec<Type> CODEC;
        private final String name;
        private final CardinalLighting lighting;

        private Type(String name, CardinalLighting lighting) {
            this.name = name;
            this.lighting = lighting;
        }

        public CardinalLighting get() {
            return this.lighting;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }
}

