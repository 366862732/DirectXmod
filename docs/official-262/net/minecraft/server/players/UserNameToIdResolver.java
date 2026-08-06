/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.players;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.players.NameAndId;

public interface UserNameToIdResolver {
    public void add(NameAndId var1);

    public Optional<NameAndId> get(String var1);

    public Optional<NameAndId> get(UUID var1);

    public void resolveOfflineUsers(boolean var1);

    public void save();
}

