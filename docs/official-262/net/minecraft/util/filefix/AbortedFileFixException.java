/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix;

import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.FileSystemCapabilities;
import net.minecraft.util.filefix.virtualfilesystem.FileMove;
import org.jspecify.annotations.Nullable;

public final class AbortedFileFixException
extends FileFixException {
    private final List<FileMove> notRevertedMoves;

    public AbortedFileFixException(Exception cause, List<FileMove> notRevertedMoves, @Nullable FileSystemCapabilities fileSystemCapabilities) {
        super(cause, fileSystemCapabilities);
        this.notRevertedMoves = notRevertedMoves;
    }

    public AbortedFileFixException(Exception cause) {
        this(cause, List.of(), null);
    }

    public List<FileMove> notRevertedMoves() {
        return this.notRevertedMoves;
    }

    @Override
    protected CrashReport createCrashReport() {
        CrashReport crashReport = super.createCrashReport();
        CrashReportCategory failedReverts = crashReport.addCategory("Moves that failed to revert");
        for (int i = 0; i < this.notRevertedMoves.size(); ++i) {
            FileMove notRevertedMove = this.notRevertedMoves.get(i);
            failedReverts.setDetail(String.valueOf(i), String.valueOf(notRevertedMove.from()) + " -> " + String.valueOf(notRevertedMove.to()));
        }
        return crashReport;
    }
}

