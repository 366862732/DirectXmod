/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public interface MinecraftAllowListService {
    public Collection<UserWhiteListEntry> getEntries();

    public boolean add(UserWhiteListEntry var1, ClientInfo var2);

    public void clear(ClientInfo var1);

    public void remove(NameAndId var1, ClientInfo var2);

    public void kickUnlistedPlayers(ClientInfo var1);
}

