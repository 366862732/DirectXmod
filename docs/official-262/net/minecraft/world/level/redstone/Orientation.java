/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.level.redstone;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class Orientation {
    public static final StreamCodec<ByteBuf, Orientation> STREAM_CODEC = ByteBufCodecs.idMapper(Orientation::fromIndex, Orientation::getIndex);
    private static final Orientation[] ORIENTATIONS = Util.make(() -> {
        Orientation[] orientations = new Orientation[48];
        Orientation.generateContext(new Orientation(Direction.UP, Direction.NORTH, SideBias.LEFT), orientations);
        return orientations;
    });
    private final Direction up;
    private final Direction front;
    private final Direction side;
    private final SideBias sideBias;
    private final int index;
    private final List<Direction> neighbors;
    private final List<Direction> horizontalNeighbors;
    private final List<Direction> verticalNeighbors;
    private final Map<Direction, Orientation> withFront = new EnumMap<Direction, Orientation>(Direction.class);
    private final Map<Direction, Orientation> withUp = new EnumMap<Direction, Orientation>(Direction.class);
    private final Map<SideBias, Orientation> withSideBias = new EnumMap<SideBias, Orientation>(SideBias.class);

    private Orientation(Direction up, Direction front, SideBias sideBias) {
        this.up = up;
        this.front = front;
        this.sideBias = sideBias;
        this.index = Orientation.generateIndex(up, front, sideBias);
        Vec3i rightVector = front.getUnitVec3i().cross(up.getUnitVec3i());
        Direction side = Direction.getNearest(rightVector, null);
        Objects.requireNonNull(side);
        this.side = this.sideBias == SideBias.RIGHT ? side : side.getOpposite();
        this.neighbors = List.of(this.front.getOpposite(), this.front, this.side, this.side.getOpposite(), this.up.getOpposite(), this.up);
        this.horizontalNeighbors = this.neighbors.stream().filter(d -> d.getAxis() != this.up.getAxis()).toList();
        this.verticalNeighbors = this.neighbors.stream().filter(d -> d.getAxis() == this.up.getAxis()).toList();
    }

    public static Orientation of(Direction up, Direction front, SideBias sideBias) {
        return ORIENTATIONS[Orientation.generateIndex(up, front, sideBias)];
    }

    public Orientation withUp(Direction up) {
        return this.withUp.get(up);
    }

    public Orientation withFront(Direction front) {
        return this.withFront.get(front);
    }

    public Orientation withFrontPreserveUp(Direction front) {
        if (front.getAxis() == this.up.getAxis()) {
            return this;
        }
        return this.withFront.get(front);
    }

    public Orientation withFrontAdjustSideBias(Direction front) {
        Orientation withFront = this.withFront(front);
        if (this.front == withFront.side) {
            return withFront.withMirror();
        }
        return withFront;
    }

    public Orientation withSideBias(SideBias sideBias) {
        return this.withSideBias.get((Object)sideBias);
    }

    public Orientation withMirror() {
        return this.withSideBias(this.sideBias.getOpposite());
    }

    public Direction getFront() {
        return this.front;
    }

    public Direction getUp() {
        return this.up;
    }

    public Direction getSide() {
        return this.side;
    }

    public SideBias getSideBias() {
        return this.sideBias;
    }

    public List<Direction> getDirections() {
        return this.neighbors;
    }

    public List<Direction> getHorizontalDirections() {
        return this.horizontalNeighbors;
    }

    public List<Direction> getVerticalDirections() {
        return this.verticalNeighbors;
    }

    public String toString() {
        return "[up=" + String.valueOf(this.up) + ",front=" + String.valueOf(this.front) + ",sideBias=" + String.valueOf((Object)this.sideBias) + "]";
    }

    public int getIndex() {
        return this.index;
    }

    public static Orientation fromIndex(int index) {
        return ORIENTATIONS[index];
    }

    public static Orientation random(RandomSource rand) {
        return Util.getRandom(ORIENTATIONS, rand);
    }

    private static Orientation generateContext(Orientation self, Orientation[] lookup) {
        if (lookup[self.getIndex()] != null) {
            return lookup[self.getIndex()];
        }
        lookup[self.getIndex()] = self;
        for (SideBias sideBias : SideBias.values()) {
            self.withSideBias.put(sideBias, Orientation.generateContext(new Orientation(self.up, self.front, sideBias), lookup));
        }
        for (Enum enum_ : Direction.values()) {
            Direction up = self.up;
            if (enum_ == self.up) {
                up = self.front.getOpposite();
            }
            if (enum_ == self.up.getOpposite()) {
                up = self.front;
            }
            self.withFront.put((Direction)enum_, Orientation.generateContext(new Orientation(up, (Direction)enum_, self.sideBias), lookup));
        }
        for (Enum enum_ : Direction.values()) {
            Direction front = self.front;
            if (enum_ == self.front) {
                front = self.up.getOpposite();
            }
            if (enum_ == self.front.getOpposite()) {
                front = self.up;
            }
            self.withUp.put((Direction)enum_, Orientation.generateContext(new Orientation((Direction)enum_, front, self.sideBias), lookup));
        }
        return self;
    }

    @VisibleForTesting
    protected static int generateIndex(Direction up, Direction front, SideBias sideBias) {
        if (up.getAxis() == front.getAxis()) {
            throw new IllegalStateException("Up-vector and front-vector can not be on the same axis");
        }
        int frontAxisKey = up.getAxis() == Direction.Axis.Y ? (front.getAxis() == Direction.Axis.X ? 1 : 0) : (front.getAxis() == Direction.Axis.Y ? 1 : 0);
        int frontKey = frontAxisKey << 1 | front.getAxisDirection().ordinal();
        return ((up.ordinal() << 2) + frontKey << 1) + sideBias.ordinal();
    }

    public static enum SideBias {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        private SideBias(String name) {
            this.name = name;
        }

        public SideBias getOpposite() {
            return this == LEFT ? RIGHT : LEFT;
        }

        public String toString() {
            return this.name;
        }
    }
}

