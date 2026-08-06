/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.filefix.FileFixUtil;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record Move(String from, String to) implements FileFixOperation
{
    public Move relative(String sourceFolder, String targetFolder) {
        return new Move(sourceFolder + "/" + this.from, targetFolder + "/" + this.to);
    }

    @Override
    public void fix(Path baseDirectory, UpgradeProgress upgradeProgress) throws IOException {
        FileFixUtil.moveFile(baseDirectory, this.from, this.to);
    }
}

