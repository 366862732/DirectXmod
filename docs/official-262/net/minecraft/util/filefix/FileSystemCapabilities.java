/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix;

import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;

public record FileSystemCapabilities(boolean atomicMove, boolean hardLinks) {
    public CopyOption[] getMoveOptions() {
        CopyOption[] moveOptions = this.atomicMove ? new CopyOption[]{StandardCopyOption.ATOMIC_MOVE, LinkOption.NOFOLLOW_LINKS} : new CopyOption[]{LinkOption.NOFOLLOW_LINKS};
        return moveOptions;
    }
}

