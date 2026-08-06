/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Comparators
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util;

import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.EasingType;
import net.minecraft.util.Keyframe;
import net.minecraft.util.KeyframeTrackSampler;
import net.minecraft.world.attribute.LerpFunction;

public record KeyframeTrack<T>(List<Keyframe<T>> keyframes, EasingType easingType) {
    public KeyframeTrack {
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("Track has no keyframes");
        }
    }

    public static <T> MapCodec<KeyframeTrack<T>> mapCodec(Codec<T> valueCodec) {
        Codec keyframesCodec = Keyframe.codec(valueCodec).listOf().validate(KeyframeTrack::validateKeyframes);
        return RecordCodecBuilder.mapCodec((T i) -> i.group((App)keyframesCodec.fieldOf("keyframes").forGetter(KeyframeTrack::keyframes), (App)EasingType.CODEC.optionalFieldOf("ease", (Object)EasingType.LINEAR).forGetter(KeyframeTrack::easingType)).apply((Applicative)i, KeyframeTrack::new));
    }

    private static <T> DataResult<List<Keyframe<T>>> validateKeyframes(List<Keyframe<T>> keyframes) {
        if (keyframes.isEmpty()) {
            return DataResult.error(() -> "Keyframes must not be empty");
        }
        if (!Comparators.isInOrder(keyframes, Comparator.comparingInt(Keyframe::ticks))) {
            return DataResult.error(() -> "Keyframes must be ordered by ticks field");
        }
        if (keyframes.size() > 1) {
            int repeatCount = 0;
            int lastTicks = keyframes.getLast().ticks();
            for (Keyframe keyframe : keyframes) {
                if (keyframe.ticks() == lastTicks) {
                    if (++repeatCount > 2) {
                        return DataResult.error(() -> "More than 2 keyframes on same tick: " + keyframe.ticks());
                    }
                } else {
                    repeatCount = 0;
                }
                lastTicks = keyframe.ticks();
            }
        }
        return DataResult.success(keyframes);
    }

    public static DataResult<KeyframeTrack<?>> validatePeriod(KeyframeTrack<?> track, int periodTicks) {
        for (Keyframe<?> keyframe : track.keyframes()) {
            int tick = keyframe.ticks();
            if (tick >= 0 && tick <= periodTicks) continue;
            return DataResult.error(() -> "Keyframe at tick " + keyframe.ticks() + " must be in range [0; " + periodTicks + "]");
        }
        return DataResult.success(track);
    }

    public KeyframeTrackSampler<T> bakeSampler(Optional<Integer> periodTicks, LerpFunction<T> lerp) {
        return new KeyframeTrackSampler<T>(this, periodTicks, lerp);
    }

    public static class Builder<T> {
        private final ImmutableList.Builder<Keyframe<T>> keyframes = ImmutableList.builder();
        private EasingType easing = EasingType.LINEAR;

        public Builder<T> addKeyframe(int ticks, T value) {
            this.keyframes.add(new Keyframe<T>(ticks, value));
            return this;
        }

        public Builder<T> setEasing(EasingType easing) {
            this.easing = easing;
            return this;
        }

        public KeyframeTrack<T> build() {
            List keyframes = (List)KeyframeTrack.validateKeyframes(this.keyframes.build()).getOrThrow();
            return new KeyframeTrack(keyframes, this.easing);
        }
    }
}

