/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.input.CountingInputStream
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class FileUpload
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RETRIES = 5;
    private static final String UPLOAD_PATH = "/upload";
    private final File file;
    private final long realmId;
    private final int slotId;
    private final UploadInfo uploadInfo;
    private final String sessionId;
    private final String username;
    private final String clientVersion;
    private final String worldVersion;
    private final UploadStatus uploadStatus;
    private final HttpClient client;

    public FileUpload(File file, long realmId, int slotId, UploadInfo uploadInfo, User user, String clientVersion, String worldVersion, UploadStatus uploadStatus) {
        this.file = file;
        this.realmId = realmId;
        this.slotId = slotId;
        this.uploadInfo = uploadInfo;
        this.sessionId = user.getSessionId();
        this.username = user.getName();
        this.clientVersion = clientVersion;
        this.worldVersion = worldVersion;
        this.uploadStatus = uploadStatus;
        this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
    }

    @Override
    public void close() {
        this.client.close();
    }

    public CompletableFuture<UploadResult> startUpload() {
        long fileSize = this.file.length();
        this.uploadStatus.setTotalBytes(fileSize);
        return this.requestUpload(0, fileSize);
    }

    private CompletableFuture<UploadResult> requestUpload(int currentAttempt, long fileSize) {
        HttpRequest.BodyPublisher publisher = FileUpload.inputStreamPublisherWithSize(() -> {
            try {
                return new UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to open file {}", (Object)this.file, (Object)e);
                return null;
            }
        }, fileSize);
        HttpRequest request = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId)).timeout(Duration.ofMinutes(10L)).setHeader("Cookie", this.uploadCookie()).setHeader("Content-Type", "application/octet-stream").POST(publisher).build();
        return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(response -> {
            long retryDelaySeconds = this.getRetryDelaySeconds((HttpResponse<?>)response);
            if (this.shouldRetry(retryDelaySeconds, currentAttempt)) {
                this.uploadStatus.restart();
                try {
                    Thread.sleep(Duration.ofSeconds(retryDelaySeconds));
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                return this.requestUpload(currentAttempt + 1, fileSize);
            }
            return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)response));
        });
    }

    private static HttpRequest.BodyPublisher inputStreamPublisherWithSize(Supplier<@Nullable InputStream> inputStreamSupplier, long fileSize) {
        return HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofInputStream(inputStreamSupplier), fileSize);
    }

    private String uploadCookie() {
        return "sid=" + this.sessionId + ";token=" + this.uploadInfo.token() + ";user=" + this.username + ";version=" + this.clientVersion + ";worldVersion=" + this.worldVersion;
    }

    private UploadResult handleResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode == 401) {
            LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
        }
        String errorMessage = null;
        String body = response.body();
        if (body != null && !body.isBlank()) {
            try {
                JsonElement errorMsgElement = LenientJsonParser.parse(body).getAsJsonObject().get("errorMsg");
                if (errorMsgElement != null) {
                    errorMessage = errorMsgElement.getAsString();
                }
            }
            catch (Exception e) {
                LOGGER.warn("Failed to parse response {}", (Object)body, (Object)e);
            }
        }
        return new UploadResult(statusCode, errorMessage);
    }

    private boolean shouldRetry(long retryDelaySeconds, int currentAttempt) {
        return retryDelaySeconds > 0L && currentAttempt + 1 < 5;
    }

    private long getRetryDelaySeconds(HttpResponse<?> response) {
        return response.headers().firstValueAsLong("Retry-After").orElse(0L);
    }

    @Environment(value=EnvType.CLIENT)
    private static class UploadCountingInputStream
    extends CountingInputStream {
        private final UploadStatus uploadStatus;

        private UploadCountingInputStream(InputStream proxy, UploadStatus uploadStatus) {
            super(proxy);
            this.uploadStatus = uploadStatus;
        }

        protected void afterRead(int n) throws IOException {
            super.afterRead(n);
            this.uploadStatus.onWrite(this.getByteCount());
        }
    }
}

