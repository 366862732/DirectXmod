/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;

public interface ClientLoginPacketListener
extends ClientCookiePacketListener {
    @Override
    default public ConnectionProtocol protocol() {
        return ConnectionProtocol.LOGIN;
    }

    public void handleHello(ClientboundHelloPacket var1);

    public void handleLoginFinished(ClientboundLoginFinishedPacket var1);

    public void handleDisconnect(ClientboundLoginDisconnectPacket var1);

    public void handleCompression(ClientboundLoginCompressionPacket var1);

    public void handleCustomQuery(ClientboundCustomQueryPacket var1);
}

