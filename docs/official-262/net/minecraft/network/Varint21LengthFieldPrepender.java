/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.channel.ChannelHandler$Sharable
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.handler.codec.EncoderException
 *  io.netty.handler.codec.MessageToByteEncoder
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.VarInt;

@ChannelHandler.Sharable
public class Varint21LengthFieldPrepender
extends MessageToByteEncoder<ByteBuf> {
    public static final int MAX_VARINT21_BYTES = 3;

    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int bodyLength = msg.readableBytes();
        int headerLength = VarInt.getByteSize(bodyLength);
        if (headerLength > 3) {
            throw new EncoderException("Packet too large: size " + bodyLength + " is over 8");
        }
        out.ensureWritable(headerLength + bodyLength);
        VarInt.write(out, bodyLength);
        out.writeBytes(msg, msg.readerIndex(), bodyLength);
    }
}

