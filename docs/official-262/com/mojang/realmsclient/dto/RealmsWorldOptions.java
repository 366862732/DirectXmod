/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.Exclude;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ValueObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsWorldOptions
extends ValueObject
implements ReflectionBasedSerialization {
    @SerializedName(value="spawnProtection")
    public int spawnProtection = 0;
    @SerializedName(value="forceGameMode")
    public boolean forceGameMode = false;
    @SerializedName(value="difficulty")
    public int difficulty = 2;
    @SerializedName(value="gameMode")
    public int gameMode = 0;
    @SerializedName(value="slotName")
    private String slotName = "";
    @SerializedName(value="version")
    public String version = "";
    @SerializedName(value="compatibility")
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
    @SerializedName(value="worldTemplateId")
    public long templateId = -1L;
    @SerializedName(value="worldTemplateImage")
    public @Nullable String templateImage = null;
    @Exclude
    public boolean empty;

    private RealmsWorldOptions() {
    }

    public RealmsWorldOptions(int spawnProtection, int difficulty, int gameMode, boolean forceGameMode, String slotName, String version, RealmsServer.Compatibility compatibility) {
        this.spawnProtection = spawnProtection;
        this.difficulty = difficulty;
        this.gameMode = gameMode;
        this.forceGameMode = forceGameMode;
        this.slotName = slotName;
        this.version = version;
        this.compatibility = compatibility;
    }

    public static RealmsWorldOptions createDefaults() {
        return new RealmsWorldOptions();
    }

    public static RealmsWorldOptions createDefaultsWith(GameType gameMode, Difficulty difficulty, boolean hardcore, String version, String worldName) {
        RealmsWorldOptions options = RealmsWorldOptions.createDefaults();
        options.difficulty = difficulty.getId();
        options.gameMode = gameMode.getId();
        options.slotName = worldName;
        options.version = version;
        return options;
    }

    public static RealmsWorldOptions createFromSettings(LevelSettings settings, String worldVersion) {
        return RealmsWorldOptions.createDefaultsWith(settings.gameType(), settings.difficultySettings().difficulty(), settings.difficultySettings().hardcore(), worldVersion, settings.levelName());
    }

    public static RealmsWorldOptions createEmptyDefaults() {
        RealmsWorldOptions options = RealmsWorldOptions.createDefaults();
        options.setEmpty(true);
        return options;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public static RealmsWorldOptions parse(GuardedSerializer gson, String json) {
        RealmsWorldOptions options = gson.fromJson(json, RealmsWorldOptions.class);
        if (options == null) {
            return RealmsWorldOptions.createDefaults();
        }
        RealmsWorldOptions.finalize(options);
        return options;
    }

    private static void finalize(RealmsWorldOptions options) {
        if (options.slotName == null) {
            options.slotName = "";
        }
        if (options.version == null) {
            options.version = "";
        }
        if (options.compatibility == null) {
            options.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
        }
    }

    public String getSlotName(int i) {
        if (StringUtil.isBlank(this.slotName)) {
            if (this.empty) {
                return I18n.get("mco.configure.world.slot.empty", new Object[0]);
            }
            return this.getDefaultSlotName(i);
        }
        return this.slotName;
    }

    public String getDefaultSlotName(int i) {
        return I18n.get("mco.configure.world.slot", i);
    }

    public RealmsWorldOptions copy() {
        return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
    }
}

