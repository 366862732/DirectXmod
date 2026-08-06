/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.ping;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;

public interface ClientPongPacketListener
extends PacketListener {
    public void handlePongResponse(ClientboundPongResponsePacket var1);
}

