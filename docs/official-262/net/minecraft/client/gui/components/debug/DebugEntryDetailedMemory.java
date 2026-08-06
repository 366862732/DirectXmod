/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryDetailedMemory
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("memory");
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        displayer.addToGroup(GROUP, List.of(DebugEntryDetailedMemory.printMemoryUsage(this.memoryBean.getHeapMemoryUsage(), "heap"), DebugEntryDetailedMemory.printMemoryUsage(this.memoryBean.getNonHeapMemoryUsage(), "non-heap")));
    }

    private static long bytesToMebibytes(long used) {
        return used / 1024L / 1024L;
    }

    private static String printMemoryUsage(MemoryUsage memoryUsage, String type) {
        return String.format(Locale.ROOT, "Memory (%s): i=%03dMiB u=%03dMiB c=%03dMiB m=%03dMiB", type, DebugEntryDetailedMemory.bytesToMebibytes(memoryUsage.getInit()), DebugEntryDetailedMemory.bytesToMebibytes(memoryUsage.getUsed()), DebugEntryDetailedMemory.bytesToMebibytes(memoryUsage.getCommitted()), DebugEntryDetailedMemory.bytesToMebibytes(memoryUsage.getMax()));
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }
}

