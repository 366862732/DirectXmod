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
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class ProjectileStoredWeaponFix
extends DataFix {
    public ProjectileStoredWeaponFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type inputEntityType = this.getInputSchema().getType(References.ENTITY);
        Type outputEntityType = this.getOutputSchema().getType(References.ENTITY);
        return this.fixTypeEverywhereTyped("Fix Arrow stored weapon", inputEntityType, outputEntityType, ExtraDataFixUtils.chainAllFilters(this.fixChoice("minecraft:arrow"), this.fixChoice("minecraft:spectral_arrow")));
    }

    private Function<Typed<?>, Typed<?>> fixChoice(String entityName) {
        Type inputEntityChoiceType = this.getInputSchema().getChoiceType(References.ENTITY, entityName);
        Type outputEntityChoiceType = this.getOutputSchema().getChoiceType(References.ENTITY, entityName);
        return ProjectileStoredWeaponFix.fixChoiceCap(entityName, inputEntityChoiceType, outputEntityChoiceType);
    }

    private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(String entityName, Type<?> inputEntityChoiceType, Type<T> outputEntityChoiceType) {
        OpticFinder entityF = DSL.namedChoice((String)entityName, inputEntityChoiceType);
        return input -> input.updateTyped(entityF, outputEntityChoiceType, typed -> Util.writeAndReadTypedOrThrow(typed, outputEntityChoiceType, UnaryOperator.identity()));
    }
}

