/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientCommonPacketListener
extends ClientCookiePacketListener {
    public void handleKeepAlive(ClientboundKeepAlivePacket var1);

    public void handlePing(ClientboundPingPacket var1);

    public void handleCustomPayload(ClientboundCustomPayloadPacket var1);

    public void handleDisconnect(ClientboundDisconnectPacket var1);

    public void handleResourcePackPush(ClientboundResourcePackPushPacket var1);

    public void handleResourcePackPop(ClientboundResourcePackPopPacket var1);

    public void handleUpdateTags(ClientboundUpdateTagsPacket var1);

    public void handleStoreCookie(ClientboundStoreCookiePacket var1);

    public void handleTransfer(ClientboundTransferPacket var1);

    public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket var1);

    public void handleServerLinks(ClientboundServerLinksPacket var1);

    public void handleClearDialog(ClientboundClearDialogPacket var1);

    public void handleShowDialog(ClientboundShowDialogPacket var1);
}

