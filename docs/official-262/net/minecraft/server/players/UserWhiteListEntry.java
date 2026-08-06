/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.StoredUserEntry;

public class UserWhiteListEntry
extends StoredUserEntry<NameAndId> {
    public UserWhiteListEntry(NameAndId user) {
        super(user);
    }

    public UserWhiteListEntry(JsonObject object) {
        super(NameAndId.fromJson(object));
    }

    @Override
    protected void serialize(JsonObject object) {
        if (this.getUser() == null) {
            return;
        }
        ((NameAndId)this.getUser()).appendTo(object);
    }
}

