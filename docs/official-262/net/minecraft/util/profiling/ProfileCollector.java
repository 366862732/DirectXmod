/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling;

import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;

public interface ProfileCollector
extends ProfilerFiller {
    public ProfileResults getResults();

    public @Nullable ActiveProfiler.PathEntry getEntry(String var1);

    public Set<Pair<String, MetricCategory>> getChartedPaths();
}

