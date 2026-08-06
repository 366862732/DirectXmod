/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.cookie;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;

public class CookiePacketTypes {
    public static final PacketType<ClientboundCookieRequestPacket> CLIENTBOUND_COOKIE_REQUEST = CookiePacketTypes.createClientbound("cookie_request");
    public static final PacketType<ServerboundCookieResponsePacket> SERVERBOUND_COOKIE_RESPONSE = CookiePacketTypes.createServerbound("cookie_response");

    private static <T extends Packet<ClientCookiePacketListener>> PacketType<T> createClientbound(String id) {
        return new PacketType(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace(id));
    }

    private static <T extends Packet<ServerCookiePacketListener>> PacketType<T> createServerbound(String id) {
        return new PacketType(PacketFlow.SERVERBOUND, Identifier.withDefaultNamespace(id));
    }
}

