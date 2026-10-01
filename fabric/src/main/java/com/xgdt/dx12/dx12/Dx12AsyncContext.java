package com.xgdt.dx12.dx12;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P33：CPU/GPU 异步渲染上下文（Java 侧持有者）。
 *
 * <p>持有 native 侧的分区描述符分配器与 Bundle 录制器池，并维护执行 bundle
 * 录制的 worker 线程池。绘制热路径（{@link Dx12RenderPassBackend#drawMultipleIndexed}）
 * 在批量足够大时，把同一 pass + 同一 PSO + 同一 scissor 下的 draw 段切给多个
 * worker，各自录制成 D3D12 BUNDLE，再由主列表按原顺序 {@code ExecuteBundle} 回放。
 *
 * <p>描述符不共享：native 把 shader-visible drawHeap 按 {@code (frameSlot, worker)}
 * 切成互不相交的区域（见 native {@code PartitionedDescriptorAllocator}），worker 只写
 * 自己的区域，因此并发写描述符不存在数据竞争。
 *
 * <p>回退开关：{@code -Ddx12.async=0} 关闭并行录制；{@code -Ddx12.async.workers=N}
 * 指定 worker 数（默认 4）。任何 native 初始化失败都会自动退回单线程录制。
 */
@Environment(EnvType.CLIENT)
public final class Dx12AsyncContext implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static final String PROP_ENABLE = "dx12.async";
    private static final String PROP_WORKERS = "dx12.async.workers";
    /** 与 native kMaxAsyncWorkers 一致。 */
    private static final int MAX_WORKERS = 8;
    /** 默认 worker 数：每帧段 32768 槽，同步 ring 保留 8192，每 worker 6144 槽。 */
    private static final int DEFAULT_WORKERS = 4;

    /** 全局开关：默认开启。 */
    static final boolean ENABLED = !"0".equals(System.getProperty(PROP_ENABLE, "1"));

    private static volatile @Nullable Dx12AsyncContext instance;

    private final int workerCount;
    private final long descriptorAlloc;
    private final long bundlePool;
    private @Nullable ExecutorService workers;
    private int lastResetFrameSlot = -1;
    private volatile boolean closed;

    /** 由 {@link Dx12Device} 构造时调用；返回 false 表示不可用（自动退回串行录制）。 */
    static boolean init() {
        if (!ENABLED) {
            LOGGER.info("[dx12] P33 async disabled via -D{}=0", PROP_ENABLE);
            return false;
        }
        if (instance != null) {
            return true;
        }
        Dx12AsyncContext created = new Dx12AsyncContext();
        if (!created.available()) {
            created.close();
            return false;
        }
        instance = created;
        return true;
    }

    /** 由 {@link Dx12Device#close()} 调用。 */
    static void shutdown() {
        Dx12AsyncContext current = instance;
        instance = null;
        if (current != null) {
            current.close();
        }
    }

    /** 当前上下文；未启用或初始化失败时返回 null。 */
    static @Nullable Dx12AsyncContext get() {
        return instance;
    }

    private Dx12AsyncContext() {
        int requested = parseWorkerCount();
        this.workerCount = requested;
        this.descriptorAlloc = Dx12Native.dx12AsyncDescriptorCreate(requested);
        this.bundlePool = Dx12Native.dx12AsyncBundlePoolCreate(requested);
        if (this.descriptorAlloc == 0 || this.bundlePool == 0) {
            LOGGER.warn("[dx12] P33 async init failed (descriptor=0x{} bundle=0x{}); "
                    + "falling back to serial recording",
                Long.toHexString(this.descriptorAlloc), Long.toHexString(this.bundlePool));
            return;
        }
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "dx12-async-worker-" + this.seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        this.workers = Executors.newFixedThreadPool(requested, factory);
        LOGGER.info("[dx12] P33 async ready: {} workers", requested);
    }

    private static int parseWorkerCount() {
        int n = DEFAULT_WORKERS;
        String prop = System.getProperty(PROP_WORKERS);
        if (prop != null) {
            try {
                n = Integer.parseInt(prop.trim());
            } catch (NumberFormatException ignored) {
                // 非法值回退默认
            }
        }
        return Math.max(1, Math.min(MAX_WORKERS, n));
    }

    boolean available() {
        return !this.closed && this.descriptorAlloc != 0 && this.bundlePool != 0
            && this.workers != null;
    }

    int workerCount() {
        return this.workerCount;
    }

    /** worker 线程池；仅在 {@link #available()} 为 true 时非 null。 */
    @Nullable
    ExecutorService workers() {
        return this.workers;
    }

    long descriptorAlloc() {
        return this.descriptorAlloc;
    }

    long bundlePool() {
        return this.bundlePool;
    }

    /**
     * 返回当前帧的 drawHeap 段号，并在进入新帧时清零该段的 worker 分配游标。
     *
     * <p>必须在命令列表已 begin 之后调用——native 侧 {@code drawHeapSlotBase} 已按
     * 同一段号（{@code fenceValue % 4}）设定，worker 区域因此与主列表写入位置对齐。
     * 段号在 4 帧内轮转一轮，复用时其 GPU 工作必然已完成，reset 安全。
     *
     * @return 段号（0..3）；取不到时返回 -1（调用方退回串行）
     */
    int frameSlot(long ctx) {
        int slot = Dx12Native.dx12AsyncCurrentFrameSlot(ctx);
        if (slot < 0) {
            return -1;
        }
        if (slot != this.lastResetFrameSlot) {
            Dx12Native.dx12AsyncDescriptorResetFrame(this.descriptorAlloc, slot);
            this.lastResetFrameSlot = slot;
        }
        return slot;
    }

    /** 运行时统计：本帧回退到串行路径的批次数（仅诊断用）。 */
    private static final AtomicInteger fallbackCount = new AtomicInteger();

    static void countFallback() {
        fallbackCount.incrementAndGet();
    }

    static int fallbackCount() {
        return fallbackCount.get();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        ExecutorService pool = this.workers;
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
        // 先停 worker 再销毁 native 资源：销毁后 worker 再调 JNI 会拿到悬空指针。
        if (this.bundlePool != 0) {
            Dx12Native.dx12AsyncBundlePoolDestroy(this.bundlePool);
        }
        if (this.descriptorAlloc != 0) {
            Dx12Native.dx12AsyncDescriptorDestroy(this.descriptorAlloc);
        }
    }
}
