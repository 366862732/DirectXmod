/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.References;

public class LegacyDimensionIdFix
extends DataFix {
    public LegacyDimensionIdFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        TypeRewriteRule playerRule = this.fixTypeEverywhereTyped("PlayerLegacyDimensionFix", this.getInputSchema().getType(References.PLAYER), input -> input.update(DSL.remainderFinder(), this::fixPlayer));
        Type dataType = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
        OpticFinder mapDataF = dataType.findField("data");
        TypeRewriteRule mapRule = this.fixTypeEverywhereTyped("MapLegacyDimensionFix", dataType, input -> input.updateTyped(mapDataF, data -> data.update(DSL.remainderFinder(), this::fixMap)));
        return TypeRewriteRule.seq((TypeRewriteRule)playerRule, (TypeRewriteRule)mapRule);
    }

    private <T> Dynamic<T> fixMap(Dynamic<T> remainder) {
        return remainder.update("dimension", this::fixDimensionId);
    }

    private <T> Dynamic<T> fixPlayer(Dynamic<T> remainder) {
        return remainder.update("Dimension", this::fixDimensionId);
    }

    private <T> Dynamic<T> fixDimensionId(Dynamic<T> id) {
        return (Dynamic)DataFixUtils.orElse(id.asNumber().result().map(legacyId -> switch (legacyId.intValue()) {
            case -1 -> id.createString("minecraft:the_nether");
            case 1 -> id.createString("minecraft:the_end");
            default -> id.createString("minecraft:overworld");
        }), id);
    }
}

