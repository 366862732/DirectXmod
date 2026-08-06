/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class VillagerDataFix
extends NamedEntityFix {
    public VillagerDataFix(Schema schema, String entityType) {
        super(schema, false, "Villager profession data fix (" + entityType + ")", References.ENTITY, entityType);
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        Dynamic remainder = (Dynamic)entity.get(DSL.remainderFinder());
        return entity.set(DSL.remainderFinder(), (Object)remainder.remove("Profession").remove("Career").remove("CareerLevel").set("VillagerData", remainder.createMap((Map)ImmutableMap.of((Object)remainder.createString("type"), (Object)remainder.createString("minecraft:plains"), (Object)remainder.createString("profession"), (Object)remainder.createString(VillagerDataFix.upgradeData(remainder.get("Profession").asInt(0), remainder.get("Career").asInt(0))), (Object)remainder.createString("level"), (Object)((Dynamic)DataFixUtils.orElse((Optional)remainder.get("CareerLevel").result(), (Object)remainder.createInt(1)))))));
    }

    private static String upgradeData(int profession, int career) {
        if (profession == 0) {
            if (career == 2) {
                return "minecraft:fisherman";
            }
            if (career == 3) {
                return "minecraft:shepherd";
            }
            if (career == 4) {
                return "minecraft:fletcher";
            }
            return "minecraft:farmer";
        }
        if (profession == 1) {
            if (career == 2) {
                return "minecraft:cartographer";
            }
            return "minecraft:librarian";
        }
        if (profession == 2) {
            return "minecraft:cleric";
        }
        if (profession == 3) {
            if (career == 2) {
                return "minecraft:weaponsmith";
            }
            if (career == 3) {
                return "minecraft:toolsmith";
            }
            return "minecraft:armorer";
        }
        if (profession == 4) {
            if (career == 2) {
                return "minecraft:leatherworker";
            }
            return "minecraft:butcher";
        }
        if (profession == 5) {
            return "minecraft:nitwit";
        }
        return "minecraft:none";
    }
}

