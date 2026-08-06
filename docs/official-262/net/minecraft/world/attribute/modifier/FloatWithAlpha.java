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
package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FloatWithAlpha(float value, float alpha) {
    private static final Codec<FloatWithAlpha> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.FLOAT.fieldOf("value").forGetter(FloatWithAlpha::value), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("alpha", (Object)Float.valueOf(1.0f)).forGetter(FloatWithAlpha::alpha)).apply((Applicative)i, FloatWithAlpha::new));
    public static final Codec<FloatWithAlpha> CODEC = Codec.either((Codec)Codec.FLOAT, FULL_CODEC).xmap(either -> (FloatWithAlpha)either.map(FloatWithAlpha::new, p -> p), parameter -> parameter.alpha() == 1.0f ? Either.left((Object)Float.valueOf(parameter.value())) : Either.right((Object)parameter));

    public FloatWithAlpha(float value) {
        this(value, 1.0f);
    }
}

