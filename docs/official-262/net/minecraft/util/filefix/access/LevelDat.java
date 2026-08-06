/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.filefix.access;

import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.filefix.access.CompressedNbt;

public class LevelDat
extends CompressedNbt {
    private final int targetVersion;

    public LevelDat(Path path, int targetVersion) {
        super(path, CompressedNbt.MissingSeverity.IMPORTANT);
        this.targetVersion = targetVersion;
    }

    @Override
    public Optional<Dynamic<Tag>> read() throws IOException {
        return this.readFile().map(readData -> {
            Dynamic content = readData.get("Data").orElseEmptyMap();
            int dataVersion = NbtUtils.getDataVersion(content);
            return DataFixers.getDataFixer().update(References.LEVEL, content, dataVersion, this.targetVersion);
        });
    }

    @Override
    public <T> void write(Dynamic<T> data) {
        this.writeFile(data.emptyMap().set("Data", NbtUtils.addDataVersion(data, this.targetVersion)));
    }
}

