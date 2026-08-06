/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class LevelLoadTracker
implements LevelLoadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long CLIENT_WAIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30L);
    public static final long LEVEL_LOAD_CLOSE_DELAY_MS = 500L;
    private final LevelLoadProgressTracker serverProgressTracker = new LevelLoadProgressTracker(true);
    private @Nullable ChunkLoadStatusView serverChunkStatusView;
    private volatile @Nullable LevelLoadListener.Stage serverStage;
    private @Nullable ClientState clientState;
    private final long closeDelayMs;

    public LevelLoadTracker() {
        this(0L);
    }

    public LevelLoadTracker(long closeDelayMs) {
        this.closeDelayMs = closeDelayMs;
    }

    public void setServerChunkStatusView(ChunkLoadStatusView serverChunkStatusView) {
        this.serverChunkStatusView = serverChunkStatusView;
    }

    public void startClientLoad(LocalPlayer player, ClientLevel level) {
        this.clientState = new WaitingForServer(player, level, Util.getMillis() + CLIENT_WAIT_TIMEOUT_MS);
    }

    public void tickClientLoad() {
        if (this.clientState != null) {
            this.clientState = this.clientState.tick();
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean isLevelReady() {
        long readyAt;
        ClientState clientState = this.clientState;
        if (!(clientState instanceof ClientLevelReady)) return false;
        ClientLevelReady clientLevelReady = (ClientLevelReady)clientState;
        try {
            long l;
            long l2 = l = clientLevelReady.readyAt();
            if (!true) return false;
            readyAt = l;
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
        if (Util.getMillis() < readyAt + this.closeDelayMs) return false;
        return true;
    }

    public void loadingPacketsReceived() {
        if (this.clientState != null) {
            this.clientState = this.clientState.loadingPacketsReceived();
        }
    }

    @Override
    public void start(LevelLoadListener.Stage stage, int totalChunks) {
        this.serverProgressTracker.start(stage, totalChunks);
        this.serverStage = stage;
    }

    @Override
    public void update(LevelLoadListener.Stage stage, int currentChunks, int totalChunks) {
        this.serverProgressTracker.update(stage, currentChunks, totalChunks);
    }

    @Override
    public void finish(LevelLoadListener.Stage stage) {
        this.serverProgressTracker.finish(stage);
    }

    @Override
    public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        if (this.serverChunkStatusView != null) {
            this.serverChunkStatusView.moveTo(dimension, chunkPos);
        }
    }

    public @Nullable ChunkLoadStatusView statusView() {
        return this.serverChunkStatusView;
    }

    public float serverProgress() {
        return this.serverProgressTracker.get();
    }

    public boolean hasProgress() {
        return this.serverStage != null;
    }

    public @Nullable Runnable getPlayerCompiledSectionCallback() {
        ClientState clientState = this.clientState;
        if (clientState instanceof WaitingForPlayerChunk) {
            WaitingForPlayerChunk waitingForPlayerChunk = (WaitingForPlayerChunk)clientState;
            return () -> waitingForPlayerChunk.playerSectionReady().set(true);
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    private record WaitingForServer(LocalPlayer player, ClientLevel level, long timeoutAfter) implements ClientState
    {
        @Override
        public ClientState loadingPacketsReceived() {
            return new WaitingForPlayerChunk(this.player, this.level, new AtomicBoolean(), this.timeoutAfter);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static sealed interface ClientState
    permits ClientLevelReady, WaitingForPlayerChunk, WaitingForServer {
        default public ClientState tick() {
            return this;
        }

        default public ClientState loadingPacketsReceived() {
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ClientLevelReady(long readyAt) implements ClientState
    {
    }

    @Environment(value=EnvType.CLIENT)
    private record WaitingForPlayerChunk(LocalPlayer player, ClientLevel level, AtomicBoolean playerSectionReady, long timeoutAfter) implements ClientState
    {
        @Override
        public ClientState tick() {
            return this.isReady() ? new ClientLevelReady(Util.getMillis()) : this;
        }

        private boolean isReady() {
            if (Util.getMillis() > this.timeoutAfter) {
                LOGGER.warn("Timed out while waiting for the client to load chunks, letting the player into the world anyway");
                return true;
            }
            BlockPos playerPos = this.player.blockPosition();
            BlockPos cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().blockPosition();
            if (this.level.isOutsideBuildHeight(playerPos.getY()) || this.level.isOutsideBuildHeight(cameraPos.getY()) || this.player.isSpectator() || !this.player.isAlive()) {
                return true;
            }
            return this.playerSectionReady.get();
        }
    }
}

