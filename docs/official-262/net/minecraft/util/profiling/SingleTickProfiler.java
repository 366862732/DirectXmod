/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.LongSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SingleTickProfiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LongSupplier realTime;
    private final long saveThreshold;
    private int tick;
    private final File location;
    private ProfileCollector profiler = InactiveProfiler.INSTANCE;

    public SingleTickProfiler(LongSupplier realTime, String location, long saveThresholdNs) {
        this.realTime = realTime;
        this.location = new File("debug", location);
        this.saveThreshold = saveThresholdNs;
    }

    public ProfilerFiller startTick() {
        this.profiler = new ActiveProfiler(this.realTime, () -> this.tick, () -> true);
        ++this.tick;
        return this.profiler;
    }

    public void endTick() {
        if (this.profiler == InactiveProfiler.INSTANCE) {
            return;
        }
        ProfileResults results = this.profiler.getResults();
        this.profiler = InactiveProfiler.INSTANCE;
        if (results.getNanoDuration() >= this.saveThreshold) {
            File file = new File(this.location, "tick-results-" + Util.getFilenameFormattedDateTime() + ".txt");
            results.saveResults(file.toPath());
            LOGGER.info("Recorded long tick -- wrote info to: {}", (Object)file.getAbsolutePath());
        }
    }

    public static @Nullable SingleTickProfiler createTickProfiler(String name) {
        if (SharedConstants.DEBUG_MONITOR_TICK_TIMES) {
            return new SingleTickProfiler(Util.timeSource(), name, SharedConstants.MAXIMUM_TICK_TIME_NANOS);
        }
        return null;
    }

    public static ProfilerFiller decorateFiller(ProfilerFiller filler, @Nullable SingleTickProfiler tickProfiler) {
        if (tickProfiler != null) {
            return ProfilerFiller.combine(tickProfiler.startTick(), filler);
        }
        return filler;
    }
}

