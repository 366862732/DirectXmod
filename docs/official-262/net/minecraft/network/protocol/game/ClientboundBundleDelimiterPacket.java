/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundBundleDelimiterPacket
extends BundleDelimiterPacket<ClientGamePacketListener> {
    @Override
    public PacketType<ClientboundBundleDelimiterPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BUNDLE_DELIMITER;
    }
}

