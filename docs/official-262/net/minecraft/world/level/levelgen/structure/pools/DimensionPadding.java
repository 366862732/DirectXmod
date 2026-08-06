/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;

public record DimensionPadding(int bottom, int top) {
    private static final Codec<DimensionPadding> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("bottom", (Object)0).forGetter(r -> r.bottom), (App)ExtraCodecs.NON_NEGATIVE_INT.lenientOptionalFieldOf("top", (Object)0).forGetter(r -> r.top)).apply((Applicative)i, DimensionPadding::new));
    public static final Codec<DimensionPadding> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, RECORD_CODEC).xmap(e -> (DimensionPadding)e.map(DimensionPadding::new, Function.identity()), padding -> padding.hasEqualTopAndBottom() ? Either.left((Object)padding.bottom) : Either.right((Object)padding));
    public static final DimensionPadding ZERO = new DimensionPadding(0);

    public DimensionPadding(int value) {
        this(value, value);
    }

    public boolean hasEqualTopAndBottom() {
        return this.top == this.bottom;
    }
}

