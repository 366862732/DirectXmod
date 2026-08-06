/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.fixes.References;

public class ChunkLightRemoveFix
extends DataFix {
    public ChunkLightRemoveFix(Schema schema, boolean changesType) {
        super(schema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        Type levelType = chunkType.findFieldType("Level");
        OpticFinder levelF = DSL.fieldFinder((String)"Level", (Type)levelType);
        return this.fixTypeEverywhereTyped("ChunkLightRemoveFix", chunkType, this.getOutputSchema().getType(References.CHUNK), input -> input.updateTyped(levelF, level -> level.update(DSL.remainderFinder(), tag -> tag.remove("isLightOn"))));
    }
}

