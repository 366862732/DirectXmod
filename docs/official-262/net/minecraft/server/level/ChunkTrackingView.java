/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
    public static final ChunkTrackingView EMPTY = new ChunkTrackingView(){

        @Override
        public boolean contains(int chunkX, int chunkZ, boolean includeNeighbors) {
            return false;
        }

        @Override
        public void forEach(Consumer<ChunkPos> consumer) {
        }
    };

    public static ChunkTrackingView of(ChunkPos center, int radius) {
        return new Positioned(center, radius);
    }

    /*
     * Enabled aggressive block sorting
     */
    public static void difference(ChunkTrackingView from, ChunkTrackingView to, Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave) {
        Positioned next;
        Positioned last;
        block8: {
            block7: {
                if (from.equals(to)) {
                    return;
                }
                if (!(from instanceof Positioned)) break block7;
                last = (Positioned)from;
                if (to instanceof Positioned && last.squareIntersects(next = (Positioned)to)) break block8;
            }
            from.forEach(onLeave);
            to.forEach(onEnter);
            return;
        }
        int minX = Math.min(last.minX(), next.minX());
        int minZ = Math.min(last.minZ(), next.minZ());
        int maxX = Math.max(last.maxX(), next.maxX());
        int maxZ = Math.max(last.maxZ(), next.maxZ());
        int x = minX;
        while (x <= maxX) {
            for (int z = minZ; z <= maxZ; ++z) {
                boolean sees;
                boolean saw = last.contains(x, z);
                if (saw == (sees = next.contains(x, z))) continue;
                if (sees) {
                    onEnter.accept(new ChunkPos(x, z));
                    continue;
                }
                onLeave.accept(new ChunkPos(x, z));
            }
            ++x;
        }
        return;
    }

    default public boolean contains(ChunkPos pos) {
        return this.contains(pos.x(), pos.z());
    }

    default public boolean contains(int x, int z) {
        return this.contains(x, z, true);
    }

    public boolean contains(int var1, int var2, boolean var3);

    public void forEach(Consumer<ChunkPos> var1);

    default public boolean isInViewDistance(int chunkX, int chunkZ) {
        return this.contains(chunkX, chunkZ, false);
    }

    public static boolean isInViewDistance(int centerX, int centerZ, int viewDistance, int chunkX, int chunkZ) {
        return ChunkTrackingView.isWithinDistance(centerX, centerZ, viewDistance, chunkX, chunkZ, false);
    }

    public static boolean isWithinDistance(int centerX, int centerZ, int viewDistance, int chunkX, int chunkZ, boolean includeNeighbors) {
        int bufferRange = includeNeighbors ? 2 : 1;
        long deltaX = Math.max(0, Math.abs(chunkX - centerX) - bufferRange);
        long deltaZ = Math.max(0, Math.abs(chunkZ - centerZ) - bufferRange);
        long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
        int radiusSquared = viewDistance * viewDistance;
        return distanceSquared < (long)radiusSquared;
    }

    public record Positioned(ChunkPos center, int viewDistance) implements ChunkTrackingView
    {
        private int minX() {
            return this.center.x() - this.viewDistance - 1;
        }

        private int minZ() {
            return this.center.z() - this.viewDistance - 1;
        }

        private int maxX() {
            return this.center.x() + this.viewDistance + 1;
        }

        private int maxZ() {
            return this.center.z() + this.viewDistance + 1;
        }

        @VisibleForTesting
        boolean squareIntersects(Positioned other) {
            return this.minX() <= other.maxX() && this.maxX() >= other.minX() && this.minZ() <= other.maxZ() && this.maxZ() >= other.minZ();
        }

        @Override
        public boolean contains(int chunkX, int chunkZ, boolean includeNeighbors) {
            return ChunkTrackingView.isWithinDistance(this.center.x(), this.center.z(), this.viewDistance, chunkX, chunkZ, includeNeighbors);
        }

        @Override
        public void forEach(Consumer<ChunkPos> consumer) {
            for (int x = this.minX(); x <= this.maxX(); ++x) {
                for (int z = this.minZ(); z <= this.maxZ(); ++z) {
                    if (!this.contains(x, z)) continue;
                    consumer.accept(new ChunkPos(x, z));
                }
            }
        }
    }
}

