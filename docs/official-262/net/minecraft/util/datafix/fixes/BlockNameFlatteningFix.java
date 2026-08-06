/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Objects;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BlockNameFlatteningFix
extends DataFix {
    public BlockNameFlatteningFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type blockType = this.getInputSchema().getType(References.BLOCK_NAME);
        Type newBlockType = this.getOutputSchema().getType(References.BLOCK_NAME);
        Type expectedBlockType = DSL.named((String)References.BLOCK_NAME.typeName(), (Type)DSL.or((Type)DSL.intType(), NamespacedSchema.namespacedString()));
        Type expectedNewBlockType = DSL.named((String)References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
        if (!Objects.equals(blockType, expectedBlockType) || !Objects.equals(newBlockType, expectedNewBlockType)) {
            throw new IllegalStateException("Expected and actual types don't match.");
        }
        return this.fixTypeEverywhere("BlockNameFlatteningFix", expectedBlockType, expectedNewBlockType, ops -> block -> block.mapSecond(choice -> (String)choice.map(BlockStateData::upgradeBlock, name -> BlockStateData.upgradeBlock(NamespacedSchema.ensureNamespaced(name)))));
    }
}

