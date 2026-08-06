/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryMemory
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("memory");
    private final AllocationRateCalculator allocationRateCalculator = new AllocationRateCalculator();

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        long max = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = total - free;
        displayer.addToGroup(GROUP, List.of(String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMiB", used * 100L / max, DebugEntryMemory.bytesToMebibytes(used), DebugEntryMemory.bytesToMebibytes(max)), String.format(Locale.ROOT, "Allocation rate: %03dMiB/s", DebugEntryMemory.bytesToMebibytes(this.allocationRateCalculator.bytesAllocatedPerSecond(used))), String.format(Locale.ROOT, "Allocated: %2d%% %03dMiB", total * 100L / max, DebugEntryMemory.bytesToMebibytes(total))));
    }

    private static long bytesToMebibytes(long used) {
        return used / 1024L / 1024L;
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    private static class AllocationRateCalculator {
        private static final int UPDATE_INTERVAL_MS = 500;
        private static final List<GarbageCollectorMXBean> GC_MBEANS = ManagementFactory.getGarbageCollectorMXBeans();
        private long lastTime = 0L;
        private long lastHeapUsage = -1L;
        private long lastGcCounts = -1L;
        private long lastRate = 0L;

        private AllocationRateCalculator() {
        }

        private long bytesAllocatedPerSecond(long currentHeapUsage) {
            long time = System.currentTimeMillis();
            if (time - this.lastTime < 500L) {
                return this.lastRate;
            }
            long gcCounts = AllocationRateCalculator.gcCounts();
            if (this.lastTime != 0L && gcCounts == this.lastGcCounts) {
                double multiplier = (double)TimeUnit.SECONDS.toMillis(1L) / (double)(time - this.lastTime);
                long delta = currentHeapUsage - this.lastHeapUsage;
                this.lastRate = Math.round((double)delta * multiplier);
            }
            this.lastTime = time;
            this.lastHeapUsage = currentHeapUsage;
            this.lastGcCounts = gcCounts;
            return this.lastRate;
        }

        private static long gcCounts() {
            long total = 0L;
            for (GarbageCollectorMXBean gcBean : GC_MBEANS) {
                total += gcBean.getCollectionCount();
            }
            return total;
        }
    }
}

