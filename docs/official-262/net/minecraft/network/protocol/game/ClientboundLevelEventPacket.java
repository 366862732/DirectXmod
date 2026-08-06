/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundLevelEventPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLevelEventPacket> STREAM_CODEC = Packet.codec(ClientboundLevelEventPacket::write, ClientboundLevelEventPacket::new);
    private final int type;
    private final BlockPos pos;
    private final int data;
    private final boolean globalEvent;

    public ClientboundLevelEventPacket(int type, BlockPos pos, int data, boolean globalEvent) {
        this.type = type;
        this.pos = pos.immutable();
        this.data = data;
        this.globalEvent = globalEvent;
    }

    private ClientboundLevelEventPacket(FriendlyByteBuf input) {
        this.type = input.readInt();
        this.pos = input.readBlockPos();
        this.data = input.readInt();
        this.globalEvent = input.readBoolean();
    }

    private void write(FriendlyByteBuf output) {
        output.writeInt(this.type);
        output.writeBlockPos(this.pos);
        output.writeInt(this.data);
        output.writeBoolean(this.globalEvent);
    }

    @Override
    public PacketType<ClientboundLevelEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LEVEL_EVENT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLevelEvent(this);
    }

    public boolean isGlobalEvent() {
        return this.globalEvent;
    }

    public int getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}

