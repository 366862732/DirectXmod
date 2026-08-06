/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.level.gameevent;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.phys.Vec3;

public record BlockPositionSource(BlockPos pos) implements PositionSource
{
    public static final MapCodec<BlockPositionSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BlockPos.CODEC.fieldOf("pos").forGetter(BlockPositionSource::pos)).apply((Applicative)i, BlockPositionSource::new));
    public static final StreamCodec<ByteBuf, BlockPositionSource> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BlockPositionSource::pos, BlockPositionSource::new);

    @Override
    public Optional<Vec3> getPosition(Level level) {
        return Optional.of(Vec3.atCenterOf(this.pos));
    }

    public PositionSourceType<BlockPositionSource> getType() {
        return PositionSourceType.BLOCK;
    }

    public static class Type
    implements PositionSourceType<BlockPositionSource> {
        @Override
        public MapCodec<BlockPositionSource> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<ByteBuf, BlockPositionSource> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

