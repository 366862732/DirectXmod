/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix;

import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.FileSystemCapabilities;

public class AtomicMoveNotSupportedFileFixException
extends FileFixException {
    public AtomicMoveNotSupportedFileFixException(FileSystemCapabilities fileSystemCapabilities) {
        super(null, fileSystemCapabilities);
    }
}

