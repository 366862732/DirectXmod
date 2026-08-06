/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.util.Date;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.BanListEntry;
import org.jspecify.annotations.Nullable;

public class IpBanListEntry
extends BanListEntry<String> {
    public IpBanListEntry(String address) {
        this(address, (Date)null, (String)null, (Date)null, (String)null);
    }

    public IpBanListEntry(String address, @Nullable Date created, @Nullable String source, @Nullable Date expires, @Nullable String reason) {
        super(address, created, source, expires, reason);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(String.valueOf(this.getUser()));
    }

    public IpBanListEntry(JsonObject object) {
        super(IpBanListEntry.createIpInfo(object), object);
    }

    private static String createIpInfo(JsonObject object) {
        return object.has("ip") ? object.get("ip").getAsString() : null;
    }

    @Override
    protected void serialize(JsonObject object) {
        if (this.getUser() == null) {
            return;
        }
        object.addProperty("ip", (String)this.getUser());
        super.serialize(object);
    }
}

