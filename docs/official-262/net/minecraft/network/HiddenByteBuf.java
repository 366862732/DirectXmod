/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufUtil
 *  io.netty.util.ReferenceCounted
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted
{
    public HiddenByteBuf(ByteBuf contents) {
        this.contents = ByteBufUtil.ensureAccessible((ByteBuf)contents);
    }

    public static Object pack(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf)msg;
            return new HiddenByteBuf(buf);
        }
        return msg;
    }

    public static Object unpack(Object msg) {
        if (msg instanceof HiddenByteBuf) {
            HiddenByteBuf buf = (HiddenByteBuf)msg;
            return ByteBufUtil.ensureAccessible((ByteBuf)buf.contents);
        }
        return msg;
    }

    public int refCnt() {
        return this.contents.refCnt();
    }

    public HiddenByteBuf retain() {
        this.contents.retain();
        return this;
    }

    public HiddenByteBuf retain(int increment) {
        this.contents.retain(increment);
        return this;
    }

    public HiddenByteBuf touch() {
        this.contents.touch();
        return this;
    }

    public HiddenByteBuf touch(Object hint) {
        this.contents.touch(hint);
        return this;
    }

    public boolean release() {
        return this.contents.release();
    }

    public boolean release(int decrement) {
        return this.contents.release(decrement);
    }
}

