/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.filefix.access;

import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.filefix.access.CompressedNbt;

public class SavedDataNbt
extends CompressedNbt {
    private final DSL.TypeReference type;
    private final int targetVersion;

    public SavedDataNbt(DSL.TypeReference type, Path path, int targetVersion, CompressedNbt.MissingSeverity missingSeverity) {
        super(path, missingSeverity);
        this.type = type;
        this.targetVersion = targetVersion;
    }

    @Override
    public Optional<Dynamic<Tag>> read() throws IOException {
        return this.readFile().map(readData -> {
            int version = NbtUtils.getDataVersion(readData);
            return DataFixers.getDataFixer().update(this.type, readData, version, this.targetVersion).get("data").orElseEmptyMap();
        });
    }

    @Override
    public <T> void write(Dynamic<T> data) {
        Dynamic dataTag = data.emptyMap().set("data", data);
        Dynamic wrappedAndWithDataVersion = NbtUtils.addDataVersion(dataTag, this.targetVersion);
        this.writeFile(wrappedAndWithDataVersion);
    }
}

