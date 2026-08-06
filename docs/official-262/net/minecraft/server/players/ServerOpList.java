/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;

public class ServerOpList
extends StoredUserList<NameAndId, ServerOpListEntry> {
    public ServerOpList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject object) {
        return new ServerOpListEntry(object);
    }

    @Override
    public String[] getUserList() {
        return (String[])this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    @Override
    public boolean add(ServerOpListEntry infos) {
        if (super.add(infos)) {
            if (infos.getUser() != null) {
                this.notificationService.playerOped(infos);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(NameAndId user) {
        ServerOpListEntry entry = (ServerOpListEntry)this.get(user);
        if (super.remove(user)) {
            if (entry != null) {
                this.notificationService.playerDeoped(entry);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (ServerOpListEntry user : this.getEntries()) {
            if (user.getUser() == null) continue;
            this.notificationService.playerDeoped(user);
        }
        super.clear();
    }

    public boolean canBypassPlayerLimit(NameAndId user) {
        ServerOpListEntry entry = (ServerOpListEntry)this.get(user);
        if (entry != null) {
            return entry.getBypassesPlayerLimit();
        }
        return false;
    }

    @Override
    protected String getKeyForUser(NameAndId user) {
        return user.id().toString();
    }
}

