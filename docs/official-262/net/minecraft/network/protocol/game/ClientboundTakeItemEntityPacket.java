/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundTakeItemEntityPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTakeItemEntityPacket> STREAM_CODEC = Packet.codec(ClientboundTakeItemEntityPacket::write, ClientboundTakeItemEntityPacket::new);
    private final int itemId;
    private final int playerId;
    private final int amount;

    public ClientboundTakeItemEntityPacket(int itemId, int playerId, int amount) {
        this.itemId = itemId;
        this.playerId = playerId;
        this.amount = amount;
    }

    private ClientboundTakeItemEntityPacket(FriendlyByteBuf input) {
        this.itemId = input.readVarInt();
        this.playerId = input.readVarInt();
        this.amount = input.readVarInt();
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.itemId);
        output.writeVarInt(this.playerId);
        output.writeVarInt(this.amount);
    }

    @Override
    public PacketType<ClientboundTakeItemEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleTakeItemEntity(this);
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public int getAmount() {
        return this.amount;
    }
}

