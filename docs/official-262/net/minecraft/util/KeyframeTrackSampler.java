/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.EasingType;
import net.minecraft.util.Keyframe;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.world.attribute.LerpFunction;

public class KeyframeTrackSampler<T> {
    private final Optional<Integer> periodTicks;
    private final LerpFunction<T> lerp;
    private final List<Segment<T>> segments;

    KeyframeTrackSampler(KeyframeTrack<T> track, Optional<Integer> periodTicks, LerpFunction<T> lerp) {
        this.periodTicks = periodTicks;
        this.lerp = lerp;
        this.segments = KeyframeTrackSampler.bakeSegments(track, periodTicks);
    }

    private static <T> List<Segment<T>> bakeSegments(KeyframeTrack<T> track, Optional<Integer> periodTicks) {
        List<Keyframe<T>> keyframes = track.keyframes();
        if (keyframes.size() == 1) {
            T value = keyframes.getFirst().value();
            return List.of(new Segment<T>(EasingType.CONSTANT, value, 0, value, 0));
        }
        ArrayList<Segment<T>> segments = new ArrayList<Segment<T>>();
        if (periodTicks.isPresent()) {
            Keyframe<T> firstKeyframe = keyframes.getFirst();
            Keyframe<T> lastKeyframe = keyframes.getLast();
            segments.add(new Segment<T>(track, lastKeyframe, lastKeyframe.ticks() - periodTicks.get(), firstKeyframe, firstKeyframe.ticks()));
            KeyframeTrackSampler.addSegmentsFromKeyframes(track, keyframes, segments);
            segments.add(new Segment<T>(track, lastKeyframe, lastKeyframe.ticks(), firstKeyframe, firstKeyframe.ticks() + periodTicks.get()));
        } else {
            KeyframeTrackSampler.addSegmentsFromKeyframes(track, keyframes, segments);
        }
        return List.copyOf(segments);
    }

    private static <T> void addSegmentsFromKeyframes(KeyframeTrack<T> track, List<Keyframe<T>> keyframes, List<Segment<T>> output) {
        for (int i = 0; i < keyframes.size() - 1; ++i) {
            Keyframe<T> keyframe = keyframes.get(i);
            Keyframe<T> nextKeyframe = keyframes.get(i + 1);
            output.add(new Segment<T>(track, keyframe, keyframe.ticks(), nextKeyframe, nextKeyframe.ticks()));
        }
    }

    public T sample(long ticks) {
        long sampleTicks = this.loopTicks(ticks);
        Segment<T> segment = this.getSegmentAt(sampleTicks);
        if (sampleTicks <= (long)segment.fromTicks) {
            return segment.fromValue;
        }
        if (sampleTicks >= (long)segment.toTicks) {
            return segment.toValue;
        }
        float alpha = (float)(sampleTicks - (long)segment.fromTicks) / (float)(segment.toTicks - segment.fromTicks);
        float easedAlpha = segment.easing.apply(alpha);
        return this.lerp.apply(easedAlpha, segment.fromValue, segment.toValue);
    }

    private Segment<T> getSegmentAt(long currentTicks) {
        for (Segment<T> segment : this.segments) {
            if (currentTicks >= (long)segment.toTicks) continue;
            return segment;
        }
        return this.segments.getLast();
    }

    private long loopTicks(long ticks) {
        if (this.periodTicks.isPresent()) {
            return Math.floorMod(ticks, (int)this.periodTicks.get());
        }
        return ticks;
    }

    private record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {
        public Segment(KeyframeTrack<T> track, Keyframe<T> from, int fromTicks, Keyframe<T> to, int toTicks) {
            this(track.easingType(), from.value(), fromTicks, to.value(), toTicks);
        }
    }
}

