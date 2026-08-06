/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 *  com.google.common.io.Files
 *  com.mojang.logging.LogUtils
 *  javax.annotation.CheckReturnValue
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.compress.archivers.tar.TarArchiveEntry
 *  org.apache.commons.compress.archivers.tar.TarArchiveInputStream
 *  org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.io.output.CountingOutputStream
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class FileDownload {
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile boolean cancelled;
    private volatile boolean finished;
    private volatile boolean error;
    private volatile boolean extracting;
    private volatile @Nullable File tempFile;
    private volatile File resourcePackPath;
    private volatile @Nullable CompletableFuture<?> pendingRequest;
    private @Nullable Thread currentThread;
    private static final String[] INVALID_FILE_NAMES = new String[]{"CON", "COM", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    private <T> @Nullable T joinCancellableRequest(CompletableFuture<T> pendingRequest) throws Throwable {
        this.pendingRequest = pendingRequest;
        if (this.cancelled) {
            pendingRequest.cancel(true);
            return null;
        }
        try {
            try {
                return pendingRequest.join();
            }
            catch (CompletionException e) {
                throw e.getCause();
            }
        }
        catch (CancellationException e) {
            return null;
        }
    }

    private static HttpClient createClient() {
        return HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofMinutes(2L)).build();
    }

    private static HttpRequest.Builder createRequest(String downloadLink) {
        return HttpRequest.newBuilder(URI.create(downloadLink)).timeout(Duration.ofMinutes(2L));
    }

    @CheckReturnValue
    public static OptionalLong contentLength(String downloadLink) {
        HttpClient client = FileDownload.createClient();
        try {
            HttpResponse<Void> response = client.send(FileDownload.createRequest(downloadLink).HEAD().build(), HttpResponse.BodyHandlers.discarding());
            OptionalLong optionalLong = response.headers().firstValueAsLong("Content-Length");
            if (client != null) {
                client.close();
            }
            return optionalLong;
        }
        catch (Throwable throwable) {
            try {
                if (client != null) {
                    try {
                        client.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (Exception e) {
                LOGGER.error("Unable to get content length for download");
                return OptionalLong.empty();
            }
        }
    }

    public void download(WorldDownload worldDownload, String worldName, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, LevelStorageSource levelStorageSource) {
        if (this.currentThread != null) {
            return;
        }
        this.currentThread = new Thread(() -> {
            try (HttpClient client = FileDownload.createClient();){
                try {
                    this.tempFile = File.createTempFile("backup", ".tar.gz");
                    this.download(downloadStatus, client, worldDownload.downloadLink(), this.tempFile);
                    this.finishWorldDownload(worldName.trim(), this.tempFile, levelStorageSource, downloadStatus);
                }
                catch (Exception e) {
                    LOGGER.error("Caught exception while downloading world", (Throwable)e);
                    this.error = true;
                }
                finally {
                    this.pendingRequest = null;
                    if (this.tempFile != null) {
                        this.tempFile.delete();
                    }
                    this.tempFile = null;
                }
                if (this.error) {
                    return;
                }
                String resourcePackLink = worldDownload.resourcePackUrl();
                if (!resourcePackLink.isEmpty() && !worldDownload.resourcePackHash().isEmpty()) {
                    try {
                        this.tempFile = File.createTempFile("resources", ".tar.gz");
                        this.download(downloadStatus, client, resourcePackLink, this.tempFile);
                        this.finishResourcePackDownload(downloadStatus, this.tempFile, worldDownload);
                    }
                    catch (Exception e) {
                        LOGGER.error("Caught exception while downloading resource pack", (Throwable)e);
                        this.error = true;
                    }
                    finally {
                        this.pendingRequest = null;
                        if (this.tempFile != null) {
                            this.tempFile.delete();
                        }
                        this.tempFile = null;
                    }
                }
                this.finished = true;
            }
        }, "Realms world download");
        this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
        this.currentThread.start();
    }

    private void download(RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, HttpClient client, String url, File target) throws IOException {
        HttpResponse<InputStream> response;
        HttpRequest request = FileDownload.createRequest(url).GET().build();
        try {
            response = this.joinCancellableRequest(client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()));
        }
        catch (Error e) {
            throw e;
        }
        catch (Throwable e) {
            LOGGER.error("Failed to download {}", (Object)url, (Object)e);
            this.error = true;
            return;
        }
        if (response == null || this.cancelled) {
            return;
        }
        if (response.statusCode() != 200) {
            this.error = true;
            return;
        }
        downloadStatus.totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0L);
        try (InputStream is = response.body();
             FileOutputStream os = new FileOutputStream(target);){
            is.transferTo((OutputStream)((Object)new DownloadCountingOutputStream(os, downloadStatus)));
        }
    }

    public void cancel() {
        if (this.tempFile != null) {
            this.tempFile.delete();
            this.tempFile = null;
        }
        this.cancelled = true;
        CompletableFuture<?> pendingRequest = this.pendingRequest;
        if (pendingRequest != null) {
            pendingRequest.cancel(true);
        }
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isError() {
        return this.error;
    }

    public boolean isExtracting() {
        return this.extracting;
    }

    public static String findAvailableFolderName(String folder) {
        folder = ((String)folder).replaceAll("[\\./\"]", "_");
        for (String invalidName : INVALID_FILE_NAMES) {
            if (!((String)folder).equalsIgnoreCase(invalidName)) continue;
            folder = "_" + (String)folder + "_";
        }
        return folder;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void untarGzipArchive(String name, @Nullable File file, LevelStorageSource levelStorageSource) throws IOException {
        Object finalName;
        Pattern namePattern = Pattern.compile(".*-([0-9]+)$");
        int number = 1;
        for (char replacer : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
            name = name.replace(replacer, '_');
        }
        if (StringUtils.isEmpty((CharSequence)name)) {
            name = "Realm";
        }
        name = FileDownload.findAvailableFolderName(name);
        try {
            Object object = levelStorageSource.findLevelCandidates().iterator();
            while (object.hasNext()) {
                LevelStorageSource.LevelDirectory level = (LevelStorageSource.LevelDirectory)object.next();
                String levelId = level.directoryName();
                if (!levelId.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) continue;
                Matcher matcher = namePattern.matcher(levelId);
                if (matcher.matches()) {
                    int parsedNumber = Integer.parseInt(matcher.group(1));
                    if (parsedNumber <= number) continue;
                    number = parsedNumber;
                    continue;
                }
                ++number;
            }
        }
        catch (Exception e) {
            LOGGER.error("Error getting level list", (Throwable)e);
            this.error = true;
            return;
        }
        if (!levelStorageSource.isNewLevelIdAcceptable(name) || number > 1) {
            finalName = name + (String)(number == 1 ? "" : "-" + number);
            if (!levelStorageSource.isNewLevelIdAcceptable((String)finalName)) {
                boolean foundName = false;
                while (!foundName) {
                    if (!levelStorageSource.isNewLevelIdAcceptable((String)(finalName = name + (String)(++number == 1 ? "" : "-" + number)))) continue;
                    foundName = true;
                }
            }
        } else {
            finalName = name;
        }
        TarArchiveInputStream tarIn = null;
        Path worldPath = Minecraft.getInstance().getLevelSource().getLevelPath((String)finalName).normalize();
        try {
            FileUtil.createDirectoriesSafe(worldPath);
            tarIn = new TarArchiveInputStream((InputStream)new GzipCompressorInputStream((InputStream)new BufferedInputStream(new FileInputStream(file))));
            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            while (tarEntry != null) {
                Path destPath = worldPath.resolve(Path.of("world", new String[0]).relativize(Path.of(tarEntry.getName(), new String[0]))).normalize();
                if (!destPath.startsWith(worldPath)) {
                    LOGGER.warn("Unexpected entry in Realms world download: {}", (Object)tarEntry.getName());
                    tarEntry = tarIn.getNextTarEntry();
                    continue;
                }
                if (tarEntry.isDirectory()) {
                    FileUtil.createDirectoriesSafe(destPath);
                } else {
                    Path parent = destPath.getParent();
                    if (parent != null) {
                        FileUtil.createDirectoriesSafe(parent);
                    }
                    try (FileOutputStream output = new FileOutputStream(destPath.toFile());){
                        IOUtils.copy((InputStream)tarIn, (OutputStream)output);
                    }
                }
                tarEntry = tarIn.getNextTarEntry();
            }
        }
        catch (Exception e) {
            LOGGER.error("Error extracting world", (Throwable)e);
            this.error = true;
        }
        finally {
            if (tarIn != null) {
                tarIn.close();
            }
            if (file != null) {
                file.delete();
            }
            try (LevelStorageSource.LevelStorageAccess access = levelStorageSource.validateAndCreateAccess((String)finalName);){
                access.renameAndDropPlayer((String)finalName);
            }
            catch (IOException | NbtException | ReportedNbtException e) {
                LOGGER.error("Failed to modify unpacked realms level {}", finalName, (Object)e);
            }
            catch (ContentValidationException e) {
                LOGGER.warn("Failed to download file", (Throwable)e);
            }
            this.resourcePackPath = worldPath.resolve(LevelResource.MAP_RESOURCE_FILE.id()).toFile();
        }
    }

    private void finishWorldDownload(String worldName, File tempFile, LevelStorageSource levelStorageSource, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
        if (downloadStatus.bytesWritten >= downloadStatus.totalBytes && !this.cancelled && !this.error) {
            try {
                this.extracting = true;
                this.untarGzipArchive(worldName, tempFile, levelStorageSource);
            }
            catch (IOException e) {
                LOGGER.error("Error extracting archive", (Throwable)e);
                this.error = true;
            }
        }
    }

    private void finishResourcePackDownload(RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, File tempFile, WorldDownload worldDownload) {
        if (downloadStatus.bytesWritten >= downloadStatus.totalBytes && !this.cancelled) {
            try {
                String actualHash = Hashing.sha1().hashBytes(Files.toByteArray((File)tempFile)).toString();
                if (actualHash.equals(worldDownload.resourcePackHash())) {
                    FileUtils.copyFile((File)tempFile, (File)this.resourcePackPath);
                    this.finished = true;
                } else {
                    LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", (Object)worldDownload.resourcePackHash(), (Object)actualHash);
                    FileUtils.deleteQuietly((File)tempFile);
                    this.error = true;
                }
            }
            catch (IOException e) {
                LOGGER.error("Error copying resourcepack file: {}", (Object)e.getMessage());
                this.error = true;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class DownloadCountingOutputStream
    extends CountingOutputStream {
        private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

        public DownloadCountingOutputStream(OutputStream out, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
            super(out);
            this.downloadStatus = downloadStatus;
        }

        protected void afterWrite(int n) throws IOException {
            super.afterWrite(n);
            this.downloadStatus.bytesWritten = this.getByteCount();
        }
    }
}

