/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Category(value={"Minecraft", "Storage"})
@StackTrace(value=false)
@Enabled(value=false)
public abstract class ChunkRegionIoEvent
extends Event {
    @Name(value="regionPosX")
    @Label(value="Region X Position")
    public final int regionPosX;
    @Name(value="regionPosZ")
    @Label(value="Region Z Position")
    public final int regionPosZ;
    @Name(value="localPosX")
    @Label(value="Local X Position")
    public final int localChunkPosX;
    @Name(value="localPosZ")
    @Label(value="Local Z Position")
    public final int localChunkPosZ;
    @Name(value="chunkPosX")
    @Label(value="Chunk X Position")
    public final int chunkPosX;
    @Name(value="chunkPosZ")
    @Label(value="Chunk Z Position")
    public final int chunkPosZ;
    @Name(value="level")
    @Label(value="Level Id")
    public final String levelId;
    @Name(value="dimension")
    @Label(value="Dimension")
    public final String dimension;
    @Name(value="type")
    @Label(value="Type")
    public final String type;
    @Name(value="compression")
    @Label(value="Compression")
    public final String compression;
    @Name(value="bytes")
    @Label(value="Bytes")
    public final int bytes;

    protected ChunkRegionIoEvent(RegionStorageInfo info, ChunkPos chunkPos, RegionFileVersion version, int bytes) {
        this.regionPosX = chunkPos.getRegionX();
        this.regionPosZ = chunkPos.getRegionZ();
        this.localChunkPosX = chunkPos.getRegionLocalX();
        this.localChunkPosZ = chunkPos.getRegionLocalZ();
        this.chunkPosX = chunkPos.x();
        this.chunkPosZ = chunkPos.z();
        this.levelId = info.level();
        this.dimension = info.dimension().identifier().toString();
        this.type = info.type();
        this.compression = "standard:" + version.getId();
        this.bytes = bytes;
    }

    public static class Fields {
        public static final String REGION_POS_X = "regionPosX";
        public static final String REGION_POS_Z = "regionPosZ";
        public static final String LOCAL_POS_X = "localPosX";
        public static final String LOCAL_POS_Z = "localPosZ";
        public static final String CHUNK_POS_X = "chunkPosX";
        public static final String CHUNK_POS_Z = "chunkPosZ";
        public static final String LEVEL = "level";
        public static final String DIMENSION = "dimension";
        public static final String TYPE = "type";
        public static final String COMPRESSION = "compression";
        public static final String BYTES = "bytes";

        private Fields() {
        }
    }
}

