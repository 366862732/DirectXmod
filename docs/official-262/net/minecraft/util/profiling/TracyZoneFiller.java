/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.Plot
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.jtracy.Zone
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling;

import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.slf4j.Logger;

public class TracyZoneFiller
implements ProfilerFiller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 5);
    private final List<Zone> activeZones = new ArrayList<Zone>();
    private final Map<String, PlotAndValue> plots = new HashMap<String, PlotAndValue>();
    private final String name = Thread.currentThread().getName();

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
        for (PlotAndValue plotAndValue : this.plots.values()) {
            plotAndValue.set(0);
        }
    }

    @Override
    public void push(String name) {
        Optional result;
        String function = "";
        String file = "";
        int line = 0;
        if (SharedConstants.IS_RUNNING_IN_IDE && (result = STACK_WALKER.walk(s -> s.filter(frame -> frame.getDeclaringClass() != TracyZoneFiller.class && frame.getDeclaringClass() != ProfilerFiller.CombinedProfileFiller.class).findFirst())).isPresent()) {
            StackWalker.StackFrame frame = (StackWalker.StackFrame)result.get();
            function = frame.getMethodName();
            file = frame.getFileName();
            line = frame.getLineNumber();
        }
        Zone zone = TracyClient.beginZone((String)name, (String)function, (String)file, (int)line);
        this.activeZones.add(zone);
    }

    @Override
    public void push(Supplier<String> name) {
        this.push(name.get());
    }

    @Override
    public void pop() {
        if (this.activeZones.isEmpty()) {
            LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
            return;
        }
        Zone zone = this.activeZones.removeLast();
        zone.close();
    }

    @Override
    public void popPush(String name) {
        this.pop();
        this.push(name);
    }

    @Override
    public void popPush(Supplier<String> name) {
        this.pop();
        this.push(name.get());
    }

    @Override
    public void markForCharting(MetricCategory category) {
    }

    @Override
    public void incrementCounter(String name, int amount) {
        this.plots.computeIfAbsent(name, s -> new PlotAndValue(this.name + " " + name)).add(amount);
    }

    @Override
    public void incrementCounter(Supplier<String> name, int amount) {
        this.incrementCounter(name.get(), amount);
    }

    private Zone activeZone() {
        return this.activeZones.getLast();
    }

    @Override
    public void addZoneText(String text) {
        this.activeZone().addText(text);
    }

    @Override
    public void addZoneValue(long value) {
        this.activeZone().addValue(value);
    }

    @Override
    public void setZoneColor(int color) {
        this.activeZone().setColor(color);
    }

    private static final class PlotAndValue {
        private final Plot plot;
        private int value;

        private PlotAndValue(String name) {
            this.plot = TracyClient.createPlot((String)name);
            this.value = 0;
        }

        public void set(int value) {
            this.value = value;
            this.plot.setValue((double)value);
        }

        public void add(int amount) {
            this.set(this.value + amount);
        }
    }
}

