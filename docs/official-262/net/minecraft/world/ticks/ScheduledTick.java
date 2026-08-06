/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.Hash$Strategy
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

public record ScheduledTick<T>(T type, BlockPos pos, long triggerTick, TickPriority priority, long subTickOrder) {
    public static final Comparator<ScheduledTick<?>> DRAIN_ORDER = (o1, o2) -> {
        int compare = Long.compare(o1.triggerTick, o2.triggerTick);
        if (compare != 0) {
            return compare;
        }
        compare = o1.priority.compareTo(o2.priority);
        if (compare != 0) {
            return compare;
        }
        return Long.compare(o1.subTickOrder, o2.subTickOrder);
    };
    public static final Comparator<ScheduledTick<?>> INTRA_TICK_DRAIN_ORDER = (o1, o2) -> {
        int compare = o1.priority.compareTo(o2.priority);
        if (compare != 0) {
            return compare;
        }
        return Long.compare(o1.subTickOrder, o2.subTickOrder);
    };
    public static final Hash.Strategy<ScheduledTick<?>> UNIQUE_TICK_HASH = new Hash.Strategy<ScheduledTick<?>>(){

        public int hashCode(ScheduledTick<?> o) {
            return 31 * o.pos().hashCode() + o.type().hashCode();
        }

        public boolean equals(@Nullable ScheduledTick<?> a, @Nullable ScheduledTick<?> b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            return a.type() == b.type() && a.pos().equals(b.pos());
        }
    };

    public ScheduledTick(T type, BlockPos pos, long triggerTick, long subTickOrder) {
        this(type, pos, triggerTick, TickPriority.NORMAL, subTickOrder);
    }

    public ScheduledTick {
        pos = pos.immutable();
    }

    public static <T> ScheduledTick<T> probe(T type, BlockPos pos) {
        return new ScheduledTick<T>(type, pos, 0L, TickPriority.NORMAL, 0L);
    }

    public SavedTick<T> toSavedTick(long currentTick) {
        return new SavedTick<T>(this.type, this.pos, (int)(this.triggerTick - currentTick), this.priority);
    }
}

