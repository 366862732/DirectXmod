/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum RandomSpreadType implements StringRepresentable
{
    LINEAR("linear"),
    TRIANGULAR("triangular");

    public static final Codec<RandomSpreadType> CODEC;
    private final String id;

    private RandomSpreadType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public int evaluate(RandomSource random, int limit) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> random.nextInt(limit);
            case 1 -> (random.nextInt(limit) + random.nextInt(limit)) / 2;
        };
    }

    static {
        CODEC = StringRepresentable.fromEnum(RandomSpreadType::values);
    }
}

