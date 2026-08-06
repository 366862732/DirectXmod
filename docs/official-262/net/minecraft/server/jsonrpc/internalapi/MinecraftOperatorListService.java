/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public interface MinecraftOperatorListService {
    public Collection<ServerOpListEntry> getEntries();

    public void op(NameAndId var1, Optional<PermissionLevel> var2, Optional<Boolean> var3, ClientInfo var4);

    public void op(NameAndId var1, ClientInfo var2);

    public void deop(NameAndId var1, ClientInfo var2);

    public void clear(ClientInfo var1);
}

