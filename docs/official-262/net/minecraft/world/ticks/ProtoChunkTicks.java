/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
 */
package net.minecraft.world.ticks;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunkTicks<T>
implements TickContainerAccess<T>,
SerializableTickContainer<T> {
    private final List<SavedTick<T>> ticks = Lists.newArrayList();
    private final Set<SavedTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(SavedTick.UNIQUE_TICK_HASH);

    @Override
    public void schedule(ScheduledTick<T> tick) {
        SavedTick<T> newTick = new SavedTick<T>(tick.type(), tick.pos(), 0, tick.priority());
        this.schedule(newTick);
    }

    @Override
    private void schedule(SavedTick<T> newTick) {
        if (this.ticksPerPosition.add(newTick)) {
            this.ticks.add(newTick);
        }
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T type) {
        return this.ticksPerPosition.contains(SavedTick.probe(type, pos));
    }

    @Override
    public int count() {
        return this.ticks.size();
    }

    @Override
    public List<SavedTick<T>> pack(long currentTick) {
        return this.ticks;
    }

    public List<SavedTick<T>> scheduledTicks() {
        return List.copyOf(this.ticks);
    }

    public static <T> ProtoChunkTicks<T> load(List<SavedTick<T>> ticks) {
        ProtoChunkTicks<T> result = new ProtoChunkTicks<T>();
        ticks.forEach(result::schedule);
        return result;
    }
}

