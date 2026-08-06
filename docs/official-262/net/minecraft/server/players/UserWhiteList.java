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
import net.minecraft.server.players.UserWhiteListEntry;

public class UserWhiteList
extends StoredUserList<NameAndId, UserWhiteListEntry> {
    public UserWhiteList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject object) {
        return new UserWhiteListEntry(object);
    }

    public boolean isWhiteListed(NameAndId user) {
        return this.contains(user);
    }

    @Override
    public boolean add(UserWhiteListEntry infos) {
        if (super.add(infos)) {
            if (infos.getUser() != null) {
                this.notificationService.playerAddedToAllowlist((NameAndId)infos.getUser());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(NameAndId user) {
        if (super.remove(user)) {
            this.notificationService.playerRemovedFromAllowlist(user);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (UserWhiteListEntry user : this.getEntries()) {
            if (user.getUser() == null) continue;
            this.notificationService.playerRemovedFromAllowlist((NameAndId)user.getUser());
        }
        super.clear();
    }

    @Override
    public String[] getUserList() {
        return (String[])this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    @Override
    protected String getKeyForUser(NameAndId user) {
        return user.id().toString();
    }
}

