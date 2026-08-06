/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.util.profiling.jfr.event.ChunkRegionIoEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Name(value="minecraft.ChunkRegionRead")
@Label(value="Region File Read")
public class ChunkRegionReadEvent
extends ChunkRegionIoEvent {
    public static final String EVENT_NAME = "minecraft.ChunkRegionRead";
    public static final EventType TYPE = EventType.getEventType(ChunkRegionReadEvent.class);

    public ChunkRegionReadEvent(RegionStorageInfo info, ChunkPos chunkPos, RegionFileVersion version, int bytes) {
        super(info, chunkPos, version, bytes);
    }
}

