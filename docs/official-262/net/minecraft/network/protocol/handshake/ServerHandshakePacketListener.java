/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.game.ServerPacketListener;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;

public interface ServerHandshakePacketListener
extends ServerPacketListener {
    @Override
    default public ConnectionProtocol protocol() {
        return ConnectionProtocol.HANDSHAKING;
    }

    public void handleIntention(ClientIntentionPacket var1);
}

