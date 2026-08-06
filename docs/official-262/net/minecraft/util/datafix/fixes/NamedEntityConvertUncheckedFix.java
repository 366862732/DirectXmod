/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.NamedEntityFix;

public class NamedEntityConvertUncheckedFix
extends NamedEntityFix {
    public NamedEntityConvertUncheckedFix(Schema outputSchema, String name, DSL.TypeReference type, String entityName) {
        super(outputSchema, true, name, type, entityName);
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        Type outputType = this.getOutputSchema().getChoiceType(this.type, this.entityName);
        return ExtraDataFixUtils.cast(outputType, entity);
    }
}

