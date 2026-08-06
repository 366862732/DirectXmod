/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionBitmap;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionFile
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SECTOR_BYTES = 4096;
    @VisibleForTesting
    protected static final int SECTOR_INTS = 1024;
    private static final int CHUNK_HEADER_SIZE = 5;
    private static final int HEADER_OFFSET = 0;
    private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
    private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
    private static final int EXTERNAL_STREAM_FLAG = 128;
    private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
    private static final int CHUNK_NOT_PRESENT = 0;
    private final RegionStorageInfo info;
    private final Path path;
    private final FileChannel file;
    private final Path externalFileDir;
    private final RegionFileVersion version;
    private final ByteBuffer header = ByteBuffer.allocateDirect(8192);
    private final IntBuffer offsets;
    private final IntBuffer timestamps;
    @VisibleForTesting
    protected final RegionBitmap usedSectors = new RegionBitmap();

    public RegionFile(RegionStorageInfo info, Path path, Path externalFileDir, boolean sync) throws IOException {
        this(info, path, externalFileDir, RegionFileVersion.getSelected(), sync);
    }

    public RegionFile(RegionStorageInfo info, Path path, Path externalFileDir, RegionFileVersion version, boolean sync) throws IOException {
        this.info = info;
        this.path = path;
        this.version = version;
        if (!Files.isDirectory(externalFileDir, new LinkOption[0])) {
            throw new IllegalArgumentException("Expected directory, got " + String.valueOf(externalFileDir.toAbsolutePath()));
        }
        this.externalFileDir = externalFileDir;
        this.offsets = this.header.asIntBuffer();
        this.offsets.limit(1024);
        this.header.position(4096);
        this.timestamps = this.header.asIntBuffer();
        this.file = sync ? FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC) : FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        this.usedSectors.force(0, 2);
        this.header.position(0);
        int readHeaderBytes = this.file.read(this.header, 0L);
        if (readHeaderBytes != -1) {
            if (readHeaderBytes != 8192) {
                LOGGER.warn("Region file {} has truncated header: {}", (Object)path, (Object)readHeaderBytes);
            }
            long size = Files.size(path);
            for (int i = 0; i < 1024; ++i) {
                int offset = this.offsets.get(i);
                if (offset == 0) continue;
                int sectorNumber = RegionFile.getSectorNumber(offset);
                int numSectors = RegionFile.getNumSectors(offset);
                if (sectorNumber < 2) {
                    LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", new Object[]{path, i, sectorNumber});
                    this.offsets.put(i, 0);
                    continue;
                }
                if (numSectors == 0) {
                    LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", (Object)path, (Object)i);
                    this.offsets.put(i, 0);
                    continue;
                }
                if ((long)sectorNumber * 4096L > size) {
                    LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", new Object[]{path, i, sectorNumber});
                    this.offsets.put(i, 0);
                    continue;
                }
                this.usedSectors.force(sectorNumber, numSectors);
            }
        }
    }

    public Path getPath() {
        return this.path;
    }

    private Path getExternalChunkPath(ChunkPos pos) {
        String externalFileName = "c." + pos.x() + "." + pos.z() + EXTERNAL_FILE_EXTENSION;
        return this.externalFileDir.resolve(externalFileName);
    }

    public synchronized @Nullable DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException {
        int offset = this.getOffset(pos);
        if (offset == 0) {
            return null;
        }
        int sectorNumber = RegionFile.getSectorNumber(offset);
        int numSectors = RegionFile.getNumSectors(offset);
        int sectorsLength = numSectors * 4096;
        ByteBuffer buffer = ByteBuffer.allocate(sectorsLength);
        this.file.read(buffer, sectorNumber * 4096);
        buffer.flip();
        if (buffer.remaining() < 5) {
            LOGGER.error("Chunk {} header is truncated: expected {} but read {}", new Object[]{pos, sectorsLength, buffer.remaining()});
            return null;
        }
        int length = buffer.getInt();
        byte versionId = buffer.get();
        if (length == 0) {
            LOGGER.warn("Chunk {} is allocated, but stream is missing", (Object)pos);
            return null;
        }
        int streamLength = length - 1;
        if (RegionFile.isExternalStreamChunk(versionId)) {
            if (streamLength != 0) {
                LOGGER.warn("Chunk has both internal and external streams");
            }
            return this.createExternalChunkInputStream(pos, RegionFile.getExternalChunkVersion(versionId));
        }
        if (streamLength > buffer.remaining()) {
            LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", new Object[]{pos, streamLength, buffer.remaining()});
            return null;
        }
        if (streamLength < 0) {
            LOGGER.error("Declared size {} of chunk {} is negative", (Object)length, (Object)pos);
            return null;
        }
        JvmProfiler.INSTANCE.onRegionFileRead(this.info, pos, this.version, streamLength);
        return this.createChunkInputStream(pos, versionId, RegionFile.createStream(buffer, streamLength));
    }

    private static int getTimestamp() {
        return (int)(Util.getEpochMillis() / 1000L);
    }

    private static boolean isExternalStreamChunk(byte version) {
        return (version & 0x80) != 0;
    }

    private static byte getExternalChunkVersion(byte version) {
        return (byte)(version & 0xFFFFFF7F);
    }

    private @Nullable DataInputStream createChunkInputStream(ChunkPos pos, byte versionId, InputStream chunkStream) throws IOException {
        RegionFileVersion version = RegionFileVersion.fromId(versionId);
        if (version == RegionFileVersion.VERSION_CUSTOM) {
            String type = new DataInputStream(chunkStream).readUTF();
            Identifier id = Identifier.tryParse(type);
            if (id != null) {
                LOGGER.error("Unrecognized custom compression {}", (Object)id);
                return null;
            }
            LOGGER.error("Invalid custom compression id {}", (Object)type);
            return null;
        }
        if (version == null) {
            LOGGER.error("Chunk {} has invalid chunk stream version {}", (Object)pos, (Object)versionId);
            return null;
        }
        return new DataInputStream(version.wrap(chunkStream));
    }

    private @Nullable DataInputStream createExternalChunkInputStream(ChunkPos pos, byte versionId) throws IOException {
        Path externalFile = this.getExternalChunkPath(pos);
        if (!Files.isRegularFile(externalFile, new LinkOption[0])) {
            LOGGER.error("External chunk path {} is not file", (Object)externalFile);
            return null;
        }
        return this.createChunkInputStream(pos, versionId, Files.newInputStream(externalFile, new OpenOption[0]));
    }

    private static ByteArrayInputStream createStream(ByteBuffer buffer, int length) {
        return new ByteArrayInputStream(buffer.array(), buffer.position(), length);
    }

    private int packSectorOffset(int index, int size) {
        return index << 8 | size;
    }

    private static int getNumSectors(int offset) {
        return offset & 0xFF;
    }

    private static int getSectorNumber(int offset) {
        return offset >> 8 & 0xFFFFFF;
    }

    private static int sizeToSectors(int size) {
        return (size + 4096 - 1) / 4096;
    }

    public boolean doesChunkExist(ChunkPos pos) {
        int offset = this.getOffset(pos);
        if (offset == 0) {
            return false;
        }
        int sectorNumber = RegionFile.getSectorNumber(offset);
        int numSectors = RegionFile.getNumSectors(offset);
        ByteBuffer streamHeader = ByteBuffer.allocate(5);
        try {
            this.file.read(streamHeader, sectorNumber * 4096);
            streamHeader.flip();
            if (streamHeader.remaining() != 5) {
                return false;
            }
            int length = streamHeader.getInt();
            byte versionId = streamHeader.get();
            if (RegionFile.isExternalStreamChunk(versionId)) {
                if (!RegionFileVersion.isValidVersion(RegionFile.getExternalChunkVersion(versionId))) {
                    return false;
                }
                if (!Files.isRegularFile(this.getExternalChunkPath(pos), new LinkOption[0])) {
                    return false;
                }
            } else {
                if (!RegionFileVersion.isValidVersion(versionId)) {
                    return false;
                }
                if (length == 0) {
                    return false;
                }
                int streamLength = length - 1;
                if (streamLength < 0 || streamLength > 4096 * numSectors) {
                    return false;
                }
            }
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    public DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException {
        return new DataOutputStream(this.version.wrap(new ChunkBuffer(this, pos)));
    }

    public void flush() throws IOException {
        this.file.force(true);
    }

    public void clear(ChunkPos pos) throws IOException {
        int offsetIndex = RegionFile.getOffsetIndex(pos);
        int offset = this.offsets.get(offsetIndex);
        if (offset == 0) {
            return;
        }
        this.offsets.put(offsetIndex, 0);
        this.timestamps.put(offsetIndex, RegionFile.getTimestamp());
        this.writeHeader();
        Files.deleteIfExists(this.getExternalChunkPath(pos));
        this.usedSectors.free(RegionFile.getSectorNumber(offset), RegionFile.getNumSectors(offset));
    }

    protected synchronized void write(ChunkPos pos, ByteBuffer data) throws IOException {
        CommitOp commitOp;
        int newSectorNumber;
        int offsetIndex = RegionFile.getOffsetIndex(pos);
        int offset = this.offsets.get(offsetIndex);
        int sectorNumber = RegionFile.getSectorNumber(offset);
        int currentSectorCount = RegionFile.getNumSectors(offset);
        int dataSize = data.remaining();
        int sectorsNeeded = RegionFile.sizeToSectors(dataSize);
        if (sectorsNeeded >= 256) {
            Path externalChunkPath = this.getExternalChunkPath(pos);
            LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", new Object[]{pos, dataSize, externalChunkPath});
            sectorsNeeded = 1;
            newSectorNumber = this.usedSectors.allocate(sectorsNeeded);
            commitOp = this.writeToExternalFile(externalChunkPath, data);
            ByteBuffer stub = this.createExternalStub();
            this.file.write(stub, newSectorNumber * 4096);
        } else {
            newSectorNumber = this.usedSectors.allocate(sectorsNeeded);
            commitOp = () -> Files.deleteIfExists(this.getExternalChunkPath(pos));
            this.file.write(data, newSectorNumber * 4096);
        }
        this.offsets.put(offsetIndex, this.packSectorOffset(newSectorNumber, sectorsNeeded));
        this.timestamps.put(offsetIndex, RegionFile.getTimestamp());
        this.writeHeader();
        commitOp.run();
        if (sectorNumber != 0) {
            this.usedSectors.free(sectorNumber, currentSectorCount);
        }
    }

    private ByteBuffer createExternalStub() {
        ByteBuffer stub = ByteBuffer.allocate(5);
        stub.putInt(1);
        stub.put((byte)(this.version.getId() | 0x80));
        stub.flip();
        return stub;
    }

    private CommitOp writeToExternalFile(Path path, ByteBuffer data) throws IOException {
        Path tmpPath = Files.createTempFile(this.externalFileDir, "tmp", null, new FileAttribute[0]);
        try (FileChannel extFile = FileChannel.open(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);){
            data.position(5);
            extFile.write(data);
        }
        return () -> Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeHeader() throws IOException {
        this.header.position(0);
        this.file.write(this.header, 0L);
    }

    private int getOffset(ChunkPos pos) {
        return this.offsets.get(RegionFile.getOffsetIndex(pos));
    }

    public boolean hasChunk(ChunkPos pos) {
        return this.getOffset(pos) != 0;
    }

    private static int getOffsetIndex(ChunkPos pos) {
        return pos.getRegionLocalX() + pos.getRegionLocalZ() * 32;
    }

    @Override
    public void close() throws IOException {
        try {
            this.padToFullSector();
        }
        finally {
            try {
                this.file.force(true);
            }
            finally {
                this.file.close();
            }
        }
    }

    private void padToFullSector() throws IOException {
        int paddedSize;
        int fileSize = (int)this.file.size();
        if (fileSize != (paddedSize = RegionFile.sizeToSectors(fileSize) * 4096)) {
            ByteBuffer padding = PADDING_BUFFER.duplicate();
            padding.position(0);
            this.file.write(padding, paddedSize - 1);
        }
    }

    private class ChunkBuffer
    extends ByteArrayOutputStream {
        private final ChunkPos pos;
        final /* synthetic */ RegionFile this$0;

        public ChunkBuffer(RegionFile regionFile, ChunkPos pos) {
            RegionFile regionFile2 = regionFile;
            Objects.requireNonNull(regionFile2);
            this.this$0 = regionFile2;
            super(8096);
            super.write(0);
            super.write(0);
            super.write(0);
            super.write(0);
            super.write(regionFile.version.getId());
            this.pos = pos;
        }

        @Override
        public void close() throws IOException {
            ByteBuffer result = ByteBuffer.wrap(this.buf, 0, this.count);
            int streamLength = this.count - 5 + 1;
            JvmProfiler.INSTANCE.onRegionFileWrite(this.this$0.info, this.pos, this.this$0.version, streamLength);
            result.putInt(0, streamLength);
            this.this$0.write(this.pos, result);
        }
    }

    private static interface CommitOp {
        public void run() throws IOException;
    }
}

