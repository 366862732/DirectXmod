/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.FileAccess;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.access.FileResourceTypes;
import net.minecraft.util.filefix.access.LevelDat;
import net.minecraft.util.filefix.access.SavedDataNbt;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class ReenableSpectatorsGenerateChunksInHardcoreWorldsFileFix
extends FileFix {
    public ReenableSpectatorsGenerateChunksInHardcoreWorldsFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        this.addFileContentFix(files -> {
            FileAccess<LevelDat> levelDat = files.getFileAccess(FileResourceTypes.LEVEL_DAT, FileRelation.ORIGIN.forFile("level.dat"));
            FileAccess<SavedDataNbt> gameRules = files.getFileAccess(FileResourceTypes.savedData(References.SAVED_DATA_GAME_RULES), FileRelation.DATA.forFile("minecraft/game_rules.dat"));
            return upgradeProgress -> {
                boolean hardcore;
                upgradeProgress.setType(UpgradeProgress.Type.FILES);
                Optional<Dynamic<Tag>> levelDatData = ((LevelDat)levelDat.getOnlyFile()).read();
                if (levelDatData.isEmpty()) {
                    return;
                }
                Dynamic<Tag> data = levelDatData.get();
                boolean wasEverLoadedInSingleplayer = data.get("singleplayer_uuid").get().isSuccess();
                if (wasEverLoadedInSingleplayer && (hardcore = data.get("difficulty_settings").orElseEmptyMap().get("hardcore").asBoolean(false))) {
                    SavedDataNbt gameRulesFile = (SavedDataNbt)gameRules.getOnlyFile();
                    gameRulesFile.read().ifPresent(dataTag -> gameRulesFile.write(dataTag.update("minecraft:spectators_generate_chunks", old -> old.createBoolean(true))));
                }
            };
        });
    }
}

