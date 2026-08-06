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

public class DayTimeToClockFix
extends DataFix {
    public DayTimeToClockFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("DayTimeToClockFix", this.getInputSchema().getType(References.LEVEL), typed -> typed.update(DSL.remainderFinder(), input -> {
            long gameTime = input.get("Time").asLong(0L);
            long dayTime = input.get("DayTime").asLong(gameTime);
            input = input.remove("DayTime");
            Dynamic<?> overworldClock = DayTimeToClockFix.createClock(input, dayTime);
            return input.set("world_clocks", input.emptyMap().set("minecraft:overworld", overworldClock));
        }));
    }

    private static Dynamic<?> createClock(Dynamic<?> input, long totalTicks) {
        return input.emptyMap().set("total_ticks", input.createLong(totalTicks)).set("paused", input.createBoolean(false));
    }
}

