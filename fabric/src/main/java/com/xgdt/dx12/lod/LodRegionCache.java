package com.xgdt.dx12.lod;

import com.xgdt.dx12.config.Dx12Config;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远视距 LOD 的地形数据源：直接读存档的 region 文件，取出每个区块
 * {@link Heightmap.Types#WORLD_SURFACE} 高度图（16 × 16 列）以及每一列表层方块的
 * {@link MapColor}，供原版视距之外的 LOD 网格使用。
 *
 * <p>客户端 {@code ClientChunkCache} 只缓存 {@code renderDistance + 3} 区块，超出部分
 * 用 {@code getChunk(..., false)} 一律拿到 {@code null}；要做出 Voxy 级别的远景只能读盘。
 *
 * <p>所有 IO 都在单条后台守护线程上完成，渲染线程只做 {@link #heightAt(int, int)} /
 * {@link #colorAt(int, int)} 的查表与入队，不会阻塞主线程。未探索过（region 里没有）的
 * 区块记为空数据并留在缓存里，不会反复重试。仅单人游戏可用，多人游戏下静默退化为不可用。
 */
@Environment(EnvType.CLIENT)
public final class LodRegionCache {
    private static final LodRegionCache INSTANCE = new LodRegionCache();

    /** 缓存区块数上限（每块 ~1.6KB，65536 块约 100MB）。 */
    private static final int MAX_CHUNKS = 65536;
    /**
     * 无数据区块的登记上限。必须与 {@link #columns} 分开：大视距下一次重建会采样上万格，
     * 未探索地形全是无数据，若混在一起会把已读到的高度数据全部挤出去。
     */
    private static final int MAX_MISSING = 131072;
    /** 待读队列上限，超出后丢弃本次请求（下一次重建会重试）。 */
    private static final int QUEUE_LIMIT = 32768;
    /** 后台读盘线程数：大视距下一次重建会请求上万区块，单线程追不上。 */
    private static final int WORKERS = 3;
    /** 高度图每区块的列数（16 × 16）。 */
    private static final int COLUMNS = 256;
    /** 单个 section 的方块数（16 × 16 × 16）。 */
    private static final int SECTION_VOLUME = 4096;
    /** 单列无数据标记。 */
    private static final short UNAVAILABLE = Short.MIN_VALUE;
    /** 整个区块不可用的标记（用空数组区分，避免为每列存一份哨兵）。 */
    private static final ChunkColumns MISSING = new ChunkColumns(new short[0], new int[0]);
    /** 区块确定没有 NBT 数据（region/区块不存在）。用独立实例区分「临时失败」。 */
    private static final CompoundTag MISSING_TAG = new CompoundTag();
    /** {@link #sample(int, int)} 的「无数据」返回值。 */
    public static final long NO_SAMPLE = (long) Integer.MIN_VALUE << 32;

    /**
     * 地表是水体的标记位（bit 24 起，与 RGB 不冲突）。
     *
     * <p>以前渲染端靠「颜色等于 0x4040FF」来认出水面，而 {@code MapColor.WATER} 的实际色值是
     * 会随版本变的（现在是 {@code 0x3F76E4}），于是水体全部漏判、被当成实心蓝方块画出来。
     * 改成在解包调色板时按方块名打标记，两边都不再猜颜色。
     */
    public static final int FLAG_WATER = 0x0100_0000;

    /** 水体标记：地表方块是 water / flowing_water。负数是「未采样」哨兵，不能当水体。 */
    public static boolean isWater(int rgb) {
        return rgb >= 0 && (rgb & FLAG_WATER) != 0;
    }

    /** 去掉标记位后的纯 RGB。 */
    public static int stripFlags(int rgb) {
        return rgb & 0xFFFFFF;
    }

    /**
     * 一个区块的地表数据。{@code heights[i]} 是第 i 列最高方块的 y（{@link #UNAVAILABLE}
     * 表示该列没有数据），{@code colors[i]} 是第 i 列地表方块的 RGB（{@code -1} 表示未知）。
     * 列下标 = {@code (x & 15) + (z & 15) * 16}。
     */
    public record ChunkColumns(short[] heights, int[] colors) {
    }

    /**
     * 一个区块的完整方块数据（3D 体素 LOD 用）。
     *
     * <p>{@code blocks} 是按 {@code ((y - minY) * 16 + z) * 16 + x} 排列的调色板下标，
     * {@code palette[index]} 是该方块的 RGB（{@code -1} 表示空气/透明）。整块数据约
     * 16×16×384 项，只在建网时临时存在，不进入长期缓存。
     */
    public record VoxelChunk(int minY, int height, int topY, int[] palette, short[] blocks) {
        /** 区块内坐标（x/z 为 0..15，y 为绝对世界坐标）是否有实心方块。 */
        public boolean solid(int x, int y, int z) {
            int ly = y - this.minY;
            if (ly < 0 || ly >= this.height || x < 0 || x > 15 || z < 0 || z > 15) {
                return false;
            }
            return this.palette[this.blocks[(ly * 16 + z) * 16 + x] & 0xFFFF] != -1;
        }

        /** 区块内坐标的方块颜色（{@code -1} = 空气）。 */
        public int color(int x, int y, int z) {
            int ly = y - this.minY;
            if (ly < 0 || ly >= this.height || x < 0 || x > 15 || z < 0 || z > 15) {
                return -1;
            }
            return this.palette[this.blocks[(ly * 16 + z) * 16 + x] & 0xFFFF];
        }
    }

    /** 方块注册名 → RGB（{@code -1} = 空气或无法解析），避免每列都做一次注册表查询。 */
    private static final Map<String, Integer> BLOCK_COLORS = new ConcurrentHashMap<>();
    /**
     * 方块注册名 → 是否按「实心方块」参与体素 LOD。
     *
     * <p>高度场只看地表方块，无所谓；3D 体素要逐格判断邻居，草、花、火把、地毯、台阶
     * 这些非整方块如果也算实心，一个平原区块会凭空多出上千个面。这里用
     * {@code canOcclude()} 判整方块，另外保留树叶、原木、水这几类「不遮挡但必须画」的。
     */
    private static final Map<String, Boolean> BLOCK_SOLID = new ConcurrentHashMap<>();

    private final ExecutorService worker = Executors.newFixedThreadPool(WORKERS, task -> {
        Thread thread = new Thread(task, "dx12-lod-region");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private final ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    private final Set<Long> pending = ConcurrentHashMap.newKeySet();
    /** 当前正在跑的读盘任务数，最多 {@link #WORKERS} 个。 */
    private final AtomicInteger draining = new AtomicInteger();

    /** 区块 key → 地表数据（{@link #MISSING} 表示整块读不到）。 */
    private final Map<Long, ChunkColumns> columns = Collections.synchronizedMap(
        new LinkedHashMap<Long, ChunkColumns>(2048, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, ChunkColumns> eldest) {
                return size() > MAX_CHUNKS;
            }
        });

    /** 已确认没有高度数据的区块（region 文件不存在 / 区块未生成），避免反复入队重读。 */
    private final Set<Long> missing = Collections.synchronizedSet(
        Collections.newSetFromMap(new LinkedHashMap<Long, Boolean>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                return size() > MAX_MISSING;
            }
        }));

    /** 当前存档的 region 目录；{@code null} 表示不可用（多人游戏等）。 */
    private volatile Path regionDir;
    private volatile ResourceKey<Level> dimension;
    private volatile int minY;
    private volatile int height;
    private volatile int bits;

    private static final Logger LOGGER = LoggerFactory.getLogger("Dx12LodCache");
    /** region 文件 (size, mtime) 的短期缓存，避免每个区块都 stat 一次。 */
    private static final long REGION_STAT_TTL_NS = 1_000_000_000L;

    /** 解码后 section 的持久化 KV 存储；{@code null} = 不可用（多人游戏/开关关闭/打开失败）。 */
    private volatile LodSectionStore store;
    private volatile Path storeDir;
    private final Map<Long, long[]> regionStats = new ConcurrentHashMap<>();

    private LodRegionCache() {
    }

    public static LodRegionCache getInstance() {
        return INSTANCE;
    }

    /** 当前是否拿得到 region 文件（单人游戏且已绑定存档目录）。 */
    public boolean isAvailable() {
        return this.regionDir != null;
    }

    /**
     * 每帧调用一次，绑定当前世界与维度。仅比较路径，不做 IO。
     */
    public void prepare(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            reset();
            return;
        }
        Path dir;
        try {
            dir = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT))
                .resolve("region");
        } catch (Throwable t) {
            reset();
            return;
        }
        if (dir.equals(this.regionDir) && level.dimension().equals(this.dimension)) {
            return;
        }
        this.regionDir = dir;
        this.dimension = level.dimension();
        this.minY = level.getMinY();
        this.height = level.getHeight();
        this.bits = Mth.ceillog2(level.getHeight() + 1);
        this.columns.clear();
        this.missing.clear();
        this.pending.clear();
        this.queue.clear();
        this.regionStats.clear();
        openStore(dir);
        // 体素网格是按方块数据建的，换世界/维度后全部失效
        LodVoxelCache.getInstance().clear();
    }

    /**
     * 打开当前维度的持久化 section 存储。目录放在该维度 region 目录的旁边
     * （主世界 {@code <world>/gl4dx12_lod}，下界 {@code <world>/DIM-1/gl4dx12_lod}），
     * 随存档一起复制/删除；打开失败只降级为「每次都重新解析 NBT」，不影响渲染。
     */
    private void openStore(Path regionDir) {
        closeStore();
        Path parent = regionDir.getParent();
        Path dir = (parent == null ? regionDir : parent).resolve("gl4dx12_lod");
        this.storeDir = dir;
        if (Dx12Config.getInstance().getLodCache() == 0) {
            return;
        }
        try {
            this.store = LodSectionStore.open(dir);
            LOGGER.info("[lod] Section store opened: {} ({} entries, {} KB)",
                dir, this.store.entryCount(), this.store.sizeBytes() / 1024);
        } catch (Throwable t) {
            this.store = null;
            LOGGER.warn("[lod] Failed to open section store {}: {}", dir, t.toString());
        }
    }

    private void closeStore() {
        LodSectionStore current = this.store;
        this.store = null;
        if (current != null) {
            try {
                current.flush();
                LOGGER.info("[lod] Section store closed: {} entries, {} KB",
                    current.entryCount(), current.sizeBytes() / 1024);
                current.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private void reset() {
        if (this.regionDir == null) {
            return;
        }
        this.regionDir = null;
        this.dimension = null;
        this.columns.clear();
        this.missing.clear();
        this.pending.clear();
        this.queue.clear();
        this.regionStats.clear();
        closeStore();
        this.storeDir = null;
        LodVoxelCache.getInstance().clear();
    }

    /**
     * 取某个方块位置所在列的地表高度（首个空气方块的 y，即地表顶面）。尚未读到则返回
     * {@link Integer#MIN_VALUE} 并在后台发起该区块的读取。
     */
    public int heightAt(int blockX, int blockZ) {
        ChunkColumns data = lookup(blockX, blockZ);
        if (data == null) {
            return Integer.MIN_VALUE;
        }
        short value = data.heights[(blockX & 15) + (blockZ & 15) * 16];
        return value == UNAVAILABLE ? Integer.MIN_VALUE : value;
    }

    /**
     * 取某个方块位置所在列的地表方块颜色（RGB）。未知返回 {@code -1}。
     */
    public int colorAt(int blockX, int blockZ) {
        ChunkColumns data = lookup(blockX, blockZ);
        if (data == null) {
            return -1;
        }
        return data.colors[(blockX & 15) + (blockZ & 15) * 16];
    }

    /**
     * 一次性取「地表高度 + 地表颜色」的打包结果：高 32 位是高度，低 32 位是 RGB。
     * 高度或颜色任一未知即返回 {@link #NO_SAMPLE}，渲染端据此跳过该顶点。
     */
    public long sample(int blockX, int blockZ) {
        ChunkColumns data = lookup(blockX, blockZ);
        if (data == null) {
            return NO_SAMPLE;
        }
        int index = (blockX & 15) + (blockZ & 15) * 16;
        short height = data.heights[index];
        if (height == UNAVAILABLE) {
            return NO_SAMPLE;
        }
        int rgb = data.colors[index];
        if (rgb < 0) {
            return NO_SAMPLE;
        }
        return ((long) height << 32) | (rgb & 0xFFFFFFFFL);
    }

    private ChunkColumns lookup(int blockX, int blockZ) {
        if (this.regionDir == null) {
            return null;
        }
        long key = ChunkPos.pack(blockX >> 4, blockZ >> 4);
        ChunkColumns data = this.columns.get(key);
        if (data == null) {
            if (!this.missing.contains(key)) {
                request(key);
            }
            return null;
        }
        return data.heights.length == 0 ? null : data;
    }

    /** 已缓存且可读的区块数（调试用）。 */
    public int cachedCount() {
        return this.columns.size();
    }

    private void request(long key) {
        if (!this.pending.add(key)) {
            return;
        }
        if (!this.queue.offer(key)) {
            this.pending.remove(key);
            return;
        }
        scheduleDrain();
    }

    /** 队列非空且并发未满时拉起一条新的读盘任务。 */
    private void scheduleDrain() {
        while (!this.queue.isEmpty()) {
            int active = this.draining.get();
            if (active >= WORKERS) {
                return;
            }
            if (!this.draining.compareAndSet(active, active + 1)) {
                continue;
            }
            this.worker.execute(this::drain);
            return;
        }
    }

    private void drain() {
        try {
            Path dir = this.regionDir;
            ResourceKey<Level> dimension = this.dimension;
            int minY = this.minY;
            int bits = this.bits;
            Long key;
            while (dir != null && dimension != null && (key = this.queue.poll()) != null) {
                this.pending.remove(key);
                ChunkColumns decoded = readChunk(key, dir, minY, bits);
                // decoded == null 表示本次 IO 失败（如服务端正在写该 region），留待下次重试
                if (decoded == null || !dir.equals(this.regionDir) || !dimension.equals(this.dimension)) {
                    continue;
                }
                if (decoded == MISSING) {
                    // 无数据只登记在 missing 里，不占用 columns 的容量
                    this.missing.add(key);
                } else {
                    this.columns.put(key, decoded);
                    this.missing.remove(key);
                }
            }
        } catch (Throwable ignored) {
            // 后台线程不允许抛出，否则后续请求全部失效
        } finally {
            this.draining.decrementAndGet();
        }
        // 本条任务结束时可能还有残留请求（入队时并发已满没拉到线程），这里补一次
        scheduleDrain();
    }

    /**
     * 读一个区块的地表数据；返回 {@link #MISSING} 表示确实没有数据，返回 {@code null} 表示临时失败。
     *
     * <p>这里不用 {@code RegionFileStorage}：它在构造时把 8KB 表头缓存在内存里，
     * 而服务端会不断往同一个 region 追加区块，缓存的扇区表很快过期，读到错位数据后
     * 会刷出大量 "stream is truncated / external chunk path is not file"。改为每次
     * 现读 4 字节表头，牺牲一点 IO 换取位置永远是最新的。
     */
    private ChunkColumns readChunk(long key, Path dir, int minY, int bits) {
        CompoundTag tag = readTag(key, dir);
        if (tag == null) {
            return null;
        }
        if (tag == MISSING_TAG) {
            return MISSING;
        }
        try {
            Optional<long[]> rawHeights = surfaceHeightmap(tag);
            if (rawHeights.isEmpty()) {
                return MISSING;
            }
            short[] heights = unpack(rawHeights.get(), minY, bits);
            if (heights == MISSING.heights) {
                return MISSING;
            }
            short[] colorHeights = colorHeightmap(tag, minY, bits, heights);
            return new ChunkColumns(heights,
                decodeColumns(tag.getListOrEmpty("sections"), heights, colorHeights));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 读一个区块的完整方块数据，供 3D 体素 LOD 建网。返回 {@code null} 表示不可用
     * （region 不存在、区块未生成或 IO 失败）。
     *
     * <p>优先命中 {@link LodSectionStore}：解码后的调色板+索引比 NBT 小得多，也省掉
     * 逐 section 查注册表的开销。存储里记着 region 文件的 {@code (size, mtime)}，
     * 签名不符（玩家改过地形 / 服务端追加了新区块）就当 miss，重新解析并回写。
     */
    public VoxelChunk readVoxel(int chunkX, int chunkZ) {
        Path dir = this.regionDir;
        if (dir == null) {
            return null;
        }
        long key = ChunkPos.pack(chunkX, chunkZ);
        Path regionFile = regionFile(dir, chunkX, chunkZ);
        long regionKey = ChunkPos.pack(chunkX >> 5, chunkZ >> 5);
        long[] stat = regionStat(regionFile, regionKey);
        LodSectionStore cache = this.store;
        if (cache != null && stat != null) {
            byte[] payload = cache.read(key, stat[0], stat[1]);
            if (payload != null) {
                VoxelChunk cached = LodChunkCodec.decode(payload);
                if (cached != null) {
                    return cached;
                }
            }
        }
        CompoundTag tag = readTag(key, dir);
        if (tag == null || tag == MISSING_TAG) {
            return null;
        }
        try {
            VoxelChunk chunk = voxelFromTag(tag, this.minY, this.height);
            if (cache != null && stat != null) {
                byte[] payload = LodChunkCodec.encode(chunk);
                if (payload != null) {
                    cache.write(key, payload, stat[0], stat[1]);
                }
            }
            return chunk;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 取 region 文件的 {@code (size, mtime, 采样时刻)}，短期缓存一秒。
     *
     * <p>一次重建会请求上万区块，同一 region 下的区块不该各自 stat 一次；但缓存也不能
     * 太久，否则玩家刚改的方块会一直读到旧 LOD。
     */
    private long[] regionStat(Path file, long regionKey) {
        long now = System.nanoTime();
        long[] cached = this.regionStats.get(regionKey);
        if (cached != null && now - cached[2] < REGION_STAT_TTL_NS) {
            return cached;
        }
        try {
            long[] value = {Files.size(file), Files.getLastModifiedTime(file).toMillis(), now};
            this.regionStats.put(regionKey, value);
            return value;
        } catch (IOException e) {
            return null;
        }
    }

    private static Path regionFile(Path dir, int chunkX, int chunkZ) {
        return dir.resolve("r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
    }

    /** 读一个区块的 NBT；{@link #MISSING_TAG} 表示确定没有数据，{@code null} 表示临时失败。 */
    private static CompoundTag readTag(long key, Path dir) {
        int chunkX = ChunkPos.getX(key);
        int chunkZ = ChunkPos.getZ(key);
        Path file = regionFile(dir, chunkX, chunkZ);
        // 先判存在：不存在的 region 不去打开
        if (!Files.isRegularFile(file)) {
            return MISSING_TAG;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer offsetBytes = ByteBuffer.allocate(4);
            int index = (chunkX & 31) + (chunkZ & 31) * 32;
            if (channel.read(offsetBytes, (long) index * 4L) != 4) {
                return MISSING_TAG;
            }
            int offset = offsetBytes.flip().getInt();
            int sectorNumber = offset >> 8 & 0xFFFFFF;
            int numSectors = offset & 0xFF;
            if (sectorNumber < 2 || numSectors == 0) {
                return MISSING_TAG;
            }
            ByteBuffer data = ByteBuffer.allocate(numSectors * 4096);
            if (channel.read(data, (long) sectorNumber * 4096L) <= 0) {
                return MISSING_TAG;
            }
            data.flip();
            if (data.remaining() < 5) {
                return MISSING_TAG;
            }
            int length = data.getInt();
            byte versionId = data.get();
            int streamLength = length - 1;
            if (streamLength < 0) {
                return MISSING_TAG;
            }
            boolean external = (versionId & 0x80) != 0;
            RegionFileVersion compression = RegionFileVersion.fromId(external ? versionId & 0x7F : versionId);
            if (compression == null || compression == RegionFileVersion.VERSION_CUSTOM) {
                return MISSING_TAG;
            }
            InputStream raw;
            if (external) {
                Path externalFile = dir.resolve("c." + chunkX + "." + chunkZ + ".mcc");
                if (!Files.isRegularFile(externalFile)) {
                    return MISSING_TAG;
                }
                raw = Files.newInputStream(externalFile);
            } else {
                if (streamLength > data.remaining()) {
                    return MISSING_TAG;
                }
                byte[] payload = new byte[streamLength];
                data.get(payload);
                raw = new ByteArrayInputStream(payload);
            }
            try (DataInputStream in = new DataInputStream(compression.wrap(raw))) {
                CompoundTag tag = NbtIo.read(in);
                return tag == null ? MISSING_TAG : tag;
            }
        } catch (Throwable t) {
            // IO 失败（region 正在被写/暂时不可读）：不缓存，下次重建再试
            return null;
        }
    }

    /**
     * 把区块 NBT 展开成整块调色板下标数组。
     *
     * <p>每个 section 的 {@code block_states} 里 palette 项数通常只有几十，逐项查注册表
     * 太慢，所以按方块名去重成一份区块级调色板；空气固定为下标 0，颜色 {@code -1}。
     */
    private static VoxelChunk voxelFromTag(CompoundTag tag, int minY, int height) {
        int sections = height / 16;
        short[] blocks = new short[16 * 16 * height];
        int[] palette = new int[256];
        palette[0] = -1;
        int paletteSize = 1;

        ListTag list = tag.getListOrEmpty("sections");
        int topY = minY;
        for (int s = 0; s < list.size(); s++) {
            CompoundTag section = list.getCompoundOrEmpty(s);
            int sectionY = section.getByteOr("Y", (byte) 0) - Math.floorDiv(minY, 16);
            if (sectionY < 0 || sectionY >= sections) {
                continue;
            }
            SectionData data = buildSection(section);
            if (data == null) {
                continue;
            }
            // section 内的调色板下标 → 区块级调色板下标
            int[] remap = new int[data.paletteColors.length];
            for (int p = 0; p < remap.length; p++) {
                int color = data.paletteColors[p];
                // 非实心方块（草、花、火把、地毯、台阶……）在体素 LOD 里一律当空气：
                // 它们在远处根本看不见，算进去只会让一个平原区块凭空多出上千个面。
                if (color == -1 || !data.paletteSolid[p]) {
                    remap[p] = 0;
                    continue;
                }
                // 同一颜色合并（不同方块同名同色即可共用），避免调色板无限增长
                int found = -1;
                for (int q = 1; q < paletteSize; q++) {
                    if (palette[q] == color) {
                        found = q;
                        break;
                    }
                }
                if (found < 0) {
                    if (paletteSize == palette.length) {
                        palette = Arrays.copyOf(palette, paletteSize * 2);
                    }
                    found = paletteSize;
                    palette[paletteSize++] = color;
                }
                remap[p] = found;
            }
            int base = sectionY * 16;
            for (int i = 0; i < SECTION_VOLUME; i++) {
                int index = data.storage == null ? 0 : data.storage.get(i);
                int mapped = index >= 0 && index < remap.length ? remap[index] : 0;
                if (mapped == 0) {
                    continue;
                }
                int y = i >> 8;
                int z = (i >> 4) & 15;
                int x = i & 15;
                blocks[((base + y) * 16 + z) * 16 + x] = (short) mapped;
                int absoluteY = minY + base + y;
                if (absoluteY > topY) {
                    topY = absoluteY;
                }
            }
        }
        return new VoxelChunk(minY, height, topY, Arrays.copyOf(palette, paletteSize), blocks);
    }

    /**
     * 上色用的高度：最高非空气方块（{@code WORLD_SURFACE}）。
     *
     * <p>几何高度必须跳过树叶，否则树冠把地形抬成一根根尖刺；但颜色不能跟着跳，
     * 否则整片森林都刷成地面草的浅绿，远景变成一片光滑的绿丘陵。上色仍取最高层，
     * 森林是深绿树冠、雪线是白的、水面是蓝的，层次才和原版一致。
     */
    private static short[] colorHeightmap(CompoundTag tag, int minY, int bits, short[] fallback) {
        Optional<long[]> raw = tag.getCompoundOrEmpty("Heightmaps")
            .getLongArray(Heightmap.Types.WORLD_SURFACE.getSerializationKey());
        if (raw.isEmpty()) {
            return fallback;
        }
        short[] heights = unpack(raw.get(), minY, bits);
        return heights == MISSING.heights ? fallback : heights;
    }

    /**
     * 取「地表」高度图，优先 {@code MOTION_BLOCKING_NO_LEAVES}。
     *
     * <p>{@code WORLD_SURFACE} 是最高非空气方块，树冠会把高度抬十几格，远景森林因此变成
     * 一根根深绿尖刺；{@code MOTION_BLOCKING_NO_LEAVES} 跳过树叶、把水面算作地表，
     * 高度和颜色都落在真正的地面上，和客户端侧 {@code surfaceHeight} 保持一致。
     */
    private static Optional<long[]> surfaceHeightmap(CompoundTag tag) {
        CompoundTag heightmaps = tag.getCompoundOrEmpty("Heightmaps");
        Optional<long[]> heights = heightmaps.getLongArray(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.getSerializationKey());
        if (heights.isEmpty()) {
            heights = heightmaps.getLongArray(Heightmap.Types.OCEAN_FLOOR.getSerializationKey());
        }
        if (heights.isEmpty()) {
            heights = heightmaps.getLongArray(Heightmap.Types.WORLD_SURFACE.getSerializationKey());
        }
        return heights;
    }

    /**
     * 解包高度图：每位表示「最高方块 + 1 − minY」，即该列<b>首个空气方块</b>的 y，
     * 也就是地表顶面所在的那一层。
     *
     * <p>这里不能像以前那样再减 1。原版 {@code chunk.getHeight(...)} 返回的同样是首个
     * 空气方块的 y，而顶点直接写在这个高度上；磁盘侧减 1 会让两条来源差一格，已加载
     * 区块和远景之间在边界上出现一条台阶缝。
     */
    private static short[] unpack(long[] data, int minY, int bits) {
        if (bits <= 0 || bits > 32) {
            return MISSING.heights;
        }
        int valuesPerLong = 64 / bits;
        // SimpleBitStorage 构造时会校验长度，长度不符直接抛异常，这里先自行判断
        if (data.length != (COLUMNS + valuesPerLong - 1) / valuesPerLong) {
            return MISSING.heights;
        }
        SimpleBitStorage storage;
        try {
            storage = new SimpleBitStorage(bits, COLUMNS, data);
        } catch (Throwable t) {
            return MISSING.heights;
        }
        short[] result = new short[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            int stored = storage.get(i);
            // stored == 0 表示整列没有实心方块，不能当成「顶面在 minY」这种有效高度
            int top = stored + minY;
            result[i] = stored > 0 ? (short) top : UNAVAILABLE;
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 地表方块颜色
    // ------------------------------------------------------------------

    /**
     * 一次遍历算出每列的地表色，并把树干列的高度压回地面 —— 两者共用同一份 section
     * 解析结果，避免同一块区块被解析两遍。
     *
     * <p><b>压树干</b>：{@code MOTION_BLOCKING_NO_LEAVES} 只跳过树叶，树干本身挡光，
     * 所以树干那一列会比周围地面高出十几格。远景里 1~2 格宽的树干被整格宽的 LOD
     * 格子一撑，就变成一根根柱子。这里从几何高度往下跳过连续原木，落到第一块非原木
     * 方块上。
     *
     * <p><b>上色</b>：仍取 {@code WORLD_SURFACE}（含树叶、屋顶）的高度，森林是深绿
     * 树冠、房屋是屋顶色，层次才和原版一致。
     *
     * <p>任一列解不出来就留 {@code -1}，由渲染端按高度给近似色。
     */
    private static int[] decodeColumns(ListTag sections, short[] heights, short[] colorHeights) {
        int[] colors = new int[COLUMNS];
        Arrays.fill(colors, -1);
        if (sections == null || sections.size() == 0) {
            return colors;
        }
        // sectionY → 已解好的 palette/storage，同一个区块内复用
        Map<Integer, SectionData> decoded = new HashMap<>();
        for (int i = 0; i < COLUMNS; i++) {
            stripLogsAt(decoded, sections, heights, i);
            int surface = colorHeights[i];
            if (surface == UNAVAILABLE) {
                continue;
            }
            // 高度图是「首个空气方块的 y」，地表方块在它下面一格
            int y = surface - 1;
            // 磁盘上的 sections[*].Y 是绝对 section 坐标（SectionPos.blockToSectionCoord）
            SectionData data = decoded.computeIfAbsent(Math.floorDiv(y, 16),
                sectionY -> findSection(sections, sectionY));
            if (data == null) {
                continue;
            }
            int color = data.colorAt(y & 15, i & 15, i >> 4);
            if (color == -1) {
                // 地表方块是空气/未解析（高度图与方块错位）：在 section 内向下找第一个实心方块
                for (int probe = (y & 15) - 1; probe >= 0 && color == -1; probe--) {
                    color = data.colorAt(probe, i & 15, i >> 4);
                }
            }
            colors[i] = color;
        }
        return colors;
    }

    /** 把第 {@code i} 列的几何高度从树干顶压回真正的地面（原地改 {@code heights}）。 */
    private static void stripLogsAt(Map<Integer, SectionData> decoded, ListTag sections,
                                    short[] heights, int i) {
        int top = heights[i];
        if (top == UNAVAILABLE) {
            return;
        }
        int y = top - 1;
        int guard = 0;
        while (guard++ < 32) {
            SectionData data = decoded.computeIfAbsent(Math.floorDiv(y, 16),
                sectionY -> findSection(sections, sectionY));
            if (data == null || !data.isLogAt(y & 15, i & 15, i >> 4)) {
                break;
            }
            y--;
        }
        if (y + 1 != top) {
            heights[i] = (short) (y + 1);
        }
    }

    /** 在 {@code sections} 列表里找出 Y 等于 {@code sectionY} 的那一节（列表通常按 Y 升序）。 */
    private static SectionData findSection(ListTag sections, int sectionY) {
        for (int s = sections.size() - 1; s >= 0; s--) {
            CompoundTag section = sections.getCompoundOrEmpty(s);
            if (section.getByteOr("Y", (byte) 0) == sectionY) {
                return buildSection(section);
            }
        }
        return null;
    }

    /** 解析一个 section 的 {@code block_states}：palette 颜色表 + 位流。 */
    private static SectionData buildSection(CompoundTag section) {
        CompoundTag blockStates = section.getCompoundOrEmpty("block_states");
        ListTag palette = blockStates.getListOrEmpty("palette");
        int size = palette.size();
        if (size == 0) {
            return null;
        }
        int[] colors = new int[size];
        boolean[] logs = new boolean[size];
        boolean[] solid = new boolean[size];
        for (int p = 0; p < size; p++) {
            CompoundTag entry = palette.getCompoundOrEmpty(p);
            String name = entry.getStringOr("Name", "minecraft:air");
            colors[p] = blockColor(name);
            logs[p] = isLogName(name);
            solid[p] = isSolidBlock(name);
        }
        if (size == 1) {
            // 整节只有一种方块，磁盘上没有 data
            return new SectionData(colors, logs, solid, null);
        }
        Optional<long[]> rawData = blockStates.getLongArray("data");
        if (rawData.isEmpty()) {
            return null;
        }
        long[] data = rawData.get();
        // 磁盘位宽：palette 2..16 项固定 4 位，之后就是 ceillog2(项数)
        int entryBits = Math.max(4, Mth.ceillog2(size));
        int valuesPerLong = 64 / entryBits;
        if (data.length != (SECTION_VOLUME + valuesPerLong - 1) / valuesPerLong) {
            return null;
        }
        SimpleBitStorage storage;
        try {
            storage = new SimpleBitStorage(entryBits, SECTION_VOLUME, data);
        } catch (Throwable t) {
            return null;
        }
        return new SectionData(colors, logs, solid, storage);
    }

    /** 方块注册名是否是树干类方块（原木 / 木头 / 菌柄 / 菌丝体）。 */
    private static boolean isLogName(String name) {
        return name.endsWith("_log") || name.endsWith("_wood")
            || name.endsWith("_stem") || name.endsWith("_hyphae");
    }

    /**
     * 该方块在体素 LOD 里算不算「实心」。
     *
     * <p>整方块（{@code canOcclude()}）全部算；树叶、原木、水虽然不遮挡，但远景里必须
     * 画出来，否则森林和水面会整片消失。草、花、火把、地毯、台阶、栅栏这些不算 ——
     * 它们在 LOD 距离上本来就看不见，算进去只会让面数翻倍。
     */
    private static boolean isSolidBlock(String name) {
        Boolean cached = BLOCK_SOLID.get(name);
        if (cached != null) {
            return cached;
        }
        boolean solid;
        try {
            if (name.endsWith(":air") || name.endsWith(":cave_air") || name.endsWith(":void_air")) {
                solid = false;
            } else if (name.endsWith("leaves") || isLogName(name) || name.endsWith(":water")) {
                solid = true;
            } else {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
                solid = block != null && block.defaultBlockState().canOcclude();
            }
        } catch (Throwable t) {
            solid = false;
        }
        BLOCK_SOLID.put(name, solid);
        return solid;
    }

    /** 已解析的 section：{@code storage == null} 表示整节同一种方块。 */
    private record SectionData(int[] paletteColors, boolean[] paletteLogs, boolean[] paletteSolid,
                               SimpleBitStorage storage) {
        /** {@code y}/{@code x}/{@code z} 均为 section 内的 0..15 坐标。 */
        int colorAt(int y, int x, int z) {
            int index = this.storage == null ? 0 : this.storage.get((y << 4 | z) << 4 | x);
            return index >= 0 && index < this.paletteColors.length ? this.paletteColors[index] : -1;
        }

        /** 该格是否是原木/木头（树干）。 */
        boolean isLogAt(int y, int x, int z) {
            int index = this.storage == null ? 0 : this.storage.get((y << 4 | z) << 4 | x);
            return index >= 0 && index < this.paletteLogs.length && this.paletteLogs[index];
        }
    }

    /** 方块注册名 → RGB；空气与非方块返回 {@code -1}。 */
    private static int blockColor(String name) {
        Integer cached = BLOCK_COLORS.get(name);
        if (cached != null) {
            return cached;
        }
        int rgb = -1;
        try {
            if (name.endsWith(":water")) {
                // 水体：所有 level 的流动水也序列化成 minecraft:water，名字后缀即可判定
                rgb = FLAG_WATER | (MapColor.WATER.col & 0xFFFFFF);
            } else if (!name.endsWith(":air")) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
                if (block != null) {
                    MapColor color = block.defaultMapColor();
                    rgb = color == null ? 0x7F7F7F : color.col;
                    if (rgb == 0) {
                        rgb = 0x7F7F7F;
                    }
                    rgb = adjustMapColor(rgb);
                }
            }
        } catch (Throwable t) {
            rgb = -1;
        }
        BLOCK_COLORS.put(name, rgb);
        return rgb;
    }

    /**
     * 校正几个不适合直接当渲染色的 {@link MapColor}。
     *
     * <p>{@code MapColor.PLANT}（树叶）是 {@code 0x007C00}：几乎纯绿、比真实树叶纹理暗得多，
     * 远端森林整片刷上这个颜色就是一块过饱和的死绿。同一个色相提到纹理感正常的绿。
     */
    public static int adjustMapColor(int rgb) {
        return (rgb & 0xFFFFFF) == 0x007C00 ? 0x5C8C3C : rgb;
    }
}
