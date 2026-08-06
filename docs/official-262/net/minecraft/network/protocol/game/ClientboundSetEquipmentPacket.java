/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ClientboundSetEquipmentPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetEquipmentPacket> STREAM_CODEC = Packet.codec(ClientboundSetEquipmentPacket::write, ClientboundSetEquipmentPacket::new);
    private static final byte CONTINUE_MASK = -128;
    private final int entity;
    private final List<Pair<EquipmentSlot, ItemStack>> slots;

    public ClientboundSetEquipmentPacket(int entity, List<Pair<EquipmentSlot, ItemStack>> slots) {
        this.entity = entity;
        this.slots = slots;
    }

    private ClientboundSetEquipmentPacket(RegistryFriendlyByteBuf input) {
        byte slotId;
        this.entity = input.readVarInt();
        this.slots = Lists.newArrayList();
        do {
            slotId = input.readByte();
            EquipmentSlot slot = EquipmentSlot.VALUES.get(slotId & 0x7F);
            ItemStack itemStack = (ItemStack)ItemStack.OPTIONAL_STREAM_CODEC.decode(input);
            this.slots.add((Pair<EquipmentSlot, ItemStack>)Pair.of((Object)slot, (Object)itemStack));
        } while ((slotId & 0xFFFFFF80) != 0);
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeVarInt(this.entity);
        int size = this.slots.size();
        for (int i = 0; i < size; ++i) {
            Pair<EquipmentSlot, ItemStack> e = this.slots.get(i);
            EquipmentSlot slotType = (EquipmentSlot)e.getFirst();
            boolean shouldContinue = i != size - 1;
            int slotId = slotType.ordinal();
            output.writeByte(shouldContinue ? slotId | 0xFFFFFF80 : slotId);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(output, (ItemStack)e.getSecond());
        }
    }

    @Override
    public PacketType<ClientboundSetEquipmentPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetEquipment(this);
    }

    public int getEntity() {
        return this.entity;
    }

    public List<Pair<EquipmentSlot, ItemStack>> getSlots() {
        return this.slots;
    }
}

