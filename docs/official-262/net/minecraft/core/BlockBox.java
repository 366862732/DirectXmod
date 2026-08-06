/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

public record BlockBox(BlockPos min, BlockPos max) implements Iterable<BlockPos>
{
    public static final StreamCodec<ByteBuf, BlockBox> STREAM_CODEC = new StreamCodec<ByteBuf, BlockBox>(){

        @Override
        public BlockBox decode(ByteBuf input) {
            return new BlockBox(FriendlyByteBuf.readBlockPos(input), FriendlyByteBuf.readBlockPos(input));
        }

        @Override
        public void encode(ByteBuf output, BlockBox value) {
            FriendlyByteBuf.writeBlockPos(output, value.min());
            FriendlyByteBuf.writeBlockPos(output, value.max());
        }
    };

    public BlockBox(BlockPos min, BlockPos max) {
        this.min = BlockPos.min(min, max);
        this.max = BlockPos.max(min, max);
    }

    public static BlockBox of(BlockPos pos) {
        return new BlockBox(pos, pos);
    }

    public static BlockBox of(BlockPos a, BlockPos b) {
        return new BlockBox(a, b);
    }

    public BlockBox include(BlockPos pos) {
        return new BlockBox(BlockPos.min(this.min, pos), BlockPos.max(this.max, pos));
    }

    public boolean isBlock() {
        return this.min.equals(this.max);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= this.min.getX() && pos.getY() >= this.min.getY() && pos.getZ() >= this.min.getZ() && pos.getX() <= this.max.getX() && pos.getY() <= this.max.getY() && pos.getZ() <= this.max.getZ();
    }

    public AABB aabb() {
        return AABB.encapsulatingFullBlocks(this.min, this.max);
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return BlockPos.betweenClosed(this.min, this.max).iterator();
    }

    public int sizeX() {
        return this.max.getX() - this.min.getX() + 1;
    }

    public int sizeY() {
        return this.max.getY() - this.min.getY() + 1;
    }

    public int sizeZ() {
        return this.max.getZ() - this.min.getZ() + 1;
    }

    public BlockBox extend(Direction direction, int amount) {
        if (amount == 0) {
            return this;
        }
        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            return BlockBox.of(this.min, BlockPos.max(this.min, this.max.relative(direction, amount)));
        }
        return BlockBox.of(BlockPos.min(this.min.relative(direction, amount), this.max), this.max);
    }

    public BlockBox move(Direction direction, int amount) {
        if (amount == 0) {
            return this;
        }
        return new BlockBox(this.min.relative(direction, amount), this.max.relative(direction, amount));
    }

    public BlockBox offset(Vec3i offset) {
        return new BlockBox(this.min.offset(offset), this.max.offset(offset));
    }
}

