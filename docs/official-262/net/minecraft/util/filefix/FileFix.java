/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.filefix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.filefix.CanceledFileFixException;
import net.minecraft.util.filefix.access.FileAccessProvider;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.filefix.operations.ModifyContent;
import net.minecraft.util.worldupdate.UpgradeProgress;

public abstract class FileFix {
    private final Schema schema;
    private final List<FileFixOperation> fileFixOperations = new ArrayList<FileFixOperation>();

    public FileFix(Schema schema) {
        this.schema = schema;
        this.makeFixer();
    }

    public final Schema getSchema() {
        return this.schema;
    }

    public final int getVersion() {
        return DataFixUtils.getVersion((int)this.schema.getVersionKey());
    }

    public void addFileFixOperation(FileFixOperation fileFixOperation) {
        this.fileFixOperations.add(fileFixOperation);
    }

    public void addFileContentFix(ModifyContent.FileAccessFunction fileAccessFunction) {
        FileAccessProvider fileAccessProvider = new FileAccessProvider(this.getVersion());
        ModifyContent.FixFunction fixFunction = fileAccessFunction.make(fileAccessProvider);
        fileAccessProvider.freeze();
        this.fileFixOperations.add(new ModifyContent(fileAccessProvider, fixFunction));
    }

    public int countFileOperations() {
        return this.fileFixOperations.size();
    }

    public void runFixOperations(Path baseFolder, UpgradeProgress upgradeProgress) throws IOException {
        for (FileFixOperation fileFixOperation : this.fileFixOperations) {
            if (upgradeProgress.isCanceled()) {
                throw new CanceledFileFixException();
            }
            fileFixOperation.fix(baseFolder, upgradeProgress);
            upgradeProgress.incrementFinishedOperations();
        }
    }

    public abstract void makeFixer();
}

