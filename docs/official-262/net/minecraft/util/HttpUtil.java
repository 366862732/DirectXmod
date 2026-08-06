/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Funnels
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.HashFunction
 *  com.google.common.hash.Hasher
 *  com.google.common.hash.PrimitiveSink
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.PrimitiveSink;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import net.minecraft.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class HttpUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private HttpUtil() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Loose catch block
     */
    public static Path downloadFile(Path targetDir, URL url, Map<String, String> headers, HashFunction hashFunction, @Nullable HashCode requestedHash, int maxSize, Proxy proxy, DownloadProgressListener listener) {
        InputStream input;
        HttpURLConnection connection;
        block21: {
            Path targetFile;
            connection = null;
            input = null;
            listener.requestStart();
            if (requestedHash != null) {
                targetFile = HttpUtil.cachedFilePath(targetDir, requestedHash);
                try {
                    if (HttpUtil.checkExistingFile(targetFile, hashFunction, requestedHash)) {
                        LOGGER.info("Returning cached file since actual hash matches requested");
                        listener.requestFinished(true);
                        HttpUtil.updateModificationTime(targetFile);
                        return targetFile;
                    }
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to check cached file {}", (Object)targetFile, (Object)e);
                }
                try {
                    LOGGER.warn("Existing file {} not found or had mismatched hash", (Object)targetFile);
                    Files.deleteIfExists(targetFile);
                }
                catch (IOException e) {
                    listener.requestFinished(false);
                    throw new UncheckedIOException("Failed to remove existing file " + String.valueOf(targetFile), e);
                }
            }
            targetFile = null;
            connection = (HttpURLConnection)url.openConnection(proxy);
            connection.setInstanceFollowRedirects(true);
            headers.forEach(connection::setRequestProperty);
            input = connection.getInputStream();
            long contentLength = connection.getContentLengthLong();
            OptionalLong size = contentLength != -1L ? OptionalLong.of(contentLength) : OptionalLong.empty();
            FileUtil.createDirectoriesSafe(targetDir);
            listener.downloadStart(size);
            if (size.isPresent() && size.getAsLong() > (long)maxSize) {
                throw new IOException("Filesize is bigger than maximum allowed (file is " + String.valueOf(size) + ", limit is " + maxSize + ")");
            }
            if (targetFile == null) break block21;
            HashCode actualHash = HttpUtil.downloadAndHash(hashFunction, maxSize, listener, input, targetFile);
            if (!actualHash.equals((Object)requestedHash)) {
                throw new IOException("Hash of downloaded file (" + String.valueOf(actualHash) + ") did not match requested (" + String.valueOf(requestedHash) + ")");
            }
            listener.requestFinished(true);
            Path path = targetFile;
            IOUtils.closeQuietly((InputStream)input);
            return path;
        }
        Path tmpPath = Files.createTempFile(targetDir, "download", ".tmp", new FileAttribute[0]);
        HashCode actualHash = HttpUtil.downloadAndHash(hashFunction, maxSize, listener, input, tmpPath);
        Path actualPath = HttpUtil.cachedFilePath(targetDir, actualHash);
        if (!HttpUtil.checkExistingFile(actualPath, hashFunction, actualHash)) {
            Files.move(tmpPath, actualPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            HttpUtil.updateModificationTime(actualPath);
        }
        listener.requestFinished(true);
        Path path = actualPath;
        Files.deleteIfExists(tmpPath);
        IOUtils.closeQuietly((InputStream)input);
        return path;
        {
            catch (Throwable throwable) {
                try {
                    try {
                        Files.deleteIfExists(tmpPath);
                        throw throwable;
                    }
                    catch (Throwable t) {
                        InputStream error;
                        if (connection != null && (error = connection.getErrorStream()) != null) {
                            try {
                                LOGGER.error("HTTP response error: {}", (Object)IOUtils.toString((InputStream)error, (Charset)StandardCharsets.UTF_8));
                            }
                            catch (Exception e) {
                                LOGGER.error("Failed to read response from server");
                            }
                        }
                        listener.requestFinished(false);
                        throw new IllegalStateException("Failed to download file " + String.valueOf(url), t);
                    }
                }
                catch (Throwable throwable2) {
                    IOUtils.closeQuietly(input);
                    throw throwable2;
                }
            }
        }
    }

    private static void updateModificationTime(Path targetFile) {
        try {
            Files.setLastModifiedTime(targetFile, FileTime.from(Instant.now()));
        }
        catch (IOException e) {
            LOGGER.warn("Failed to update modification time of {}", (Object)targetFile, (Object)e);
        }
    }

    private static HashCode hashFile(Path file, HashFunction hashFunction) throws IOException {
        Hasher hasher = hashFunction.newHasher();
        try (OutputStream outputStream = Funnels.asOutputStream((PrimitiveSink)hasher);
             InputStream fileInput = Files.newInputStream(file, new OpenOption[0]);){
            fileInput.transferTo(outputStream);
        }
        return hasher.hash();
    }

    private static boolean checkExistingFile(Path file, HashFunction hashFunction, HashCode expectedHash) throws IOException {
        if (Files.exists(file, new LinkOption[0])) {
            HashCode actualHash = HttpUtil.hashFile(file, hashFunction);
            if (actualHash.equals((Object)expectedHash)) {
                return true;
            }
            LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", new Object[]{file, expectedHash, actualHash});
        }
        return false;
    }

    private static Path cachedFilePath(Path targetDir, HashCode requestedHash) {
        return targetDir.resolve(requestedHash.toString());
    }

    private static HashCode downloadAndHash(HashFunction hashFunction, int maxSize, DownloadProgressListener listener, InputStream input, Path downloadFile) throws IOException {
        try (OutputStream output = Files.newOutputStream(downloadFile, StandardOpenOption.CREATE);){
            int read;
            Hasher hasher = hashFunction.newHasher();
            byte[] buffer = new byte[8196];
            long readSoFar = 0L;
            while ((read = input.read(buffer)) >= 0) {
                listener.downloadedBytes(readSoFar += (long)read);
                if (readSoFar > (long)maxSize) {
                    throw new IOException("Filesize was bigger than maximum allowed (got >= " + readSoFar + ", limit was " + maxSize + ")");
                }
                if (Thread.interrupted()) {
                    LOGGER.error("INTERRUPTED");
                    throw new IOException("Download interrupted");
                }
                output.write(buffer, 0, read);
                hasher.putBytes(buffer, 0, read);
            }
            HashCode hashCode = hasher.hash();
            return hashCode;
        }
    }

    public static int getAvailablePort() {
        int n;
        ServerSocket server = new ServerSocket(0);
        try {
            n = server.getLocalPort();
        }
        catch (Throwable throwable) {
            try {
                try {
                    server.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException ignored) {
                return 25564;
            }
        }
        server.close();
        return n;
    }

    public static boolean isPortAvailable(int port) {
        boolean bl;
        if (port < 0 || port > 65535) {
            return false;
        }
        ServerSocket server = new ServerSocket(port);
        try {
            bl = server.getLocalPort() == port;
        }
        catch (Throwable throwable) {
            try {
                try {
                    server.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException ignored) {
                return false;
            }
        }
        server.close();
        return bl;
    }

    public static interface DownloadProgressListener {
        public void requestStart();

        public void downloadStart(OptionalLong var1);

        public void downloadedBytes(long var1);

        public void requestFinished(boolean var1);
    }
}

