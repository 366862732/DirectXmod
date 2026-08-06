/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class PlayerStorageFileFix
extends FileFix {
    public PlayerStorageFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        this.addFileFixOperation(FileFixOperations.move("advancements", "players/advancements"));
        this.addFileFixOperation(FileFixOperations.move("playerdata", "players/data"));
        this.addFileFixOperation(FileFixOperations.move("stats", "players/stats"));
    }
}

