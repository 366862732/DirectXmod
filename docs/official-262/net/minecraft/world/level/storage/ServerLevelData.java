/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage;

import java.util.Locale;
import net.minecraft.CrashReportCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.storage.WritableLevelData;

public interface ServerLevelData
extends WritableLevelData {
    public String getLevelName();

    @Override
    default public void fillCrashReportCategory(CrashReportCategory category, LevelHeightAccessor levelHeightAccessor) {
        WritableLevelData.super.fillCrashReportCategory(category, levelHeightAccessor);
        category.setDetail("Level name", this::getLevelName);
        category.setDetail("Level game mode", () -> String.format(Locale.ROOT, "Game mode: %s (ID %d). Hardcore: %b. Commands: %b", this.getGameType().getName(), this.getGameType().getId(), this.isHardcore(), this.isAllowCommands()));
    }

    public GameType getGameType();

    public boolean isInitialized();

    public void setInitialized(boolean var1);

    public boolean isAllowCommands();

    public void setAllowCommands(boolean var1);

    public void setGameType(GameType var1);

    public void setGameTime(long var1);
}

