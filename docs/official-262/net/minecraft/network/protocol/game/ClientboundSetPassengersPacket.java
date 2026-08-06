/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.entity.Entity;

public class ClientboundSetPassengersPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetPassengersPacket> STREAM_CODEC = Packet.codec(ClientboundSetPassengersPacket::write, ClientboundSetPassengersPacket::new);
    private final int vehicle;
    private final int[] passengers;

    public ClientboundSetPassengersPacket(Entity vehicle) {
        this.vehicle = vehicle.getId();
        List<Entity> entities = vehicle.getPassengers();
        this.passengers = new int[entities.size()];
        for (int i = 0; i < entities.size(); ++i) {
            this.passengers[i] = entities.get(i).getId();
        }
    }

    private ClientboundSetPassengersPacket(FriendlyByteBuf input) {
        this.vehicle = input.readVarInt();
        this.passengers = input.readVarIntArray();
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.vehicle);
        output.writeVarIntArray(this.passengers);
    }

    @Override
    public PacketType<ClientboundSetPassengersPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_PASSENGERS;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetEntityPassengersPacket(this);
    }

    public int[] getPassengers() {
        return this.passengers;
    }

    public int getVehicle() {
        return this.vehicle;
    }
}

