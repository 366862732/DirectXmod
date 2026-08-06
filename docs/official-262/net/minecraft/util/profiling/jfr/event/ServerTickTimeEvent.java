/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

@Name(value="minecraft.ServerTickTime")
@Label(value="Server Tick Time")
@Category(value={"Minecraft", "Ticking"})
@StackTrace(value=false)
@Period(value="1 s")
public class ServerTickTimeEvent
extends Event {
    public static final String EVENT_NAME = "minecraft.ServerTickTime";
    public static final EventType TYPE = EventType.getEventType(ServerTickTimeEvent.class);
    @Name(value="averageTickDuration")
    @Label(value="Average Server Tick Duration")
    @Timespan
    public final long averageTickDurationNanos;

    public ServerTickTimeEvent(float averageTickTimeMs) {
        this.averageTickDurationNanos = (long)(1000000.0f * averageTickTimeMs);
    }

    public static class Fields {
        public static final String AVERAGE_TICK_DURATION = "averageTickDuration";

        private Fields() {
        }
    }
}

