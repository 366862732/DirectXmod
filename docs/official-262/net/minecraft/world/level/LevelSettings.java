/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;

public record LevelSettings(String levelName, GameType gameType, DifficultySettings difficultySettings, boolean allowCommands, WorldDataConfiguration dataConfiguration) {
    public static LevelSettings parse(Dynamic<?> input, WorldDataConfiguration loadConfig) {
        GameType gameType = GameType.byId(input.get("GameType").asInt(0));
        return new LevelSettings(input.get("LevelName").asString(""), gameType, input.get("difficulty_settings").read(DifficultySettings.CODEC).result().orElse(DifficultySettings.DEFAULT), input.get("allowCommands").asBoolean(gameType == GameType.CREATIVE), loadConfig);
    }

    public LevelSettings withGameType(GameType gameType) {
        return new LevelSettings(this.levelName, gameType, this.difficultySettings, this.allowCommands, this.dataConfiguration);
    }

    public LevelSettings withAllowCommands(boolean allowCommands) {
        return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, allowCommands, this.dataConfiguration);
    }

    public LevelSettings withDifficulty(Difficulty difficulty) {
        return new LevelSettings(this.levelName, this.gameType, new DifficultySettings(difficulty, this.difficultySettings.hardcore(), this.difficultySettings.locked()), this.allowCommands, this.dataConfiguration);
    }

    public LevelSettings withDifficultyLock(boolean locked) {
        return new LevelSettings(this.levelName, this.gameType, new DifficultySettings(this.difficultySettings.difficulty(), this.difficultySettings.hardcore(), locked), this.allowCommands, this.dataConfiguration);
    }

    public LevelSettings withDataConfiguration(WorldDataConfiguration dataConfiguration) {
        return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, this.allowCommands, dataConfiguration);
    }

    public LevelSettings copy() {
        return new LevelSettings(this.levelName, this.gameType, this.difficultySettings, this.allowCommands, this.dataConfiguration);
    }

    public record DifficultySettings(Difficulty difficulty, boolean hardcore, boolean locked) {
        public static final DifficultySettings DEFAULT = new DifficultySettings(Difficulty.NORMAL, false, false);
        public static final Codec<DifficultySettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)Difficulty.CODEC.fieldOf("difficulty").forGetter(DifficultySettings::difficulty), (App)Codec.BOOL.fieldOf("hardcore").forGetter(DifficultySettings::hardcore), (App)Codec.BOOL.fieldOf("locked").forGetter(DifficultySettings::locked)).apply((Applicative)i, DifficultySettings::new));
    }
}

