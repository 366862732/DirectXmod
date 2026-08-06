/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  org.slf4j.Logger
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.AbstractUUIDFix;
import net.minecraft.util.datafix.fixes.References;
import org.slf4j.Logger;

public class LevelUUIDFix
extends AbstractUUIDFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public LevelUUIDFix(Schema outputSchema) {
        super(outputSchema, References.LEVEL);
    }

    protected TypeRewriteRule makeRule() {
        Type type = this.getInputSchema().getType(this.typeReference);
        OpticFinder customBossEventsF = type.findField("CustomBossEvents");
        OpticFinder customBossEventF = DSL.typeFinder((Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"Name", (Type)this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT))), (Type)DSL.remainderType()));
        return this.fixTypeEverywhereTyped("LevelUUIDFix", type, input -> input.update(DSL.remainderFinder(), tag -> {
            tag = this.updateDragonFight((Dynamic<?>)tag);
            tag = this.updateWanderingTrader((Dynamic<?>)tag);
            return tag;
        }).updateTyped(customBossEventsF, customBossEvents -> customBossEvents.updateTyped(customBossEventF, event -> event.update(DSL.remainderFinder(), this::updateCustomBossEvent))));
    }

    private Dynamic<?> updateWanderingTrader(Dynamic<?> tag) {
        return LevelUUIDFix.replaceUUIDString(tag, "WanderingTraderId", "WanderingTraderId").orElse(tag);
    }

    private Dynamic<?> updateDragonFight(Dynamic<?> tag) {
        return tag.update("DimensionData", dimensionDataMap -> dimensionDataMap.updateMapValues(dimensionDataPair -> dimensionDataPair.mapSecond(dimensionData -> dimensionData.update("DragonFight", dragonfight -> LevelUUIDFix.replaceUUIDLeastMost(dragonfight, "DragonUUID", "Dragon").orElse((Dynamic<?>)dragonfight)))));
    }

    private Dynamic<?> updateCustomBossEvent(Dynamic<?> tag) {
        return tag.update("Players", players -> tag.createList(players.asStream().map(player -> LevelUUIDFix.createUUIDFromML(player).orElseGet(() -> {
            LOGGER.warn("CustomBossEvents contains invalid UUIDs.");
            return player;
        }))));
    }
}

