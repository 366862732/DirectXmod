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
package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.worldupload.RealmsUploadCanceledException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadFailedException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadWorldNotClosedException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadWorldPacker;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUploadStatusTracker;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.UploadResult;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.User;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsWorldUpload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int UPLOAD_RETRIES = 20;
    private final RealmsClient client = RealmsClient.getOrCreate();
    private final Path worldFolder;
    private final RealmsSlot realmsSlot;
    private final User user;
    private final long realmId;
    private final RealmsWorldUploadStatusTracker statusCallback;
    private volatile boolean cancelled;
    private volatile @Nullable CompletableFuture<?> uploadTask;

    public RealmsWorldUpload(Path worldFolder, RealmsSlot realmsSlot, User user, long realmId, RealmsWorldUploadStatusTracker statusCallback) {
        this.worldFolder = worldFolder;
        this.realmsSlot = realmsSlot;
        this.user = user;
        this.realmId = realmId;
        this.statusCallback = statusCallback;
    }

    public CompletableFuture<?> packAndUpload() {
        return CompletableFuture.runAsync(() -> {
            File archive = null;
            try {
                UploadInfo uploadInfo = this.requestUploadInfoWithRetries();
                archive = RealmsUploadWorldPacker.pack(this.worldFolder, () -> this.cancelled);
                this.statusCallback.setUploading();
                try (FileUpload fileUpload = new FileUpload(archive, this.realmId, this.realmsSlot.slotId, uploadInfo, this.user, SharedConstants.getCurrentVersion().name(), this.realmsSlot.options.version, this.statusCallback.getUploadStatus());){
                    UploadResult join;
                    CompletableFuture<UploadResult> uploadTask = fileUpload.startUpload();
                    this.uploadTask = uploadTask;
                    if (this.cancelled) {
                        uploadTask.cancel(true);
                        return;
                    }
                    try {
                        join = uploadTask.join();
                    }
                    catch (CompletionException e) {
                        throw e.getCause();
                    }
                    String errorMessage = join.getSimplifiedErrorMessage();
                    if (errorMessage != null) {
                        throw new RealmsUploadFailedException(errorMessage);
                    }
                    UploadTokenCache.invalidate(this.realmId);
                    this.client.updateSlot(this.realmId, this.realmsSlot.slotId, this.realmsSlot.options, this.realmsSlot.settings);
                }
            }
            catch (RealmsServiceException e) {
                throw new RealmsUploadFailedException(e.realmsError.errorMessage());
            }
            catch (InterruptedException | CancellationException e) {
                throw new RealmsUploadCanceledException();
            }
            catch (RealmsUploadException e) {
                throw e;
            }
            catch (Throwable e) {
                if (e instanceof Error) {
                    Error error = (Error)e;
                    throw error;
                }
                throw new RealmsUploadFailedException(e.getMessage());
            }
            finally {
                if (archive != null) {
                    LOGGER.debug("Deleting file {}", (Object)archive.getAbsolutePath());
                    archive.delete();
                }
            }
        }, Util.backgroundExecutor());
    }

    public void cancel() {
        this.cancelled = true;
        CompletableFuture<?> uploadTask = this.uploadTask;
        if (uploadTask != null) {
            uploadTask.cancel(true);
        }
    }

    private UploadInfo requestUploadInfoWithRetries() throws RealmsServiceException, InterruptedException {
        for (int i = 0; i < 20; ++i) {
            try {
                UploadInfo uploadInfo = this.client.requestUploadInfo(this.realmId);
                if (this.cancelled) {
                    throw new RealmsUploadCanceledException();
                }
                if (uploadInfo == null) continue;
                if (!uploadInfo.worldClosed()) {
                    throw new RealmsUploadWorldNotClosedException();
                }
                return uploadInfo;
            }
            catch (RetryCallException e) {
                Thread.sleep((long)e.delaySeconds * 1000L);
            }
        }
        throw new RealmsUploadWorldNotClosedException();
    }
}

