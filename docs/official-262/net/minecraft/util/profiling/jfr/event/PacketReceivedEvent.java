/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.event;

import java.net.SocketAddress;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.util.profiling.jfr.event.PacketEvent;

@Name(value="minecraft.PacketReceived")
@Label(value="Network Packet Received")
public class PacketReceivedEvent
extends PacketEvent {
    public static final String NAME = "minecraft.PacketReceived";
    public static final EventType TYPE = EventType.getEventType(PacketReceivedEvent.class);

    public PacketReceivedEvent(String protocolId, String packetDirection, String packetId, SocketAddress remoteAddress, int readableBytes) {
        super(protocolId, packetDirection, packetId, remoteAddress, readableBytes);
    }
}

