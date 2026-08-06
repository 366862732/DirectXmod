/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.FriendsService
 *  com.mojang.authlib.yggdrasil.FriendsService$ResultCode
 *  com.mojang.authlib.yggdrasil.response.FriendData
 *  com.mojang.authlib.yggdrasil.response.FriendDto
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.social;

import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.response.FriendData;
import com.mojang.authlib.yggdrasil.response.FriendDto;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.FriendToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public final class RemoteFriendListUpdateHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long FOREGROUND_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1L);
    private static final long BACKGROUND_INTERVAL_MULTIPLIER = 5L;
    private static final long POLL_INTERVAL_SECONDS = 1L;
    private final FriendsService friendsService;
    private final Minecraft minecraft;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final Set<Runnable> updateListeners = new CopyOnWriteArraySet<Runnable>();
    private volatile long lastUpdateNanos = 0L;
    private volatile FriendData latestFriendData = FriendData.empty();
    private volatile State state = State.LOADING;
    private volatile Set<FriendDto> knownFriends = new HashSet<FriendDto>();
    private volatile Set<FriendDto> knownIncoming = new HashSet<FriendDto>();
    private volatile Set<FriendDto> knownOutgoing = new HashSet<FriendDto>();
    private @Nullable ScheduledFuture<?> scheduledTick;

    public RemoteFriendListUpdateHandler(FriendsService friendsService, Minecraft minecraft) {
        this.friendsService = friendsService;
        this.minecraft = minecraft;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Friends List");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void runBackgroundTick() {
        if (this.updateInProgress.get() || !this.enabled.get()) {
            return;
        }
        long now = System.nanoTime();
        if (this.lastUpdateNanos == 0L || now - this.lastUpdateNanos >= this.getUpdateIntervalNanos()) {
            this.runUpdateFriendDataInternal();
        }
    }

    public FriendData getLatestFriendData() {
        return this.latestFriendData;
    }

    public State getState() {
        return this.state;
    }

    public void addUpdateListener(Runnable listener) {
        this.updateListeners.add(listener);
    }

    public void removeUpdateListener(Runnable listener) {
        this.updateListeners.remove(listener);
    }

    private long getUpdateIntervalNanos() {
        long foregroundNanos = this.friendsService.getFriendsPollInterval().map(Duration::toNanos).orElse(FOREGROUND_INTERVAL_NANOS);
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof FriendsOverlayScreen) {
            return foregroundNanos;
        }
        return foregroundNanos * 5L;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void runUpdateFriendDataInternal() {
        if (!this.updateInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Attempted to run Friends List update but update is already in progress");
            return;
        }
        LOGGER.debug("Performing Friends List update");
        AtomicReference<FriendData> friendData = new AtomicReference<FriendData>(FriendData.empty());
        boolean shouldNotifyListeners = false;
        try {
            FriendData data;
            FriendsService.ResultCode resultCode = this.friendsService.getFriendData(friendData::set);
            State newState = RemoteFriendListUpdateHandler.mapResultCodeToState(resultCode);
            State previousState = this.state;
            boolean stateTransition = previousState != newState;
            this.state = newState;
            if (resultCode != FriendsService.ResultCode.SUCCESS) {
                LOGGER.warn("Friends List update failed with result code: {}", (Object)resultCode);
                shouldNotifyListeners = true;
                return;
            }
            this.latestFriendData = data = friendData.get();
            boolean dataChanged = this.detectChangesAndShowToast(data, previousState);
            shouldNotifyListeners = dataChanged || stateTransition;
        }
        catch (Throwable e) {
            LOGGER.warn("Failed to update friend data", e);
        }
        finally {
            this.updateInProgress.set(false);
            this.lastUpdateNanos = System.nanoTime();
            if (shouldNotifyListeners) {
                this.notifyListeners();
            }
        }
    }

    private static State mapResultCodeToState(FriendsService.ResultCode resultCode) {
        return switch (resultCode) {
            default -> throw new MatchException(null, null);
            case FriendsService.ResultCode.TEMPORARY_UNAVAILABLE, FriendsService.ResultCode.FORBIDDEN, FriendsService.ResultCode.SERVICE_NOT_AVAILABLE, FriendsService.ResultCode.TOO_MANY_REQUESTS -> State.TEMPORARY_UNAVAILABLE;
            case FriendsService.ResultCode.CONNECTION_ISSUE -> State.CONNECTION_ISSUE;
            case FriendsService.ResultCode.UPGRADE_NEEDED -> State.UPGRADE_NEEDED;
            case FriendsService.ResultCode.UNKNOWN_PROFILE -> State.USER_MAY_LACK_ACTIVE_PROFILE;
            case FriendsService.ResultCode.UNAUTHORIZED -> State.UNAUTHORIZED;
            case FriendsService.ResultCode.GENERIC_ERROR, FriendsService.ResultCode.ERROR -> State.GENERIC_ERROR;
            case FriendsService.ResultCode.SUCCESS -> State.SUCCESS;
        };
    }

    private void notifyListeners() {
        if (this.updateListeners.isEmpty()) {
            return;
        }
        LOGGER.debug("Notifying {} Friends List update listeners", (Object)this.updateListeners.size());
        this.minecraft.execute(() -> {
            for (Runnable listener : this.updateListeners) {
                try {
                    listener.run();
                }
                catch (Throwable e) {
                    LOGGER.warn("Friends List callback failed", e);
                }
            }
        });
    }

    private boolean detectChangesAndShowToast(FriendData friendData, State previousState) {
        HashSet<FriendDto> currentFriends = new HashSet<FriendDto>(friendData.friends());
        HashSet<FriendDto> currentIncoming = new HashSet<FriendDto>(friendData.incomingRequests());
        HashSet<FriendDto> currentOutgoing = new HashSet<FriendDto>(friendData.outgoingRequests());
        if (previousState != State.SUCCESS) {
            this.knownFriends = currentFriends;
            this.knownIncoming = currentIncoming;
            this.knownOutgoing = currentOutgoing;
            return true;
        }
        if (!this.isInGameAndToastsDisabled()) {
            for (FriendDto friendDto : currentFriends) {
                if (this.knownFriends.contains(friendDto)) continue;
                if (this.knownOutgoing.contains(friendDto) || this.knownIncoming.contains(friendDto)) {
                    this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendRequestAccepted);
                    continue;
                }
                this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendAdded);
            }
            for (FriendDto friendDto : currentIncoming) {
                if (this.knownIncoming.contains(friendDto) || currentFriends.contains(friendDto)) continue;
                this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendRequestReceived);
            }
            for (FriendDto friendDto : currentOutgoing) {
                if (this.knownOutgoing.contains(friendDto) || currentFriends.contains(friendDto)) continue;
                this.minecraft.execute(() -> FriendToast.showFriendRequestSent(this.minecraft, friendDto.name()));
            }
        }
        boolean hasChanges = !this.knownFriends.equals(currentFriends) || !this.knownIncoming.equals(currentIncoming) || !this.knownOutgoing.equals(currentOutgoing);
        this.knownFriends = currentFriends;
        this.knownIncoming = currentIncoming;
        this.knownOutgoing = currentOutgoing;
        return hasChanges;
    }

    private boolean isInGameAndToastsDisabled() {
        return this.minecraft.level != null && this.minecraft.options.inGameNotification().get() == false;
    }

    private void emitToastWithSkin(UUID playerId, String playerName, FriendToast.SkinToastEmitter emitter) {
        this.minecraft.execute(() -> emitter.emit(this.minecraft, playerName, playerId));
    }

    public CompletableFuture<Void> forceUpdate() {
        if (!this.enabled.get() || this.scheduler.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        try {
            this.scheduler.execute(() -> {
                try {
                    this.runUpdateFriendDataInternal();
                }
                finally {
                    future.complete(null);
                }
            });
        }
        catch (Throwable e) {
            LOGGER.warn("Failed to schedule forced Friends List update", e);
            future.complete(null);
        }
        return future;
    }

    public synchronized void start() {
        if (this.scheduler.isShutdown()) {
            LOGGER.warn("Attempted to start Friends List updater but scheduler is already shut down");
            return;
        }
        if (!this.enabled.compareAndSet(false, true)) {
            return;
        }
        if (this.scheduledTick == null || this.scheduledTick.isCancelled() || this.scheduledTick.isDone()) {
            this.scheduledTick = this.scheduler.scheduleWithFixedDelay(this::runBackgroundTick, 0L, 1L, TimeUnit.SECONDS);
        }
    }

    public synchronized void stop() {
        this.enabled.set(false);
        if (this.scheduledTick != null) {
            this.scheduledTick.cancel(false);
            this.scheduledTick = null;
        }
    }

    public synchronized void close() {
        this.stop();
        this.scheduler.shutdownNow();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum State {
        LOADING,
        UPGRADE_NEEDED,
        CONNECTION_ISSUE,
        USER_MAY_LACK_ACTIVE_PROFILE,
        UNAUTHORIZED,
        TEMPORARY_UNAVAILABLE,
        GENERIC_ERROR,
        SUCCESS;

    }
}

