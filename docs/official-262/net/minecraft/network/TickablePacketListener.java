/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network;

import net.minecraft.network.PacketListener;

public interface TickablePacketListener
extends PacketListener {
    public void tick();
}

