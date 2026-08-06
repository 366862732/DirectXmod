/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.List;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class GeneratedStructuresRenameFileFix
extends FileFix {
    public GeneratedStructuresRenameFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        this.addFileFixOperation(FileFixOperations.applyInFolders(FileRelation.GENERATED_NAMESPACES, List.of(FileFixOperations.move("structures", "structure"))));
    }
}

