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
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderSizePacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderSizePacket> STREAM_CODEC = Packet.codec(ClientboundSetBorderSizePacket::write, ClientboundSetBorderSizePacket::new);
    private final double size;

    public ClientboundSetBorderSizePacket(WorldBorder border) {
        this.size = border.getLerpTarget();
    }

    private ClientboundSetBorderSizePacket(FriendlyByteBuf input) {
        this.size = input.readDouble();
    }

    private void write(FriendlyByteBuf output) {
        output.writeDouble(this.size);
    }

    @Override
    public PacketType<ClientboundSetBorderSizePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_BORDER_SIZE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetBorderSize(this);
    }

    public double getSize() {
        return this.size;
    }
}

