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

@Name(value="minecraft.ClientFps")
@Label(value="Client fps")
@Category(value={"Minecraft", "Ticking"})
@StackTrace(value=false)
@Period(value="1 s")
public class ClientFpsEvent
extends Event {
    public static final String EVENT_NAME = "minecraft.ClientFps";
    public static final EventType TYPE = EventType.getEventType(ClientFpsEvent.class);
    @Name(value="fps")
    @Label(value="Client fps")
    public final int fps;

    public ClientFpsEvent(int fps) {
        this.fps = fps;
    }

    public static class Fields {
        public static final String FPS = "fps";

        private Fields() {
        }
    }
}

