/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public record ServerboundContainerSlotStateChangedPacket(int slotId, int containerId, boolean newState) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundContainerSlotStateChangedPacket> STREAM_CODEC = Packet.codec(ServerboundContainerSlotStateChangedPacket::write, ServerboundContainerSlotStateChangedPacket::new);

    private ServerboundContainerSlotStateChangedPacket(FriendlyByteBuf input) {
        this(input.readVarInt(), input.readContainerId(), input.readBoolean());
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.slotId);
        output.writeContainerId(this.containerId);
        output.writeBoolean(this.newState);
    }

    @Override
    public PacketType<ServerboundContainerSlotStateChangedPacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_SLOT_STATE_CHANGED;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleContainerSlotStateChanged(this);
    }
}

