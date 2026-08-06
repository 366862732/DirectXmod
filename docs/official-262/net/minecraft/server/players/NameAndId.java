/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.yggdrasil.response.NameAndId
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.Nullable;

public record NameAndId(UUID id, String name) {
    public static final Codec<NameAndId> CODEC = RecordCodecBuilder.create(i -> i.group((App)UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(NameAndId::id), (App)Codec.STRING.fieldOf("name").forGetter(NameAndId::name)).apply((Applicative)i, NameAndId::new));

    public NameAndId(GameProfile profile) {
        this(profile.id(), profile.name());
    }

    public NameAndId(com.mojang.authlib.yggdrasil.response.NameAndId profile) {
        this(profile.id(), profile.name());
    }

    public static @Nullable NameAndId fromJson(JsonObject object) {
        UUID uuid;
        if (!object.has("uuid") || !object.has("name")) {
            return null;
        }
        String uuidString = object.get("uuid").getAsString();
        try {
            uuid = UUID.fromString(uuidString);
        }
        catch (Throwable ignored) {
            return null;
        }
        return new NameAndId(uuid, object.get("name").getAsString());
    }

    public void appendTo(JsonObject output) {
        output.addProperty("uuid", this.id().toString());
        output.addProperty("name", this.name());
    }

    public static NameAndId createOffline(String name) {
        UUID id = UUIDUtil.createOfflinePlayerUUID(name);
        return new NameAndId(id, name);
    }
}

