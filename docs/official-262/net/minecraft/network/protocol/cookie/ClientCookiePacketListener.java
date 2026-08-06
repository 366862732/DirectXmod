/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.cookie;

import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;

public interface ClientCookiePacketListener
extends ClientboundPacketListener {
    public void handleRequestCookie(ClientboundCookieRequestPacket var1);
}

