/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 */
package net.minecraft.server;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.util.Queue;
import net.minecraft.util.ArrayListDeque;

public class SuppressedExceptionCollector {
    private static final int LATEST_ENTRY_COUNT = 8;
    private final Queue<LongEntry> latestEntries = new ArrayListDeque<LongEntry>();
    private final Object2IntLinkedOpenHashMap<ShortEntry> entryCounts = new Object2IntLinkedOpenHashMap();

    private static long currentTimeMs() {
        return System.currentTimeMillis();
    }

    public synchronized void addEntry(String location, Throwable throwable) {
        long now = SuppressedExceptionCollector.currentTimeMs();
        String message = throwable.getMessage();
        this.latestEntries.add(new LongEntry(now, location, throwable.getClass(), message));
        while (this.latestEntries.size() > 8) {
            this.latestEntries.remove();
        }
        ShortEntry key = new ShortEntry(location, throwable.getClass());
        int currentValue = this.entryCounts.getInt((Object)key);
        this.entryCounts.putAndMoveToFirst((Object)key, currentValue + 1);
    }

    public synchronized String dump() {
        long current = SuppressedExceptionCollector.currentTimeMs();
        StringBuilder result = new StringBuilder();
        if (!this.latestEntries.isEmpty()) {
            result.append("\n\t\tLatest entries:\n");
            for (LongEntry e : this.latestEntries) {
                result.append("\t\t\t").append(e.location).append(":").append(e.cls).append(": ").append(e.message).append(" (").append(current - e.timestampMs).append("ms ago)").append("\n");
            }
        }
        if (!this.entryCounts.isEmpty()) {
            if (result.isEmpty()) {
                result.append("\n");
            }
            result.append("\t\tEntry counts:\n");
            for (LongEntry e : Object2IntMaps.fastIterable(this.entryCounts)) {
                result.append("\t\t\t").append(((ShortEntry)e.getKey()).location).append(":").append(((ShortEntry)e.getKey()).cls).append(" x ").append(e.getIntValue()).append("\n");
            }
        }
        if (result.isEmpty()) {
            return "~~NONE~~";
        }
        return result.toString();
    }

    private record LongEntry(long timestampMs, String location, Class<? extends Throwable> cls, String message) {
    }

    private record ShortEntry(String location, Class<? extends Throwable> cls) {
    }
}

