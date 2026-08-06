/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.ai.memory;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record ExpirableValue<T>(T value, Optional<Long> timeToLive) {
    public static <T> ExpirableValue<T> of(T value) {
        return new ExpirableValue<T>(value, Optional.empty());
    }

    public static <T> ExpirableValue<T> of(T value, long ticksUntilExpiry) {
        return new ExpirableValue<T>(value, Optional.of(ticksUntilExpiry));
    }

    @Override
    public String toString() {
        return String.valueOf(this.value) + (String)(this.timeToLive.isPresent() ? " (ttl: " + String.valueOf(this.timeToLive.get()) + ")" : "");
    }

    public static <T> Codec<ExpirableValue<T>> codec(Codec<T> valueCodec) {
        return RecordCodecBuilder.create(i -> i.group((App)valueCodec.fieldOf("value").forGetter(ExpirableValue::value), (App)Codec.LONG.lenientOptionalFieldOf("ttl").forGetter(ExpirableValue::timeToLive)).apply((Applicative)i, ExpirableValue::new));
    }
}

