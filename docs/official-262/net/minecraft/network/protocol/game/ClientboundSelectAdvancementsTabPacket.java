/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ClientboundSelectAdvancementsTabPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSelectAdvancementsTabPacket> STREAM_CODEC = Packet.codec(ClientboundSelectAdvancementsTabPacket::write, ClientboundSelectAdvancementsTabPacket::new);
    private final @Nullable Identifier tab;

    public ClientboundSelectAdvancementsTabPacket(@Nullable Identifier tab) {
        this.tab = tab;
    }

    private ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf input) {
        this.tab = input.readNullable(FriendlyByteBuf::readIdentifier);
    }

    private void write(FriendlyByteBuf output) {
        output.writeNullable(this.tab, FriendlyByteBuf::writeIdentifier);
    }

    @Override
    public PacketType<ClientboundSelectAdvancementsTabPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SELECT_ADVANCEMENTS_TAB;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSelectAdvancementsTab(this);
    }

    public @Nullable Identifier getTab() {
        return this.tab;
    }
}

