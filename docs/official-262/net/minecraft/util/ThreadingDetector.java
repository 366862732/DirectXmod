/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ThreadingDetector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String name;
    private final Semaphore lock = new Semaphore(1);
    private final Lock stackTraceLock = new ReentrantLock();
    private volatile @Nullable Thread threadThatFailedToAcquire;
    private volatile @Nullable ReportedException fullException;

    public ThreadingDetector(String name) {
        this.name = name;
    }

    public void checkAndLock() {
        block6: {
            boolean released = false;
            try {
                this.stackTraceLock.lock();
                if (this.lock.tryAcquire()) break block6;
                this.threadThatFailedToAcquire = Thread.currentThread();
                released = true;
                this.stackTraceLock.unlock();
                try {
                    this.lock.acquire();
                }
                catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                throw this.fullException;
            }
            finally {
                if (!released) {
                    this.stackTraceLock.unlock();
                }
            }
        }
    }

    public void checkAndUnlock() {
        try {
            this.stackTraceLock.lock();
            Thread threadThatFailedToAcquire = this.threadThatFailedToAcquire;
            if (threadThatFailedToAcquire != null) {
                ReportedException fullException;
                this.fullException = fullException = ThreadingDetector.makeThreadingException(this.name, threadThatFailedToAcquire);
                this.lock.release();
                throw fullException;
            }
            this.lock.release();
        }
        finally {
            this.stackTraceLock.unlock();
        }
    }

    public static ReportedException makeThreadingException(String name, @Nullable Thread threadThatFailedToAcquire) {
        String threads = Stream.of(Thread.currentThread(), threadThatFailedToAcquire).filter(Objects::nonNull).map(ThreadingDetector::stackTrace).collect(Collectors.joining("\n"));
        String error = "Accessing " + name + " from multiple threads";
        CrashReport report = new CrashReport(error, new IllegalStateException(error));
        CrashReportCategory category = report.addCategory("Thread dumps");
        category.setDetail("Thread dumps", threads);
        LOGGER.error("Thread dumps: \n{}", (Object)threads);
        return new ReportedException(report);
    }

    private static String stackTrace(Thread thread) {
        return thread.getName() + ": \n\tat " + Arrays.stream(thread.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n\tat "));
    }
}

