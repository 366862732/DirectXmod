/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.FriendsService
 *  com.mojang.authlib.yggdrasil.response.PresenceResponse
 *  com.mojang.authlib.yggdrasil.response.PresenceStatus
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.social;

import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.response.PresenceResponse;
import com.mojang.authlib.yggdrasil.response.PresenceStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PresenceSharing;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class PresenceHandler {
    private static final Duration PRESENCE_UPDATE_INTERVAL = Duration.ofMinutes(1L);
    private static final long MAX_PRESENCE_INTERVAL_MULTIPLIER = 5L;
    private final Minecraft minecraft;
    private final FriendsService friendsService;
    private PresenceResponse latestPresence = new PresenceResponse(new ArrayList());
    private Instant lastPresencePost = Instant.now();
    private boolean updatePresence;

    public PresenceHandler(Minecraft minecraft, FriendsService friendsService) {
        this.minecraft = minecraft;
        this.friendsService = friendsService;
        this.updatePresence = true;
    }

    private void updatePresence() {
        this.updatePresence = false;
        this.lastPresencePost = Instant.now();
        PresenceStatus publicPresenceStatus = this.getPublicPresenceStatus();
        CompletableFuture.runAsync(() -> {
            PresenceResponse newPresence = this.friendsService.presence(publicPresenceStatus.name());
            this.minecraft.execute(() -> {
                Screen patt0$temp;
                boolean refreshPresence = !Objects.equals(this.latestPresence, newPresence);
                this.latestPresence = newPresence;
                if (refreshPresence && (patt0$temp = this.minecraft.gui.screen()) instanceof FriendsOverlayScreen) {
                    FriendsOverlayScreen friendsOverlayScreen = (FriendsOverlayScreen)patt0$temp;
                    friendsOverlayScreen.applyPresenceUpdate();
                }
            });
        }, Util.nonCriticalIoPool());
    }

    private boolean shouldRefreshPresence() {
        PlayerSocialManager socialManager = this.minecraft.getPlayerSocialManager();
        if (!socialManager.isFriendListEnabled() || socialManager.getFriends().isEmpty()) {
            return false;
        }
        Duration sinceLastPresence = Duration.between(this.lastPresencePost, Instant.now());
        Duration interval = this.friendsService.getPresencePollInterval().orElse(PRESENCE_UPDATE_INTERVAL);
        Duration maxInterval = interval.multipliedBy(5L);
        return this.updatePresence && sinceLastPresence.compareTo(interval) >= 0 || sinceLastPresence.compareTo(maxInterval) >= 0;
    }

    public void tick() {
        if (this.shouldRefreshPresence()) {
            this.updatePresence();
        }
    }

    public void tryUpdatePresence() {
        this.updatePresence = true;
    }

    public PresenceResponse getLatestPresence() {
        return this.latestPresence;
    }

    private PresenceStatus getPublicPresenceStatus() {
        return switch (this.minecraft.options.sharePresence().get()) {
            default -> throw new MatchException(null, null);
            case PresenceSharing.NONE -> PresenceStatus.OFFLINE;
            case PresenceSharing.LIMITED -> PresenceStatus.ONLINE;
            case PresenceSharing.ALL -> this.getPresenceStatus();
        };
    }

    private PresenceStatus getPresenceStatus() {
        IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            return singleplayerServer.getMultiplayerScope() == MinecraftServer.MultiplayerScope.LAN ? PresenceStatus.PLAYING_HOSTED_SERVER : PresenceStatus.PLAYING_OFFLINE;
        }
        ServerData server = this.minecraft.getCurrentServer();
        if (server != null) {
            return server.isRealm() ? PresenceStatus.PLAYING_REALMS : PresenceStatus.PLAYING_SERVER;
        }
        return PresenceStatus.ONLINE;
    }
}

