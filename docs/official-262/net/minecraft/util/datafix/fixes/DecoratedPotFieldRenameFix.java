/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.fixes.References;

public class DecoratedPotFieldRenameFix
extends DataFix {
    private static final String DECORATED_POT_ID = "minecraft:decorated_pot";

    public DecoratedPotFieldRenameFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldDecoratedPot = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, DECORATED_POT_ID);
        Type newDecoratedPot = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, DECORATED_POT_ID);
        return this.convertUnchecked("DecoratedPotFieldRenameFix", oldDecoratedPot, newDecoratedPot);
    }
}

