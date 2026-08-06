/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network;

public enum ConnectionProtocol {
    HANDSHAKING("handshake"),
    PLAY("play"),
    STATUS("status"),
    LOGIN("login"),
    CONFIGURATION("configuration");

    private final String id;

    private ConnectionProtocol(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}

