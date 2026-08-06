/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class OverreachingTickFix
extends DataFix {
    public OverreachingTickFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder blockTicksFinder = chunkType.findField("block_ticks");
        return this.fixTypeEverywhereTyped("Handle ticks saved in the wrong chunk", chunkType, chunk -> {
            Optional blockTicksOpt = chunk.getOptionalTyped(blockTicksFinder);
            Optional blockTicks = blockTicksOpt.isPresent() ? ((Typed)blockTicksOpt.get()).write().result() : Optional.empty();
            return chunk.update(DSL.remainderFinder(), remainder -> {
                int chunkX = remainder.get("xPos").asInt(0);
                int chunkZ = remainder.get("zPos").asInt(0);
                Optional fluidTicks = remainder.get("fluid_ticks").get().result();
                remainder = OverreachingTickFix.extractOverreachingTicks(remainder, chunkX, chunkZ, blockTicks, "neighbor_block_ticks");
                remainder = OverreachingTickFix.extractOverreachingTicks(remainder, chunkX, chunkZ, fluidTicks, "neighbor_fluid_ticks");
                return remainder;
            });
        });
    }

    private static Dynamic<?> extractOverreachingTicks(Dynamic<?> remainder, int chunkX, int chunkZ, Optional<? extends Dynamic<?>> ticks, String nameInUpgradeData) {
        List<Dynamic> overreachingTicks;
        if (ticks.isPresent() && !(overreachingTicks = ticks.get().asStream().filter(tick -> {
            int x = tick.get("x").asInt(0);
            int z = tick.get("z").asInt(0);
            int distX = Math.abs(chunkX - (x >> 4));
            int distZ = Math.abs(chunkZ - (z >> 4));
            return (distX != 0 || distZ != 0) && distX <= 1 && distZ <= 1;
        }).toList()).isEmpty()) {
            remainder = remainder.set("UpgradeData", remainder.get("UpgradeData").orElseEmptyMap().set(nameInUpgradeData, remainder.createList(overreachingTicks.stream())));
        }
        return remainder;
    }
}

