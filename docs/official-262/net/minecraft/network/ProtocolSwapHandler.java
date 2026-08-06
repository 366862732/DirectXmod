/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 */
package net.minecraft.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.UnconfiguredPipelineHandler;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
    public static void handleInboundTerminalPacket(ChannelHandlerContext ctx, Packet<?> packet) {
        if (packet.isTerminal()) {
            ctx.channel().config().setAutoRead(false);
            ctx.pipeline().addBefore(ctx.name(), "inbound_config", (ChannelHandler)new UnconfiguredPipelineHandler.Inbound());
            ctx.pipeline().remove(ctx.name());
        }
    }

    public static void handleOutboundTerminalPacket(ChannelHandlerContext ctx, Packet<?> packet) {
        if (packet.isTerminal()) {
            ctx.pipeline().addAfter(ctx.name(), "outbound_config", (ChannelHandler)new UnconfiguredPipelineHandler.Outbound());
            ctx.pipeline().remove(ctx.name());
        }
    }
}

