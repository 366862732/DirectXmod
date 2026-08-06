/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundBundlePacket
extends BundlePacket<ClientGamePacketListener> {
    public ClientboundBundlePacket(Iterable<Packet<? super ClientGamePacketListener>> packets) {
        super(packets);
    }

    @Override
    public PacketType<ClientboundBundlePacket> type() {
        return GamePacketTypes.CLIENTBOUND_BUNDLE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBundlePacket(this);
    }
}

