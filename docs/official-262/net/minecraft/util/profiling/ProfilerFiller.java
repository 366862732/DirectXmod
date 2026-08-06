/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
    public static final String ROOT = "root";

    public void startTick();

    public void endTick();

    public void push(String var1);

    public void push(Supplier<String> var1);

    public void pop();

    public void popPush(String var1);

    public void popPush(Supplier<String> var1);

    default public void addZoneText(String text) {
    }

    default public void addZoneValue(long value) {
    }

    default public void setZoneColor(int color) {
    }

    default public Zone zone(String name) {
        this.push(name);
        return new Zone(this);
    }

    default public Zone zone(Supplier<String> name) {
        this.push(name);
        return new Zone(this);
    }

    public void markForCharting(MetricCategory var1);

    default public void incrementCounter(String name) {
        this.incrementCounter(name, 1);
    }

    public void incrementCounter(String var1, int var2);

    default public void incrementCounter(Supplier<String> name) {
        this.incrementCounter(name, 1);
    }

    public void incrementCounter(Supplier<String> var1, int var2);

    public static ProfilerFiller combine(ProfilerFiller first, ProfilerFiller second) {
        if (first == InactiveProfiler.INSTANCE) {
            return second;
        }
        if (second == InactiveProfiler.INSTANCE) {
            return first;
        }
        return new CombinedProfileFiller(first, second);
    }

    public static class CombinedProfileFiller
    implements ProfilerFiller {
        private final ProfilerFiller first;
        private final ProfilerFiller second;

        public CombinedProfileFiller(ProfilerFiller first, ProfilerFiller second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void startTick() {
            this.first.startTick();
            this.second.startTick();
        }

        @Override
        public void endTick() {
            this.first.endTick();
            this.second.endTick();
        }

        @Override
        public void push(String name) {
            this.first.push(name);
            this.second.push(name);
        }

        @Override
        public void push(Supplier<String> name) {
            this.first.push(name);
            this.second.push(name);
        }

        @Override
        public void markForCharting(MetricCategory category) {
            this.first.markForCharting(category);
            this.second.markForCharting(category);
        }

        @Override
        public void pop() {
            this.first.pop();
            this.second.pop();
        }

        @Override
        public void popPush(String name) {
            this.first.popPush(name);
            this.second.popPush(name);
        }

        @Override
        public void popPush(Supplier<String> name) {
            this.first.popPush(name);
            this.second.popPush(name);
        }

        @Override
        public void incrementCounter(String name, int amount) {
            this.first.incrementCounter(name, amount);
            this.second.incrementCounter(name, amount);
        }

        @Override
        public void incrementCounter(Supplier<String> name, int amount) {
            this.first.incrementCounter(name, amount);
            this.second.incrementCounter(name, amount);
        }

        @Override
        public void addZoneText(String text) {
            this.first.addZoneText(text);
            this.second.addZoneText(text);
        }

        @Override
        public void addZoneValue(long value) {
            this.first.addZoneValue(value);
            this.second.addZoneValue(value);
        }

        @Override
        public void setZoneColor(int color) {
            this.first.setZoneColor(color);
            this.second.setZoneColor(color);
        }
    }
}

