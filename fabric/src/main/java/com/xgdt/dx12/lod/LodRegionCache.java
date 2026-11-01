package com.xgdt.dx12.lod;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
    /** {@link #sample(int, int)} 的「无数据」返回值。 */
    public static final long NO_SAMPLE = (long) Integer.MIN_VALUE << 32;

    /**
     * 一个区块的地表数据。{@code heights[i]} 是第 i 列最高方块的 y（{@link #UNAVAILABLE}
     * 表示该列没有数据），{@code colors[i]} 是第 i 列地表方块的 RGB（{@code -1} 表示未知）。
     * 列下标 = {@code (x & 15) + (z & 15) * 16}。
     */
    public record ChunkColumns(short[] heights, int[] colors) {
    }

    /** 方块注册名 → RGB（{@code -1} = 空气或无法解析），避免每列都做一次注册表查询。 */
    private static final Map<String, Integer> BLOCK_COLORS = new ConcurrentHashMap<>();

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
    private volatile int bits;

    private LodRegionCache() {
    }

    public static LodRegionCache getInstance() {
        return INSTANCE;
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
        this.bits = Mth.ceillog2(level.getHeight() + 1);
        this.columns.clear();
        this.missing.clear();
        this.pending.clear();
        this.queue.clear();
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
    }

    /**
     * 取某个方块位置所在列的地表高度（最高非空气方块的 y）。尚未读到则返回
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
        int chunkX = ChunkPos.getX(key);
        int chunkZ = ChunkPos.getZ(key);
        Path file = dir.resolve("r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
        // 先判存在：不存在的 region 不去打开
        if (!Files.isRegularFile(file)) {
            return MISSING;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer offsetBytes = ByteBuffer.allocate(4);
            int index = (chunkX & 31) + (chunkZ & 31) * 32;
            if (channel.read(offsetBytes, (long) index * 4L) != 4) {
                return MISSING;
            }
            int offset = offsetBytes.flip().getInt();
            int sectorNumber = offset >> 8 & 0xFFFFFF;
            int numSectors = offset & 0xFF;
            if (sectorNumber < 2 || numSectors == 0) {
                return MISSING;
            }
            ByteBuffer data = ByteBuffer.allocate(numSectors * 4096);
            if (channel.read(data, (long) sectorNumber * 4096L) <= 0) {
                return MISSING;
            }
            data.flip();
            if (data.remaining() < 5) {
                return MISSING;
            }
            int length = data.getInt();
            byte versionId = data.get();
            int streamLength = length - 1;
            if (streamLength < 0) {
                return MISSING;
            }
            boolean external = (versionId & 0x80) != 0;
            RegionFileVersion compression = RegionFileVersion.fromId(external ? versionId & 0x7F : versionId);
            if (compression == null || compression == RegionFileVersion.VERSION_CUSTOM) {
                return MISSING;
            }
            InputStream raw;
            if (external) {
                Path externalFile = dir.resolve("c." + chunkX + "." + chunkZ + ".mcc");
                if (!Files.isRegularFile(externalFile)) {
                    return MISSING;
                }
                raw = Files.newInputStream(externalFile);
            } else {
                if (streamLength > data.remaining()) {
                    return MISSING;
                }
                byte[] payload = new byte[streamLength];
                data.get(payload);
                raw = new ByteArrayInputStream(payload);
            }
            CompoundTag tag;
            try (DataInputStream in = new DataInputStream(compression.wrap(raw))) {
                tag = NbtIo.read(in);
            }
            if (tag == null) {
                return MISSING;
            }
            Optional<long[]> rawHeights = tag.getCompoundOrEmpty("Heightmaps")
                .getLongArray(Heightmap.Types.WORLD_SURFACE.getSerializationKey());
            if (rawHeights.isEmpty()) {
                return MISSING;
            }
            short[] heights = unpack(rawHeights.get(), minY, bits);
            if (heights == MISSING.heights) {
                return MISSING;
            }
            return new ChunkColumns(heights, decodeColors(tag, heights));
        } catch (Throwable t) {
            // IO 失败（region 正在被写/暂时不可读）：不缓存，下次重建再试
            return null;
        }
    }

    /**
     * 解包 {@code Heightmaps.WORLD_SURFACE}：每位表示「最高方块 + 1 − minY」。
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
            int surface = storage.get(i) + minY - 1;
            result[i] = surface >= minY ? (short) surface : UNAVAILABLE;
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 地表方块颜色
    // ------------------------------------------------------------------

    /**
     * 借助高度图定位每一列的地表方块，只解该列所在的 section，避免整块 4096 项全量扫描。
     * 任一列解不出来就留 {@code -1}，由渲染端按高度给近似色。
     */
    private static int[] decodeColors(CompoundTag tag, short[] heights) {
        int[] colors = new int[COLUMNS];
        Arrays.fill(colors, -1);
        ListTag sections = tag.getListOrEmpty("sections");
        int sectionCount = sections.size();
        if (sectionCount == 0) {
            return colors;
        }
        // sectionY → 已解好的 palette/storage，同一个区块内复用
        Map<Integer, SectionData> decoded = new HashMap<>();
        for (int i = 0; i < COLUMNS; i++) {
            int surface = heights[i];
            if (surface == UNAVAILABLE) {
                continue;
            }
            // 磁盘上的 sections[*].Y 是绝对 section 坐标（SectionPos.blockToSectionCoord）
            int sectionY = Math.floorDiv(surface, 16);
            SectionData data = decoded.computeIfAbsent(sectionY,
                y -> findSection(sections, y));
            if (data == null) {
                continue;
            }
            int color = data.colorAt((surface & 15), i & 15, i >> 4);
            if (color == -1) {
                // 地表方块是空气/未解析（高度图与方块错位）：在 section 内向下找第一个实心方块
                for (int y = (surface & 15) - 1; y >= 0 && color == -1; y--) {
                    color = data.colorAt(y, i & 15, i >> 4);
                }
            }
            colors[i] = color;
        }
        return colors;
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
        for (int p = 0; p < size; p++) {
            CompoundTag entry = palette.getCompoundOrEmpty(p);
            colors[p] = blockColor(entry.getStringOr("Name", "minecraft:air"));
        }
        if (size == 1) {
            // 整节只有一种方块，磁盘上没有 data
            return new SectionData(colors, null);
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
        return new SectionData(colors, storage);
    }

    /** 已解析的 section：{@code storage == null} 表示整节同一种方块。 */
    private record SectionData(int[] paletteColors, SimpleBitStorage storage) {
        /** {@code y}/{@code x}/{@code z} 均为 section 内的 0..15 坐标。 */
        int colorAt(int y, int x, int z) {
            int index = this.storage == null ? 0 : this.storage.get((y << 4 | z) << 4 | x);
            return index >= 0 && index < this.paletteColors.length ? this.paletteColors[index] : -1;
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
            if (!name.endsWith(":air")) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
                if (block != null) {
                    MapColor color = block.defaultMapColor();
                    rgb = color == null ? 0x7F7F7F : color.col;
                    if (rgb == 0) {
                        rgb = 0x7F7F7F;
                    }
                }
            }
        } catch (Throwable t) {
            rgb = -1;
        }
        BLOCK_COLORS.put(name, rgb);
        return rgb;
    }
}
