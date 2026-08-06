/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BedPart implements StringRepresentable
{
    HEAD("head"),
    FOOT("foot");

    public static final Codec<BedPart> CODEC;
    private final String name;

    private BedPart(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        CODEC = StringRepresentable.fromEnum(BedPart::values);
    }
}

