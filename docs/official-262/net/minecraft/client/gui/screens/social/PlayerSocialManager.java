/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.minecraft.UserApiService
 *  com.mojang.authlib.yggdrasil.FriendsService
 *  com.mojang.authlib.yggdrasil.FriendsService$ResultCode
 *  com.mojang.authlib.yggdrasil.response.FriendDto
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.social;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.response.FriendDto;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.social.PresenceHandler;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class PlayerSocialManager {
    private static final Component FRIEND_ACTION_FAILED_MESSAGE = Component.translatable("gui.friends.error.failed.message");
    private static final Component FRIEND_ACTION_RATE_LIMITED_MESSAGE = Component.translatable("gui.friends.error.rateLimited.message");
    private static final Component FRIEND_ACTION_FORBIDDEN_MESSAGE = Component.translatable("gui.friends.error.forbidden.message");
    private static final Component FRIEND_ACTION_UNKNOWN_PROFILE = Component.translatable("gui.friends.error.user_may_lack_active_profile");
    private static final Component FRIEND_ACTION_UNAUTHORIZED = Component.translatable("gui.friends.error.unauthorized");
    private static final Component FRIEND_ACTION_UNAVAILABLE_MESSAGE = Component.translatable("gui.friends.error.unavailable.message");
    private final Minecraft minecraft;
    private final Set<UUID> hiddenPlayers = Sets.newHashSet();
    private final UserApiService service;
    private final FriendsService friendsService;
    private final PresenceHandler presenceHandler;
    private final Map<String, UUID> discoveredNamesToUUID = Maps.newHashMap();
    private boolean onlineMode;
    private CompletableFuture<?> pendingBlockListRefresh = CompletableFuture.completedFuture(null);
    private final RemoteFriendListUpdateHandler remoteFriendListUpdateHandler;
    private boolean friendListEnabled;
    private boolean allowFriendRequests;

    public PlayerSocialManager(Minecraft minecraft, UserApiService service, FriendsService friendsService, RemoteFriendListUpdateHandler remoteFriendListUpdateHandler) {
        this.minecraft = minecraft;
        this.service = service;
        this.friendsService = friendsService;
        this.remoteFriendListUpdateHandler = remoteFriendListUpdateHandler;
        this.friendListEnabled = minecraft.friendsEnabled();
        this.allowFriendRequests = minecraft.allowFriendRequests();
        this.presenceHandler = new PresenceHandler(minecraft, friendsService);
    }

    public void addFriendListUpdateListener(Runnable listener) {
        this.remoteFriendListUpdateHandler.addUpdateListener(listener);
    }

    public void removeFriendListUpdateListener(Runnable listener) {
        this.remoteFriendListUpdateHandler.removeUpdateListener(listener);
    }

    public List<PlayerData> getFriends() {
        return PlayerSocialManager.remap(this.remoteFriendListUpdateHandler.getLatestFriendData().friends());
    }

    public List<PlayerData> getIncomingRequests() {
        return PlayerSocialManager.remap(this.remoteFriendListUpdateHandler.getLatestFriendData().incomingRequests());
    }

    public List<PlayerData> getOutgoingRequests() {
        return PlayerSocialManager.remap(this.remoteFriendListUpdateHandler.getLatestFriendData().outgoingRequests());
    }

    public RemoteFriendListUpdateHandler.State getFriendListState() {
        return this.remoteFriendListUpdateHandler.getState();
    }

    public void hidePlayer(UUID id) {
        this.hiddenPlayers.add(id);
    }

    public void showPlayer(UUID id) {
        this.hiddenPlayers.remove(id);
    }

    public boolean shouldHideMessageFrom(UUID id) {
        return this.isHidden(id) || this.isBlocked(id);
    }

    public boolean isHidden(UUID id) {
        return this.hiddenPlayers.contains(id);
    }

    public void startOnlineMode() {
        this.onlineMode = true;
        this.pendingBlockListRefresh = this.pendingBlockListRefresh.thenRunAsync(() -> ((UserApiService)this.service).refreshBlockList(), Util.nonCriticalIoPool());
    }

    public void stopOnlineMode() {
        this.onlineMode = false;
    }

    public boolean isBlocked(UUID id) {
        if (!this.onlineMode) {
            return false;
        }
        this.pendingBlockListRefresh.join();
        return this.service.isBlockedPlayer(id);
    }

    public Set<UUID> getHiddenPlayers() {
        return this.hiddenPlayers;
    }

    public UUID getDiscoveredUUID(String name) {
        return this.discoveredNamesToUUID.getOrDefault(name, Util.NIL_UUID);
    }

    public void addPlayer(PlayerInfo info) {
        GameProfile gameProfile = info.getProfile();
        this.discoveredNamesToUUID.put(gameProfile.name(), gameProfile.id());
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof SocialInteractionsScreen) {
            SocialInteractionsScreen screen2 = (SocialInteractionsScreen)screen;
            screen2.onAddPlayer(info);
        }
    }

    public CompletableFuture<FriendsService.ResultCode> sendFriendRequest(String name) {
        return this.runAction(() -> this.friendsService.sendFriendRequest(name));
    }

    public void removePlayer(UUID id) {
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof SocialInteractionsScreen) {
            SocialInteractionsScreen screen2 = (SocialInteractionsScreen)screen;
            screen2.onRemovePlayer(id);
        }
    }

    public CompletableFuture<FriendsService.ResultCode> removeFriend(UUID id) {
        return this.runAction(() -> this.friendsService.removeFriend(id));
    }

    public CompletableFuture<FriendsService.ResultCode> acceptIncomingFriendRequest(UUID id) {
        return this.runAction(() -> this.friendsService.acceptIncomingFriendRequest(id));
    }

    public CompletableFuture<FriendsService.ResultCode> declineIncomingFriendRequest(UUID id) {
        return this.runAction(() -> this.friendsService.declineIncomingFriendRequest(id));
    }

    public CompletableFuture<FriendsService.ResultCode> revokeOutgoingFriendRequest(UUID id) {
        return this.runAction(() -> this.friendsService.revokeOutgoingFriendRequest(id));
    }

    public CompletableFuture<FriendsService.ResultCode> updateFriendSettings(boolean friendsListEnabled, boolean allowInvites) {
        return this.runAction(() -> this.friendsService.updateFriendSettings(friendsListEnabled, allowInvites));
    }

    private CompletableFuture<FriendsService.ResultCode> runAction(Supplier<FriendsService.ResultCode> action) {
        return CompletableFuture.supplyAsync(() -> {
            FriendsService.ResultCode result = (FriendsService.ResultCode)action.get();
            this.handleResult(result);
            return result;
        }, Util.ioPool()).thenComposeAsync(result -> {
            if (result == FriendsService.ResultCode.SUCCESS) {
                return this.remoteFriendListUpdateHandler.forceUpdate().thenApply(void_ -> result);
            }
            return CompletableFuture.completedFuture(result);
        }, (Executor)Util.ioPool());
    }

    private void handleResult(FriendsService.ResultCode result) {
        if (result != FriendsService.ResultCode.SUCCESS) {
            this.showFailureToast(result);
        }
    }

    private void showFailureToast(FriendsService.ResultCode resultCode) {
        Component title;
        switch (resultCode) {
            default: {
                throw new MatchException(null, null);
            }
            case TOO_MANY_REQUESTS: {
                Component component = FRIEND_ACTION_RATE_LIMITED_MESSAGE;
                break;
            }
            case UNKNOWN_PROFILE: {
                Component component = FRIEND_ACTION_UNKNOWN_PROFILE;
                break;
            }
            case UNAUTHORIZED: {
                Component component = FRIEND_ACTION_UNAUTHORIZED;
                break;
            }
            case FORBIDDEN: {
                Component component = FRIEND_ACTION_FORBIDDEN_MESSAGE;
                break;
            }
            case SERVICE_NOT_AVAILABLE: {
                Component component = FRIEND_ACTION_UNAVAILABLE_MESSAGE;
                break;
            }
            case ERROR: {
                Component component = FRIEND_ACTION_FAILED_MESSAGE;
                break;
            }
            case SUCCESS: 
            case UPGRADE_NEEDED: 
            case CONNECTION_ISSUE: 
            case TEMPORARY_UNAVAILABLE: 
            case GENERIC_ERROR: {
                Component component = title = null;
            }
        }
        if (title == null) {
            return;
        }
        this.minecraft.execute(() -> SystemToast.addOrUpdate(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.FRIEND_SYSTEM_NOTIFICATION, title, null));
    }

    public boolean isFriendListEnabled() {
        return this.friendListEnabled;
    }

    public void setFriendListEnabled(boolean friendListEnabled) {
        this.friendListEnabled = friendListEnabled;
        if (friendListEnabled) {
            this.remoteFriendListUpdateHandler.start();
        } else {
            this.remoteFriendListUpdateHandler.stop();
        }
    }

    public boolean isAllowFriendRequests() {
        return this.allowFriendRequests;
    }

    public void setAllowFriendRequests(boolean allowFriendRequests) {
        this.allowFriendRequests = allowFriendRequests;
    }

    public PresenceHandler getPresenceHandler() {
        return this.presenceHandler;
    }

    public boolean isFriend(UUID uuid) {
        for (PlayerData playerData : this.getFriends()) {
            if (!playerData.id.equals(uuid)) continue;
            return true;
        }
        return false;
    }

    private static List<PlayerData> remap(List<FriendDto> friends) {
        return friends.stream().map(friend -> new PlayerData(friend.profileId(), friend.name())).toList();
    }

    @Environment(value=EnvType.CLIENT)
    public record PlayerData(UUID id, String name) {
    }
}

