/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage;

import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelVersion;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class LevelSummary
implements Comparable<LevelSummary> {
    public static final Component PLAY_WORLD = Component.translatable("selectWorld.select");
    public static final Component UPGRADE_AND_PLAY_WORLD = Component.translatable("selectWorld.upgrade_and_play");
    private final LevelSettings settings;
    private final LevelVersion levelVersion;
    private final String levelId;
    private final boolean requiresManualConversion;
    private final boolean requiresFileFixing;
    private final boolean locked;
    private final boolean experimental;
    private final Path icon;
    private @Nullable Component info;

    public LevelSummary(LevelSettings settings, LevelVersion levelVersion, String levelId, boolean requiresManualConversion, boolean requiresFileFixing, boolean locked, boolean experimental, Path icon) {
        this.settings = settings;
        this.levelVersion = levelVersion;
        this.levelId = levelId;
        this.requiresFileFixing = requiresFileFixing;
        this.locked = locked;
        this.experimental = experimental;
        this.icon = icon;
        this.requiresManualConversion = requiresManualConversion;
    }

    public String getLevelId() {
        return this.levelId;
    }

    public String getLevelName() {
        return StringUtils.isEmpty((CharSequence)this.settings.levelName()) ? this.levelId : this.settings.levelName();
    }

    public Path getIcon() {
        return this.icon;
    }

    public boolean requiresManualConversion() {
        return this.requiresManualConversion;
    }

    public boolean requiresFileFixing() {
        return this.requiresFileFixing;
    }

    public boolean isExperimental() {
        return this.experimental;
    }

    public long getLastPlayed() {
        return this.levelVersion.lastPlayed();
    }

    @Override
    public int compareTo(LevelSummary rhs) {
        if (this.getLastPlayed() < rhs.getLastPlayed()) {
            return 1;
        }
        if (this.getLastPlayed() > rhs.getLastPlayed()) {
            return -1;
        }
        return this.levelId.compareTo(rhs.levelId);
    }

    public LevelSettings getSettings() {
        return this.settings;
    }

    public GameType getGameMode() {
        return this.settings.gameType();
    }

    public boolean isHardcore() {
        return this.settings.difficultySettings().hardcore();
    }

    public boolean hasCommands() {
        return this.settings.allowCommands();
    }

    public MutableComponent getWorldVersionName() {
        if (StringUtil.isNullOrEmpty(this.levelVersion.minecraftVersionName())) {
            return Component.translatable("selectWorld.versionUnknown");
        }
        return Component.literal(this.levelVersion.minecraftVersionName());
    }

    public LevelVersion levelVersion() {
        return this.levelVersion;
    }

    public boolean shouldBackup() {
        return this.backupStatus().shouldBackup();
    }

    public boolean isDowngrade() {
        return this.backupStatus() == BackupStatus.DOWNGRADE;
    }

    public BackupStatus backupStatus() {
        WorldVersion currentVersion = SharedConstants.getCurrentVersion();
        int currentVersionNumber = currentVersion.dataVersion().version();
        int levelVersionNumber = this.levelVersion.minecraftVersion().version();
        if (DataFixers.getFileFixer().requiresFileFixing(levelVersionNumber)) {
            return BackupStatus.FILE_FIXING_REQUIRED;
        }
        if (!currentVersion.stable() && levelVersionNumber < currentVersionNumber) {
            return BackupStatus.UPGRADE_TO_SNAPSHOT;
        }
        if (levelVersionNumber > currentVersionNumber) {
            return BackupStatus.DOWNGRADE;
        }
        return BackupStatus.NONE;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isDisabled() {
        if (this.isLocked() || this.requiresManualConversion()) {
            return true;
        }
        return !this.isCompatible();
    }

    public boolean isCompatible() {
        return SharedConstants.getCurrentVersion().dataVersion().isCompatible(this.levelVersion.minecraftVersion());
    }

    public Component getInfo() {
        if (this.info == null) {
            this.info = this.createInfo();
        }
        return this.info;
    }

    private Component createInfo() {
        MutableComponent result;
        if (this.isLocked()) {
            return Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
        }
        if (this.requiresManualConversion()) {
            return Component.translatable("selectWorld.conversion").withStyle(ChatFormatting.RED);
        }
        if (!this.isCompatible()) {
            return Component.translatable("selectWorld.incompatible.info", this.getWorldVersionName()).withStyle(ChatFormatting.RED);
        }
        MutableComponent mutableComponent = result = this.isHardcore() ? Component.empty().append(Component.translatable("gameMode.hardcore").withColor(-65536)) : Component.translatable("gameMode." + this.getGameMode().getName());
        if (this.hasCommands()) {
            result.append(", ").append(Component.translatable("selectWorld.commands"));
        }
        if (this.isExperimental()) {
            result.append(", ").append(Component.translatable("selectWorld.experimental").withStyle(ChatFormatting.YELLOW));
        }
        MutableComponent worldVersionName = this.getWorldVersionName();
        MutableComponent decoratedVersionName = Component.literal(", ").append(Component.translatable("selectWorld.version")).append(CommonComponents.SPACE);
        if (this.shouldBackup()) {
            decoratedVersionName.append(worldVersionName.withStyle(this.isDowngrade() ? ChatFormatting.RED : ChatFormatting.ITALIC));
        } else {
            decoratedVersionName.append(worldVersionName);
        }
        result.append(decoratedVersionName);
        return result;
    }

    public Component primaryActionMessage() {
        if (this.requiresFileFixing()) {
            return UPGRADE_AND_PLAY_WORLD;
        }
        return PLAY_WORLD;
    }

    public boolean primaryActionActive() {
        return !this.isDisabled();
    }

    public boolean canUpload() {
        return !this.requiresManualConversion() && !this.isLocked();
    }

    public boolean canEdit() {
        return !this.isDisabled() && !this.requiresFileFixing();
    }

    public boolean canRecreate() {
        return !this.isDisabled() && !this.requiresFileFixing();
    }

    public boolean canDelete() {
        return true;
    }

    public static enum BackupStatus {
        NONE(false, false, ""),
        DOWNGRADE(true, true, "downgrade"),
        UPGRADE_TO_SNAPSHOT(true, false, "snapshot"),
        FILE_FIXING_REQUIRED(true, false, "file_fixing_required");

        private final boolean shouldBackup;
        private final boolean severe;
        private final String translationKey;

        private BackupStatus(boolean shouldBackup, boolean severe, String translationKey) {
            this.shouldBackup = shouldBackup;
            this.severe = severe;
            this.translationKey = translationKey;
        }

        public boolean shouldBackup() {
            return this.shouldBackup;
        }

        public boolean isSevere() {
            return this.severe;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }

    public static class CorruptedLevelSummary
    extends LevelSummary {
        private static final Component INFO = Component.translatable("recover_world.warning").withStyle(style -> style.withColor(-65536));
        private static final Component RECOVER = Component.translatable("recover_world.button");
        private final long lastPlayed;

        public CorruptedLevelSummary(String levelId, Path icon, long lastPlayed) {
            super(null, null, levelId, false, false, false, false, icon);
            this.lastPlayed = lastPlayed;
        }

        @Override
        public String getLevelName() {
            return this.getLevelId();
        }

        @Override
        public Component getInfo() {
            return INFO;
        }

        @Override
        public long getLastPlayed() {
            return this.lastPlayed;
        }

        @Override
        public boolean isDisabled() {
            return false;
        }

        @Override
        public Component primaryActionMessage() {
            return RECOVER;
        }

        @Override
        public boolean primaryActionActive() {
            return true;
        }

        @Override
        public boolean canUpload() {
            return false;
        }

        @Override
        public boolean canEdit() {
            return false;
        }

        @Override
        public boolean canRecreate() {
            return false;
        }
    }

    public static class SymlinkLevelSummary
    extends LevelSummary {
        private static final Component MORE_INFO_BUTTON = Component.translatable("symlink_warning.more_info");
        private static final Component INFO = Component.translatable("symlink_warning.title").withColor(-65536);

        public SymlinkLevelSummary(String levelId, Path icon) {
            super(null, null, levelId, false, false, false, false, icon);
        }

        @Override
        public String getLevelName() {
            return this.getLevelId();
        }

        @Override
        public Component getInfo() {
            return INFO;
        }

        @Override
        public long getLastPlayed() {
            return -1L;
        }

        @Override
        public boolean isDisabled() {
            return false;
        }

        @Override
        public Component primaryActionMessage() {
            return MORE_INFO_BUTTON;
        }

        @Override
        public boolean primaryActionActive() {
            return true;
        }

        @Override
        public boolean canUpload() {
            return false;
        }

        @Override
        public boolean canEdit() {
            return false;
        }

        @Override
        public boolean canRecreate() {
            return false;
        }
    }
}

