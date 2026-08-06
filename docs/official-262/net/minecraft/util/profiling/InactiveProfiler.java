/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;

public class InactiveProfiler
implements ProfileCollector {
    public static final InactiveProfiler INSTANCE = new InactiveProfiler();

    private InactiveProfiler() {
    }

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
    }

    @Override
    public void push(String name) {
    }

    @Override
    public void push(Supplier<String> name) {
    }

    @Override
    public void markForCharting(MetricCategory category) {
    }

    @Override
    public void pop() {
    }

    @Override
    public void popPush(String name) {
    }

    @Override
    public void popPush(Supplier<String> name) {
    }

    @Override
    public Zone zone(String name) {
        return Zone.INACTIVE;
    }

    @Override
    public Zone zone(Supplier<String> name) {
        return Zone.INACTIVE;
    }

    @Override
    public void incrementCounter(String name, int amount) {
    }

    @Override
    public void incrementCounter(Supplier<String> name, int amount) {
    }

    @Override
    public ProfileResults getResults() {
        return EmptyProfileResults.EMPTY;
    }

    @Override
    public  @Nullable ActiveProfiler.PathEntry getEntry(String path) {
        return null;
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return ImmutableSet.of();
    }
}

