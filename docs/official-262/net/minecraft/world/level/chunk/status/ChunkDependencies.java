/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 */
package net.minecraft.world.level.chunk.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class ChunkDependencies {
    private final ImmutableList<ChunkStatus> dependencyByRadius;
    private final int[] radiusByDependency;

    public ChunkDependencies(ImmutableList<ChunkStatus> dependencyByRadius) {
        this.dependencyByRadius = dependencyByRadius;
        int size = dependencyByRadius.isEmpty() ? 0 : ((ChunkStatus)dependencyByRadius.getFirst()).getIndex() + 1;
        this.radiusByDependency = new int[size];
        for (int radius = 0; radius < dependencyByRadius.size(); ++radius) {
            ChunkStatus dependency = (ChunkStatus)dependencyByRadius.get(radius);
            int index = dependency.getIndex();
            for (int statusIndex = 0; statusIndex <= index; ++statusIndex) {
                this.radiusByDependency[statusIndex] = radius;
            }
        }
    }

    @VisibleForTesting
    public ImmutableList<ChunkStatus> asList() {
        return this.dependencyByRadius;
    }

    public int size() {
        return this.dependencyByRadius.size();
    }

    public int getRadiusOf(ChunkStatus status) {
        int index = status.getIndex();
        if (index >= this.radiusByDependency.length) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Requesting a ChunkStatus(%s) outside of dependency range(%s)", status, this.dependencyByRadius));
        }
        return this.radiusByDependency[index];
    }

    public int getRadius() {
        return Math.max(0, this.dependencyByRadius.size() - 1);
    }

    public ChunkStatus get(int distance) {
        return (ChunkStatus)this.dependencyByRadius.get(distance);
    }

    public String toString() {
        return this.dependencyByRadius.toString();
    }
}

