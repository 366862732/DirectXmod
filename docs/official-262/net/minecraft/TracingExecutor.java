/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.jtracy.Zone
 */
package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;

public record TracingExecutor(ExecutorService service) implements Executor
{
    public Executor forName(String name) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            return command -> this.service.execute(() -> {
                Thread thread = Thread.currentThread();
                String oldName = thread.getName();
                thread.setName(name);
                try (Zone ignored = TracyClient.beginZone((String)name, (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                    command.run();
                }
                finally {
                    thread.setName(oldName);
                }
            });
        }
        if (TracyClient.isAvailable()) {
            return command -> this.service.execute(() -> {
                try (Zone ignored = TracyClient.beginZone((String)name, (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                    command.run();
                }
            });
        }
        return this.service;
    }

    @Override
    public void execute(Runnable command) {
        this.service.execute(TracingExecutor.wrapUnnamed(command));
    }

    public void shutdownAndAwait(long timeout, TimeUnit unit) {
        boolean terminated;
        this.service.shutdown();
        try {
            terminated = this.service.awaitTermination(timeout, unit);
        }
        catch (InterruptedException e) {
            terminated = false;
        }
        if (!terminated) {
            this.service.shutdownNow();
        }
    }

    private static Runnable wrapUnnamed(Runnable command) {
        if (!TracyClient.isAvailable()) {
            return command;
        }
        return () -> {
            try (Zone ignored = TracyClient.beginZone((String)"task", (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                command.run();
            }
        };
    }
}

