/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk.storage;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

public interface ChunkIOErrorReporter {
    public void reportChunkLoadFailure(Throwable var1, RegionStorageInfo var2, ChunkPos var3);

    public void reportChunkSaveFailure(Throwable var1, RegionStorageInfo var2, ChunkPos var3);

    public static ReportedException createMisplacedChunkReport(ChunkPos storedPos, ChunkPos requestedPos) {
        CrashReport report = CrashReport.forThrowable(new IllegalStateException("Retrieved chunk position " + String.valueOf(storedPos) + " does not match requested " + String.valueOf(requestedPos)), "Chunk found in invalid location");
        CrashReportCategory category = report.addCategory("Misplaced Chunk");
        category.setDetail("Stored Position", storedPos::toString);
        return new ReportedException(report);
    }

    default public void reportMisplacedChunk(ChunkPos storedPos, ChunkPos requestedPos, RegionStorageInfo storageInfo) {
        this.reportChunkLoadFailure(ChunkIOErrorReporter.createMisplacedChunkReport(storedPos, requestedPos), storageInfo, requestedPos);
    }
}

