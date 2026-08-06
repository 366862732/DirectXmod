/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.lang3.ArrayUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.runtime.SwitchBootstraps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SystemReport;
import net.minecraft.util.FileUtil;
import net.minecraft.util.MemoryReserve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CrashReport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private final String title;
    private final Throwable exception;
    private final List<CrashReportCategory> details = Lists.newArrayList();
    private @Nullable Path saveFile;
    private boolean trackingStackTrace = true;
    private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];
    private final SystemReport systemReport = new SystemReport();

    public CrashReport(String title, Throwable t) {
        this.title = title;
        this.exception = t;
    }

    public String getTitle() {
        return this.title;
    }

    public Throwable getException() {
        return this.exception;
    }

    public String getDetails() {
        StringBuilder builder = new StringBuilder();
        this.getDetails(builder);
        return builder.toString();
    }

    public void getDetails(StringBuilder builder) {
        if (this.uncategorizedStackTrace.length <= 0 && !this.details.isEmpty()) {
            this.uncategorizedStackTrace = (StackTraceElement[])ArrayUtils.subarray((Object[])this.details.get(0).getStacktrace(), (int)0, (int)1);
        }
        if (this.uncategorizedStackTrace.length > 0) {
            builder.append("-- Head --\n");
            builder.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
            builder.append("Stacktrace:\n");
            for (StackTraceElement element : this.uncategorizedStackTrace) {
                builder.append("\t").append("at ").append(element);
                builder.append("\n");
            }
            builder.append("\n");
        }
        for (CrashReportCategory entry : this.details) {
            entry.getDetails(builder);
            builder.append("\n\n");
        }
        this.systemReport.appendToCrashReportString(builder);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getExceptionMessage() {
        String string;
        StringWriter writer = null;
        PrintWriter printWriter = null;
        Throwable exception = this.exception;
        if (exception.getMessage() == null) {
            exception = CrashReport.replaceMessage(exception, this.title);
        }
        try {
            writer = new StringWriter();
            printWriter = new PrintWriter(writer);
            exception.printStackTrace(printWriter);
            string = writer.toString();
        }
        catch (Throwable throwable) {
            IOUtils.closeQuietly((Writer)writer);
            IOUtils.closeQuietly(printWriter);
            throw throwable;
        }
        IOUtils.closeQuietly((Writer)writer);
        IOUtils.closeQuietly((Writer)printWriter);
        return string;
    }

    private static Throwable copyProperties(Throwable original, Throwable copy) {
        try {
            Throwable[] suppressed;
            Throwable cause = original.getCause();
            if (cause != null) {
                copy.initCause(cause);
            }
            for (Throwable throwable : suppressed = original.getSuppressed()) {
                copy.addSuppressed(throwable);
            }
            copy.setStackTrace(original.getStackTrace());
            return copy;
        }
        catch (Throwable e) {
            return original;
        }
    }

    private static Throwable replaceMessage(Throwable original, String title) {
        Throwable throwable = original;
        Objects.requireNonNull(throwable);
        Throwable throwable2 = throwable;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{NullPointerException.class, StackOverflowError.class, OutOfMemoryError.class}, (Throwable)throwable2, n)) {
            case 0 -> CrashReport.copyProperties(original, new NullPointerException(title));
            case 1 -> CrashReport.copyProperties(original, new StackOverflowError(title));
            case 2 -> CrashReport.copyProperties(original, new OutOfMemoryError(title));
            default -> original;
        };
    }

    public String getFriendlyReport(ReportType reportType, List<String> extraComments) {
        StringBuilder builder = new StringBuilder();
        reportType.appendHeader(builder, extraComments);
        builder.append("Time: ");
        builder.append(DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
        builder.append("\n");
        builder.append("Description: ");
        builder.append(this.title);
        builder.append("\n\n");
        builder.append(this.getExceptionMessage());
        builder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");
        for (int i = 0; i < 87; ++i) {
            builder.append("-");
        }
        builder.append("\n\n");
        this.getDetails(builder);
        return builder.toString();
    }

    public String getFriendlyReport(ReportType reportType) {
        return this.getFriendlyReport(reportType, List.of());
    }

    public @Nullable Path getSaveFile() {
        return this.saveFile;
    }

    public boolean saveToFile(Path saveFile, ReportType reportType, List<String> extraComments) {
        if (this.saveFile != null) {
            return false;
        }
        try {
            if (saveFile.getParent() != null) {
                FileUtil.createDirectoriesSafe(saveFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(saveFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                writer.write(this.getFriendlyReport(reportType, extraComments));
            }
            this.saveFile = saveFile;
            return true;
        }
        catch (Throwable t) {
            LOGGER.error("Could not save crash report to {}", (Object)saveFile, (Object)t);
            return false;
        }
    }

    public boolean saveToFile(Path file, ReportType reportType) {
        return this.saveToFile(file, reportType, List.of());
    }

    public SystemReport getSystemReport() {
        return this.systemReport;
    }

    public CrashReportCategory addCategory(String name) {
        return this.addCategory(name, 1);
    }

    public CrashReportCategory addCategory(String name, int nestedOffset) {
        CrashReportCategory category = new CrashReportCategory(name);
        if (this.trackingStackTrace) {
            int size = category.fillInStackTrace(nestedOffset);
            StackTraceElement[] fullTrace = this.exception.getStackTrace();
            StackTraceElement source = null;
            StackTraceElement next = null;
            int traceIndex = fullTrace.length - size;
            if (traceIndex < 0) {
                LOGGER.error("Negative index in crash report handler ({}/{})", (Object)fullTrace.length, (Object)size);
            }
            if (0 <= traceIndex && traceIndex < fullTrace.length) {
                source = fullTrace[traceIndex];
                if (fullTrace.length + 1 - size < fullTrace.length) {
                    next = fullTrace[fullTrace.length + 1 - size];
                }
            }
            this.trackingStackTrace = category.validateStackTrace(source, next);
            if (fullTrace.length >= size && 0 <= traceIndex && traceIndex < fullTrace.length) {
                this.uncategorizedStackTrace = new StackTraceElement[traceIndex];
                System.arraycopy(fullTrace, 0, this.uncategorizedStackTrace, 0, this.uncategorizedStackTrace.length);
            } else {
                this.trackingStackTrace = false;
            }
        }
        this.details.add(category);
        return category;
    }

    public static CrashReport forThrowable(Throwable t, String title) {
        CrashReport report;
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof ReportedException) {
            ReportedException reportedException = (ReportedException)t;
            report = reportedException.getReport();
        } else {
            report = new CrashReport(title, t);
        }
        return report;
    }

    public static void preload() {
        MemoryReserve.allocate();
        new CrashReport("Don't panic!", new Throwable()).getFriendlyReport(ReportType.CRASH);
    }
}

