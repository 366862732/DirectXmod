package com.xgdt.dx12.lod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Voxy 风格的 LOD 数据 KV 存储：分片 append-only 文件 + 内存偏移索引 + Deflate 压缩。
 *
 * <p>Voxy 用 LMDB/RocksDB 存 32³ section（调色板 + 16 位下标），这里不引原生依赖，
 * 用同样思路做一个纯 Java 的简化版：
 * <ul>
 *   <li>按 key 的 64 位混合哈希分到 {@link #SHARDS} 个文件，避免单文件无限增长；</li>
 *   <li>每条记录 = 固定头 + 压缩后的 {@link LodChunkCodec} 负载；</li>
 *   <li>启动时顺序扫一遍文件重建「key → 偏移」索引，崩溃留下的半截记录会被截掉；</li>
 *   <li>同一个 key 再次写入时追加新记录、索引指向新偏移，旧记录变成死字节；</li>
 *   <li>死字节超过文件一半时自动压实（只保留活记录，原子替换）。</li>
 * </ul>
 *
 * <p>这是一个<b>派生缓存</b>：真正的真源仍是存档里的 region 文件。记录里带
 * {@code regionSize/regionMtime}，读取时签名不符就当作 miss，让调用方重新解析 NBT。
 * 这样既能在远景区块上跳过昂贵的 NBT 解析，又不会把玩家改过的地形永久写死。
 *
 * <p>线程安全：读走读锁、写/压实走写锁；{@link FileChannel} 的位置读写本身也是线程安全的。
 */
public final class LodSectionStore implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("Dx12LodStore");

    /** 'LODS'。 */
    private static final int MAGIC = 0x4C4F4453;
    private static final int VERSION = 1;
    /** 固定头：magic(4) + version(2) + flags(2) + key(8) + regionSize(8) + regionMtime(8) + rawLen(4) + compLen(4) + crc(4)。 */
    private static final int HEADER = 44;
    /** 分片数，必须是 2 的幂。 */
    private static final int SHARDS = 16;
    /** 单条记录压缩后长度的合理上限，防止损坏的头把扫描带飞。 */
    private static final int MAX_RECORD = 64 * 1024 * 1024;
    /** 文件超过这个大小、且死字节过半时才压实。 */
    private static final long COMPACT_MIN_BYTES = 8L * 1024 * 1024;
    /** 整个缓存的软上限；超过后只读不写，避免把玩家磁盘塞满。 */
    private static final long MAX_STORE_BYTES = 8L * 1024 * 1024 * 1024L;

    private static final ThreadLocal<Deflater> DEFLATER =
        ThreadLocal.withInitial(() -> new Deflater(Deflater.DEFAULT_COMPRESSION, false));
    private static final ThreadLocal<Inflater> INFLATER =
        ThreadLocal.withInitial(Inflater::new);

    /** 一条索引项。{@code offset} 在压实时会变，所以是可变类而不是 record。 */
    private static final class Entry {
        long offset;
        final int length;
        final int rawLength;
        final int compressedLength;
        final int crc;
        final long regionSize;
        final long regionMtime;

        Entry(long offset, int length, int rawLength, int compressedLength, int crc,
              long regionSize, long regionMtime) {
            this.offset = offset;
            this.length = length;
            this.rawLength = rawLength;
            this.compressedLength = compressedLength;
            this.crc = crc;
            this.regionSize = regionSize;
            this.regionMtime = regionMtime;
        }
    }

    /** 一个分片：一个文件 + 内存索引 + 死字节统计。 */
    private static final class Shard {
        final Path path;
        FileChannel channel;
        long fileSize;
        long deadBytes;
        final Map<Long, Entry> index = new HashMap<>();

        Shard(Path path) {
            this.path = path;
        }
    }

    private final Path directory;
    private final Shard[] shards = new Shard[SHARDS];
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private long totalBytes;
    private boolean warnedFull;

    private LodSectionStore(Path directory) {
        this.directory = directory;
    }

    /** 打开（或创建）一个存储目录。目录打不开时抛 {@link IOException}。 */
    public static LodSectionStore open(Path directory) throws IOException {
        Files.createDirectories(directory);
        LodSectionStore store = new LodSectionStore(directory);
        try {
            store.load();
        } catch (IOException e) {
            store.closeQuietly();
            throw e;
        }
        return store;
    }

    private void load() throws IOException {
        for (int i = 0; i < SHARDS; i++) {
            Path path = this.directory.resolve("sections-" + i + ".bin");
            Shard shard = new Shard(path);
            shard.channel = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            shard.fileSize = shard.channel.size();
            scan(shard);
            this.shards[i] = shard;
            this.totalBytes += shard.fileSize;
        }
    }

    /**
     * 顺序扫描一个分片，重建索引。遇到损坏/半截的尾部就截断，保证下次追加从干净位置开始。
     */
    private static void scan(Shard shard) throws IOException {
        long position = 0;
        long size = shard.fileSize;
        ByteBuffer header = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN);
        while (position + HEADER <= size) {
            header.clear();
            if (readFully(shard.channel, header, position) != HEADER) {
                break;
            }
            header.flip();
            int magic = header.getInt();
            int version = header.getShort() & 0xFFFF;
            header.getShort(); // flags
            long key = header.getLong();
            long regionSize = header.getLong();
            long regionMtime = header.getLong();
            int rawLength = header.getInt();
            int compressedLength = header.getInt();
            int crc = header.getInt();
            if (magic != MAGIC || version != VERSION
                || compressedLength < 0 || compressedLength > MAX_RECORD
                || rawLength <= 0 || rawLength > MAX_RECORD) {
                break;
            }
            long end = position + HEADER + compressedLength;
            if (end > size) {
                break;
            }
            Entry previous = shard.index.put(key, new Entry(
                position, HEADER + compressedLength, rawLength, compressedLength, crc,
                regionSize, regionMtime));
            if (previous != null) {
                shard.deadBytes += previous.length;
            }
            position = end;
        }
        if (position < size) {
            // 崩溃时可能留下半截记录；截掉它，避免后续追加落在坏数据后面。
            LOGGER.warn("[lod] Truncating corrupt tail in {}: {} -> {} bytes",
                shard.path.getFileName(), size, position);
            shard.channel.truncate(position);
            shard.fileSize = position;
        }
    }

    /**
     * 读一条记录；不存在、region 大小变了或损坏时返回 {@code null}。
     *
     * <p>只看 region 文件大小、不看 mtime：自动存档每写一个区块就会刷新整个 region
     * 文件的 mtime，按 mtime 严格比对等于一次存档就把本 region 的缓存全部作废，
     * 远景每次进世界都要重解上万区块的 NBT。大小变了才说明文件被追加/重写，
     * 那才可能和缓存时的内容对不上。
     */
    public byte[] read(long key, long regionSize, long regionMtime) {
        Shard shard = shardFor(key);
        this.lock.readLock().lock();
        try {
            if (shard.channel == null) {
                return null;
            }
            Entry entry = shard.index.get(key);
            if (entry == null || entry.regionSize != regionSize) {
                return null;
            }
            ByteBuffer compressed = ByteBuffer.allocate(entry.compressedLength);
            if (readFully(shard.channel, compressed, entry.offset + HEADER) != entry.compressedLength) {
                return null;
            }
            byte[] bytes = compressed.array();
            CRC32 crc = new CRC32();
            crc.update(bytes);
            if ((int) crc.getValue() != entry.crc) {
                return null;
            }
            return decompress(bytes, entry.rawLength);
        } catch (IOException e) {
            LOGGER.warn("[lod] Section store read failed: {}", e.toString());
            return null;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /** 追加写一条记录；IO 失败时静默降级（缓存不是真源）。 */
    public void write(long key, byte[] payload, long regionSize, long regionMtime) {
        if (payload == null || payload.length == 0) {
            return;
        }
        byte[] compressed = compress(payload);
        if (compressed.length == 0 || compressed.length > MAX_RECORD) {
            return;
        }
        CRC32 crc = new CRC32();
        crc.update(compressed);
        int crcValue = (int) crc.getValue();

        ByteBuffer header = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(MAGIC);
        header.putShort((short) VERSION);
        header.putShort((short) 0);
        header.putLong(key);
        header.putLong(regionSize);
        header.putLong(regionMtime);
        header.putInt(payload.length);
        header.putInt(compressed.length);
        header.putInt(crcValue);
        header.flip();

        Shard shard = shardFor(key);
        this.lock.writeLock().lock();
        try {
            if (shard.channel == null) {
                return;
            }
            if (this.totalBytes > MAX_STORE_BYTES) {
                if (!this.warnedFull) {
                    this.warnedFull = true;
                    LOGGER.warn("[lod] Section store reached soft limit ({} MB); writes disabled",
                        MAX_STORE_BYTES / (1024 * 1024));
                }
                return;
            }
            long offset = shard.fileSize;
            writeFully(shard.channel, header, offset);
            writeFully(shard.channel, ByteBuffer.wrap(compressed), offset + HEADER);
            int length = HEADER + compressed.length;
            Entry previous = shard.index.put(key, new Entry(
                offset, length, payload.length, compressed.length, crcValue,
                regionSize, regionMtime));
            shard.fileSize += length;
            this.totalBytes += length;
            if (previous != null) {
                shard.deadBytes += previous.length;
            }
            if (shard.fileSize > COMPACT_MIN_BYTES && shard.deadBytes > shard.fileSize / 2) {
                compact(shard);
            }
        } catch (IOException e) {
            LOGGER.warn("[lod] Section store write failed: {}", e.toString());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /** 把 {@code shard} 里的活记录重写到临时文件，然后原子替换。调用方必须持有写锁。 */
    private void compact(Shard shard) throws IOException {
        long oldSize = shard.fileSize;
        Path temp = shard.path.resolveSibling(shard.path.getFileName() + ".tmp");
        long newSize;
        try {
            try (FileChannel out = FileChannel.open(temp,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                long position = 0;
                for (Entry entry : shard.index.values()) {
                    ByteBuffer record = ByteBuffer.allocate(entry.length);
                    if (readFully(shard.channel, record, entry.offset) != entry.length) {
                        throw new IOException("short read while compacting " + shard.path.getFileName());
                    }
                    record.flip();
                    writeFully(out, record, position);
                    entry.offset = position;
                    position += entry.length;
                }
                out.force(false);
                newSize = position;
            }
            shard.channel.close();
            try {
                Files.move(temp, shard.path,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(temp, shard.path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            // 失败时 index 里的 offset 可能已经改成临时文件的布局，必须按旧文件重建，
            // 否则后续读写会落到错误位置。
            if (!shard.channel.isOpen()) {
                shard.channel = FileChannel.open(shard.path,
                    StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            }
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
            try {
                shard.index.clear();
                shard.deadBytes = 0;
                shard.fileSize = shard.channel.size();
                scan(shard);
            } catch (IOException rescanFailed) {
                failure.addSuppressed(rescanFailed);
            }
            throw failure;
        }
        shard.channel = FileChannel.open(shard.path,
            StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        shard.fileSize = Files.size(shard.path);
        shard.deadBytes = 0;
        this.totalBytes -= oldSize - newSize;
        LOGGER.info("[lod] Compacted {}: {} -> {} bytes", shard.path.getFileName(), oldSize, newSize);
    }

    /** 把所有分片压实一次（测试/退出前可选调用）。 */
    void compactAll() {
        this.lock.writeLock().lock();
        try {
            for (Shard shard : this.shards) {
                if (shard != null && shard.channel != null && shard.deadBytes > 0) {
                    compact(shard);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[lod] Section store compaction failed: {}", e.toString());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /** 已索引的记录数。 */
    public int entryCount() {
        this.lock.readLock().lock();
        try {
            int count = 0;
            for (Shard shard : this.shards) {
                if (shard != null) {
                    count += shard.index.size();
                }
            }
            return count;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /** 当前磁盘占用（含未压实的死字节）。 */
    public long sizeBytes() {
        this.lock.readLock().lock();
        try {
            return this.totalBytes;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /** 把页缓存刷到磁盘。 */
    public void flush() {
        this.lock.readLock().lock();
        try {
            for (Shard shard : this.shards) {
                if (shard != null && shard.channel != null) {
                    shard.channel.force(false);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[lod] Section store flush failed: {}", e.toString());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        this.lock.writeLock().lock();
        try {
            for (Shard shard : this.shards) {
                if (shard != null && shard.channel != null) {
                    try {
                        shard.channel.force(false);
                    } catch (IOException ignored) {
                    }
                    try {
                        shard.channel.close();
                    } catch (IOException ignored) {
                    }
                    shard.channel = null;
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void closeQuietly() {
        try {
            close();
        } catch (Throwable ignored) {
        }
    }

    private Shard shardFor(long key) {
        return this.shards[(int) (mix64(key) & (SHARDS - 1))];
    }

    private static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static int readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        int total = 0;
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position + total);
            if (read <= 0) {
                break;
            }
            total += read;
        }
        return total;
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        long offset = position;
        while (buffer.hasRemaining()) {
            int written = channel.write(buffer, offset);
            if (written <= 0) {
                throw new IOException("short write");
            }
            offset += written;
        }
    }

    private static byte[] compress(byte[] raw) {
        Deflater deflater = DEFLATER.get();
        deflater.reset();
        deflater.setInput(raw);
        deflater.finish();
        byte[] out = new byte[Math.max(64, raw.length / 4)];
        int length = 0;
        while (!deflater.finished()) {
            if (length == out.length) {
                out = Arrays.copyOf(out, out.length * 2);
            }
            length += deflater.deflate(out, length, out.length - length);
        }
        return length == out.length ? out : Arrays.copyOf(out, length);
    }

    private static byte[] decompress(byte[] compressed, int rawLength) {
        if (rawLength <= 0 || rawLength > MAX_RECORD) {
            return null;
        }
        Inflater inflater = INFLATER.get();
        inflater.reset();
        inflater.setInput(compressed);
        byte[] out = new byte[rawLength];
        int length = 0;
        try {
            while (length < rawLength && !inflater.finished()) {
                int produced = inflater.inflate(out, length, rawLength - length);
                if (produced == 0) {
                    // 没有产出就说明数据不完整（或需要字典）；继续循环只会空转
                    break;
                }
                length += produced;
            }
        } catch (DataFormatException e) {
            return null;
        }
        return length == rawLength ? out : null;
    }
}
