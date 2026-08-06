/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufUtil
 *  io.netty.handler.codec.DecoderException
 *  io.netty.handler.codec.EncoderException
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.VarInt;

public class Utf8String {
    public static String read(ByteBuf input, int maxLength) {
        int maxEncodedLength = ByteBufUtil.utf8MaxBytes((int)maxLength);
        int bufferLength = VarInt.read(input);
        if (bufferLength > maxEncodedLength) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + bufferLength + " > " + maxEncodedLength + ")");
        }
        if (bufferLength < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }
        int availableBytes = input.readableBytes();
        if (bufferLength > availableBytes) {
            throw new DecoderException("Not enough bytes in buffer, expected " + bufferLength + ", but got " + availableBytes);
        }
        String result = input.toString(input.readerIndex(), bufferLength, StandardCharsets.UTF_8);
        input.readerIndex(input.readerIndex() + bufferLength);
        if (result.length() > maxLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + result.length() + " > " + maxLength + ")");
        }
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void write(ByteBuf output, CharSequence value, int maxLength) {
        if (value.length() > maxLength) {
            throw new EncoderException("String too big (was " + value.length() + " characters, max " + maxLength + ")");
        }
        int maxEncodedValueLength = ByteBufUtil.utf8MaxBytes((CharSequence)value);
        ByteBuf tmp = output.alloc().buffer(maxEncodedValueLength);
        try {
            int bytesWritten = ByteBufUtil.writeUtf8((ByteBuf)tmp, (CharSequence)value);
            int maxAllowedEncodedLength = ByteBufUtil.utf8MaxBytes((int)maxLength);
            if (bytesWritten > maxAllowedEncodedLength) {
                throw new EncoderException("String too big (was " + bytesWritten + " bytes encoded, max " + maxAllowedEncodedLength + ")");
            }
            VarInt.write(output, bytesWritten);
            output.writeBytes(tmp);
        }
        finally {
            tmp.release();
        }
    }
}

