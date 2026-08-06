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
import java.util.List;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class LevelLegacyWorldGenSettingsFix
extends DataFix {
    private static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
    private static final List<String> OLD_SETTINGS_KEYS = List.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");

    public LevelLegacyWorldGenSettingsFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("LevelLegacyWorldGenSettingsFix", this.getInputSchema().getType(References.LEVEL), input -> input.update(DSL.remainderFinder(), dataTag -> {
            Dynamic worldGenSettings = dataTag.get(WORLD_GEN_SETTINGS).orElseEmptyMap();
            for (String key : OLD_SETTINGS_KEYS) {
                Optional oldSetting = dataTag.get(key).result();
                if (!oldSetting.isPresent()) continue;
                dataTag = dataTag.remove(key);
                worldGenSettings = worldGenSettings.set(key, (Dynamic)oldSetting.get());
            }
            return dataTag.set(WORLD_GEN_SETTINGS, worldGenSettings);
        }));
    }
}

