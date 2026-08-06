/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

@Environment(value=EnvType.CLIENT)
public class DefaultPlayerSkin {
    private static final PlayerSkin[] DEFAULT_SKINS = new PlayerSkin[]{DefaultPlayerSkin.create("entity/player/slim/alex", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/ari", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/efe", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/kai", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/makena", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/noor", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/steve", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/sunny", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/slim/zuri", PlayerModelType.SLIM), DefaultPlayerSkin.create("entity/player/wide/alex", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/ari", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/efe", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/kai", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/makena", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/noor", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/steve", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/sunny", PlayerModelType.WIDE), DefaultPlayerSkin.create("entity/player/wide/zuri", PlayerModelType.WIDE)};

    public static Identifier getDefaultTexture() {
        return DefaultPlayerSkin.getDefaultSkin().body().texturePath();
    }

    public static PlayerSkin getDefaultSkin() {
        return DEFAULT_SKINS[6];
    }

    public static PlayerSkin get(UUID profileId) {
        return DEFAULT_SKINS[Math.floorMod(profileId.hashCode(), DEFAULT_SKINS.length)];
    }

    public static PlayerSkin get(GameProfile profile) {
        return DefaultPlayerSkin.get(profile.id());
    }

    private static PlayerSkin create(String body, PlayerModelType model) {
        return new PlayerSkin(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(body)), null, null, model, true);
    }
}

