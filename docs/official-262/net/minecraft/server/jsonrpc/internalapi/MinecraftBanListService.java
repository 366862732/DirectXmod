/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public interface MinecraftBanListService {
    public void addUserBan(UserBanListEntry var1, ClientInfo var2);

    public void removeUserBan(NameAndId var1, ClientInfo var2);

    public Collection<UserBanListEntry> getUserBanEntries();

    public Collection<IpBanListEntry> getIpBanEntries();

    public void addIpBan(IpBanListEntry var1, ClientInfo var2);

    public void clearIpBans(ClientInfo var1);

    public void removeIpBan(String var1, ClientInfo var2);

    public void clearUserBans(ClientInfo var1);
}

