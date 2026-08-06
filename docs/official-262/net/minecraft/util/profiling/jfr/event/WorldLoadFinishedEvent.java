/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(value="minecraft.LoadWorld")
@Label(value="Create/Load World")
@Category(value={"Minecraft", "World Generation"})
@StackTrace(value=false)
public class WorldLoadFinishedEvent
extends Event {
    public static final String EVENT_NAME = "minecraft.LoadWorld";
    public static final EventType TYPE = EventType.getEventType(WorldLoadFinishedEvent.class);
}

