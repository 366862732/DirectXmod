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
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStatePacket(float tickRate, boolean isFrozen) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ClientboundTickingStatePacket> STREAM_CODEC = Packet.codec(ClientboundTickingStatePacket::write, ClientboundTickingStatePacket::new);

    private ClientboundTickingStatePacket(FriendlyByteBuf input) {
        this(input.readFloat(), input.readBoolean());
    }

    public static ClientboundTickingStatePacket from(TickRateManager manager) {
        return new ClientboundTickingStatePacket(manager.tickrate(), manager.isFrozen());
    }

    private void write(FriendlyByteBuf output) {
        output.writeFloat(this.tickRate);
        output.writeBoolean(this.isFrozen);
    }

    @Override
    public PacketType<ClientboundTickingStatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_TICKING_STATE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleTickingState(this);
    }
}

