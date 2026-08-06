/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.state.level;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;

@Environment(value=EnvType.CLIENT)
public class WorldBorderRenderState {
    public double minX;
    public double maxX;
    public double minZ;
    public double maxZ;
    public int tint;
    public double alpha;

    public List<DistancePerDirection> closestBorder(double x, double z) {
        DistancePerDirection[] directions = new DistancePerDirection[]{new DistancePerDirection(Direction.NORTH, z - this.minZ), new DistancePerDirection(Direction.SOUTH, this.maxZ - z), new DistancePerDirection(Direction.WEST, x - this.minX), new DistancePerDirection(Direction.EAST, this.maxX - x)};
        return Arrays.stream(directions).sorted(Comparator.comparingDouble(d -> d.distance)).toList();
    }

    public void reset() {
        this.alpha = 0.0;
    }

    @Environment(value=EnvType.CLIENT)
    public record DistancePerDirection(Direction direction, double distance) {
    }
}

