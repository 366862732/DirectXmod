/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.equine;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;

public enum Markings {
    NONE(0),
    WHITE(1),
    WHITE_FIELD(2),
    WHITE_DOTS(3),
    BLACK_DOTS(4);

    private static final IntFunction<Markings> BY_ID;
    private final int id;

    private Markings(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static Markings byId(int id) {
        return BY_ID.apply(id);
    }

    static {
        BY_ID = ByIdMap.continuous(Markings::getId, Markings.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    }
}

