/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.References;

public class LevelDatDifficultyFix
extends DataFix {
    public LevelDatDifficultyFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("LevelDatDifficultyFix", this.getInputSchema().getType(References.LIGHTWEIGHT_LEVEL), input -> input.update(DSL.remainderFinder(), levelData -> {
            int difficulty = levelData.get("Difficulty").asInt(2);
            String newDifficulty = switch (difficulty) {
                case 0 -> "peaceful";
                case 1 -> "easy";
                case 3 -> "hard";
                default -> "normal";
            };
            Dynamic difficultySettings = levelData.emptyMap().set("difficulty", levelData.createString(newDifficulty)).set("hardcore", levelData.createBoolean(levelData.get("hardcore").asBoolean(false))).set("locked", levelData.createBoolean(levelData.get("DifficultyLocked").asBoolean(false)));
            return levelData.set("difficulty_settings", difficultySettings).remove("Difficulty").remove("hardcore").remove("DifficultyLocked");
        }));
    }
}

