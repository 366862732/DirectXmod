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
import java.util.stream.IntStream;
import net.minecraft.util.datafix.fixes.References;

public class ChunkTicketUnpackPosFix
extends DataFix {
    private static final long CHUNK_COORD_BITS = 32L;
    private static final long CHUNK_COORD_MASK = 0xFFFFFFFFL;

    public ChunkTicketUnpackPosFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("ChunkTicketUnpackPosFix", this.getInputSchema().getType(References.SAVED_DATA_TICKETS), input -> input.update(DSL.remainderFinder(), remainder -> remainder.update("data", data -> data.update("tickets", tickets -> tickets.createList(tickets.asStream().map(ticket -> ticket.update("chunk_pos", chunkPos -> {
            long key = chunkPos.asLong(0L);
            int x = (int)(key & 0xFFFFFFFFL);
            int z = (int)(key >>> 32 & 0xFFFFFFFFL);
            return chunkPos.createIntList(IntStream.of(x, z));
        })))))));
    }
}

