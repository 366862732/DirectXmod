/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.HashCode
 */
package net.minecraft.data;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import net.minecraft.util.FileUtil;

public interface CachedOutput {
    public static final CachedOutput NO_CACHE = (path, input, hash) -> {
        FileUtil.createDirectoriesSafe(path.getParent());
        Files.write(path, input, new OpenOption[0]);
    };

    public void writeIfNeeded(Path var1, byte[] var2, HashCode var3) throws IOException;
}

