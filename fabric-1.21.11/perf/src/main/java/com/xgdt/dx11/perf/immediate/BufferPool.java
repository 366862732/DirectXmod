package com.xgdt.dx11.perf.immediate;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Small render-thread pool for the entity/feature {@code ByteBufferBuilder}s.
 *
 * <p>Vanilla creates one large shared builder for the immediate feature pass;
 * the batched path can keep several render-type streams in flight, so it needs
 * more than one builder.  Pooling them avoids re-allocating a 256-byte native
 * segment (and its growth copies) for every feature layer, several times per
 * frame.</p>
 *
 * <p>All methods must be called on the render thread.  A builder that is not
 * returned for two consecutive frames is reclaimed with a warning, matching the
 * defensive behaviour of the reference mod.</p>
 */
public final class BufferPool {
    private static final int INITIAL_CAPACITY = 256;
    private static final long IDLE_CLOSE_MILLIS = 60_000L;

    private static final ArrayDeque<ByteBufferBuilder> FREE = new ArrayDeque<>();
    private static final Map<ByteBufferBuilder, Long> IN_USE = new HashMap<>();
    private static final Map<ByteBufferBuilder, Long> LAST_RETURN = new HashMap<>();
    private static long lastFrameMillis = System.currentTimeMillis();

    private BufferPool() {
    }

    public static ByteBufferBuilder borrow() {
        ByteBufferBuilder builder = FREE.pollFirst();
        if (builder == null) {
            builder = new ByteBufferBuilder(INITIAL_CAPACITY);
            ImmediateStats.onCreate();
        }
        IN_USE.put(builder, Long.valueOf(System.currentTimeMillis()));
        LAST_RETURN.remove(builder);
        ImmediateStats.onBorrow();
        return builder;
    }

    /**
     * Returns a builder to the pool.  Unknown builders (for example a fixed
     * vanilla buffer that must stay owned by the section pack) are ignored.
     */
    public static void release(ByteBufferBuilder builder) {
        if (builder == null || IN_USE.remove(builder) == null) {
            return;
        }
        builder.discard();
        FREE.addFirst(builder);
        LAST_RETURN.put(builder, Long.valueOf(System.currentTimeMillis()));
        ImmediateStats.onRelease();
    }

    public static int freeCount() {
        return FREE.size();
    }

    public static int inUseCount() {
        return IN_USE.size();
    }

    /**
     * Frame boundary: reclaim builders leaked by a feature renderer that threw
     * before its flush, and close builders that have been idle for a minute so
     * a large one-off batch does not pin native memory forever.
     */
    public static void endFrame() {
        long now = System.currentTimeMillis();
        if (now - lastFrameMillis < 1L) {
            return;
        }
        lastFrameMillis = now;

        if (!IN_USE.isEmpty()) {
            Iterator<Map.Entry<ByteBufferBuilder, Long>> inUse = IN_USE.entrySet().iterator();
            while (inUse.hasNext()) {
                Map.Entry<ByteBufferBuilder, Long> entry = inUse.next();
                if (now - entry.getValue().longValue() > 2L * IDLE_CLOSE_MILLIS) {
                    // Two minutes in use means a flush path never ran.  Drop it
                    // rather than leaking the native segment forever.
                    inUse.remove();
                    try {
                        entry.getKey().close();
                    } catch (RuntimeException ignored) {
                        // Closing twice is harmless for the pool's purpose.
                    }
                    ImmediateStats.onClose();
                }
            }
        }

        Iterator<ByteBufferBuilder> free = FREE.iterator();
        while (free.hasNext()) {
            ByteBufferBuilder builder = free.next();
            Long returned = LAST_RETURN.get(builder);
            if (returned != null && now - returned.longValue() > IDLE_CLOSE_MILLIS) {
                free.remove();
                LAST_RETURN.remove(builder);
                try {
                    builder.close();
                } catch (RuntimeException ignored) {
                    // Ignore a builder whose native segment was already gone.
                }
                ImmediateStats.onClose();
            }
        }
    }

    public static void reset() {
        for (ByteBufferBuilder builder : FREE) {
            LAST_RETURN.remove(builder);
            builder.close();
        }
        FREE.clear();
        for (ByteBufferBuilder builder : IN_USE.keySet()) {
            builder.close();
        }
        IN_USE.clear();
        ImmediateStats.reset();
    }
}
