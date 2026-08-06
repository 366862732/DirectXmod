/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.HashFunction
 *  com.google.common.hash.Hashing
 *  com.mojang.logging.LogUtils
 *  com.mojang.util.UndashedUuid
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.server;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.util.UndashedUuid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.resources.server.PackDownloader;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.packs.DownloadQueue;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.HttpUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class DownloadedPackSource
implements AutoCloseable {
    private static final Component SERVER_NAME = Component.translatable("resourcePack.server.name");
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RepositorySource EMPTY_SOURCE = result -> {};
    private static final PackSelectionConfig DOWNLOADED_PACK_SELECTION = new PackSelectionConfig(true, Pack.Position.TOP, true);
    private static final PackLoadFeedback LOG_ONLY_FEEDBACK = new PackLoadFeedback(){

        @Override
        public void reportUpdate(UUID id, PackLoadFeedback.Update update) {
            LOGGER.debug("Downloaded pack {} changed state to {}", (Object)id, (Object)update);
        }

        @Override
        public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
            LOGGER.debug("Downloaded pack {} finished with state {}", (Object)id, (Object)result);
        }
    };
    private final Minecraft minecraft;
    private RepositorySource packSource = EMPTY_SOURCE;
    private @Nullable PackReloadConfig.Callbacks pendingReload;
    private final ServerPackManager manager;
    private final DownloadQueue downloadQueue;
    private PackSource packType = PackSource.SERVER;
    private PackLoadFeedback packFeedback = LOG_ONLY_FEEDBACK;
    private int packIdSerialNumber;

    public DownloadedPackSource(Minecraft minecraft, Path packCache, GameConfig.UserData user) {
        this.minecraft = minecraft;
        try {
            this.downloadQueue = new DownloadQueue(packCache);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to open download queue in directory " + String.valueOf(packCache), e);
        }
        Executor executor = minecraft::schedule;
        this.manager = new ServerPackManager(this.createDownloader(this.downloadQueue, executor, user.user, user.proxy), new PackLoadFeedback(this){
            final /* synthetic */ DownloadedPackSource this$0;
            {
                DownloadedPackSource downloadedPackSource = this$0;
                Objects.requireNonNull(downloadedPackSource);
                this.this$0 = downloadedPackSource;
            }

            @Override
            public void reportUpdate(UUID id, PackLoadFeedback.Update result) {
                this.this$0.packFeedback.reportUpdate(id, result);
            }

            @Override
            public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
                this.this$0.packFeedback.reportFinalResult(id, result);
            }
        }, this.createReloadConfig(), this.createUpdateScheduler(executor), ServerPackManager.PackPromptStatus.PENDING);
    }

    private HttpUtil.DownloadProgressListener createDownloadNotifier(final int totalCount) {
        return new HttpUtil.DownloadProgressListener(){
            private final SystemToast.SystemToastId toastId;
            private Component title;
            private @Nullable Component message;
            private int count;
            private int failCount;
            private OptionalLong totalBytes;
            final /* synthetic */ DownloadedPackSource this$0;
            {
                DownloadedPackSource downloadedPackSource = this$0;
                Objects.requireNonNull(downloadedPackSource);
                this.this$0 = downloadedPackSource;
                this.toastId = new SystemToast.SystemToastId();
                this.title = Component.empty();
                this.message = null;
                this.totalBytes = OptionalLong.empty();
            }

            private void updateToast() {
                this.this$0.minecraft.execute(() -> SystemToast.addOrUpdate(this.this$0.minecraft.gui.toastManager(), this.toastId, this.title, this.message));
            }

            private void updateProgress(long bytesSoFar) {
                this.message = this.totalBytes.isPresent() ? Component.translatable("download.pack.progress.percent", bytesSoFar * 100L / this.totalBytes.getAsLong()) : Component.translatable("download.pack.progress.bytes", Unit.humanReadable(bytesSoFar));
                this.updateToast();
            }

            @Override
            public void requestStart() {
                ++this.count;
                this.title = Component.translatable("download.pack.title", this.count, totalCount);
                this.updateToast();
                LOGGER.debug("Starting pack {}/{} download", (Object)this.count, (Object)totalCount);
            }

            @Override
            public void downloadStart(OptionalLong sizeBytes) {
                LOGGER.debug("File size = {} bytes", (Object)sizeBytes);
                this.totalBytes = sizeBytes;
                this.updateProgress(0L);
            }

            @Override
            public void downloadedBytes(long bytesSoFar) {
                LOGGER.debug("Progress for pack {}: {} bytes", (Object)this.count, (Object)bytesSoFar);
                this.updateProgress(bytesSoFar);
            }

            @Override
            public void requestFinished(boolean success) {
                if (!success) {
                    LOGGER.info("Pack {} failed to download", (Object)this.count);
                    ++this.failCount;
                } else {
                    LOGGER.debug("Download ended for pack {}", (Object)this.count);
                }
                if (this.count == totalCount) {
                    if (this.failCount > 0) {
                        this.title = Component.translatable("download.pack.failed", this.failCount, totalCount);
                        this.message = null;
                        this.updateToast();
                    } else {
                        SystemToast.forceHide(this.this$0.minecraft.gui.toastManager(), this.toastId);
                    }
                }
            }
        };
    }

    private PackDownloader createDownloader(final DownloadQueue downloadQueue, final Executor mainThreadExecutor, final User user, final Proxy proxy) {
        return new PackDownloader(){
            private static final int MAX_PACK_SIZE_BYTES = 0xFA00000;
            private static final HashFunction CACHE_HASHING_FUNCTION = Hashing.sha1();
            final /* synthetic */ DownloadedPackSource this$0;
            {
                DownloadedPackSource downloadedPackSource = this$0;
                Objects.requireNonNull(downloadedPackSource);
                this.this$0 = downloadedPackSource;
            }

            private Map<String, String> createDownloadHeaders() {
                WorldVersion version = SharedConstants.getCurrentVersion();
                return Map.of("X-Minecraft-Username", user.getName(), "X-Minecraft-UUID", UndashedUuid.toString((UUID)user.getProfileId()), "X-Minecraft-Version", version.name(), "X-Minecraft-Version-ID", version.id(), "X-Minecraft-Pack-Format", String.valueOf(version.packVersion(PackType.CLIENT_RESOURCES)), "User-Agent", "Minecraft Java/" + version.name());
            }

            @Override
            public void download(Map<UUID, DownloadQueue.DownloadRequest> requests, Consumer<DownloadQueue.BatchResult> output) {
                downloadQueue.downloadBatch(new DownloadQueue.BatchConfig(CACHE_HASHING_FUNCTION, 0xFA00000, this.createDownloadHeaders(), proxy, this.this$0.createDownloadNotifier(requests.size())), requests).thenAcceptAsync((Consumer)output, mainThreadExecutor);
            }
        };
    }

    private Runnable createUpdateScheduler(final Executor mainThreadExecutor) {
        return new Runnable(){
            private boolean scheduledInMainExecutor;
            private boolean hasUpdates;
            final /* synthetic */ DownloadedPackSource this$0;
            {
                DownloadedPackSource downloadedPackSource = this$0;
                Objects.requireNonNull(downloadedPackSource);
                this.this$0 = downloadedPackSource;
            }

            @Override
            public void run() {
                this.hasUpdates = true;
                if (!this.scheduledInMainExecutor) {
                    this.scheduledInMainExecutor = true;
                    mainThreadExecutor.execute(this::runAllUpdates);
                }
            }

            private void runAllUpdates() {
                while (this.hasUpdates) {
                    this.hasUpdates = false;
                    this.this$0.manager.tick();
                }
                this.scheduledInMainExecutor = false;
            }
        };
    }

    private PackReloadConfig createReloadConfig() {
        return this::startReload;
    }

    private @Nullable List<Pack> loadRequestedPacks(List<PackReloadConfig.IdAndPath> packsToLoad) {
        ArrayList<Pack> packs = new ArrayList<Pack>(packsToLoad.size());
        for (PackReloadConfig.IdAndPath idAndPath : Lists.reverse(packsToLoad)) {
            PackFormat currentPackVersion;
            FilePackResources.FileResourcesSupplier resources;
            String name = String.format(Locale.ROOT, "server/%08X/%s", this.packIdSerialNumber++, idAndPath.id());
            Path path = idAndPath.path();
            PackLocationInfo packLocationInfo = new PackLocationInfo(name, SERVER_NAME, this.packType, Optional.empty());
            Pack.Metadata metadata = Pack.readPackMetadata(packLocationInfo, resources = new FilePackResources.FileResourcesSupplier(path), currentPackVersion = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES), PackType.CLIENT_RESOURCES);
            if (metadata == null) {
                LOGGER.warn("Invalid pack metadata in {}, ignoring all", (Object)path);
                return null;
            }
            packs.add(new Pack(packLocationInfo, resources, metadata, DOWNLOADED_PACK_SELECTION));
        }
        return packs;
    }

    public RepositorySource createRepositorySource() {
        return output -> this.packSource.loadPacks(output);
    }

    private static RepositorySource configureSource(List<Pack> packs) {
        if (packs.isEmpty()) {
            return EMPTY_SOURCE;
        }
        return packs::forEach;
    }

    private void startReload(PackReloadConfig.Callbacks callbacks) {
        this.pendingReload = callbacks;
        List<PackReloadConfig.IdAndPath> normalPacks = callbacks.packsToLoad();
        List<Pack> packs = this.loadRequestedPacks(normalPacks);
        if (packs == null) {
            callbacks.onFailure(false);
            List<PackReloadConfig.IdAndPath> recoveryPacks = callbacks.packsToLoad();
            packs = this.loadRequestedPacks(recoveryPacks);
            if (packs == null) {
                LOGGER.warn("Double failure in loading server packs");
                packs = List.of();
            }
        }
        this.packSource = DownloadedPackSource.configureSource(packs);
        this.minecraft.reloadResourcePacks();
    }

    public void onRecovery() {
        if (this.pendingReload != null) {
            this.pendingReload.onFailure(false);
            List<Pack> packs = this.loadRequestedPacks(this.pendingReload.packsToLoad());
            if (packs == null) {
                LOGGER.warn("Double failure in loading server packs");
                packs = List.of();
            }
            this.packSource = DownloadedPackSource.configureSource(packs);
        }
    }

    public void onRecoveryFailure() {
        if (this.pendingReload != null) {
            this.pendingReload.onFailure(true);
            this.pendingReload = null;
            this.packSource = EMPTY_SOURCE;
        }
    }

    public void onReloadSuccess() {
        if (this.pendingReload != null) {
            this.pendingReload.onSuccess();
            this.pendingReload = null;
        }
    }

    private static @Nullable HashCode tryParseSha1Hash(@Nullable String hash) {
        if (hash != null && SHA1.matcher(hash).matches()) {
            return HashCode.fromString((String)hash.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    public void pushPack(UUID id, URL url, @Nullable String hash) {
        HashCode parsedHash = DownloadedPackSource.tryParseSha1Hash(hash);
        this.manager.pushPack(id, url, parsedHash);
    }

    public void pushLocalPack(UUID id, Path path) {
        this.manager.pushLocalPack(id, path);
    }

    public void popPack(UUID id) {
        this.manager.popPack(id);
    }

    public void popAll() {
        this.manager.popAll();
    }

    private static PackLoadFeedback createPackResponseSender(final Connection connection) {
        return new PackLoadFeedback(){

            @Override
            public void reportUpdate(UUID id, PackLoadFeedback.Update result) {
                LOGGER.debug("Pack {} changed status to {}", (Object)id, (Object)result);
                ServerboundResourcePackPacket.Action response = switch (result) {
                    default -> throw new MatchException(null, null);
                    case PackLoadFeedback.Update.ACCEPTED -> ServerboundResourcePackPacket.Action.ACCEPTED;
                    case PackLoadFeedback.Update.DOWNLOADED -> ServerboundResourcePackPacket.Action.DOWNLOADED;
                };
                connection.send(new ServerboundResourcePackPacket(id, response));
            }

            @Override
            public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
                LOGGER.debug("Pack {} changed status to {}", (Object)id, (Object)result);
                ServerboundResourcePackPacket.Action response = switch (result) {
                    default -> throw new MatchException(null, null);
                    case PackLoadFeedback.FinalResult.APPLIED -> ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED;
                    case PackLoadFeedback.FinalResult.DOWNLOAD_FAILED -> ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD;
                    case PackLoadFeedback.FinalResult.DECLINED -> ServerboundResourcePackPacket.Action.DECLINED;
                    case PackLoadFeedback.FinalResult.DISCARDED -> ServerboundResourcePackPacket.Action.DISCARDED;
                    case PackLoadFeedback.FinalResult.ACTIVATION_FAILED -> ServerboundResourcePackPacket.Action.FAILED_RELOAD;
                };
                connection.send(new ServerboundResourcePackPacket(id, response));
            }
        };
    }

    public void configureForServerControl(Connection connection, ServerPackManager.PackPromptStatus packPromptStatus) {
        this.packType = PackSource.SERVER;
        this.packFeedback = DownloadedPackSource.createPackResponseSender(connection);
        switch (packPromptStatus) {
            case ALLOWED: {
                this.manager.allowServerPacks();
                break;
            }
            case DECLINED: {
                this.manager.rejectServerPacks();
                break;
            }
            case PENDING: {
                this.manager.resetPromptStatus();
            }
        }
    }

    public void configureForLocalWorld() {
        this.packType = PackSource.WORLD;
        this.packFeedback = LOG_ONLY_FEEDBACK;
        this.manager.allowServerPacks();
    }

    public void allowServerPacks() {
        this.manager.allowServerPacks();
    }

    public void rejectServerPacks() {
        this.manager.rejectServerPacks();
    }

    public CompletableFuture<Void> waitForPackFeedback(final UUID packId) {
        final CompletableFuture<Void> result = new CompletableFuture<Void>();
        final PackLoadFeedback original = this.packFeedback;
        this.packFeedback = new PackLoadFeedback(){
            final /* synthetic */ DownloadedPackSource this$0;
            {
                DownloadedPackSource downloadedPackSource = this$0;
                Objects.requireNonNull(downloadedPackSource);
                this.this$0 = downloadedPackSource;
            }

            @Override
            public void reportUpdate(UUID id, PackLoadFeedback.Update result2) {
                original.reportUpdate(id, result2);
            }

            @Override
            public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult status) {
                if (packId.equals(id)) {
                    this.this$0.packFeedback = original;
                    if (status == PackLoadFeedback.FinalResult.APPLIED) {
                        result.complete(null);
                    } else {
                        result.completeExceptionally(new IllegalStateException("Failed to apply pack " + String.valueOf(id) + ", reason: " + String.valueOf((Object)status)));
                    }
                }
                original.reportFinalResult(id, status);
            }
        };
        return result;
    }

    public void cleanupAfterDisconnect() {
        this.manager.popAll();
        this.packFeedback = LOG_ONLY_FEEDBACK;
        this.manager.resetPromptStatus();
    }

    @Override
    public void close() throws IOException {
        this.downloadQueue.close();
    }
}

