/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.virtualfilesystem.exception;

import java.nio.file.DirectoryNotEmptyException;

public class CowFSDirectoryNotEmptyException
extends DirectoryNotEmptyException {
    public CowFSDirectoryNotEmptyException(String message) {
        super(message);
    }
}

