/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.FileSystemCapabilities;

public final class FailedCleanupFileFixException
extends FileFixException {
    private final String newWorldFolderName;

    public FailedCleanupFileFixException(Exception cause, String newWorldFolderName, FileSystemCapabilities fileSystemCapabilities) {
        super(cause, fileSystemCapabilities);
        this.newWorldFolderName = newWorldFolderName;
    }

    @Override
    protected CrashReport createCrashReport() {
        CrashReport crashReport = super.createCrashReport();
        CrashReportCategory worldUpgrade = crashReport.addCategory("World upgrade");
        worldUpgrade.setDetail("New Name", this.newWorldFolderName);
        return crashReport;
    }

    public String newWorldFolderName() {
        return this.newWorldFolderName;
    }
}

