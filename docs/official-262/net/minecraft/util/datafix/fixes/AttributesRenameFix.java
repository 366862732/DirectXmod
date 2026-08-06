/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class AttributesRenameFix
extends DataFix {
    private final String name;
    private final UnaryOperator<String> renames;
    private final boolean oldDataComponentFormat;

    public AttributesRenameFix(Schema outputSchema, String name, UnaryOperator<String> renames) {
        this(outputSchema, name, renames, false);
    }

    public AttributesRenameFix(Schema outputSchema, String name, UnaryOperator<String> renames, boolean oldDataComponentFormat) {
        super(outputSchema, false);
        this.name = name;
        this.renames = renames;
        this.oldDataComponentFormat = oldDataComponentFormat;
    }

    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped(this.name + " (Components)", this.getInputSchema().getType(References.DATA_COMPONENTS), this.oldDataComponentFormat ? this::fixDataComponentsOld : this::fixDataComponents), (TypeRewriteRule[])new TypeRewriteRule[]{this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity), this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)});
    }

    private Typed<?> fixDataComponents(Typed<?> components) {
        return components.update(DSL.remainderFinder(), componentData -> componentData.update("minecraft:attribute_modifiers", attributeModifiers -> (Dynamic)DataFixUtils.orElse(attributeModifiers.asStreamOpt().result().map(modifierStream -> modifierStream.map(this::fixTypeField)).map(arg_0 -> ((Dynamic)attributeModifiers).createList(arg_0)), (Object)attributeModifiers)));
    }

    private Typed<?> fixDataComponentsOld(Typed<?> components) {
        return components.update(DSL.remainderFinder(), componentData -> componentData.update("minecraft:attribute_modifiers", attributeModifiers -> attributeModifiers.update("modifiers", modifiers -> (Dynamic)DataFixUtils.orElse(modifiers.asStreamOpt().result().map(modifierStream -> modifierStream.map(this::fixTypeField)).map(arg_0 -> ((Dynamic)modifiers).createList(arg_0)), (Object)modifiers))));
    }

    private Typed<?> fixEntity(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), tag -> tag.update("attributes", attributeList -> (Dynamic)DataFixUtils.orElse(attributeList.asStreamOpt().result().map(s -> s.map(this::fixIdField)).map(arg_0 -> ((Dynamic)attributeList).createList(arg_0)), (Object)attributeList)));
    }

    private Dynamic<?> fixIdField(Dynamic<?> dynamic) {
        return ExtraDataFixUtils.fixStringField(dynamic, "id", this.renames);
    }

    private Dynamic<?> fixTypeField(Dynamic<?> dynamic) {
        return ExtraDataFixUtils.fixStringField(dynamic, "type", this.renames);
    }
}

