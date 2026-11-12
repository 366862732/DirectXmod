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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
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
     * 地表颜色解析失败时的兜底色（中性灰）。
     *
     * <p>和渲染端 {@code Dx12LodRenderer.FALLBACK_COLOR} 取同一个值：高度有效但颜色
     * 未知的列不能整条丢弃，否则一整片远景会因为 sections 解析失败而完全消失。
     */
    public static final int FALLBACK_COLOR = 0x8A8A8A;

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
     * {@code palette[index]} 是该方块的 RGB（{@code -1} 表示空气/透明），
     * {@code paletteNames[index]} 是该方块的注册名（{@code null} 表示未知/空气）。
     * 整块数据约 16×16×384 项，只在建网时临时存在，不进入长期缓存。
     */
    public record VoxelChunk(int minY, int height, int topY,
                             int[] palette, String[] paletteNames, short[] blocks) {
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

        /** 取某格方块的注册名（{@code null} = 空气/未解析），供纹理系统查 sprite。 */
        public String blockName(int x, int y, int z) {
            int ly = y - this.minY;
            if (ly < 0 || ly >= this.height || x < 0 || x > 15 || z < 0 || z > 15) {
                return null;
            }
            int idx = this.blocks[(ly * 16 + z) * 16 + x] & 0xFFFF;
            if (idx >= this.paletteNames.length) {
                return null;
            }
            return this.paletteNames[idx];
        }
    }

    /** 方块注册名 → RGB（{@code -1} = 空气或无法解析），避免每列都做一次注册表查询。 */
    private static final Map<String, Integer> BLOCK_COLORS = new ConcurrentHashMap<>();

    /**
     * 方块注册名 → 图集 UV 矩形 {@code [u0, v0, u1, v1]}（{@code 0..1}，图集空间）。
     * 每次 {@link #prepare(ClientLevel)} 时从 {@link net.minecraft.client.resources.model.sprite.AtlasManager}
     * 刷新；mesher 据此把方块面 0..1 UV 映射到图集坐标。
     *
     * <p>值是 6 个 UV 矩形（{@code [u0, v0, u1, v1]}），下标为
     * {@link net.minecraft.core.Direction#ordinal()}：0=DOWN 1=UP 2=NORTH 3=SOUTH
     * 4=WEST 5=EAST。照搬 voxy 的「每面独立贴图」——草地侧面是草皮+泥土、原木侧面是
     * 树皮、顶面才是年轮，若六面共用一个矩形会全部用成顶面贴图。
     *
     * <p>渲染线程写、后台建网线程读，用 {@link ConcurrentHashMap} 保证可见性。
     */
    private final Map<String, float[][]> spriteUvMap = new ConcurrentHashMap<>();
    /**
     * 方块注册名 → 6 个面的染色值（{@code 0xRRGGBB}，{@code 0xFFFFFF} 表示不染色）。
     *
     * <p>草方块顶面、草、树叶、甘蔗等贴图本身是灰白色，颜色来自生物群系染色
     * （{@code BlockColors}）。voxy 在着色器里用 {@code tintingFunction} 给灰白像素补色；
     * 我们改成在烘焙时逐面查 {@code materialInfo().tintIndex()}，只给真的声明了
     * tintindex 的面写染色值，其余面写白——原木侧面因此不会被染绿。
     */
    private final Map<String, int[]> spriteTintMap = new ConcurrentHashMap<>();
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

    /*
     * 采样诊断计数：渲染端每次重建会采样十几万个角点，全部 miss 时只靠
     * 「已采样=0」看不出到底是数据没读到、高度解析失败、还是颜色解析失败。
     * 三个计数分别对应 sample() 的三个失败出口，随诊断日志一起打印。
     */
    private final AtomicLong statNoChunk = new AtomicLong();
    private final AtomicLong statNoHeight = new AtomicLong();
    private final AtomicLong statNoColor = new AtomicLong();
    private final AtomicLong statHit = new AtomicLong();

    /*
     * 持久化存储的命中诊断：进世界后第一轮读盘若几乎全是 miss，说明 store 没被
     * 复用（目录不对/版本变了/region 大小对不上），下一轮仍要解全部 NBT；
     * 全是 hit 则说明上次退出时写进去的数据确实生效了。随 [lod] 日志一起打印。
     */
    private final AtomicLong statVoxelStoreHit = new AtomicLong();
    private final AtomicLong statVoxelStoreMiss = new AtomicLong();
    private final AtomicLong statColumnStoreHit = new AtomicLong();
    private final AtomicLong statColumnStoreMiss = new AtomicLong();

    /**
     * 列数据加载版本：每成功读完一个区块（有数据或确认无数据）就递增。
     *
     * <p>渲染端以前用 {@code columns.size()} 判断「有没有新数据」。这个值在缓存达到上限、
     * 或大量区块同时命中/淘汰时会保持不变甚至回落，高度场就会永久冻结在进世界第一秒
     * 采到的 47% 角点上 —— 日志里的 {@code 已采样=195028/410881} 后再也没有第二条
     * {@code 网格=} 记录，远景一片空洞。改成单调递增的版本号，任何一次读盘完成都
     * 能可靠触发下一次重建。
     */
    private final AtomicLong columnVersion = new AtomicLong();

    /** 区块 key → 地表数据（{@link #MISSING} 表示整块读不到）。 */
    private final Map<Long, ChunkColumns> columns = Collections.synchronizedMap(
        new LinkedHashMap<Long, ChunkColumns>(2048, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, ChunkColumns> eldest) {
                return size() > MAX_CHUNKS;
            }
        });

    /**
     * 已确认没有高度数据的区块（region 文件不存在 / 区块未生成）→ 登记时刻（纳秒）。
     *
     * <p>以前是永久拉黑：进世界第一秒里 region 还没落盘、或服务端正往该 region 追加区块，
     * 被记成「无数据」的区块整个会话都不会再读，远景就永久缺一块。改成带 TTL 的可重试：
     * 过期后重新入队，真没数据的区块只是多一次 {@code Files.isRegularFile}，很便宜。
     */
    private static final long MISSING_TTL_NS = 15_000_000_000L;
    private final Map<Long, Long> missing = Collections.synchronizedMap(
        new LinkedHashMap<Long, Long>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
                return size() > MAX_MISSING;
            }
        });

    /** 当前存档的 region 目录；{@code null} 表示不可用（多人游戏等）。 */
    private volatile Path regionDir;
    private volatile ResourceKey<Level> dimension;
    private volatile int minY;
    private volatile int height;
    private volatile int bits;

    private static final Logger LOGGER = LoggerFactory.getLogger("Dx12LodCache");
    /** region 文件 (size, mtime) 的短期缓存，避免每个区块都 stat 一次。 */
    private static final long REGION_STAT_TTL_NS = 1_000_000_000L;
    /**
     * 图集 UV 映射刷新失败后的重试间隔。
     *
     * <p>图集通常在进世界后一两秒内加载完，之前每次 {@code prepare()} 都重刷一遍，
     * 等于每帧遍历上千个方块、烘焙全部模型部件——启动阶段白白吃掉大量帧时间。
     * 失败后隔半秒再试，最多多等半秒就绪，代价可以忽略。
     */
    private static final long SPRITE_UV_RETRY_NS = 500_000_000L;

    /** 解码后 section 的持久化 KV 存储；{@code null} = 不可用（多人游戏/开关关闭/打开失败）。 */
    private volatile LodSectionStore store;
    /**
     * 高度场地表列的持久化 KV 存储（与 {@link #store} 分开一个目录）。
     *
     * <p>体素负载按区块存的是整块 16×16×384 的方块下标（压缩后约 7.6KB），地表列只需要
     * 256 组 (高度, 颜色)（约 0.5KB）。两种负载长度完全不同，混在同一个 key 空间里会
     * 互相覆盖，所以各用一份存储。
     */
    private volatile LodSectionStore columnStore;
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
            // 图集可能晚于首次 prepare 才加载好（加载阶段 getAtlasManager 抛异常被吞掉），
            // 此时 spriteUvReady 仍为 false、后台建网被闸门挡住。带退避地补刷直到就绪，
            // 避免永远停在「没有 UV」状态，也不至于每帧重刷整个方块注册表。
            if (!this.spriteUvReady) {
                refreshSpriteUvIfDue();
            }
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
        this.columnVersion.incrementAndGet();
        openStore(dir);
        // 体素网格是按方块数据建的，换世界/维度后全部失效
        LodVoxelCache.getInstance().clear();
        // 刷新方块→图集 UV 映射（atlas 在渲染线程才可用）
        refreshSpriteUvIfDue();
    }

    /**
     * 按 {@link #SPRITE_UV_RETRY_NS} 退避地尝试刷新图集 UV 映射；已就绪时直接返回。
     *
     * <p>只在渲染线程调用。失败（图集未加载）时记下下次允许重试的时刻，
     * 成功则清掉退避。
     */
    private void refreshSpriteUvIfDue() {
        if (this.spriteUvReady) {
            return;
        }
        long now = System.nanoTime();
        if (now < this.spriteUvRetryAtNanos) {
            return;
        }
        refreshSpriteUv();
        this.spriteUvRetryAtNanos = this.spriteUvReady ? 0L : System.nanoTime() + SPRITE_UV_RETRY_NS;
    }

    /**
     * 从 {@link net.minecraft.client.resources.model.sprite.AtlasManager} 批量查询
     * {@code LOCATION_BLOCKS} 图集里所有方块的 sprite UV 矩形，写入 {@link #spriteUvMap}。
     *
     * <p>必须在渲染线程调用（{@code prepare()} 已经在渲染线程里）。
     * 失败时保留旧值，不影响渲染。
     */
    /**
     * 把方块名映射成六个方向各自的图集 sprite UV 矩形。
     *
     * <p>26.2 里方块 sprite 名是<b>贴图路径</b>（如 {@code block/grass_block_top}），
     * 不是方块注册名（{@code minecraft:grass_block}）。照搬 voxy 的 {@code bakeBlockModel}
     * 思路：走 {@code BlockStateModelSet.get(state).collectParts()} → 逐方向遍历 quad →
     * {@code materialInfo().sprite()} 拿到该面的真实 sprite，取其图集 UV 矩形。
     *
     * <p>按 {@link net.minecraft.core.Direction#ordinal()} 下标存入 6 个矩形，
     * 使草地侧面/顶面、原木树皮/年轮各不相同（voxy 的每面独立贴图）。
     * 某方向没有 quad 时回填顶面（UP）或任意已有面，避免该面退化成 0..1 乱采样。
     */
    private void refreshSpriteUv() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            var modelManager = minecraft.getModelManager();
            if (modelManager == null) {
                return;
            }
            var modelSet = modelManager.getBlockStateModelSet();
            var blockColors = minecraft.getBlockColors();
            for (Block block : BuiltInRegistries.BLOCK) {
                // 单个方块的模型异常不能拖垮整张映射：以前这里没有逐方块兜底，
                // 一个 mod 方块在 collectParts 里抛异常就让整次刷新失败、
                // spriteUvReady 永远为 false，体素建网闸门也就永远打不开。
                try {
                    refreshBlockSpriteUv(block, modelSet, blockColors);
                } catch (Throwable ignored) {
                    // 跳过这个方块，其余方块继续
                }
            }
            if (!this.activeLogged && LOGGER.isInfoEnabled()) {
                LOGGER.info("[lod] 方块 sprite UV 映射已刷新：{} 项（每面独立，BlockStateModelSet）",
                    this.spriteUvMap.size());
                this.activeLogged = true;
            }
            this.spriteUvReady = !this.spriteUvMap.isEmpty();
        } catch (Throwable t) {
            LOGGER.warn("[lod] 刷新方块 sprite UV 映射失败（图集可能未加载）", t);
        }
    }

    /**
     * 刷新单个方块的六面 sprite UV 与染色值。
     *
     * <p>模型查询/烘焙只依赖默认方块状态，不碰世界，可以在渲染线程安全调用。
     * 异常由 {@link #refreshSpriteUv()} 的逐方块 try/catch 吞掉，保证一个坏模型
     * 不会让整张映射（以及体素建网闸门）永久失效。
     */
    private void refreshBlockSpriteUv(Block block, BlockStateModelSet modelSet,
                                      BlockColors blockColors) {
        var key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return;
        }
        String blockName = key.toString();
        var state = block.defaultBlockState();
        if (state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE) {
            return;
        }
        var model = modelSet.get(state);
        if (model == null) {
            return;
        }
        List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(net.minecraft.util.RandomSource.create(42L), parts);

        float[][] rects = new float[6][];
        int[] tints = new int[6];
        Arrays.fill(tints, 0xFFFFFF);
        for (var part : parts) {
            for (var dir : net.minecraft.core.Direction.values()) {
                int idx = dir.ordinal();
                if (rects[idx] != null) {
                    continue; // 该方向已取到贴图
                }
                var quads = part.getQuads(dir);
                if (quads == null) {
                    continue;
                }
                for (var quad : quads) {
                    var materialInfo = quad.materialInfo();
                    if (materialInfo == null) {
                        continue;
                    }
                    var sprite = materialInfo.sprite();
                    if (sprite == null) {
                        continue;
                    }
                    rects[idx] = new float[]{
                        sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()
                    };
                    tints[idx] = tintColor(blockColors, state, materialInfo.tintIndex());
                    break;
                }
            }
        }
        // 回填：没有 quad 的方向借用顶面（UP=1）或任意非空面
        float[] fallback = rects[1];
        if (fallback == null) {
            for (float[] r : rects) {
                if (r != null) {
                    fallback = r;
                    break;
                }
            }
        }
        if (fallback == null) {
            return; // 完全没有贴图（如屏障），跳过
        }
        for (int i = 0; i < 6; i++) {
            if (rects[i] == null) {
                rects[i] = fallback;
            }
        }
        this.spriteUvMap.put(blockName, rects);
        this.spriteTintMap.put(blockName, tints);
    }

    /** 首次刷新时打一条日志，避免每帧都打。 */
    private boolean activeLogged;

    /** 方块→图集 UV 映射是否已就绪。渲染线程写、后台建网线程读，volatile 保证可见性。 */
    private volatile boolean spriteUvReady;

    /** 下次允许重试图集 UV 刷新的时刻（{@link System#nanoTime()}）；0 = 立即允许。 */
    private volatile long spriteUvRetryAtNanos;

    /**
     * 取方块名对应的 6 面图集 UV 矩形数组（下标 = {@code Direction.ordinal()}，
     * 每项 {@code [u0, v0, u1, v1]}）。不存在返回 {@code null}（调用方退回顶点内 0..1 UV）。
     */
    public float[][] getSpriteUv(String blockName) {
        if (blockName == null) {
            return null;
        }
        return this.spriteUvMap.get(blockName);
    }

    /**
     * 取方块名对应的 6 面生物群系染色值（{@code 0xRRGGBB}，下标同 {@link #getSpriteUv}）。
     *
     * <p>不染色的面是 {@code 0xFFFFFF}（着色器里乘 1，等价于关闭）。
     * 不存在返回 {@code null}。
     */
    public int[] getSpriteTint(String blockName) {
        if (blockName == null) {
            return null;
        }
        return this.spriteTintMap.get(blockName);
    }

    /**
     * 查某个面（模型里声明的 {@code tintindex}）的染色值。
     *
     * <p>{@code tintIndex < 0} 表示这个面本来就不该染色（原木、石头），返回白。
     * 群系相关的染色这里用 {@link net.minecraft.client.color.block.BlockTintSource#color}
     * 的「非世界默认色」——它不依赖 {@code BlockAndTintGetter}，后台线程也能安全调用；
     * 结果就是原版草色/叶色的标准值（不是按群系插值的，但绝不会是灰白）。
     */
    private static int tintColor(net.minecraft.client.color.block.BlockColors blockColors,
                                 net.minecraft.world.level.block.state.BlockState state,
                                 int tintIndex) {
        if (blockColors == null || tintIndex < 0) {
            return 0xFFFFFF;
        }
        var source = blockColors.getTintSource(state, tintIndex);
        if (source == null) {
            return 0xFFFFFF;
        }
        int argb = source.color(state);
        int rgb = argb & 0xFFFFFF;
        if (rgb == 0xFFFFFF || rgb == 0) {
            return 0xFFFFFF; // -1 / 0：不染色
        }
        return rgb;
    }

    /**
     * 方块→图集 UV 映射是否已就绪。
     *
     * <p>后台建网线程据此做闸门：{@code refreshSpriteUv()} 只在渲染线程 {@code prepare()}
     * 时按半秒退避重试，启动早期尚未刷新。若此时就建网，整块 UV 会落到未命中的回退位置
     * （[0,0,0,0]），之后映射补齐再往里走就会「外圈先闪一片、再跳变」。闸门保证
     * 只在映射就绪后才把区块烘成带 UV 的网格。
     */
    public boolean isSpriteUvReady() {
        return this.spriteUvReady;
    }

    /**
     * 打开当前维度的持久化 section 存储。目录放在该维度 region 目录的旁边
     * （主世界 {@code <world>/gl4dx12_lod}，下界 {@code <world>/DIM-1/gl4dx12_lod}），
     * 随存档一起复制/删除；打开失败只降级为「每次都重新解析 NBT」，不影响渲染。
     */
    private void openStore(Path regionDir) {
        closeStore();
        // 换维度后重新统计，避免把上一个维度的命中数混进来
        this.statVoxelStoreHit.set(0);
        this.statVoxelStoreMiss.set(0);
        this.statColumnStoreHit.set(0);
        this.statColumnStoreMiss.set(0);
        Path parent = regionDir.getParent();
        Path base = parent == null ? regionDir : parent;
        Path dir = base.resolve("gl4dx12_lod");
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
        Path columnDir = base.resolve("gl4dx12_lod_columns");
        try {
            this.columnStore = LodSectionStore.open(columnDir);
            LOGGER.info("[lod] Column store opened: {} ({} entries, {} KB)",
                columnDir, this.columnStore.entryCount(), this.columnStore.sizeBytes() / 1024);
        } catch (Throwable t) {
            this.columnStore = null;
            LOGGER.warn("[lod] Failed to open column store {}: {}", columnDir, t.toString());
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
        LodSectionStore columns = this.columnStore;
        this.columnStore = null;
        if (columns != null) {
            try {
                columns.flush();
                LOGGER.info("[lod] Column store closed: {} entries, {} KB",
                    columns.entryCount(), columns.sizeBytes() / 1024);
                columns.close();
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
        this.spriteUvMap.clear();
        this.spriteTintMap.clear();
        this.spriteUvReady = false;
        this.spriteUvRetryAtNanos = 0L;
        this.columnVersion.incrementAndGet();
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
     *
     * <p>只有区块缺失或高度不可用时才返回 {@link #NO_SAMPLE}。颜色解析失败<b>不再</b>
     * 拖累高度：以前这里是「颜色未知即整条作废」，而 {@code decodeColumns()} 只要
     * sections 解不出来就会把整块 256 列全留 {@code -1} —— 远景于是连一个角点都统计
     * 不到（日志里的 {@code 已采样=0/148225}），整张高度场是空的。颜色交给着色器
     * 按坡度/高度兜底，至少地形轮廓是对的。
     */
    public long sample(int blockX, int blockZ) {
        ChunkColumns data = lookup(blockX, blockZ);
        if (data == null) {
            this.statNoChunk.incrementAndGet();
            return NO_SAMPLE;
        }
        int index = (blockX & 15) + (blockZ & 15) * 16;
        short height = data.heights[index];
        if (height == UNAVAILABLE) {
            this.statNoHeight.incrementAndGet();
            return NO_SAMPLE;
        }
        int rgb = data.colors[index];
        if (rgb < 0) {
            this.statNoColor.incrementAndGet();
            rgb = FALLBACK_COLOR;
        } else {
            this.statHit.incrementAndGet();
        }
        return ((long) height << 32) | (rgb & 0xFFFFFFFFL);
    }

    /** 清零 {@link #sample(int, int)} 的诊断计数（渲染端每次重建前调用）。 */
    public void resetSampleStats() {
        this.statNoChunk.set(0);
        this.statNoHeight.set(0);
        this.statNoColor.set(0);
        this.statHit.set(0);
    }

    /** {@link #sample(int, int)} 的失败构成，形如 {@code 缺区块=0 无高度=0 缺颜色=148225 命中=0}。 */
    public String sampleStats() {
        return "缺区块=" + this.statNoChunk.get()
            + " 无高度=" + this.statNoHeight.get()
            + " 缺颜色=" + this.statNoColor.get()
            + " 命中=" + this.statHit.get();
    }

    /**
     * 持久化存储的命中构成，形如 {@code 体素[命中=120 未命中=4] 列[命中=8000 未命中=12]}。
     *
     * <p>命中说明上一轮写进 {@code gl4dx12_lod*} 的数据被复用，省掉了 NBT 解析；
     * 若进世界后第一轮几乎全是未命中，多半是 store 目录/版本对不上，缓存没生效。
     */
    public String storeStats() {
        return "体素[命中=" + this.statVoxelStoreHit.get()
            + " 未命中=" + this.statVoxelStoreMiss.get() + "]"
            + " 列[命中=" + this.statColumnStoreHit.get()
            + " 未命中=" + this.statColumnStoreMiss.get() + "]";
    }

    private ChunkColumns lookup(int blockX, int blockZ) {
        if (this.regionDir == null) {
            return null;
        }
        long key = ChunkPos.pack(blockX >> 4, blockZ >> 4);
        ChunkColumns data = this.columns.get(key);
        if (data == null) {
            if (!isKnownMissing(key)) {
                request(key);
            }
            return null;
        }
        return data.heights.length == 0 ? null : data;
    }

    /** 该区块是否仍在「确认无数据」的 TTL 内；过期则移除并允许重新请求。 */
    private boolean isKnownMissing(long key) {
        Long stamp = this.missing.get(key);
        if (stamp == null) {
            return false;
        }
        if (System.nanoTime() - stamp >= MISSING_TTL_NS) {
            this.missing.remove(key);
            return false;
        }
        return true;
    }

    /** 已缓存且可读的区块数（调试用）。 */
    public int cachedCount() {
        return this.columns.size();
    }

    /**
     * 列数据加载版本：每成功读完一个区块（含确认「该区块不存在」）递增。
     * 渲染端用它作为高度场重建的触发条件，避免缓存数量饱和后几何永久冻结。
     */
    public long columnVersion() {
        return this.columnVersion.get();
    }

    /**
     * 仍在排队或正在读盘的区块数。
     *
     * <p>渲染端用它判断「远景数据还在补」：刚开启 LOD 时整张网格的第一次采样几乎
     * 全是 miss，此时 {@link #cachedCount()} 可能已经有一批旧数据、看起来不再变化，
     * 只按它判断就会把几何永久冻结在「几乎没有地形」的状态。pending 归零才说明
     * 能读到的数据都读完了，不必再重建。
     */
    public int pendingCount() {
        return this.pending.size();
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
                // 先解码再摘掉 pending：反过来的话，解码还没跑完 pendingCount() 就已经归零，
                // 渲染端「pending > 0 就重建」的判据失效，几何会冻结在第一次全 miss 的状态。
                ChunkColumns decoded;
                try {
                    decoded = readChunk(key, dir, minY, bits);
                } finally {
                    this.pending.remove(key);
                }
                // decoded == null 表示本次 IO 失败（如服务端正在写该 region），留待下次重试
                if (decoded == null || !dir.equals(this.regionDir) || !dimension.equals(this.dimension)) {
                    continue;
                }
                if (decoded == MISSING) {
                    // 无数据只登记在 missing 里，不占用 columns 的容量
                    this.missing.put(key, System.nanoTime());
                } else {
                    this.columns.put(key, decoded);
                    this.missing.remove(key);
                }
                // 有数据或确认无数据都算一次「列数据状态变化」：渲染端据此重建高度场。
                // 只在有数据时递增的话，最后一批全是未生成区块时会漏掉一次重建。
                this.columnVersion.incrementAndGet();
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
        Path regionFile = regionFile(dir, chunkX, chunkZ);
        long[] stat = regionStat(regionFile, ChunkPos.pack(chunkX >> 5, chunkZ >> 5));
        LodSectionStore columns = this.columnStore;
        if (columns != null && stat != null) {
            // 命中列存储就不必再解一遍 NBT：一次重建要上万区块，NBT 解析是主要瓶颈
            byte[] payload = columns.read(key, stat[0], stat[1]);
            ChunkColumns cached = LodChunkCodec.decodeColumns(payload);
            if (cached != null) {
                this.statColumnStoreHit.incrementAndGet();
                return cached;
            }
            this.statColumnStoreMiss.incrementAndGet();
        }
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
            ChunkColumns decoded = new ChunkColumns(heights,
                decodeColumns(tag.getListOrEmpty("sections"), heights));
            if (columns != null && stat != null) {
                byte[] payload = LodChunkCodec.encodeColumns(decoded);
                if (payload != null) {
                    columns.write(key, payload, stat[0], stat[1]);
                }
            }
            return decoded;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 读一个区块的完整方块数据，供 3D 体素 LOD 建网。返回 {@code null} 表示不可用
     * （region 不存在、区块未生成或 IO 失败）。
     *
     * <p>优先命中 {@link LodSectionStore}：解码后的调色板+索引比 NBT 小得多，也省掉
     * 逐 section 查注册表的开销。存储里记着 region 文件的 {@code (size, mtime)}，只在
     * 大小变了（文件被重写/追加）时当 miss；mtime 单独变化多半只是自动存档顺手刷新了
     * 时间戳，按它判 miss 等于一次存档作废整个 region 的缓存。
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
                    this.statVoxelStoreHit.incrementAndGet();
                    return cached;
                }
            }
            this.statVoxelStoreMiss.incrementAndGet();
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
        String[] paletteNames = new String[256];
        palette[0] = -1;
        paletteNames[0] = null;
        int paletteSize = 1;
        // 方块名 → 区块级调色板下标：同名方块合并，避免调色板爆炸
        Map<String, Integer> nameToIndex = new HashMap<>();

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
                String name = p < data.paletteNames.length ? data.paletteNames[p] : null;
                // 非实心方块（草、花、火把、地毯、台阶……）在体素 LOD 里一律当空气：
                // 它们在远处根本看不见，算进去只会让一个平原区块凭空多出上千个面。
                if (color == -1 || !data.paletteSolid[p]) {
                    remap[p] = 0;
                    continue;
                }
                // 同名方块合并（同一注册名共享同一调色板槽），避免调色板无限增长
                int found = -1;
                if (name != null) {
                    Integer cached = nameToIndex.get(name);
                    if (cached != null) {
                        found = cached;
                    }
                }
                if (found < 0) {
                    if (paletteSize == palette.length) {
                        int newCap = paletteSize * 2;
                        palette = Arrays.copyOf(palette, newCap);
                        paletteNames = Arrays.copyOf(paletteNames, newCap);
                    }
                    found = paletteSize;
                    palette[paletteSize] = color;
                    paletteNames[paletteSize] = name;
                    if (name != null) {
                        nameToIndex.put(name, paletteSize);
                    }
                    paletteSize++;
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
        return new VoxelChunk(minY, height, topY,
            Arrays.copyOf(palette, paletteSize),
            Arrays.copyOf(paletteNames, paletteSize),
            blocks);
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
     * <p><b>上色</b>：取的高度必须和几何用的高度是同一列（压过树干的地面顶面），
     * 颜色才是这张高度场真正画出来的那层方块的色。以前用 {@code WORLD_SURFACE}
     * （含树冠）取色、却按地面高度建模：森林地区的角点颜色在树叶的深绿和草地绿之间
     * 逐点跳变，跨几十方块的大格插值后就是山坡上一道道放射状的深绿色涂抹。
     *
     * <p>任一列解不出来就留 {@code -1}，由渲染端按高度给近似色。
     */
    private static int[] decodeColumns(ListTag sections, short[] heights) {
        int[] colors = new int[COLUMNS];
        Arrays.fill(colors, -1);
        if (sections == null || sections.size() == 0) {
            return colors;
        }
        // sectionY → 已解好的 palette/storage，同一个区块内复用
        Map<Integer, SectionData> decoded = new HashMap<>();
        for (int i = 0; i < COLUMNS; i++) {
            stripLogsAt(decoded, sections, heights, i);
            // 高度图是「首个空气方块的 y」，地表方块在它下面一格；stripLogsAt 已经把
            // heights[i] 从树干顶压回地面，这里直接用同一份高度
            int surface = heights[i];
            if (surface == UNAVAILABLE) {
                continue;
            }
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
        String[] names = new String[size];
        for (int p = 0; p < size; p++) {
            CompoundTag entry = palette.getCompoundOrEmpty(p);
            String name = entry.getStringOr("Name", "minecraft:air");
            colors[p] = blockColor(name);
            logs[p] = isLogName(name);
            solid[p] = isSolidBlock(name);
            names[p] = name;
        }
        if (size == 1) {
            // 整节只有一种方块，磁盘上没有 data
            return new SectionData(colors, logs, solid, names, null);
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
        return new SectionData(colors, logs, solid, names, storage);
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
                               String[] paletteNames, SimpleBitStorage storage) {
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
        int color = rgb & 0xFFFFFF;
        switch (color) {
            case 0x007C00:
                // MapColor.PLANT（树叶）：几乎纯绿，比真实树叶纹理暗得多，整片刷上去是死绿
                return 0x5C8C3C;
            case 0x7FB238:
                // MapColor.GRASS：荧光黄绿。真实草方块顶面纹理带大量明暗，平均下来更暗更灰
                return 0x6E9B46;
            case 0xFFFFFF:
                // MapColor.SNOW：纯白叠加远景光照后直接过曝，整片冰原糊成一张白纸
                return 0xE4EBF1;
            case 0xA0A0FF:
                // MapColor.ICE：偏紫的淡蓝，压成冰块的灰蓝
                return 0x9FB4CF;
            default:
                return rgb;
        }
    }
}
