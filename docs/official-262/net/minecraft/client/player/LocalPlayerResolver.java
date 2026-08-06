/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.players.ProfileResolver;

@Environment(value=EnvType.CLIENT)
public class LocalPlayerResolver
implements ProfileResolver {
    private final Minecraft minecraft;
    private final ProfileResolver parentResolver;

    public LocalPlayerResolver(Minecraft minecraft, ProfileResolver parentResolver) {
        this.minecraft = minecraft;
        this.parentResolver = parentResolver;
    }

    @Override
    public Optional<GameProfile> fetchByName(String name) {
        PlayerInfo playerInfo;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null && (playerInfo = connection.getPlayerInfoIgnoreCase(name)) != null) {
            return Optional.of(playerInfo.getProfile());
        }
        return this.parentResolver.fetchByName(name);
    }

    @Override
    public Optional<GameProfile> fetchById(UUID id) {
        PlayerInfo playerInfo;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null && (playerInfo = connection.getPlayerInfo(id)) != null) {
            return Optional.of(playerInfo.getProfile());
        }
        return this.parentResolver.fetchById(id);
    }
}

