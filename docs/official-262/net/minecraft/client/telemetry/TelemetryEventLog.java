/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.IOUtils
 *  org.slf4j.Logger
 */
package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryEventInstance;
import net.minecraft.client.telemetry.TelemetryEventLogger;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TelemetryEventLog
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final JsonEventLog<TelemetryEventInstance> log;
    private final ConsecutiveExecutor consecutiveExecutor;

    public TelemetryEventLog(FileChannel channel, Executor executor) {
        this.log = new JsonEventLog<TelemetryEventInstance>(TelemetryEventInstance.CODEC, channel);
        this.consecutiveExecutor = new ConsecutiveExecutor(executor, "telemetry-event-log");
    }

    public TelemetryEventLogger logger() {
        return event -> this.consecutiveExecutor.schedule(() -> {
            try {
                this.log.write(event);
            }
            catch (IOException e) {
                LOGGER.error("Failed to write telemetry event to log", (Throwable)e);
            }
        });
    }

    @Override
    public void close() {
        this.consecutiveExecutor.schedule(() -> IOUtils.closeQuietly(this.log));
        this.consecutiveExecutor.close();
    }
}

