/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public enum FaceInfo {
    DOWN(new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z), new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z)),
    UP(new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z), new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z)),
    NORTH(new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z)),
    SOUTH(new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z), new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z)),
    WEST(new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z), new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z), new VertexInfo(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z)),
    EAST(new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z), new VertexInfo(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z), new VertexInfo(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z));

    private static final Map<Direction, FaceInfo> BY_FACING;
    private final VertexInfo[] infos;

    public static FaceInfo fromFacing(Direction direction) {
        return BY_FACING.get(direction);
    }

    private FaceInfo(VertexInfo ... infos) {
        this.infos = infos;
    }

    public VertexInfo getVertexInfo(int index) {
        return this.infos[index];
    }

    static {
        BY_FACING = Util.make(new EnumMap(Direction.class), map -> {
            map.put(Direction.DOWN, DOWN);
            map.put(Direction.UP, UP);
            map.put(Direction.NORTH, NORTH);
            map.put(Direction.SOUTH, SOUTH);
            map.put(Direction.WEST, WEST);
            map.put(Direction.EAST, EAST);
        });
    }

    @Environment(value=EnvType.CLIENT)
    public record VertexInfo(Extent xFace, Extent yFace, Extent zFace) {
        public Vector3f select(Vector3fc min, Vector3fc max) {
            return new Vector3f(this.xFace.select(min, max), this.yFace.select(min, max), this.zFace.select(min, max));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Extent {
        MIN_X,
        MIN_Y,
        MIN_Z,
        MAX_X,
        MAX_Y,
        MAX_Z;


        public float select(Vector3fc min, Vector3fc max) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> min.x();
                case 1 -> min.y();
                case 2 -> min.z();
                case 3 -> max.x();
                case 4 -> max.y();
                case 5 -> max.z();
            };
        }

        public float select(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> minX;
                case 1 -> minY;
                case 2 -> minZ;
                case 3 -> maxX;
                case 4 -> maxY;
                case 5 -> maxZ;
            };
        }
    }
}

