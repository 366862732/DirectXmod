/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DSL$TypeReference
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
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class NamedEntityWriteReadFix
extends DataFix {
    private final String name;
    private final String entityName;
    private final DSL.TypeReference type;

    public NamedEntityWriteReadFix(Schema outputSchema, boolean changesType, String name, DSL.TypeReference type, String entityName) {
        super(outputSchema, changesType);
        this.name = name;
        this.type = type;
        this.entityName = entityName;
    }

    public TypeRewriteRule makeRule() {
        Type inputEntityType = this.getInputSchema().getType(this.type);
        Type inputEntityChoiceType = this.getInputSchema().getChoiceType(this.type, this.entityName);
        Type outputEntityType = this.getOutputSchema().getType(this.type);
        OpticFinder entityF = DSL.namedChoice((String)this.entityName, (Type)inputEntityChoiceType);
        Type<?> patchedEntityType = ExtraDataFixUtils.patchSubType(inputEntityType, inputEntityType, outputEntityType);
        return this.fix(inputEntityType, outputEntityType, patchedEntityType, entityF);
    }

    private <S, T, A> TypeRewriteRule fix(Type<S> inputEntityType, Type<T> outputEntityType, Type<?> patchedEntityType, OpticFinder<A> choiceFinder) {
        return this.fixTypeEverywhereTyped(this.name, inputEntityType, outputEntityType, typed -> {
            if (typed.getOptional(choiceFinder).isEmpty()) {
                return ExtraDataFixUtils.cast(outputEntityType, typed);
            }
            Typed fakeTyped = ExtraDataFixUtils.cast(patchedEntityType, typed);
            return Util.writeAndReadTypedOrThrow(fakeTyped, outputEntityType, this::fix);
        });
    }

    protected abstract <T> Dynamic<T> fix(Dynamic<T> var1);
}

