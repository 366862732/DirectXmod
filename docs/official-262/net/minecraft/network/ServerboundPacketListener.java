/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;

public interface ServerboundPacketListener
extends PacketListener {
    @Override
    default public PacketFlow flow() {
        return PacketFlow.SERVERBOUND;
    }
}

