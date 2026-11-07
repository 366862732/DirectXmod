package com.xgdt.dx12.lod;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 3D 体素 LOD 的区块网格缓存。
 *
 * <p>读 region 文件 + 网格化一块 16×16×384 的区块要几十毫秒，绝不能放在渲染线程上。
 * 这里用三条后台线程按需建网，主线程只取已经建好的结果；没建好的先跳过，下一帧再取，
 * 于是地形从近往远逐步补齐，不会卡住一帧。
 *
 * <p>顶点坐标是「世界坐标 − 锚点」：着色器里会用 {@code CameraPos} 做相机相对变换，
 * 锚点只在玩家走远 8000 方块时才换一次，所以缓存不会因为正常移动而失效。
 */
public final class LodVoxelCache {

    /** 已建好网格的区块上限。每块典型几十 KB，4096 块约 250 MB 上限，靠 LRU 回收。 */
    private static final int MAX_MESHES = 4096;
    /** 确认读不到数据的区块上限，避免反复重读同一个未生成的区块。 */
    private static final int MAX_MISSING = 32768;
    /** 后台建网线程数。 */
    private static final int WORKERS = 3;
    /** 待建队列上限。 */
    private static final int QUEUE_LIMIT = 16384;
    /** 顶点坐标锚点对齐粒度（方块）。 */
    private static final int ANCHOR_SNAP = 16384;
    /** 相机离锚点超过这个距离就换锚点并重建缓存。 */
    private static final int ANCHOR_SLACK = 8192;

    private record Key(int x, int z, int shift) {
    }

