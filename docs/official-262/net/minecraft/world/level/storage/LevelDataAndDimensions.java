/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage;

import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.WorldData;

public record LevelDataAndDimensions(WorldDataAndGenSettings worldDataAndGenSettings, WorldDimensions.Complete dimensions) {
    public static LevelDataAndDimensions create(WorldData data, WorldGenSettings genSettings, WorldDimensions.Complete dimensions) {
        return new LevelDataAndDimensions(new WorldDataAndGenSettings(data, genSettings), dimensions);
    }

    public record WorldDataAndGenSettings(WorldData data, WorldGenSettings genSettings) {
    }
}

