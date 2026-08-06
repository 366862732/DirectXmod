/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.HashCode
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.server;

import com.google.common.hash.HashCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.server.PackDownloader;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.DownloadQueue;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ServerPackManager {
    private final PackDownloader downloader;
    private final PackLoadFeedback packLoadFeedback;
    private final PackReloadConfig reloadConfig;
    private final Runnable updateRequest;
    private PackPromptStatus packPromptStatus;
    private final List<ServerPackData> packs = new ArrayList<ServerPackData>();

    public ServerPackManager(PackDownloader downloader, PackLoadFeedback packLoadFeedback, PackReloadConfig reloadConfig, Runnable updateRequest, PackPromptStatus packPromptStatus) {
        this.downloader = downloader;
        this.packLoadFeedback = packLoadFeedback;
        this.reloadConfig = reloadConfig;
        this.updateRequest = updateRequest;
        this.packPromptStatus = packPromptStatus;
    }

    private void registerForUpdate() {
        this.updateRequest.run();
    }

    private void markExistingPacksAsRemoved(UUID id) {
        for (ServerPackData pack : this.packs) {
            if (!pack.id.equals(id)) continue;
            pack.setRemovalReasonIfNotSet(RemovalReason.SERVER_REPLACED);
        }
    }

    public void pushPack(UUID id, URL url, @Nullable HashCode hash) {
        if (this.packPromptStatus == PackPromptStatus.DECLINED) {
            this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.DECLINED);
            return;
        }
        this.pushNewPack(id, new ServerPackData(id, url, hash));
    }

    public void pushLocalPack(UUID id, Path path) {
        URL url;
        if (this.packPromptStatus == PackPromptStatus.DECLINED) {
            this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.DECLINED);
            return;
        }
        try {
            url = path.toUri().toURL();
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException("Can't convert path to URL " + String.valueOf(path), e);
        }
        ServerPackData pack = new ServerPackData(id, url, null);
        pack.downloadStatus = PackDownloadStatus.DONE;
        pack.path = path;
        this.pushNewPack(id, pack);
    }

    private void pushNewPack(UUID id, ServerPackData pack) {
        this.markExistingPacksAsRemoved(id);
        this.packs.add(pack);
        if (this.packPromptStatus == PackPromptStatus.ALLOWED) {
            this.acceptPack(pack);
        }
        this.registerForUpdate();
    }

    private void acceptPack(ServerPackData pack) {
        this.packLoadFeedback.reportUpdate(pack.id, PackLoadFeedback.Update.ACCEPTED);
        pack.promptAccepted = true;
    }

    private @Nullable ServerPackData findPackInfo(UUID id) {
        for (ServerPackData pack : this.packs) {
            if (pack.isRemoved() || !pack.id.equals(id)) continue;
            return pack;
        }
        return null;
    }

    public void popPack(UUID id) {
        ServerPackData packInfo = this.findPackInfo(id);
        if (packInfo != null) {
            packInfo.setRemovalReasonIfNotSet(RemovalReason.SERVER_REMOVED);
            this.registerForUpdate();
        }
    }

    public void popAll() {
        for (ServerPackData pack : this.packs) {
            pack.setRemovalReasonIfNotSet(RemovalReason.SERVER_REMOVED);
        }
        this.registerForUpdate();
    }

    public void allowServerPacks() {
        this.packPromptStatus = PackPromptStatus.ALLOWED;
        for (ServerPackData pack : this.packs) {
            if (pack.promptAccepted || pack.isRemoved()) continue;
            this.acceptPack(pack);
        }
        this.registerForUpdate();
    }

    public void rejectServerPacks() {
        this.packPromptStatus = PackPromptStatus.DECLINED;
        for (ServerPackData pack : this.packs) {
            if (pack.promptAccepted) continue;
            pack.setRemovalReasonIfNotSet(RemovalReason.DECLINED);
        }
        this.registerForUpdate();
    }

    public void resetPromptStatus() {
        this.packPromptStatus = PackPromptStatus.PENDING;
    }

    public void tick() {
        boolean downloadsPending = this.updateDownloads();
        if (!downloadsPending) {
            this.triggerReloadIfNeeded();
        }
        this.cleanupRemovedPacks();
    }

    private void cleanupRemovedPacks() {
        this.packs.removeIf(data -> {
            if (data.activationStatus != ActivationStatus.INACTIVE) {
                return false;
            }
            if (data.removalReason != null) {
                PackLoadFeedback.FinalResult response = data.removalReason.serverResponse;
                if (response != null) {
                    this.packLoadFeedback.reportFinalResult(data.id, response);
                }
                return true;
            }
            return false;
        });
    }

    private void onDownload(Collection<ServerPackData> data, DownloadQueue.BatchResult result) {
        if (!result.failed().isEmpty()) {
            for (ServerPackData pack : this.packs) {
                if (pack.activationStatus == ActivationStatus.ACTIVE) continue;
                if (result.failed().contains(pack.id)) {
                    pack.setRemovalReasonIfNotSet(RemovalReason.DOWNLOAD_FAILED);
                    continue;
                }
                pack.setRemovalReasonIfNotSet(RemovalReason.DISCARDED);
            }
        }
        for (ServerPackData pack : data) {
            Path packFile = result.downloaded().get(pack.id);
            if (packFile == null) continue;
            pack.downloadStatus = PackDownloadStatus.DONE;
            pack.path = packFile;
            if (pack.isRemoved()) continue;
            this.packLoadFeedback.reportUpdate(pack.id, PackLoadFeedback.Update.DOWNLOADED);
        }
        this.registerForUpdate();
    }

    private boolean updateDownloads() {
        ArrayList<ServerPackData> downloadPacks = new ArrayList<ServerPackData>();
        boolean downloadsInProgress = false;
        for (ServerPackData pack : this.packs) {
            if (pack.isRemoved() || !pack.promptAccepted) continue;
            if (pack.downloadStatus != PackDownloadStatus.DONE) {
                downloadsInProgress = true;
            }
            if (pack.downloadStatus != PackDownloadStatus.REQUESTED) continue;
            pack.downloadStatus = PackDownloadStatus.PENDING;
            downloadPacks.add(pack);
        }
        if (!downloadPacks.isEmpty()) {
            HashMap<UUID, DownloadQueue.DownloadRequest> downloadRequests = new HashMap<UUID, DownloadQueue.DownloadRequest>();
            for (ServerPackData pack : downloadPacks) {
                downloadRequests.put(pack.id, new DownloadQueue.DownloadRequest(pack.url, pack.hash));
            }
            this.downloader.download(downloadRequests, result -> this.onDownload((Collection<ServerPackData>)downloadPacks, (DownloadQueue.BatchResult)result));
        }
        return downloadsInProgress;
    }

    private void triggerReloadIfNeeded() {
        boolean needsReload = false;
        final ArrayList<ServerPackData> packsToLoad = new ArrayList<ServerPackData>();
        final ArrayList<ServerPackData> packsToUnload = new ArrayList<ServerPackData>();
        for (ServerPackData pack : this.packs) {
            boolean shouldBeActive;
            if (pack.activationStatus == ActivationStatus.PENDING) {
                return;
            }
            boolean bl = shouldBeActive = pack.promptAccepted && pack.downloadStatus == PackDownloadStatus.DONE && !pack.isRemoved();
            if (shouldBeActive && pack.activationStatus == ActivationStatus.INACTIVE) {
                packsToLoad.add(pack);
                needsReload = true;
            }
            if (pack.activationStatus != ActivationStatus.ACTIVE) continue;
            if (!shouldBeActive) {
                needsReload = true;
                packsToUnload.add(pack);
                continue;
            }
            packsToLoad.add(pack);
        }
        if (needsReload) {
            for (ServerPackData pack : packsToLoad) {
                if (pack.activationStatus == ActivationStatus.ACTIVE) continue;
                pack.activationStatus = ActivationStatus.PENDING;
            }
            for (ServerPackData pack : packsToUnload) {
                pack.activationStatus = ActivationStatus.PENDING;
            }
            this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks(){
                final /* synthetic */ ServerPackManager this$0;
                {
                    ServerPackManager serverPackManager = this$0;
                    Objects.requireNonNull(serverPackManager);
                    this.this$0 = serverPackManager;
                }

                @Override
                public void onSuccess() {
                    for (ServerPackData pack : packsToLoad) {
                        pack.activationStatus = ActivationStatus.ACTIVE;
                        if (pack.removalReason != null) continue;
                        this.this$0.packLoadFeedback.reportFinalResult(pack.id, PackLoadFeedback.FinalResult.APPLIED);
                    }
                    for (ServerPackData pack : packsToUnload) {
                        pack.activationStatus = ActivationStatus.INACTIVE;
                    }
                    this.this$0.registerForUpdate();
                }

                @Override
                public void onFailure(boolean isRecovery) {
                    if (!isRecovery) {
                        packsToLoad.clear();
                        for (ServerPackData pack : this.this$0.packs) {
                            switch (pack.activationStatus.ordinal()) {
                                case 2: {
                                    packsToLoad.add(pack);
                                    break;
                                }
                                case 1: {
                                    pack.activationStatus = ActivationStatus.INACTIVE;
                                    pack.setRemovalReasonIfNotSet(RemovalReason.ACTIVATION_FAILED);
                                    break;
                                }
                                case 0: {
                                    pack.setRemovalReasonIfNotSet(RemovalReason.DISCARDED);
                                }
                            }
                        }
                        this.this$0.registerForUpdate();
                    } else {
                        for (ServerPackData pack : this.this$0.packs) {
                            if (pack.activationStatus != ActivationStatus.PENDING) continue;
                            pack.activationStatus = ActivationStatus.INACTIVE;
                        }
                    }
                }

                @Override
                public List<PackReloadConfig.IdAndPath> packsToLoad() {
                    return packsToLoad.stream().map(pack -> new PackReloadConfig.IdAndPath(pack.id, pack.path)).toList();
                }
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum PackPromptStatus {
        PENDING,
        ALLOWED,
        DECLINED;

    }

    @Environment(value=EnvType.CLIENT)
    private static class ServerPackData {
        private final UUID id;
        private final URL url;
        private final @Nullable HashCode hash;
        private @Nullable Path path;
        private @Nullable RemovalReason removalReason;
        private PackDownloadStatus downloadStatus = PackDownloadStatus.REQUESTED;
        private ActivationStatus activationStatus = ActivationStatus.INACTIVE;
        private boolean promptAccepted;

        private ServerPackData(UUID id, URL url, @Nullable HashCode hash) {
            this.id = id;
            this.url = url;
            this.hash = hash;
        }

        public void setRemovalReasonIfNotSet(RemovalReason removalReason) {
            if (this.removalReason == null) {
                this.removalReason = removalReason;
            }
        }

        public boolean isRemoved() {
            return this.removalReason != null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum RemovalReason {
        DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
        ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
        DECLINED(PackLoadFeedback.FinalResult.DECLINED),
        DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
        SERVER_REMOVED(null),
        SERVER_REPLACED(null);

        private final @Nullable PackLoadFeedback.FinalResult serverResponse;

        private RemovalReason(PackLoadFeedback.FinalResult serverResponse) {
            this.serverResponse = serverResponse;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum PackDownloadStatus {
        REQUESTED,
        PENDING,
        DONE;

    }

    @Environment(value=EnvType.CLIENT)
    private static enum ActivationStatus {
        INACTIVE,
        PENDING,
        ACTIVE;

    }
}

