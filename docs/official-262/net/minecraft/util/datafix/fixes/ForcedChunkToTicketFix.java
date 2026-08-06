/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.fixes.References;

public class ForcedChunkToTicketFix
extends DataFix {
    public ForcedChunkToTicketFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("ForcedChunkToTicketFix", this.getInputSchema().getType(References.SAVED_DATA_TICKETS), input -> input.update(DSL.remainderFinder(), remainder -> remainder.update("data", data -> data.renameAndFixField("Forced", "tickets", forcedChunks -> forcedChunks.createList(forcedChunks.asLongStream().mapToObj(l -> remainder.emptyMap().set("type", remainder.createString("minecraft:forced")).set("level", remainder.createInt(31)).set("ticks_left", remainder.createLong(0L)).set("chunk_pos", remainder.createLong(l))))))));
    }
}

