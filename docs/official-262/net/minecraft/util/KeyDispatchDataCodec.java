/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.util;

import com.mojang.serialization.MapCodec;

public record KeyDispatchDataCodec<A>(MapCodec<A> codec) {
    public static <A> KeyDispatchDataCodec<A> of(MapCodec<A> codec) {
        return new KeyDispatchDataCodec<A>(codec);
    }
}

