/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.datafix.fixes.References;

public class ChunkStatusFix2
extends DataFix {
    private static final Map<String, String> RENAMES_AND_DOWNGRADES = ImmutableMap.builder().put((Object)"structure_references", (Object)"empty").put((Object)"biomes", (Object)"empty").put((Object)"base", (Object)"surface").put((Object)"carved", (Object)"carvers").put((Object)"liquid_carved", (Object)"liquid_carvers").put((Object)"decorated", (Object)"features").put((Object)"lighted", (Object)"light").put((Object)"mobs_spawned", (Object)"spawn").put((Object)"finalized", (Object)"heightmaps").put((Object)"fullchunk", (Object)"full").build();

    public ChunkStatusFix2(Schema schema, boolean changesType) {
        super(schema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        Type levelType = chunkType.findFieldType("Level");
        OpticFinder levelF = DSL.fieldFinder((String)"Level", (Type)levelType);
        return this.fixTypeEverywhereTyped("ChunkStatusFix2", chunkType, this.getOutputSchema().getType(References.CHUNK), input -> input.updateTyped(levelF, level -> {
            String newStatus;
            Dynamic tag = (Dynamic)level.get(DSL.remainderFinder());
            String status = tag.get("Status").asString("empty");
            if (Objects.equals(status, newStatus = RENAMES_AND_DOWNGRADES.getOrDefault(status, "empty"))) {
                return level;
            }
            return level.set(DSL.remainderFinder(), (Object)tag.set("Status", tag.createString(newStatus)));
        }));
    }
}

