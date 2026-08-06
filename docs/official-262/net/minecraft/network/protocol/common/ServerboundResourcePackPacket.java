/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;

public record ServerboundResourcePackPacket(UUID id, Action action) implements Packet<ServerCommonPacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundResourcePackPacket> STREAM_CODEC = Packet.codec(ServerboundResourcePackPacket::write, ServerboundResourcePackPacket::new);

    private ServerboundResourcePackPacket(FriendlyByteBuf input) {
        this(input.readUUID(), input.readEnum(Action.class));
    }

    private void write(FriendlyByteBuf output) {
        output.writeUUID(this.id);
        output.writeEnum(this.action);
    }

    @Override
    public PacketType<ServerboundResourcePackPacket> type() {
        return CommonPacketTypes.SERVERBOUND_RESOURCE_PACK;
    }

    @Override
    public void handle(ServerCommonPacketListener listener) {
        listener.handleResourcePackResponse(this);
    }

    public static enum Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED,
        DOWNLOADED,
        INVALID_URL,
        FAILED_RELOAD,
        DISCARDED;


        public boolean isTerminal() {
            return this != ACCEPTED && this != DOWNLOADED;
        }
    }
}

