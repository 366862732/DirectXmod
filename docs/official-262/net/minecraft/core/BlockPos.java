/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.AbstractIterator
 *  com.google.common.collect.ImmutableList
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  javax.annotation.concurrent.Immutable
 *  org.apache.commons.lang3.Validate
 */
package net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

@Immutable
public class BlockPos
extends Vec3i {
    public static final Codec<BlockPos> CODEC = Codec.INT_STREAM.comapFlatMap(input -> Util.fixedSize(input, 3).map(ints -> new BlockPos(ints[0], ints[1], ints[2])), pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ())).stable();
    public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>(){

        @Override
        public BlockPos decode(ByteBuf input) {
            return FriendlyByteBuf.readBlockPos(input);
        }

        @Override
        public void encode(ByteBuf output, BlockPos value) {
            FriendlyByteBuf.writeBlockPos(output, value);
        }
    };
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);
    public static final int PACKED_HORIZONTAL_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
    public static final int PACKED_Y_LENGTH = 64 - 2 * PACKED_HORIZONTAL_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_HORIZONTAL_LENGTH;
    public static final int MAX_HORIZONTAL_COORDINATE = (1 << PACKED_HORIZONTAL_LENGTH) / 2 - 1;

    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPos(Vec3i vec3i) {
        this(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }

    public static long offset(long blockNode, Direction offset) {
        return BlockPos.offset(blockNode, offset.getStepX(), offset.getStepY(), offset.getStepZ());
    }

    public static long offset(long blockNode, int stepX, int stepY, int stepZ) {
        return BlockPos.asLong(BlockPos.getX(blockNode) + stepX, BlockPos.getY(blockNode) + stepY, BlockPos.getZ(blockNode) + stepZ);
    }

    public static int getX(long blockNode) {
        return (int)(blockNode << 64 - X_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
    }

    public static int getY(long blockNode) {
        return (int)(blockNode << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int getZ(long blockNode) {
        return (int)(blockNode << 64 - Z_OFFSET - PACKED_HORIZONTAL_LENGTH >> 64 - PACKED_HORIZONTAL_LENGTH);
    }

    public static BlockPos of(long blockNode) {
        return new BlockPos(BlockPos.getX(blockNode), BlockPos.getY(blockNode), BlockPos.getZ(blockNode));
    }

    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    public static BlockPos containing(Position pos) {
        return BlockPos.containing(pos.x(), pos.y(), pos.z());
    }

    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public long asLong() {
        return BlockPos.asLong(this.getX(), this.getY(), this.getZ());
    }

    public static long asLong(int x, int y, int z) {
        long node = 0L;
        node |= ((long)x & PACKED_X_MASK) << X_OFFSET;
        node |= ((long)y & PACKED_Y_MASK) << 0;
        return node |= ((long)z & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static long getFlatIndex(long neighborBlockNode) {
        return neighborBlockNode & 0xFFFFFFFFFFFFFFF0L;
    }

    @Override
    public BlockPos offset(int x, int y, int z) {
        if (x == 0 && y == 0 && z == 0) {
            return this;
        }
        return new BlockPos(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    @Override
    public BlockPos offset(Vec3i vec) {
        return this.offset(vec.getX(), vec.getY(), vec.getZ());
    }

    @Override
    public BlockPos subtract(Vec3i vec) {
        return this.offset(-vec.getX(), -vec.getY(), -vec.getZ());
    }

    @Override
    public BlockPos multiply(int scale) {
        if (scale == 1) {
            return this;
        }
        if (scale == 0) {
            return ZERO;
        }
        return new BlockPos(this.getX() * scale, this.getY() * scale, this.getZ() * scale);
    }

    @Override
    public BlockPos above() {
        return this.relative(Direction.UP);
    }

    @Override
    public BlockPos above(int steps) {
        return this.relative(Direction.UP, steps);
    }

    @Override
    public BlockPos below() {
        return this.relative(Direction.DOWN);
    }

    @Override
    public BlockPos below(int steps) {
        return this.relative(Direction.DOWN, steps);
    }

    @Override
    public BlockPos north() {
        return this.relative(Direction.NORTH);
    }

    @Override
    public BlockPos north(int steps) {
        return this.relative(Direction.NORTH, steps);
    }

    @Override
    public BlockPos south() {
        return this.relative(Direction.SOUTH);
    }

    @Override
    public BlockPos south(int steps) {
        return this.relative(Direction.SOUTH, steps);
    }

    @Override
    public BlockPos west() {
        return this.relative(Direction.WEST);
    }

    @Override
    public BlockPos west(int steps) {
        return this.relative(Direction.WEST, steps);
    }

    @Override
    public BlockPos east() {
        return this.relative(Direction.EAST);
    }

    @Override
    public BlockPos east(int steps) {
        return this.relative(Direction.EAST, steps);
    }

    @Override
    public BlockPos relative(Direction direction) {
        return new BlockPos(this.getX() + direction.getStepX(), this.getY() + direction.getStepY(), this.getZ() + direction.getStepZ());
    }

    @Override
    public BlockPos relative(Direction direction, int steps) {
        if (steps == 0) {
            return this;
        }
        return new BlockPos(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
    }

    @Override
    public BlockPos relative(Direction.Axis axis, int steps) {
        if (steps == 0) {
            return this;
        }
        int xStep = axis == Direction.Axis.X ? steps : 0;
        int yStep = axis == Direction.Axis.Y ? steps : 0;
        int zStep = axis == Direction.Axis.Z ? steps : 0;
        return new BlockPos(this.getX() + xStep, this.getY() + yStep, this.getZ() + zStep);
    }

    public BlockPos rotate(Rotation rotation) {
        return switch (rotation) {
            default -> throw new MatchException(null, null);
            case Rotation.CLOCKWISE_90 -> new BlockPos(-this.getZ(), this.getY(), this.getX());
            case Rotation.CLOCKWISE_180 -> new BlockPos(-this.getX(), this.getY(), -this.getZ());
            case Rotation.COUNTERCLOCKWISE_90 -> new BlockPos(this.getZ(), this.getY(), -this.getX());
            case Rotation.NONE -> this;
        };
    }

    @Override
    public BlockPos cross(Vec3i upVector) {
        return new BlockPos(this.getY() * upVector.getZ() - this.getZ() * upVector.getY(), this.getZ() * upVector.getX() - this.getX() * upVector.getZ(), this.getX() * upVector.getY() - this.getY() * upVector.getX());
    }

    public BlockPos atY(int y) {
        return new BlockPos(this.getX(), y, this.getZ());
    }

    public BlockPos immutable() {
        return this;
    }

    public MutableBlockPos mutable() {
        return new MutableBlockPos(this.getX(), this.getY(), this.getZ());
    }

    public Vec3 clampLocationWithin(Vec3 location) {
        return new Vec3(Mth.clamp(location.x, (double)((float)this.getX() + 1.0E-5f), (double)this.getX() + 1.0 - (double)1.0E-5f), Mth.clamp(location.y, (double)((float)this.getY() + 1.0E-5f), (double)this.getY() + 1.0 - (double)1.0E-5f), Mth.clamp(location.z, (double)((float)this.getZ() + 1.0E-5f), (double)this.getZ() + 1.0 - (double)1.0E-5f));
    }

    public static Iterable<BlockPos> randomInCube(RandomSource random, int limit, BlockPos center, int sizeToScanInAllDirections) {
        return BlockPos.randomBetweenClosed(random, limit, center.getX() - sizeToScanInAllDirections, center.getY() - sizeToScanInAllDirections, center.getZ() - sizeToScanInAllDirections, center.getX() + sizeToScanInAllDirections, center.getY() + sizeToScanInAllDirections, center.getZ() + sizeToScanInAllDirections);
    }

    @Deprecated
    public static Stream<BlockPos> squareOutSouthEast(BlockPos from) {
        return Stream.of(from, from.south(), from.east(), from.south().east());
    }

    public static Iterable<BlockPos> randomBetweenClosed(final RandomSource random, final int limit, final int minX, final int minY, final int minZ, int maxX, int maxY, int maxZ) {
        final int width = maxX - minX + 1;
        final int height = maxY - minY + 1;
        final int depth = maxZ - minZ + 1;
        return () -> new AbstractIterator<BlockPos>(){
            private final MutableBlockPos nextPos = new MutableBlockPos();
            private int counter = limit;

            protected BlockPos computeNext() {
                if (this.counter <= 0) {
                    return (BlockPos)this.endOfData();
                }
                MutableBlockPos next = this.nextPos.set(minX + random.nextInt(width), minY + random.nextInt(height), minZ + random.nextInt(depth));
                --this.counter;
                return next;
            }
        };
    }

    public static Iterable<BlockPos> withinManhattan(BlockPos origin, final int reachX, final int reachY, final int reachZ) {
        final int maxDepth = reachX + reachY + reachZ;
        final int originX = origin.getX();
        final int originY = origin.getY();
        final int originZ = origin.getZ();
        return () -> new AbstractIterator<BlockPos>(){
            private final MutableBlockPos cursor = new MutableBlockPos();
            private int currentDepth;
            private int maxX;
            private int maxY;
            private int x;
            private int y;
            private boolean zMirror;

            protected BlockPos computeNext() {
                if (this.zMirror) {
                    this.zMirror = false;
                    this.cursor.setZ(originZ - (this.cursor.getZ() - originZ));
                    return this.cursor;
                }
                MutableBlockPos found = null;
                while (found == null) {
                    if (this.y > this.maxY) {
                        ++this.x;
                        if (this.x > this.maxX) {
                            ++this.currentDepth;
                            if (this.currentDepth > maxDepth) {
                                return (BlockPos)this.endOfData();
                            }
                            this.maxX = Math.min(reachX, this.currentDepth);
                            this.x = -this.maxX;
                        }
                        this.maxY = Math.min(reachY, this.currentDepth - Math.abs(this.x));
                        this.y = -this.maxY;
                    }
                    int xx = this.x;
                    int yy = this.y;
                    int zz = this.currentDepth - Math.abs(xx) - Math.abs(yy);
                    if (zz <= reachZ) {
                        this.zMirror = zz != 0;
                        found = this.cursor.set(originX + xx, originY + yy, originZ + zz);
                    }
                    ++this.y;
                }
                return found;
            }
        };
    }

    public static Optional<BlockPos> findClosestMatch(BlockPos startPos, int horizontalSearchRadius, int verticalSearchRadius, Predicate<BlockPos> predicate) {
        for (BlockPos blockPos : BlockPos.withinManhattan(startPos, horizontalSearchRadius, verticalSearchRadius, horizontalSearchRadius)) {
            if (!predicate.test(blockPos)) continue;
            return Optional.of(blockPos);
        }
        return Optional.empty();
    }

    public static Stream<BlockPos> withinManhattanStream(BlockPos origin, int reachX, int reachY, int reachZ) {
        return StreamSupport.stream(BlockPos.withinManhattan(origin, reachX, reachY, reachZ).spliterator(), false);
    }

    public static Iterable<BlockPos> betweenClosed(AABB box) {
        BlockPos startPos = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos endPos = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        return BlockPos.betweenClosed(startPos, endPos);
    }

    public static Iterable<BlockPos> betweenClosed(BlockPos a, BlockPos b) {
        return BlockPos.betweenClosed(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public static Stream<BlockPos> betweenClosedStream(BlockPos a, BlockPos b) {
        return StreamSupport.stream(BlockPos.betweenClosed(a, b).spliterator(), false);
    }

    public static Stream<BlockPos> betweenClosedStream(BoundingBox boundingBox) {
        return BlockPos.betweenClosedStream(Math.min(boundingBox.minX(), boundingBox.maxX()), Math.min(boundingBox.minY(), boundingBox.maxY()), Math.min(boundingBox.minZ(), boundingBox.maxZ()), Math.max(boundingBox.minX(), boundingBox.maxX()), Math.max(boundingBox.minY(), boundingBox.maxY()), Math.max(boundingBox.minZ(), boundingBox.maxZ()));
    }

    public static Stream<BlockPos> betweenClosedStream(AABB box) {
        return BlockPos.betweenClosedStream(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ));
    }

    public static Stream<BlockPos> betweenClosedStream(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return StreamSupport.stream(BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ).spliterator(), false);
    }

    public static Iterable<BlockPos> betweenClosed(final int minX, final int minY, final int minZ, int maxX, int maxY, int maxZ) {
        final int width = maxX - minX + 1;
        final int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        final int end = width * height * depth;
        return () -> new AbstractIterator<BlockPos>(){
            private final MutableBlockPos cursor = new MutableBlockPos();
            private int index;

            protected BlockPos computeNext() {
                if (this.index == end) {
                    return (BlockPos)this.endOfData();
                }
                int x = this.index % width;
                int slice = this.index / width;
                int y = slice % height;
                int z = slice / height;
                ++this.index;
                return this.cursor.set(minX + x, minY + y, minZ + z);
            }
        };
    }

    public static Iterable<BlockPos> neighborColumn(final int startX, final int startY, final int startZ, int endY) {
        final int yDirection = endY > startY ? 1 : -1;
        final int height = Math.abs(endY - startY) + 1;
        final Vec3i[] steps = new Vec3i[]{new Vec3i(0, 0, 0), Direction.NORTH.getUnitVec3i(), Direction.EAST.getUnitVec3i(), Direction.SOUTH.getUnitVec3i(), Direction.WEST.getUnitVec3i()};
        final int stepCount = steps.length * height;
        return () -> new AbstractIterator<BlockPos>(){
            private final MutableBlockPos cursor = new MutableBlockPos();
            private int index;

            protected BlockPos computeNext() {
                if (this.index == stepCount) {
                    return (BlockPos)this.endOfData();
                }
                int y = this.index % height;
                int stepIndex = this.index / height;
                Vec3i step = steps[stepIndex];
                ++this.index;
                return this.cursor.set(startX + step.getX(), startY + y * yDirection, startZ + step.getZ());
            }
        };
    }

    public static Iterable<MutableBlockPos> spiralAround(final BlockPos center, final int radius, final Direction firstDirection, final Direction secondDirection) {
        Validate.validState((firstDirection.getAxis() != secondDirection.getAxis() ? 1 : 0) != 0, (String)"The two directions cannot be on the same axis", (Object[])new Object[0]);
        return () -> new AbstractIterator<MutableBlockPos>(){
            private final Direction[] directions;
            private final MutableBlockPos cursor;
            private final int legs;
            private int leg;
            private int legSize;
            private int legIndex;
            private int lastX;
            private int lastY;
            private int lastZ;
            {
                this.directions = new Direction[]{firstDirection, secondDirection, firstDirection.getOpposite(), secondDirection.getOpposite()};
                this.cursor = center.mutable().move(secondDirection);
                this.legs = 4 * radius;
                this.leg = -1;
                this.lastX = this.cursor.getX();
                this.lastY = this.cursor.getY();
                this.lastZ = this.cursor.getZ();
            }

            protected MutableBlockPos computeNext() {
                this.cursor.set(this.lastX, this.lastY, this.lastZ).move(this.directions[(this.leg + 4) % 4]);
                this.lastX = this.cursor.getX();
                this.lastY = this.cursor.getY();
                this.lastZ = this.cursor.getZ();
                if (this.legIndex >= this.legSize) {
                    if (this.leg >= this.legs) {
                        return (MutableBlockPos)this.endOfData();
                    }
                    ++this.leg;
                    this.legIndex = 0;
                    this.legSize = this.leg / 2 + 1;
                }
                ++this.legIndex;
                return this.cursor;
            }
        };
    }

    public static int breadthFirstTraversal(BlockPos startPos, int maxDepth, int maxCount, BiConsumer<BlockPos, Consumer<BlockPos>> neighbourProvider, Function<BlockPos, TraversalNodeStatus> nodeProcessor) {
        ArrayDeque<Node> nodes = new ArrayDeque<Node>();
        LongOpenHashSet visited = new LongOpenHashSet();
        record Node(BlockPos pos, int depth) {
        }
        nodes.add(new Node(startPos, 0));
        int count = 0;
        while (!nodes.isEmpty()) {
            TraversalNodeStatus next;
            Node node = (Node)nodes.poll();
            BlockPos currentPos = node.pos;
            int depth = node.depth;
            long currentPosLong = currentPos.asLong();
            if (!visited.add(currentPosLong) || (next = nodeProcessor.apply(currentPos)) == TraversalNodeStatus.SKIP) continue;
            if (next == TraversalNodeStatus.STOP) break;
            if (++count >= maxCount) {
                return count;
            }
            if (depth >= maxDepth) continue;
            neighbourProvider.accept(currentPos, pos -> nodes.add(new Node((BlockPos)pos, depth + 1)));
        }
        return count;
    }

    public static Iterable<BlockPos> betweenCornersInDirection(AABB aabb, Vec3 direction) {
        Vec3 minCorner = aabb.getMinPosition();
        int firstCornerX = Mth.floor(minCorner.x());
        int firstCornerY = Mth.floor(minCorner.y());
        int firstCornerZ = Mth.floor(minCorner.z());
        Vec3 maxCorner = aabb.getMaxPosition();
        int secondCornerX = Mth.floor(maxCorner.x());
        int secondCornerY = Mth.floor(maxCorner.y());
        int secondCornerZ = Mth.floor(maxCorner.z());
        return BlockPos.betweenCornersInDirection(firstCornerX, firstCornerY, firstCornerZ, secondCornerX, secondCornerY, secondCornerZ, direction);
    }

    public static Iterable<BlockPos> betweenCornersInDirection(BlockPos firstCorner, BlockPos secondCorner, Vec3 direction) {
        return BlockPos.betweenCornersInDirection(firstCorner.getX(), firstCorner.getY(), firstCorner.getZ(), secondCorner.getX(), secondCorner.getY(), secondCorner.getZ(), direction);
    }

    public static Iterable<BlockPos> betweenCornersInDirection(int firstCornerX, int firstCornerY, int firstCornerZ, int secondCornerX, int secondCornerY, int secondCornerZ, Vec3 direction) {
        int minCornerX = Math.min(firstCornerX, secondCornerX);
        int minCornerY = Math.min(firstCornerY, secondCornerY);
        int minCornerZ = Math.min(firstCornerZ, secondCornerZ);
        int maxCornerX = Math.max(firstCornerX, secondCornerX);
        int maxCornerY = Math.max(firstCornerY, secondCornerY);
        int maxCornerZ = Math.max(firstCornerZ, secondCornerZ);
        int diffX = maxCornerX - minCornerX;
        int diffY = maxCornerY - minCornerY;
        int diffZ = maxCornerZ - minCornerZ;
        final int startCornerX = direction.x >= 0.0 ? minCornerX : maxCornerX;
        final int startCornerY = direction.y >= 0.0 ? minCornerY : maxCornerY;
        final int startCornerZ = direction.z >= 0.0 ? minCornerZ : maxCornerZ;
        ImmutableList<Direction.Axis> axes = Direction.axisStepOrder(direction);
        Direction.Axis firstVisitAxis = (Direction.Axis)axes.get(0);
        Direction.Axis secondVisitAxis = (Direction.Axis)axes.get(1);
        Direction.Axis thirdVisitAxis = (Direction.Axis)axes.get(2);
        final Direction firstVisitDir = direction.get(firstVisitAxis) >= 0.0 ? firstVisitAxis.getPositive() : firstVisitAxis.getNegative();
        final Direction secondVisitDir = direction.get(secondVisitAxis) >= 0.0 ? secondVisitAxis.getPositive() : secondVisitAxis.getNegative();
        final Direction thirdVisitDir = direction.get(thirdVisitAxis) >= 0.0 ? thirdVisitAxis.getPositive() : thirdVisitAxis.getNegative();
        final int firstMax = firstVisitAxis.choose(diffX, diffY, diffZ);
        final int secondMax = secondVisitAxis.choose(diffX, diffY, diffZ);
        final int thirdMax = thirdVisitAxis.choose(diffX, diffY, diffZ);
        return () -> new AbstractIterator<BlockPos>(){
            private final MutableBlockPos cursor = new MutableBlockPos();
            private int firstIndex;
            private int secondIndex;
            private int thirdIndex;
            private boolean end;
            private final int firstDirX = firstVisitDir.getStepX();
            private final int firstDirY = firstVisitDir.getStepY();
            private final int firstDirZ = firstVisitDir.getStepZ();
            private final int secondDirX = secondVisitDir.getStepX();
            private final int secondDirY = secondVisitDir.getStepY();
            private final int secondDirZ = secondVisitDir.getStepZ();
            private final int thirdDirX = thirdVisitDir.getStepX();
            private final int thirdDirY = thirdVisitDir.getStepY();
            private final int thirdDirZ = thirdVisitDir.getStepZ();

            protected BlockPos computeNext() {
                if (this.end) {
                    return (BlockPos)this.endOfData();
                }
                this.cursor.set(startCornerX + this.firstDirX * this.firstIndex + this.secondDirX * this.secondIndex + this.thirdDirX * this.thirdIndex, startCornerY + this.firstDirY * this.firstIndex + this.secondDirY * this.secondIndex + this.thirdDirY * this.thirdIndex, startCornerZ + this.firstDirZ * this.firstIndex + this.secondDirZ * this.secondIndex + this.thirdDirZ * this.thirdIndex);
                if (this.thirdIndex < thirdMax) {
                    ++this.thirdIndex;
                } else if (this.secondIndex < secondMax) {
                    ++this.secondIndex;
                    this.thirdIndex = 0;
                } else if (this.firstIndex < firstMax) {
                    ++this.firstIndex;
                    this.thirdIndex = 0;
                    this.secondIndex = 0;
                } else {
                    this.end = true;
                }
                return this.cursor;
            }
        };
    }

    public static class MutableBlockPos
    extends BlockPos {
        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(int x, int y, int z) {
            super(x, y, z);
        }

        public MutableBlockPos(double x, double y, double z) {
            this(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        }

        @Override
        public BlockPos offset(int x, int y, int z) {
            return super.offset(x, y, z).immutable();
        }

        @Override
        public BlockPos multiply(int scale) {
            return super.multiply(scale).immutable();
        }

        @Override
        public BlockPos relative(Direction direction, int steps) {
            return super.relative(direction, steps).immutable();
        }

        @Override
        public BlockPos relative(Direction.Axis axis, int steps) {
            return super.relative(axis, steps).immutable();
        }

        @Override
        public BlockPos rotate(Rotation rotation) {
            return super.rotate(rotation).immutable();
        }

        public MutableBlockPos set(int x, int y, int z) {
            this.setX(x);
            this.setY(y);
            this.setZ(z);
            return this;
        }

        public MutableBlockPos set(double x, double y, double z) {
            return this.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        }

        public MutableBlockPos set(Vec3i vec) {
            return this.set(vec.getX(), vec.getY(), vec.getZ());
        }

        public MutableBlockPos set(long pos) {
            return this.set(MutableBlockPos.getX(pos), MutableBlockPos.getY(pos), MutableBlockPos.getZ(pos));
        }

        public MutableBlockPos set(AxisCycle transform, int x, int y, int z) {
            return this.set(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
        }

        public MutableBlockPos setWithOffset(Vec3i pos, Direction direction) {
            return this.set(pos.getX() + direction.getStepX(), pos.getY() + direction.getStepY(), pos.getZ() + direction.getStepZ());
        }

        public MutableBlockPos setWithOffset(Vec3i pos, int x, int y, int z) {
            return this.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        }

        public MutableBlockPos setWithOffset(Vec3i pos, Vec3i offset) {
            return this.set(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
        }

        public MutableBlockPos move(Direction direction) {
            return this.move(direction, 1);
        }

        public MutableBlockPos move(Direction direction, int steps) {
            return this.set(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
        }

        public MutableBlockPos move(int x, int y, int z) {
            return this.set(this.getX() + x, this.getY() + y, this.getZ() + z);
        }

        public MutableBlockPos move(Vec3i pos) {
            return this.set(this.getX() + pos.getX(), this.getY() + pos.getY(), this.getZ() + pos.getZ());
        }

        public MutableBlockPos clamp(Direction.Axis axis, int minimum, int maximum) {
            return switch (axis) {
                default -> throw new MatchException(null, null);
                case Direction.Axis.X -> this.set(Mth.clamp(this.getX(), minimum, maximum), this.getY(), this.getZ());
                case Direction.Axis.Y -> this.set(this.getX(), Mth.clamp(this.getY(), minimum, maximum), this.getZ());
                case Direction.Axis.Z -> this.set(this.getX(), this.getY(), Mth.clamp(this.getZ(), minimum, maximum));
            };
        }

        @Override
        public MutableBlockPos setX(int x) {
            super.setX(x);
            return this;
        }

        @Override
        public MutableBlockPos setY(int y) {
            super.setY(y);
            return this;
        }

        @Override
        public MutableBlockPos setZ(int z) {
            super.setZ(z);
            return this;
        }

        @Override
        public BlockPos immutable() {
            return new BlockPos(this);
        }
    }

    public static enum TraversalNodeStatus {
        ACCEPT,
        SKIP,
        STOP;

    }
}