    private final Map<Key, LodVoxelMesher.Mesh> ready = Collections.synchronizedMap(
        new LinkedHashMap<Key, LodVoxelMesher.Mesh>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, LodVoxelMesher.Mesh> eldest) {
                return size() > MAX_MESHES;
            }
        });

    private final Set<Key> missing = Collections.synchronizedSet(
        Collections.newSetFromMap(new LinkedHashMap<Key, Boolean>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, Boolean> eldest) {
                return size() > MAX_MISSING;
            }
        }));

    private final Set<Key> pending = ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor pool;

    /** 已完成建网的数量，主线程据此判断要不要重拼顶点缓冲。 */
    private final AtomicInteger completed = new AtomicInteger();

    /**
     * 缓存代次：换锚点 / 换世界时递增。后台线程建完网后核对代次，避免把旧坐标
     * （旧锚点）或旧世界的网格塞进刚清空的缓存。
     */
    private final AtomicInteger generation = new AtomicInteger();

    /**
     * 顶点坐标的参考锚点（方块）。顶点写成「世界坐标 − 锚点」，这样即使世界坐标很大，
     * float32 也仍然精确到毫米级。锚点只在玩家走远时才换一次，缓存长期有效。
     */
    private volatile int anchorX = Integer.MIN_VALUE;
    private volatile int anchorZ = Integer.MIN_VALUE;

    private LodVoxelCache() {
        // 队列满时直接抛 RejectedExecutionException，由 request() 把 key 从 pending
        // 里摘掉；否则 key 会永远卡在 pending 里，那一块地形再也建不出来。
        this.pool = new ThreadPoolExecutor(WORKERS, WORKERS, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_LIMIT), task -> {
                Thread thread = new Thread(task, "dx12-lod-voxel");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });
        this.pool.allowCoreThreadTimeOut(true);
    }

    private static final LodVoxelCache INSTANCE = new LodVoxelCache();

    public static LodVoxelCache getInstance() {
        return INSTANCE;
    }

    /** 已完成建网的数量；渲染端据此判断要不要重拼顶点缓冲。 */
    public int completedCount() {
        return this.completed.get();
    }

    /** 缓存代次：换世界/维度或锚点漂移时递增，渲染端据此丢弃旧顶点。 */
    public int generation() {
        return this.generation.get();
    }

    public int anchorX() {
        return this.anchorX;
    }

    public int anchorZ() {
        return this.anchorZ;
    }

    /**
     * 保证锚点在相机附近（{@link #ANCHOR_SNAP} 方块对齐）。走远了才换锚点并清空缓存，
     * 因此正常游玩时顶点缓存一直有效。
     */
    public void ensureAnchor(int cameraBlockX, int cameraBlockZ) {
        int x = this.anchorX;
        int z = this.anchorZ;
        if (x != Integer.MIN_VALUE
            && Math.abs(cameraBlockX - x) < ANCHOR_SLACK
            && Math.abs(cameraBlockZ - z) < ANCHOR_SLACK) {
            return;
        }
        synchronized (this) {
            this.anchorX = Math.floorDiv(cameraBlockX, ANCHOR_SNAP) * ANCHOR_SNAP;
            this.anchorZ = Math.floorDiv(cameraBlockZ, ANCHOR_SNAP) * ANCHOR_SNAP;
            this.ready.clear();
            this.missing.clear();
            this.pending.clear();
            this.generation.incrementAndGet();
            this.completed.incrementAndGet();
        }
    }

    /**
     * 取一块区块的网格。还没建好返回 {@code null}，并在后台排入建网任务。
     */
    public LodVoxelMesher.Mesh get(int chunkX, int chunkZ, int shift) {
        Key key = new Key(chunkX, chunkZ, shift);
        LodVoxelMesher.Mesh mesh = this.ready.get(key);
        if (mesh != null) {
            return mesh;
        }
        if (this.missing.contains(key)) {
            return LodVoxelMesher.Mesh.UNAVAILABLE;
        }
        request(key);
        return null;
    }

    private void request(Key key) {
        if (!this.pending.add(key)) {
            return;
        }
        try {
            this.pool.execute(() -> build(key));
        } catch (Throwable t) {
            this.pending.remove(key);
        }
    }

    private void build(Key key) {
        try {
            // 锚点/世界可能在建网期间被换掉，那样这批顶点坐标就作废了。
            // 先记下当前代次与锚点，建完再核对；不一致就丢弃，下一帧按新值重新排队。
            int gen = this.generation.get();
            int ax = this.anchorX;
            int az = this.anchorZ;
            LodRegionCache regionCache = LodRegionCache.getInstance();
            // 图集 UV 映射未就绪时不建网：UV 会烘进顶点并被缓存复用，启动早期
            // spriteUvMap 还是空的，此时建网整块会落到未命中回退位置，之后映射补齐
            // 相机往里走就「外圈先闪一片再跳变」。等就绪再建一次即可。
            if (!regionCache.isSpriteUvReady()) {
                this.completed.incrementAndGet();
                return;
            }
            LodRegionCache.VoxelChunk chunk = regionCache.readVoxel(key.x(), key.z());
            LodVoxelMesher.Mesh mesh;
            if (chunk == null) {
                // 区块不存在或读盘失败：先记成空，避免反复重读同一个未生成的区块
                mesh = LodVoxelMesher.Mesh.EMPTY;
                if (gen == this.generation.get()) {
                    this.missing.add(key);
                }
            } else {
                // 顶点写成「世界坐标 − 锚点」：着色器里再用 CameraPos 做相机相对变换。
                // 传入 regionCache 让 mesher 查图集 UV 矩形。
                mesh = LodVoxelMesher.mesh(chunk, key.x(), key.z(), ax, az, key.shift(), regionCache);
                if (gen == this.generation.get() && ax == this.anchorX && az == this.anchorZ) {
                    this.ready.put(key, mesh);
                }
            }
            this.completed.incrementAndGet();
        } catch (Throwable ignored) {
            this.missing.add(key);
        } finally {
            this.pending.remove(key);
        }
    }

    /** 世界切换时清空。 */
    public void clear() {
        synchronized (this) {
            this.ready.clear();
            this.missing.clear();
            this.pending.clear();
            this.generation.incrementAndGet();
            // 不要归零：渲染端拿它当「有变化」的信号，归零后可能刚好等于旧值而漏掉重建
            this.completed.incrementAndGet();
        }
    }

    /**
     * 重新尝试之前判定「读不到」的区块。玩家走动时新生成的区块会陆续写进 region
     * 文件，不清掉 missing 的话体素边界永远停在第一次读取时的位置。
     */
    public void retryMissing() {
        synchronized (this) {
            if (this.missing.isEmpty()) {
                return;
            }
            this.missing.clear();
            this.completed.incrementAndGet();
        }
    }
}
