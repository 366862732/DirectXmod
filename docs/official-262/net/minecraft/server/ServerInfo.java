/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server;

public interface ServerInfo {
    public String getMotd();

    public String getServerVersion();

    public int getPlayerCount();

    public int getMaxPlayers();
}

