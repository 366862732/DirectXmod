/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.StoredUserEntry;

public class ServerOpListEntry
extends StoredUserEntry<NameAndId> {
    private final LevelBasedPermissionSet permissions;
    private final boolean bypassesPlayerLimit;

    public ServerOpListEntry(NameAndId user, LevelBasedPermissionSet permissions, boolean bypassesPlayerLimit) {
        super(user);
        this.permissions = permissions;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
    }

    public ServerOpListEntry(JsonObject object) {
        super(NameAndId.fromJson(object));
        PermissionLevel level = object.has("level") ? PermissionLevel.byId(object.get("level").getAsInt()) : PermissionLevel.ALL;
        this.permissions = LevelBasedPermissionSet.forLevel(level);
        this.bypassesPlayerLimit = object.has("bypassesPlayerLimit") && object.get("bypassesPlayerLimit").getAsBoolean();
    }

    public LevelBasedPermissionSet permissions() {
        return this.permissions;
    }

    public boolean getBypassesPlayerLimit() {
        return this.bypassesPlayerLimit;
    }

    @Override
    protected void serialize(JsonObject object) {
        if (this.getUser() == null) {
            return;
        }
        ((NameAndId)this.getUser()).appendTo(object);
        object.addProperty("level", (Number)this.permissions.level().id());
        object.addProperty("bypassesPlayerLimit", Boolean.valueOf(this.bypassesPlayerLimit));
    }
}

