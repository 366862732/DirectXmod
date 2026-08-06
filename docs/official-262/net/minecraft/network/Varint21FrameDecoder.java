/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.handler.codec.ByteToMessageDecoder
 *  io.netty.handler.codec.CorruptedFrameException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.VarInt;
import org.jspecify.annotations.Nullable;

public class Varint21FrameDecoder
extends ByteToMessageDecoder {
    private static final int MAX_VARINT21_BYTES = 3;
    private final ByteBuf helperBuf = Unpooled.directBuffer((int)3);
    private final @Nullable BandwidthDebugMonitor monitor;

    public Varint21FrameDecoder(@Nullable BandwidthDebugMonitor monitor) {
        this.monitor = monitor;
    }

    protected void handlerRemoved0(ChannelHandlerContext ctx) {
        this.helperBuf.release();
    }

    private static boolean copyVarint(ByteBuf in, ByteBuf out) {
        for (int i = 0; i < 3; ++i) {
            if (!in.isReadable()) {
                return false;
            }
            byte b = in.readByte();
            out.writeByte((int)b);
            if (VarInt.hasContinuationBit(b)) continue;
            return true;
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        this.helperBuf.clear();
        if (!Varint21FrameDecoder.copyVarint(in, this.helperBuf)) {
            in.resetReaderIndex();
            return;
        }
        int length = VarInt.read(this.helperBuf);
        if (length == 0) {
            throw new CorruptedFrameException("Frame length cannot be zero");
        }
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        if (this.monitor != null) {
            this.monitor.onReceive(length + VarInt.getByteSize(length));
        }
        out.add(in.readBytes(length));
    }
}

