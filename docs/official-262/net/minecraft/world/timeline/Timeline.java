/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Multimap
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.timeline;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.timeline.AttributeTrack;
import net.minecraft.world.timeline.AttributeTrackSampler;

public class Timeline {
    public static final Codec<Holder<Timeline>> CODEC = RegistryFixedCodec.create(Registries.TIMELINE);
    private static final Codec<Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>>> TRACKS_CODEC = Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(AttributeTrack::createCodec));
    public static final Codec<Timeline> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)WorldClock.CODEC.fieldOf("clock").forGetter(t -> t.clock), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("period_ticks").forGetter(t -> t.periodTicks), (App)TRACKS_CODEC.optionalFieldOf("tracks", Map.of()).forGetter(t -> t.tracks), (App)Codec.unboundedMap(ClockTimeMarker.KEY_CODEC, TimeMarkerInfo.CODEC).optionalFieldOf("time_markers", Map.of()).forGetter(t -> t.timeMarkers)).apply((Applicative)i, Timeline::new)).validate(Timeline::validateInternal);
    public static final Codec<Timeline> NETWORK_CODEC = DIRECT_CODEC.xmap(Timeline::filterSyncableTracks, Timeline::filterSyncableTracks);
    private final Holder<WorldClock> clock;
    private final Optional<Integer> periodTicks;
    private final Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks;
    private final Map<ResourceKey<ClockTimeMarker>, TimeMarkerInfo> timeMarkers;

    private static Timeline filterSyncableTracks(Timeline timeline) {
        Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> syncableTracks = Map.copyOf(Maps.filterKeys(timeline.tracks, EnvironmentAttribute::isSyncable));
        return new Timeline(timeline.clock, timeline.periodTicks, syncableTracks, timeline.timeMarkers);
    }

    private Timeline(Holder<WorldClock> clock, Optional<Integer> periodTicks, Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks, Map<ResourceKey<ClockTimeMarker>, TimeMarkerInfo> timeMarkers) {
        this.clock = clock;
        this.periodTicks = periodTicks;
        this.tracks = tracks;
        this.timeMarkers = timeMarkers;
    }

    public static void validateRegistry(Registry<Timeline> timelines, Map<ResourceKey<?>, Exception> loadingErrors) {
        HashMultimap timeMarkersByClock = HashMultimap.create();
        timelines.listElements().forEach(arg_0 -> Timeline.lambda$validateRegistry$0((Multimap)timeMarkersByClock, loadingErrors, arg_0));
    }

    private static DataResult<Timeline> validateInternal(Timeline timeline) {
        if (timeline.periodTicks.isEmpty()) {
            return DataResult.success((Object)timeline);
        }
        int periodTicks = timeline.periodTicks.get();
        for (Map.Entry<ResourceKey<ClockTimeMarker>, TimeMarkerInfo> entry : timeline.timeMarkers.entrySet()) {
            int ticks = entry.getValue().ticks();
            if (ticks >= 0 && ticks < periodTicks) continue;
            return DataResult.error(() -> "Time Marker " + String.valueOf(entry.getKey()) + " must be in range [0; " + periodTicks + ")");
        }
        DataResult result = DataResult.success((Object)timeline);
        for (AttributeTrack<?, ?> track : timeline.tracks.values()) {
            result = result.apply2stable((t, $) -> t, AttributeTrack.validatePeriod(track, periodTicks));
        }
        return result;
    }

    public static Builder builder(Holder<WorldClock> clock) {
        return new Builder(clock);
    }

    public int getPeriodCount(ClockManager clockManager) {
        if (this.periodTicks.isEmpty()) {
            return 0;
        }
        long totalTicks = this.getTotalTicks(clockManager);
        return (int)(totalTicks / (long)this.periodTicks.get().intValue());
    }

    public long getCurrentTicks(ClockManager clockManager) {
        long totalTicks = this.getTotalTicks(clockManager);
        if (this.periodTicks.isEmpty()) {
            return totalTicks;
        }
        return totalTicks % (long)this.periodTicks.get().intValue();
    }

    public long getTotalTicks(ClockManager clockManager) {
        return clockManager.getTotalTicks(this.clock);
    }

    public Holder<WorldClock> clock() {
        return this.clock;
    }

    public Optional<Integer> periodTicks() {
        return this.periodTicks;
    }

    public void registerTimeMarkers(BiConsumer<ResourceKey<ClockTimeMarker>, ClockTimeMarker> output) {
        for (Map.Entry<ResourceKey<ClockTimeMarker>, TimeMarkerInfo> entry : this.timeMarkers.entrySet()) {
            TimeMarkerInfo info = entry.getValue();
            output.accept(entry.getKey(), new ClockTimeMarker(this.clock, info.ticks, this.periodTicks, info.showInCommands));
        }
    }

    public Set<EnvironmentAttribute<?>> attributes() {
        return this.tracks.keySet();
    }

    public <Value> AttributeTrackSampler<Value, ?> createTrackSampler(EnvironmentAttribute<Value> attribute, ClockManager clockManager) {
        AttributeTrack<?, ?> track = this.tracks.get(attribute);
        if (track == null) {
            throw new IllegalStateException("Timeline has no track for " + String.valueOf(attribute));
        }
        return track.bakeSampler(attribute, this.clock, this.periodTicks, clockManager);
    }

    private static /* synthetic */ void lambda$validateRegistry$0(Multimap timeMarkersByClock, Map loadingErrors, Holder.Reference timeline) {
        Holder<WorldClock> clock = ((Timeline)timeline.value()).clock();
        for (ResourceKey<ClockTimeMarker> timeMarker : ((Timeline)timeline.value()).timeMarkers.keySet()) {
            if (timeMarkersByClock.put(clock, timeMarker)) continue;
            loadingErrors.put(timeline.key(), new IllegalStateException(String.valueOf(timeMarker) + " was defined multiple times in " + clock.getRegisteredName()));
        }
    }

    private record TimeMarkerInfo(int ticks, boolean showInCommands) {
        private static final Codec<TimeMarkerInfo> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(TimeMarkerInfo::ticks), (App)Codec.BOOL.optionalFieldOf("show_in_commands", (Object)false).forGetter(TimeMarkerInfo::showInCommands)).apply((Applicative)i, TimeMarkerInfo::new));
        public static final Codec<TimeMarkerInfo> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, FULL_CODEC).xmap(either -> (TimeMarkerInfo)either.map(t -> new TimeMarkerInfo((int)t, false), t -> t), timeMarker -> timeMarker.showInCommands ? Either.right((Object)timeMarker) : Either.left((Object)timeMarker.ticks));
    }

    public static class Builder {
        private final Holder<WorldClock> clock;
        private Optional<Integer> periodTicks = Optional.empty();
        private final ImmutableMap.Builder<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceKey<ClockTimeMarker>, TimeMarkerInfo> timeMarkers = ImmutableMap.builder();

        private Builder(Holder<WorldClock> clock) {
            this.clock = clock;
        }

        public Builder setPeriodTicks(int periodTicks) {
            this.periodTicks = Optional.of(periodTicks);
            return this;
        }

        public <Value, Argument> Builder addModifierTrack(EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Argument> modifier, Consumer<KeyframeTrack.Builder<Argument>> builder) {
            attribute.type().checkAllowedModifier(modifier);
            KeyframeTrack.Builder argumentTrack = new KeyframeTrack.Builder();
            builder.accept(argumentTrack);
            this.tracks.put(attribute, new AttributeTrack<Value, Argument>(modifier, argumentTrack.build()));
            return this;
        }

        public <Value> Builder addTrack(EnvironmentAttribute<Value> attribute, Consumer<KeyframeTrack.Builder<Value>> builder) {
            return this.addModifierTrack(attribute, AttributeModifier.override(), builder);
        }

        public Builder addTimeMarker(ResourceKey<ClockTimeMarker> id, int ticks) {
            return this.addTimeMarker(id, ticks, false);
        }

        public Builder addTimeMarker(ResourceKey<ClockTimeMarker> id, int ticks, boolean showInCommands) {
            this.timeMarkers.put(id, (Object)new TimeMarkerInfo(ticks, showInCommands));
            return this;
        }

        public Timeline build() {
            return new Timeline(this.clock, this.periodTicks, (Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>>)this.tracks.build(), (Map<ResourceKey<ClockTimeMarker>, TimeMarkerInfo>)this.timeMarkers.build());
        }
    }
}

