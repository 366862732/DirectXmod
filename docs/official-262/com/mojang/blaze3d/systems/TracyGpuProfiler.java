/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.GpuApi
 *  com.mojang.jtracy.GpuContext
 *  com.mojang.jtracy.TracyClient
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.jtracy.GpuApi;
import com.mojang.jtracy.GpuContext;
import com.mojang.jtracy.TracyClient;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class TracyGpuProfiler {
    private static final int MAX_QUERIES = 1024;
    private final GpuQueryPool queries;
    private final GpuContext context;
    private int head = 0;
    private int tail = 0;

    public TracyGpuProfiler(GpuDevice device) {
        this.queries = device.createTimestampQueryPool(1024);
        float period = device.getDeviceInfo().timestampPeriod();
        this.context = TracyClient.createGpuContext((GpuApi)GpuApi.OPENGL, (long)device.getTimestampNow(), (float)period);
    }

    public void close() {
        this.queries.close();
    }

    public void pushZone(CommandEncoder encoder, String name) {
        int queryId = this.nextQueryId();
        encoder.writeTimestamp(this.queries, queryId);
        this.context.beginZone(queryId, name, "", "", 0);
    }

    public void popZone(CommandEncoder encoder) {
        int queryId = this.nextQueryId();
        encoder.writeTimestamp(this.queries, queryId);
        this.context.endZone(queryId);
    }

    public void endFrame() {
        OptionalLong timestamp;
        int i;
        OptionalLong[] timestamps;
        if (this.head < this.tail) {
            timestamps = this.queries.getValues(this.tail, 1024 - this.tail);
            for (i = 0; i < timestamps.length; ++i) {
                timestamp = timestamps[i];
                if (!timestamp.isPresent()) {
                    return;
                }
                this.context.submitQueryTimestamp(this.tail, timestamp.getAsLong());
                this.tail = (this.tail + 1) % 1024;
            }
        }
        if (this.tail < this.head) {
            timestamps = this.queries.getValues(this.tail, this.head - this.tail);
            for (i = 0; i < timestamps.length; ++i) {
                timestamp = timestamps[i];
                if (!timestamp.isPresent()) {
                    return;
                }
                this.context.submitQueryTimestamp(this.tail, timestamp.getAsLong());
                this.tail = (this.tail + 1) % 1024;
            }
        }
    }

    private int nextQueryId() {
        int id = this.head;
        this.head = (this.head + 1) % 1024;
        return id;
    }
}

