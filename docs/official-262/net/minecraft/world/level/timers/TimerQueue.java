/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.HashBasedTable
 *  com.google.common.collect.Table
 *  com.google.common.primitives.UnsignedLong
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.timers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.timers.TimerCallback;
import net.minecraft.world.level.timers.TimerCallbacks;

public class TimerQueue<T>
extends SavedData {
    public static final Codec<TimerQueue<MinecraftServer>> CODEC = TimerQueue.codec(TimerCallbacks.SERVER_CALLBACKS);
    public static final SavedDataType<TimerQueue<MinecraftServer>> TYPE = new SavedDataType<TimerQueue<MinecraftServer>>(Identifier.withDefaultNamespace("scheduled_events"), TimerQueue::new, CODEC, DataFixTypes.SAVED_DATA_SCHEDULED_EVENTS);
    private final Queue<Event<T>> queue = new PriorityQueue<Event<T>>(TimerQueue.createComparator());
    private UnsignedLong sequentialId = UnsignedLong.ZERO;
    private final Table<String, Long, Event<T>> events = HashBasedTable.create();

    @VisibleForTesting
    protected static <T> Codec<TimerQueue<T>> codec(TimerCallbacks<T> callbacks) {
        return Packed.codec(callbacks.codec()).xmap(TimerQueue::new, TimerQueue::pack);
    }

    private static <T> Comparator<Event<T>> createComparator() {
        return Comparator.comparingLong(l -> l.triggerTime).thenComparing(l -> l.sequentialId);
    }

    public TimerQueue(Packed<T> packedEvents) {
        this();
        this.queue.clear();
        this.events.clear();
        this.sequentialId = UnsignedLong.ZERO;
        packedEvents.events.forEach(event -> this.schedule(event.id, event.triggerTime, event.callback));
    }

    public TimerQueue() {
    }

    public void tick(T context, long currentTick) {
        Event<T> event;
        while ((event = this.queue.peek()) != null && event.triggerTime <= currentTick) {
            this.queue.remove();
            this.events.remove((Object)event.id, (Object)currentTick);
            this.setDirty();
            event.callback.handle(context, this, currentTick);
        }
    }

    public void schedule(String id, long time, TimerCallback<T> callback) {
        if (this.events.contains((Object)id, (Object)time)) {
            return;
        }
        this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
        Event<T> newEvent = new Event<T>(time, this.sequentialId, id, callback);
        this.events.put((Object)id, (Object)time, newEvent);
        this.queue.add(newEvent);
        this.setDirty();
    }

    public int remove(String id) {
        Collection eventsToRemove = this.events.row((Object)id).values();
        eventsToRemove.forEach(this.queue::remove);
        int size = eventsToRemove.size();
        eventsToRemove.clear();
        this.setDirty();
        return size;
    }

    public Set<String> getEventsIds() {
        return Collections.unmodifiableSet(this.events.rowKeySet());
    }

    @VisibleForTesting
    protected Packed<T> pack() {
        return new Packed(this.queue.stream().sorted(TimerQueue.createComparator()).map(event -> new Event.Packed(event.triggerTime, event.id, event.callback)).toList());
    }

    public record Packed<T>(List<Event.Packed<T>> events) {
        public static <T> Codec<Packed<T>> codec(Codec<TimerCallback<T>> callbackCodec) {
            return RecordCodecBuilder.create(i -> i.group((App)Event.Packed.codec(callbackCodec).listOf().fieldOf("events").forGetter(Packed::events)).apply((Applicative)i, Packed::new));
        }
    }

    public record Event<T>(long triggerTime, UnsignedLong sequentialId, String id, TimerCallback<T> callback) {

        public record Packed<T>(long triggerTime, String id, TimerCallback<T> callback) {
            public static <T> Codec<Packed<T>> codec(Codec<TimerCallback<T>> callbackCodec) {
                return RecordCodecBuilder.create(i -> i.group((App)Codec.LONG.fieldOf("trigger_time").forGetter(Packed::triggerTime), (App)Codec.STRING.fieldOf("id").forGetter(Packed::id), (App)callbackCodec.fieldOf("callback").forGetter(Packed::callback)).apply((Applicative)i, Packed::new));
            }
        }
    }
}

