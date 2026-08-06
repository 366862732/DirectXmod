/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.server.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class LegacyProtocolUtils {
    public static final int CUSTOM_PAYLOAD_PACKET_ID = 250;
    public static final String CUSTOM_PAYLOAD_PACKET_PING_CHANNEL = "MC|PingHost";
    public static final int GET_INFO_PACKET_ID = 254;
    public static final int GET_INFO_PACKET_VERSION_1 = 1;
    public static final int DISCONNECT_PACKET_ID = 255;
    public static final int FAKE_PROTOCOL_VERSION = 127;

    public static void writeLegacyString(ByteBuf toSend, String str) {
        toSend.writeShort(str.length());
        toSend.writeCharSequence((CharSequence)str, StandardCharsets.UTF_16BE);
    }

    public static String readLegacyString(ByteBuf msg) {
        short charCount = msg.readShort();
        int byteCount = charCount * 2;
        String str = msg.toString(msg.readerIndex(), byteCount, StandardCharsets.UTF_16BE);
        msg.skipBytes(byteCount);
        return str;
    }
}

