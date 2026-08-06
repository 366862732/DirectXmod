/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.util.profiling.jfr.stats.TimedStat;
import net.minecraft.world.level.ChunkPos;

public record StructureGenStat(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements TimedStat
{
    public static StructureGenStat from(RecordedEvent event) {
        return new StructureGenStat(event.getDuration(), new ChunkPos(event.getInt("chunkPosX"), event.getInt("chunkPosX")), event.getString("structure"), event.getString("level"), event.getBoolean("success"));
    }
}

