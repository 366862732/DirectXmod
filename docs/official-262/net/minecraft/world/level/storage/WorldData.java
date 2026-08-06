/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Lifecycle
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage;

import com.mojang.serialization.Lifecycle;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

public interface WorldData {
    public static final int ANVIL_VERSION_ID = 19133;
    public static final int MCREGION_VERSION_ID = 19132;

    public WorldDataConfiguration getDataConfiguration();

    public void setDataConfiguration(WorldDataConfiguration var1);

    public boolean wasModded();

    public Set<String> getKnownServerBrands();

    public Set<String> getRemovedFeatureFlags();

    public void setModdedInfo(String var1, boolean var2);

    default public void fillCrashReportCategory(CrashReportCategory category) {
        category.setDetail("Known server brands", () -> String.join((CharSequence)", ", this.getKnownServerBrands()));
        category.setDetail("Removed feature flags", () -> String.join((CharSequence)", ", this.getRemovedFeatureFlags()));
        category.setDetail("Level was modded", () -> Boolean.toString(this.wasModded()));
        category.setDetail("Level storage version", () -> {
            int version = this.getVersion();
            return String.format(Locale.ROOT, "0x%05X - %s", version, this.getStorageVersionName(version));
        });
    }

    default public String getStorageVersionName(int version) {
        return switch (version) {
            case 19133 -> "Anvil";
            case 19132 -> "McRegion";
            default -> "Unknown?";
        };
    }

    public ServerLevelData overworldData();

    public LevelSettings getLevelSettings();

    public CompoundTag createTag(@Nullable UUID var1);

    public boolean isHardcore();

    public int getVersion();

    public String getLevelName();

    public GameType getGameType();

    public void setGameType(GameType var1);

    public boolean isAllowCommands();

    public void setAllowCommands(boolean var1);

    public Difficulty getDifficulty();

    public void setDifficulty(Difficulty var1);

    public boolean isDifficultyLocked();

    public void setDifficultyLocked(boolean var1);

    public @Nullable UUID getSinglePlayerUUID();

    public boolean isFlatWorld();

    public boolean isDebugWorld();

    public Lifecycle worldGenSettingsLifecycle();

    default public FeatureFlagSet enabledFeatures() {
        return this.getDataConfiguration().enabledFeatures();
    }
}

