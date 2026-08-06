/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerCommonPacketListener
extends ServerCookiePacketListener {
    public void handleKeepAlive(ServerboundKeepAlivePacket var1);

    public void handlePong(ServerboundPongPacket var1);

    public void handleCustomPayload(ServerboundCustomPayloadPacket var1);

    public void handleResourcePackResponse(ServerboundResourcePackPacket var1);

    public void handleClientInformation(ServerboundClientInformationPacket var1);

    public void handleCustomClickAction(ServerboundCustomClickActionPacket var1);
}

