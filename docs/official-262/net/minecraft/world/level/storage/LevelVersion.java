/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.storage.DataVersion;

public class LevelVersion {
    private final int levelDataVersion;
    private final long lastPlayed;
    private final String minecraftVersionName;
    private final DataVersion minecraftVersion;
    private final boolean snapshot;

    private LevelVersion(int levelDataVersion, long lastPlayed, String minecraftVersionName, int minecraftVersion, String series, boolean snapshot) {
        this.levelDataVersion = levelDataVersion;
        this.lastPlayed = lastPlayed;
        this.minecraftVersionName = minecraftVersionName;
        this.minecraftVersion = new DataVersion(minecraftVersion, series);
        this.snapshot = snapshot;
    }

    public static LevelVersion parse(Dynamic<?> input) {
        int levelDataVersion = input.get("version").asInt(0);
        long lastPlayed = input.get("LastPlayed").asLong(0L);
        OptionalDynamic version = input.get("Version");
        if (version.result().isPresent()) {
            return new LevelVersion(levelDataVersion, lastPlayed, version.get("Name").asString(SharedConstants.getCurrentVersion().name()), version.get("Id").asInt(SharedConstants.getCurrentVersion().dataVersion().version()), version.get("Series").asString("main"), version.get("Snapshot").asBoolean(!SharedConstants.getCurrentVersion().stable()));
        }
        return new LevelVersion(levelDataVersion, lastPlayed, "", 0, "main", false);
    }

    public int levelDataVersion() {
        return this.levelDataVersion;
    }

    public long lastPlayed() {
        return this.lastPlayed;
    }

    public String minecraftVersionName() {
        return this.minecraftVersionName;
    }

    public DataVersion minecraftVersion() {
        return this.minecraftVersion;
    }

    public boolean snapshot() {
        return this.snapshot;
    }
}

