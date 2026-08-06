/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 */
package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.util.StringRepresentable;

public enum TriState implements StringRepresentable
{
    TRUE("true"),
    FALSE("false"),
    DEFAULT("default");

    public static final Codec<TriState> CODEC;
    private final String name;

    private TriState(String name) {
        this.name = name;
    }

    public static TriState from(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean toBoolean(boolean defaultValue) {
        return switch (this.ordinal()) {
            case 0 -> true;
            case 1 -> false;
            default -> defaultValue;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        CODEC = Codec.either((Codec)Codec.BOOL, StringRepresentable.fromEnum(TriState::values)).xmap(either -> (TriState)either.map(TriState::from, Function.identity()), triState -> switch (triState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 2 -> Either.right((Object)triState);
            case 0 -> Either.left((Object)true);
            case 1 -> Either.left((Object)false);
        });
    }
}

