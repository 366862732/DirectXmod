/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TaggedChoice$TaggedChoiceType
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.fixes.References;

public class BlockEntityRenameFix
extends DataFix {
    private final String name;
    private final UnaryOperator<String> nameChangeLookup;

    private BlockEntityRenameFix(Schema outputSchema, String name, UnaryOperator<String> nameChangeLookup) {
        super(outputSchema, true);
        this.name = name;
        this.nameChangeLookup = nameChangeLookup;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoice.TaggedChoiceType oldType = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        TaggedChoice.TaggedChoiceType newType = this.getOutputSchema().findChoiceType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhere(this.name, (Type)oldType, (Type)newType, ops -> input -> input.mapFirst(this.nameChangeLookup));
    }

    public static DataFix create(Schema outputSchema, String name, UnaryOperator<String> nameChangeLookup) {
        return new BlockEntityRenameFix(outputSchema, name, nameChangeLookup);
    }
}

