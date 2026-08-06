/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufAllocator
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelInboundHandlerAdapter
 *  io.netty.util.concurrent.GenericFutureListener
 *  org.slf4j.Logger
 */
package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.network.LegacyProtocolUtils;
import org.slf4j.Logger;

public class LegacyQueryHandler
extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerInfo server;

    public LegacyQueryHandler(ServerInfo server) {
        this.server = server;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf)msg;
        in.markReaderIndex();
        boolean connectNormally = true;
        try {
            if (in.readUnsignedByte() != 254) {
                return;
            }
            SocketAddress socket = ctx.channel().remoteAddress();
            int length = in.readableBytes();
            if (length == 0) {
                LOGGER.debug("Ping: (<1.3.x) from {}", (Object)socket);
                String body = LegacyQueryHandler.createVersion0Response(this.server);
                LegacyQueryHandler.sendFlushAndClose(ctx, LegacyQueryHandler.createLegacyDisconnectPacket(ctx.alloc(), body));
            } else {
                if (in.readUnsignedByte() != 1) {
                    return;
                }
                if (in.isReadable()) {
                    if (!LegacyQueryHandler.readCustomPayloadPacket(in)) {
                        return;
                    }
                    LOGGER.debug("Ping: (1.6) from {}", (Object)socket);
                } else {
                    LOGGER.debug("Ping: (1.4-1.5.x) from {}", (Object)socket);
                }
                String body = LegacyQueryHandler.createVersion1Response(this.server);
                LegacyQueryHandler.sendFlushAndClose(ctx, LegacyQueryHandler.createLegacyDisconnectPacket(ctx.alloc(), body));
            }
            in.release();
            connectNormally = false;
        }
        catch (RuntimeException runtimeException) {
        }
        finally {
            if (connectNormally) {
                in.resetReaderIndex();
                ctx.channel().pipeline().remove((ChannelHandler)this);
                ctx.fireChannelRead(msg);
            }
        }
    }

    private static boolean readCustomPayloadPacket(ByteBuf in) {
        short packetId = in.readUnsignedByte();
        if (packetId != 250) {
            return false;
        }
        String channelId = LegacyProtocolUtils.readLegacyString(in);
        if (!"MC|PingHost".equals(channelId)) {
            return false;
        }
        int payloadSize = in.readUnsignedShort();
        if (in.readableBytes() != payloadSize) {
            return false;
        }
        short protocolVersion = in.readUnsignedByte();
        if (protocolVersion < 73) {
            return false;
        }
        String host = LegacyProtocolUtils.readLegacyString(in);
        int port = in.readInt();
        return port <= 65535;
    }

    private static String createVersion0Response(ServerInfo server) {
        return String.format(Locale.ROOT, "%s\u00a7%d\u00a7%d", server.getMotd(), server.getPlayerCount(), server.getMaxPlayers());
    }

    private static String createVersion1Response(ServerInfo server) {
        return String.format(Locale.ROOT, "\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, server.getServerVersion(), server.getMotd(), server.getPlayerCount(), server.getMaxPlayers());
    }

    private static void sendFlushAndClose(ChannelHandlerContext ctx, ByteBuf out) {
        ctx.pipeline().firstContext().writeAndFlush((Object)out).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
    }

    private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator alloc, String reason) {
        ByteBuf out = alloc.buffer();
        out.writeByte(255);
        LegacyProtocolUtils.writeLegacyString(out, reason);
        return out;
    }
}

