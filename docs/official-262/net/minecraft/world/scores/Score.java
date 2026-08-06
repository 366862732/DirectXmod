/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import org.jspecify.annotations.Nullable;

public class Score
implements ReadOnlyScoreInfo {
    private int value;
    private boolean locked = true;
    private @Nullable Component display;
    private @Nullable NumberFormat numberFormat;

    public Score() {
    }

    public Score(Packed packed) {
        this.value = packed.value;
        this.locked = packed.locked;
        this.display = packed.display.orElse(null);
        this.numberFormat = packed.numberFormat.orElse(null);
    }

    public Packed pack() {
        return new Packed(this.value, this.locked, Optional.ofNullable(this.display), Optional.ofNullable(this.numberFormat));
    }

    @Override
    public int value() {
        return this.value;
    }

    public void value(int score) {
        this.value = score;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public @Nullable Component display() {
        return this.display;
    }

    public void display(@Nullable Component display) {
        this.display = display;
    }

    @Override
    public @Nullable NumberFormat numberFormat() {
        return this.numberFormat;
    }

    public void numberFormat(@Nullable NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    public record Packed(int value, boolean locked, Optional<Component> display, Optional<NumberFormat> numberFormat) {
        public static final MapCodec<Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.optionalFieldOf("Score", (Object)0).forGetter(Packed::value), (App)Codec.BOOL.optionalFieldOf("Locked", (Object)false).forGetter(Packed::locked), (App)ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(Packed::display), (App)NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Packed::numberFormat)).apply((Applicative)i, Packed::new));
    }
}

