/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetSlotPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetSlotPacket> STREAM_CODEC = Packet.codec(ClientboundContainerSetSlotPacket::write, ClientboundContainerSetSlotPacket::new);
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack itemStack;

    public ClientboundContainerSetSlotPacket(int containerId, int stateId, int slot, ItemStack itemStack) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slot = slot;
        this.itemStack = itemStack.copy();
    }

    private ClientboundContainerSetSlotPacket(RegistryFriendlyByteBuf input) {
        this.containerId = input.readContainerId();
        this.stateId = input.readVarInt();
        this.slot = input.readShort();
        this.itemStack = (ItemStack)ItemStack.OPTIONAL_STREAM_CODEC.decode(input);
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeContainerId(this.containerId);
        output.writeVarInt(this.stateId);
        output.writeShort(this.slot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(output, this.itemStack);
    }

    @Override
    public PacketType<ClientboundContainerSetSlotPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_SLOT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleContainerSetSlot(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public int getStateId() {
        return this.stateId;
    }
}

