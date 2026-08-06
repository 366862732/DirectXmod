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

public class ClientboundMountScreenOpenPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundMountScreenOpenPacket> STREAM_CODEC = Packet.codec(ClientboundMountScreenOpenPacket::write, ClientboundMountScreenOpenPacket::new);
    private final int containerId;
    private final int inventoryColumns;
    private final int entityId;

    public ClientboundMountScreenOpenPacket(int containerId, int inventoryColumns, int entityId) {
        this.containerId = containerId;
        this.inventoryColumns = inventoryColumns;
        this.entityId = entityId;
    }

    private ClientboundMountScreenOpenPacket(FriendlyByteBuf input) {
        this.containerId = input.readContainerId();
        this.inventoryColumns = input.readVarInt();
        this.entityId = input.readInt();
    }

    private void write(FriendlyByteBuf output) {
        output.writeContainerId(this.containerId);
        output.writeVarInt(this.inventoryColumns);
        output.writeInt(this.entityId);
    }

    @Override
    public PacketType<ClientboundMountScreenOpenPacket> type() {
        return GamePacketTypes.CLIENTBOUND_MOUNT_SCREEN_OPEN;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleMountScreenOpen(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getInventoryColumns() {
        return this.inventoryColumns;
    }

    public int getEntityId() {
        return this.entityId;
    }
}

