/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Pose implements StringRepresentable
{
    STANDING(0, "standing"),
    FALL_FLYING(1, "fall_flying"),
    SLEEPING(2, "sleeping"),
    SWIMMING(3, "swimming"),
    SPIN_ATTACK(4, "spin_attack"),
    CROUCHING(5, "crouching"),
    LONG_JUMPING(6, "long_jumping"),
    DYING(7, "dying"),
    CROAKING(8, "croaking"),
    USING_TONGUE(9, "using_tongue"),
    SITTING(10, "sitting"),
    ROARING(11, "roaring"),
    SNIFFING(12, "sniffing"),
    EMERGING(13, "emerging"),
    DIGGING(14, "digging"),
    SLIDING(15, "sliding"),
    SHOOTING(16, "shooting"),
    INHALING(17, "inhaling");

    public static final IntFunction<Pose> BY_ID;
    public static final Codec<Pose> CODEC;
    public static final StreamCodec<ByteBuf, Pose> STREAM_CODEC;
    private final int id;
    private final String name;

    private Pose(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        BY_ID = ByIdMap.continuous(Pose::id, Pose.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        CODEC = StringRepresentable.fromEnum(Pose::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Pose::id);
    }
}

