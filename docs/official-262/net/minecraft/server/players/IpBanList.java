/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import org.jspecify.annotations.Nullable;

public class IpBanList
extends StoredUserList<String, IpBanListEntry> {
    public IpBanList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<String> createEntry(JsonObject object) {
        return new IpBanListEntry(object);
    }

    public boolean isBanned(SocketAddress address) {
        String ip = this.getIpFromAddress(address);
        return this.contains(ip);
    }

    public boolean isBanned(String ip) {
        return this.contains(ip);
    }

    @Override
    public @Nullable IpBanListEntry get(SocketAddress address) {
        String ip = this.getIpFromAddress(address);
        return (IpBanListEntry)this.get(ip);
    }

    private String getIpFromAddress(SocketAddress address) {
        String ip = address.toString();
        if (ip.contains("/")) {
            ip = ip.substring(ip.indexOf(47) + 1);
        }
        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(58));
        }
        return ip;
    }

    @Override
    public boolean add(IpBanListEntry infos) {
        if (super.add(infos)) {
            if (infos.getUser() != null) {
                this.notificationService.ipBanned(infos);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(String ip) {
        if (super.remove(ip)) {
            this.notificationService.ipUnbanned(ip);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (IpBanListEntry user : this.getEntries()) {
            if (user.getUser() == null) continue;
            this.notificationService.ipUnbanned((String)user.getUser());
        }
        super.clear();
    }
}

