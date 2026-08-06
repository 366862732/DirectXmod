/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.serialization.JsonOps
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.status;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.StatusPacketTypes;
import net.minecraft.resources.RegistryOps;

public record ClientboundStatusResponsePacket(ServerStatus status) implements Packet<ClientStatusPacketListener>
{
    private static final RegistryOps<JsonElement> OPS = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
    public static final StreamCodec<ByteBuf, ClientboundStatusResponsePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.lenientJson(Short.MAX_VALUE).apply(ByteBufCodecs.fromCodec(OPS, ServerStatus.CODEC)), ClientboundStatusResponsePacket::status, ClientboundStatusResponsePacket::new);

    @Override
    public PacketType<ClientboundStatusResponsePacket> type() {
        return StatusPacketTypes.CLIENTBOUND_STATUS_RESPONSE;
    }

    @Override
    public void handle(ClientStatusPacketListener listener) {
        listener.handleStatusResponse(this);
    }
}

