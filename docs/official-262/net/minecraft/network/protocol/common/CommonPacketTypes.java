/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
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
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.resources.Identifier;

public class CommonPacketTypes {
    public static final PacketType<ClientboundClearDialogPacket> CLIENTBOUND_CLEAR_DIALOG = CommonPacketTypes.createClientbound("clear_dialog");
    public static final PacketType<ClientboundCustomPayloadPacket> CLIENTBOUND_CUSTOM_PAYLOAD = CommonPacketTypes.createClientbound("custom_payload");
    public static final PacketType<ClientboundCustomReportDetailsPacket> CLIENTBOUND_CUSTOM_REPORT_DETAILS = CommonPacketTypes.createClientbound("custom_report_details");
    public static final PacketType<ClientboundDisconnectPacket> CLIENTBOUND_DISCONNECT = CommonPacketTypes.createClientbound("disconnect");
    public static final PacketType<ClientboundKeepAlivePacket> CLIENTBOUND_KEEP_ALIVE = CommonPacketTypes.createClientbound("keep_alive");
    public static final PacketType<ClientboundPingPacket> CLIENTBOUND_PING = CommonPacketTypes.createClientbound("ping");
    public static final PacketType<ClientboundResourcePackPopPacket> CLIENTBOUND_RESOURCE_PACK_POP = CommonPacketTypes.createClientbound("resource_pack_pop");
    public static final PacketType<ClientboundResourcePackPushPacket> CLIENTBOUND_RESOURCE_PACK_PUSH = CommonPacketTypes.createClientbound("resource_pack_push");
    public static final PacketType<ClientboundServerLinksPacket> CLIENTBOUND_SERVER_LINKS = CommonPacketTypes.createClientbound("server_links");
    public static final PacketType<ClientboundShowDialogPacket> CLIENTBOUND_SHOW_DIALOG = CommonPacketTypes.createClientbound("show_dialog");
    public static final PacketType<ClientboundStoreCookiePacket> CLIENTBOUND_STORE_COOKIE = CommonPacketTypes.createClientbound("store_cookie");
    public static final PacketType<ClientboundTransferPacket> CLIENTBOUND_TRANSFER = CommonPacketTypes.createClientbound("transfer");
    public static final PacketType<ClientboundUpdateTagsPacket> CLIENTBOUND_UPDATE_TAGS = CommonPacketTypes.createClientbound("update_tags");
    public static final PacketType<ServerboundClientInformationPacket> SERVERBOUND_CLIENT_INFORMATION = CommonPacketTypes.createServerbound("client_information");
    public static final PacketType<ServerboundCustomPayloadPacket> SERVERBOUND_CUSTOM_PAYLOAD = CommonPacketTypes.createServerbound("custom_payload");
    public static final PacketType<ServerboundKeepAlivePacket> SERVERBOUND_KEEP_ALIVE = CommonPacketTypes.createServerbound("keep_alive");
    public static final PacketType<ServerboundPongPacket> SERVERBOUND_PONG = CommonPacketTypes.createServerbound("pong");
    public static final PacketType<ServerboundResourcePackPacket> SERVERBOUND_RESOURCE_PACK = CommonPacketTypes.createServerbound("resource_pack");
    public static final PacketType<ServerboundCustomClickActionPacket> SERVERBOUND_CUSTOM_CLICK_ACTION = CommonPacketTypes.createServerbound("custom_click_action");

    private static <T extends Packet<ClientCommonPacketListener>> PacketType<T> createClientbound(String id) {
        return new PacketType(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(id));
    }

    private static <T extends Packet<ServerCommonPacketListener>> PacketType<T> createServerbound(String id) {
        return new PacketType(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(id));
    }
}

