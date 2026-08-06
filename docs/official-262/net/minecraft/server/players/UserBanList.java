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
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import net.minecraft.server.players.UserBanListEntry;

public class UserBanList
extends StoredUserList<NameAndId, UserBanListEntry> {
    public UserBanList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject object) {
        return new UserBanListEntry(object);
    }

    public boolean isBanned(NameAndId user) {
        return this.contains(user);
    }

    @Override
    public String[] getUserList() {
        return (String[])this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    @Override
    protected String getKeyForUser(NameAndId user) {
        return user.id().toString();
    }

    @Override
    public boolean add(UserBanListEntry infos) {
        if (super.add(infos)) {
            if (infos.getUser() != null) {
                this.notificationService.playerBanned(infos);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(NameAndId user) {
        if (super.remove(user)) {
            this.notificationService.playerUnbanned(user);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (UserBanListEntry user : this.getEntries()) {
            if (user.getUser() == null) continue;
            this.notificationService.playerUnbanned((NameAndId)user.getUser());
        }
        super.clear();
    }
}

