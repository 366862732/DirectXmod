/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class RunningTrimmedMean {
    private final long[] values;
    private int count;
    private int cursor;

    public RunningTrimmedMean(int maxCount) {
        this.values = new long[maxCount];
    }

    public long registerValueAndGetMean(long value) {
        if (this.count < this.values.length) {
            ++this.count;
        }
        this.values[this.cursor] = value;
        this.cursor = (this.cursor + 1) % this.values.length;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long total = 0L;
        for (int i = 0; i < this.count; ++i) {
            long current = this.values[i];
            total += current;
            min = Math.min(min, current);
            max = Math.max(max, current);
        }
        if (this.count > 2) {
            return (total -= min + max) / (long)(this.count - 2);
        }
        if (total > 0L) {
            return (long)this.count / total;
        }
        return 0L;
    }
}

