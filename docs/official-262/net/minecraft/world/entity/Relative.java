/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum Relative {
    X(0),
    Y(1),
    Z(2),
    Y_ROT(3),
    X_ROT(4),
    DELTA_X(5),
    DELTA_Y(6),
    DELTA_Z(7),
    ROTATE_DELTA(8);

    public static final Set<Relative> ALL;
    public static final Set<Relative> ROTATION;
    public static final Set<Relative> DELTA;
    public static final StreamCodec<ByteBuf, Set<Relative>> SET_STREAM_CODEC;
    private final int bit;

    @SafeVarargs
    public static Set<Relative> union(Set<Relative> ... sets) {
        HashSet<Relative> set = new HashSet<Relative>();
        for (Set<Relative> s : sets) {
            set.addAll(s);
        }
        return set;
    }

    public static Set<Relative> rotation(boolean relativeYRot, boolean relativeXRot) {
        EnumSet<Relative> relatives = EnumSet.noneOf(Relative.class);
        if (relativeYRot) {
            relatives.add(Y_ROT);
        }
        if (relativeXRot) {
            relatives.add(X_ROT);
        }
        return relatives;
    }

    public static Set<Relative> position(boolean relativeX, boolean relativeY, boolean relativeZ) {
        EnumSet<Relative> relatives = EnumSet.noneOf(Relative.class);
        if (relativeX) {
            relatives.add(X);
        }
        if (relativeY) {
            relatives.add(Y);
        }
        if (relativeZ) {
            relatives.add(Z);
        }
        return relatives;
    }

    public static Set<Relative> direction(boolean relativeX, boolean relativeY, boolean relativeZ) {
        EnumSet<Relative> relatives = EnumSet.noneOf(Relative.class);
        if (relativeX) {
            relatives.add(DELTA_X);
        }
        if (relativeY) {
            relatives.add(DELTA_Y);
        }
        if (relativeZ) {
            relatives.add(DELTA_Z);
        }
        return relatives;
    }

    private Relative(int bit) {
        this.bit = bit;
    }

    private int getMask() {
        return 1 << this.bit;
    }

    private boolean isSet(int value) {
        return (value & this.getMask()) == this.getMask();
    }

    public static Set<Relative> unpack(int value) {
        EnumSet<Relative> result = EnumSet.noneOf(Relative.class);
        for (Relative argument : Relative.values()) {
            if (!argument.isSet(value)) continue;
            result.add(argument);
        }
        return result;
    }

    public static int pack(Set<Relative> set) {
        int result = 0;
        for (Relative argument : set) {
            result |= argument.getMask();
        }
        return result;
    }

    static {
        ALL = Set.of(Relative.values());
        ROTATION = Set.of(X_ROT, Y_ROT);
        DELTA = Set.of(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA);
        SET_STREAM_CODEC = ByteBufCodecs.INT.map(Relative::unpack, Relative::pack);
    }
}

