/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ChestType implements StringRepresentable
{
    SINGLE("single"),
    LEFT("left"),
    RIGHT("right");

    public static final Codec<ChestType> CODEC;
    private final String name;

    private ChestType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public ChestType getOpposite() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> SINGLE;
            case 1 -> RIGHT;
            case 2 -> LEFT;
        };
    }

    static {
        CODEC = StringRepresentable.fromEnum(ChestType::values);
    }
}

