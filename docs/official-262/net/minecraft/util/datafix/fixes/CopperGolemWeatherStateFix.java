/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class CopperGolemWeatherStateFix
extends NamedEntityFix {
    public CopperGolemWeatherStateFix(Schema outputSchema) {
        super(outputSchema, false, "CopperGolemWeatherStateFix", References.ENTITY, "minecraft:copper_golem");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), tag -> tag.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
    }

    private static Dynamic<?> fixWeatherState(Dynamic<?> value) {
        return switch (value.asInt(0)) {
            case 1 -> value.createString("exposed");
            case 2 -> value.createString("weathered");
            case 3 -> value.createString("oxidized");
            default -> value.createString("unaffected");
        };
    }
}

