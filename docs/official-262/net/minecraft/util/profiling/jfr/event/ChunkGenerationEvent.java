/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Name(value="minecraft.ChunkGeneration")
@Label(value="Chunk Generation")
@Category(value={"Minecraft", "World Generation"})
@StackTrace(value=false)
@Enabled(value=false)
public class ChunkGenerationEvent
extends Event {
    public static final String EVENT_NAME = "minecraft.ChunkGeneration";
    public static final EventType TYPE = EventType.getEventType(ChunkGenerationEvent.class);
    @Name(value="worldPosX")
    @Label(value="First Block X World Position")
    public final int worldPosX;
    @Name(value="worldPosZ")
    @Label(value="First Block Z World Position")
    public final int worldPosZ;
    @Name(value="chunkPosX")
    @Label(value="Chunk X Position")
    public final int chunkPosX;
    @Name(value="chunkPosZ")
    @Label(value="Chunk Z Position")
    public final int chunkPosZ;
    @Name(value="status")
    @Label(value="Status")
    public final String targetStatus;
    @Name(value="level")
    @Label(value="Level")
    public final String level;

    public ChunkGenerationEvent(ChunkPos pos, ResourceKey<Level> dimension, String name) {
        this.targetStatus = name;
        this.level = dimension.identifier().toString();
        this.chunkPosX = pos.x();
        this.chunkPosZ = pos.z();
        this.worldPosX = pos.getMinBlockX();
        this.worldPosZ = pos.getMinBlockZ();
    }

    public static class Fields {
        public static final String WORLD_POS_X = "worldPosX";
        public static final String WORLD_POS_Z = "worldPosZ";
        public static final String CHUNK_POS_X = "chunkPosX";
        public static final String CHUNK_POS_Z = "chunkPosZ";
        public static final String STATUS = "status";
        public static final String LEVEL = "level";

        private Fields() {
        }
    }
}

