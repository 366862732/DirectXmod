/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ArrayListMultimap
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Multimap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.geometry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.lang.runtime.SwitchBootstraps;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class QuadCollection {
    public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    private static final int FLAGS_NOT_COMPUTED = -1;
    private final List<BakedQuad> all;
    private final List<BakedQuad> unculled;
    private final List<BakedQuad> north;
    private final List<BakedQuad> south;
    private final List<BakedQuad> east;
    private final List<BakedQuad> west;
    private final List<BakedQuad> up;
    private final List<BakedQuad> down;
    private int materialFlags = -1;

    private QuadCollection(List<BakedQuad> all, List<BakedQuad> unculled, List<BakedQuad> north, List<BakedQuad> south, List<BakedQuad> east, List<BakedQuad> west, List<BakedQuad> up, List<BakedQuad> down) {
        this.all = all;
        this.unculled = unculled;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.up = up;
        this.down = down;
    }

    private static @BakedQuad.MaterialFlags int computeMaterialFlags(List<BakedQuad> quads) {
        int flags = 0;
        for (BakedQuad quad : quads) {
            flags |= quad.materialInfo().flags();
        }
        return flags;
    }

    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        Direction direction2 = direction;
        int n = 0;
        return switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"NORTH", "SOUTH", "EAST", "WEST", "UP", "DOWN"}, (Direction)direction2, n)) {
            default -> throw new MatchException(null, null);
            case -1 -> this.unculled;
            case 0 -> this.north;
            case 1 -> this.south;
            case 2 -> this.east;
            case 3 -> this.west;
            case 4 -> this.up;
            case 5 -> this.down;
        };
    }

    public List<BakedQuad> getAll() {
        return this.all;
    }

    public @BakedQuad.MaterialFlags int materialFlags() {
        if (this.materialFlags == -1) {
            this.materialFlags = QuadCollection.computeMaterialFlags(this.all);
        }
        return this.materialFlags;
    }

    public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag) {
        return (this.materialFlags() & flag) != 0;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

        public Builder addCulledFace(Direction direction, BakedQuad quad) {
            this.culledFaces.put((Object)direction, (Object)quad);
            return this;
        }

        public Builder addUnculledFace(BakedQuad quad) {
            this.unculledFaces.add((Object)quad);
            return this;
        }

        public Builder addAll(QuadCollection quadCollection) {
            this.culledFaces.putAll((Object)Direction.UP, quadCollection.up);
            this.culledFaces.putAll((Object)Direction.DOWN, quadCollection.down);
            this.culledFaces.putAll((Object)Direction.NORTH, quadCollection.north);
            this.culledFaces.putAll((Object)Direction.SOUTH, quadCollection.south);
            this.culledFaces.putAll((Object)Direction.EAST, quadCollection.east);
            this.culledFaces.putAll((Object)Direction.WEST, quadCollection.west);
            this.unculledFaces.addAll(quadCollection.unculled);
            return this;
        }

        private static QuadCollection createFromSublists(List<BakedQuad> all, int unculledCount, int northCount, int southCount, int eastCount, int westCount, int upCount, int downCount) {
            int index = 0;
            List<BakedQuad> unculled = all.subList(index, index += unculledCount);
            List<BakedQuad> north = all.subList(index, index += northCount);
            List<BakedQuad> south = all.subList(index, index += southCount);
            List<BakedQuad> east = all.subList(index, index += eastCount);
            List<BakedQuad> west = all.subList(index, index += westCount);
            List<BakedQuad> up = all.subList(index, index += upCount);
            List<BakedQuad> down = all.subList(index, index + downCount);
            return new QuadCollection(all, unculled, north, south, east, west, up, down);
        }

        public QuadCollection build() {
            ImmutableList unculledFaces = this.unculledFaces.build();
            if (this.culledFaces.isEmpty()) {
                if (unculledFaces.isEmpty()) {
                    return EMPTY;
                }
                return new QuadCollection((List<BakedQuad>)unculledFaces, (List<BakedQuad>)unculledFaces, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            }
            ImmutableList.Builder quads = ImmutableList.builder();
            quads.addAll((Iterable)unculledFaces);
            Collection north = this.culledFaces.get((Object)Direction.NORTH);
            quads.addAll((Iterable)north);
            Collection south = this.culledFaces.get((Object)Direction.SOUTH);
            quads.addAll((Iterable)south);
            Collection east = this.culledFaces.get((Object)Direction.EAST);
            quads.addAll((Iterable)east);
            Collection west = this.culledFaces.get((Object)Direction.WEST);
            quads.addAll((Iterable)west);
            Collection up = this.culledFaces.get((Object)Direction.UP);
            quads.addAll((Iterable)up);
            Collection down = this.culledFaces.get((Object)Direction.DOWN);
            quads.addAll((Iterable)down);
            return Builder.createFromSublists((List<BakedQuad>)quads.build(), unculledFaces.size(), north.size(), south.size(), east.size(), west.size(), up.size(), down.size());
        }
    }
}

