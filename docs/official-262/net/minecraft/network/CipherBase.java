/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.channel.ChannelHandlerContext
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class CipherBase {
    private final Cipher cipher;
    private byte[] heapIn = new byte[0];
    private byte[] heapOut = new byte[0];

    protected CipherBase(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] bufToByte(ByteBuf in) {
        int readableBytes = in.readableBytes();
        if (this.heapIn.length < readableBytes) {
            this.heapIn = new byte[readableBytes];
        }
        in.readBytes(this.heapIn, 0, readableBytes);
        return this.heapIn;
    }

    protected ByteBuf decipher(ChannelHandlerContext ctx, ByteBuf in) throws ShortBufferException {
        int readableBytes = in.readableBytes();
        byte[] heapIn = this.bufToByte(in);
        ByteBuf heapOut = ctx.alloc().heapBuffer(this.cipher.getOutputSize(readableBytes));
        heapOut.writerIndex(this.cipher.update(heapIn, 0, readableBytes, heapOut.array(), heapOut.arrayOffset()));
        return heapOut;
    }

    protected void encipher(ByteBuf in, ByteBuf out) throws ShortBufferException {
        int readableBytes = in.readableBytes();
        byte[] heapIn = this.bufToByte(in);
        int outputSize = this.cipher.getOutputSize(readableBytes);
        if (this.heapOut.length < outputSize) {
            this.heapOut = new byte[outputSize];
        }
        out.writeBytes(this.heapOut, 0, this.cipher.update(heapIn, 0, readableBytes, this.heapOut));
    }
}

