/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;

public interface ServerLoginPacketListener
extends ServerCookiePacketListener {
    @Override
    default public ConnectionProtocol protocol() {
        return ConnectionProtocol.LOGIN;
    }

    public void handleHello(ServerboundHelloPacket var1);

    public void handleKey(ServerboundKeyPacket var1);

    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket var1);

    public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket var1);
}

