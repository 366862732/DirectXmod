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
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

@Name(value="minecraft.StructureGeneration")
@Label(value="Structure Generation")
@Category(value={"Minecraft", "World Generation"})
@StackTrace(value=false)
@Enabled(value=false)
public class StructureGenerationEvent
extends Event {
    public static final String EVENT_NAME = "minecraft.StructureGeneration";
    public static final EventType TYPE = EventType.getEventType(StructureGenerationEvent.class);
    @Name(value="chunkPosX")
    @Label(value="Chunk X Position")
    public final int chunkPosX;
    @Name(value="chunkPosZ")
    @Label(value="Chunk Z Position")
    public final int chunkPosZ;
    @Name(value="structure")
    @Label(value="Structure")
    public final String structure;
    @Name(value="level")
    @Label(value="Level")
    public final String level;
    @Name(value="success")
    @Label(value="Success")
    public boolean success;

    public StructureGenerationEvent(ChunkPos sourceChunkPos, Holder<Structure> structure, ResourceKey<Level> level) {
        this.chunkPosX = sourceChunkPos.x();
        this.chunkPosZ = sourceChunkPos.z();
        this.structure = structure.getRegisteredName();
        this.level = level.identifier().toString();
    }

    public static interface Fields {
        public static final String CHUNK_POS_X = "chunkPosX";
        public static final String CHUNK_POS_Z = "chunkPosZ";
        public static final String STRUCTURE = "structure";
        public static final String LEVEL = "level";
        public static final String SUCCESS = "success";
    }
}

