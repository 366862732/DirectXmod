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
package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryEventLog;
import net.minecraft.client.telemetry.TelemetryEventLogger;
import net.minecraft.util.Util;
import net.minecraft.util.eventlog.EventLogDirectory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TelemetryLogManager
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RAW_EXTENSION = ".json";
    private static final int EXPIRY_DAYS = 7;
    private final EventLogDirectory directory;
    private @Nullable CompletableFuture<Optional<TelemetryEventLog>> sessionLog;

    private TelemetryLogManager(EventLogDirectory directory) {
        this.directory = directory;
    }

    public static CompletableFuture<Optional<TelemetryLogManager>> open(Path root) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EventLogDirectory directory = EventLogDirectory.open(root, RAW_EXTENSION);
                directory.listFiles().prune(LocalDate.now(Clock.systemDefaultZone()), 7).compressAll();
                return Optional.of(new TelemetryLogManager(directory));
            }
            catch (Exception e) {
                LOGGER.error("Failed to create telemetry log manager", (Throwable)e);
                return Optional.empty();
            }
        }, Util.backgroundExecutor());
    }

    public CompletableFuture<Optional<TelemetryEventLogger>> openLogger() {
        if (this.sessionLog == null) {
            this.sessionLog = CompletableFuture.supplyAsync(() -> {
                try {
                    EventLogDirectory.RawFile file = this.directory.createNewFile(LocalDate.now(Clock.systemDefaultZone()));
                    FileChannel channel = file.openChannel();
                    return Optional.of(new TelemetryEventLog(channel, Util.backgroundExecutor()));
                }
                catch (IOException e) {
                    LOGGER.error("Failed to open channel for telemetry event log", (Throwable)e);
                    return Optional.empty();
                }
            }, Util.backgroundExecutor());
        }
        return this.sessionLog.thenApply(log -> log.map(TelemetryEventLog::logger));
    }

    @Override
    public void close() {
        if (this.sessionLog != null) {
            this.sessionLog.thenAccept(log -> log.ifPresent(TelemetryEventLog::close));
        }
    }
}

