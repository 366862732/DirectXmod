/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.TracyClient
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling;

import com.mojang.jtracy.TracyClient;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.TracyZoneFiller;
import org.jspecify.annotations.Nullable;

public final class Profiler {
    private static final ThreadLocal<TracyZoneFiller> TRACY_FILLER = ThreadLocal.withInitial(TracyZoneFiller::new);
    private static final ThreadLocal<@Nullable ProfilerFiller> ACTIVE = new ThreadLocal();
    private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger();

    private Profiler() {
    }

    public static Scope use(ProfilerFiller filler) {
        Profiler.startUsing(filler);
        return Profiler::stopUsing;
    }

    private static void startUsing(ProfilerFiller filler) {
        if (ACTIVE.get() != null) {
            throw new IllegalStateException("Profiler is already active");
        }
        ProfilerFiller active = Profiler.decorateFiller(filler);
        ACTIVE.set(active);
        ACTIVE_COUNT.incrementAndGet();
        active.startTick();
    }

    private static void stopUsing() {
        ProfilerFiller active = ACTIVE.get();
        if (active == null) {
            throw new IllegalStateException("Profiler was not active");
        }
        ACTIVE.remove();
        ACTIVE_COUNT.decrementAndGet();
        active.endTick();
    }

    private static ProfilerFiller decorateFiller(ProfilerFiller filler) {
        return ProfilerFiller.combine(Profiler.getDefaultFiller(), filler);
    }

    public static ProfilerFiller get() {
        if (ACTIVE_COUNT.get() == 0) {
            return Profiler.getDefaultFiller();
        }
        return Objects.requireNonNullElseGet(ACTIVE.get(), Profiler::getDefaultFiller);
    }

    private static ProfilerFiller getDefaultFiller() {
        if (TracyClient.isAvailable()) {
            return TRACY_FILLER.get();
        }
        return InactiveProfiler.INSTANCE;
    }

    public static interface Scope
    extends AutoCloseable {
        @Override
        public void close();
    }
}

