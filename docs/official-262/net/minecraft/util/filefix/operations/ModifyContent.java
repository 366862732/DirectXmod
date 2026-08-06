/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.minecraft.util.filefix.access.FileAccessProvider;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class ModifyContent
implements FileFixOperation {
    private final FileAccessProvider fileAccessProvider;
    private final FixFunction fixFunction;

    public ModifyContent(FileAccessProvider fileAccessProvider, FixFunction fixFunction) {
        this.fileAccessProvider = fileAccessProvider;
        this.fixFunction = fixFunction;
    }

    @Override
    public void fix(Path baseDirectory, UpgradeProgress upgradeProgress) throws IOException {
        try {
            ScopedValue.where(this.fileAccessProvider.baseDirectory(), baseDirectory).run(() -> {
                try {
                    this.fixFunction.run(upgradeProgress);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
        finally {
            this.fileAccessProvider.close();
        }
    }

    @FunctionalInterface
    public static interface FixFunction {
        public void run(UpgradeProgress var1) throws IOException;
    }

    @FunctionalInterface
    public static interface FileAccessFunction {
        public FixFunction make(FileAccessProvider var1);
    }
}

