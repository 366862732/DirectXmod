/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class Stopwatches
extends SavedData {
    private static final Codec<Stopwatches> CODEC = Codec.unboundedMap(Identifier.CODEC, (Codec)Codec.LONG).fieldOf("stopwatches").codec().xmap(Stopwatches::unpack, Stopwatches::pack);
    public static final SavedDataType<Stopwatches> TYPE = new SavedDataType<Stopwatches>(Identifier.withDefaultNamespace("stopwatches"), Stopwatches::new, CODEC, DataFixTypes.SAVED_DATA_STOPWATCHES);
    private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap();

    private Stopwatches() {
    }

    private static Stopwatches unpack(Map<Identifier, Long> stopwatches) {
        Stopwatches result = new Stopwatches();
        long currentTime = Stopwatches.currentTime();
        stopwatches.forEach((id, accumulatedElapsedTime) -> result.stopwatches.put((Identifier)id, new Stopwatch(currentTime, (long)accumulatedElapsedTime)));
        return result;
    }

    private Map<Identifier, Long> pack() {
        long currentTime = Stopwatches.currentTime();
        TreeMap<Identifier, Long> result = new TreeMap<Identifier, Long>();
        this.stopwatches.forEach((id, stopwatch) -> result.put((Identifier)id, stopwatch.elapsedMilliseconds(currentTime)));
        return result;
    }

    public @Nullable Stopwatch get(Identifier id) {
        return this.stopwatches.get(id);
    }

    public boolean add(Identifier id, Stopwatch stopwatch) {
        if (this.stopwatches.putIfAbsent(id, stopwatch) == null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public boolean update(Identifier id, UnaryOperator<Stopwatch> update) {
        if (this.stopwatches.computeIfPresent(id, (key, value) -> (Stopwatch)update.apply((Stopwatch)value)) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public boolean remove(Identifier id) {
        boolean removed;
        boolean bl = removed = this.stopwatches.remove(id) != null;
        if (removed) {
            this.setDirty();
        }
        return removed;
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || !this.stopwatches.isEmpty();
    }

    public List<Identifier> ids() {
        return List.copyOf(this.stopwatches.keySet());
    }

    public static long currentTime() {
        return Util.getMillis();
    }
}

