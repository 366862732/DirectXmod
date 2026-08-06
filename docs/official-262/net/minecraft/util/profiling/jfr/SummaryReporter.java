/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.parse.JfrStatsParser;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SummaryReporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Runnable onDeregistration;

    protected SummaryReporter(Runnable onDeregistration) {
        this.onDeregistration = onDeregistration;
    }

    public void recordingStopped(@Nullable Path result) {
        JfrStatsResult statsResult;
        if (result == null) {
            return;
        }
        this.onDeregistration.run();
        SummaryReporter.infoWithFallback(() -> "Dumped flight recorder profiling to " + String.valueOf(result));
        try {
            statsResult = JfrStatsParser.parse(result);
        }
        catch (Throwable t) {
            SummaryReporter.warnWithFallback(() -> "Failed to parse JFR recording", t);
            return;
        }
        try {
            SummaryReporter.infoWithFallback(statsResult::asJson);
            Path jsonReport = result.resolveSibling("jfr-report-" + StringUtils.substringBefore((String)result.getFileName().toString(), (String)".jfr") + ".json");
            Files.writeString(jsonReport, (CharSequence)statsResult.asJson(), StandardOpenOption.CREATE);
            SummaryReporter.infoWithFallback(() -> "Dumped recording summary to " + String.valueOf(jsonReport));
        }
        catch (Throwable t) {
            SummaryReporter.warnWithFallback(() -> "Failed to output JFR report", t);
        }
    }

    private static void infoWithFallback(Supplier<String> message) {
        if (LogUtils.isLoggerActive()) {
            LOGGER.info(message.get());
        } else {
            Bootstrap.realStdoutPrintln(message.get());
        }
    }

    private static void warnWithFallback(Supplier<String> message, Throwable t) {
        if (LogUtils.isLoggerActive()) {
            LOGGER.warn(message.get(), t);
        } else {
            Bootstrap.realStdoutPrintln(message.get());
            t.printStackTrace(Bootstrap.STDOUT);
        }
    }
}

