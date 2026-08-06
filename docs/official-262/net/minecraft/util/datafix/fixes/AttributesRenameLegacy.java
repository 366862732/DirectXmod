/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
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
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.fixes.References;

public class AttributesRenameLegacy
extends DataFix {
    private final String name;
    private final UnaryOperator<String> renames;

    public AttributesRenameLegacy(Schema outputSchema, String name, UnaryOperator<String> renames) {
        super(outputSchema, false);
        this.name = name;
        this.renames = renames;
    }

    protected TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder tagF = itemStackType.findField("tag");
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped(this.name + " (ItemStack)", itemStackType, itemStack -> itemStack.updateTyped(tagF, this::fixItemStackTag)), (TypeRewriteRule[])new TypeRewriteRule[]{this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity), this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)});
    }

    private Dynamic<?> fixName(Dynamic<?> name) {
        return (Dynamic)DataFixUtils.orElse(name.asString().result().map(this.renames).map(arg_0 -> name.createString(arg_0)), name);
    }

    private Typed<?> fixItemStackTag(Typed<?> itemStack) {
        return itemStack.update(DSL.remainderFinder(), tag -> tag.update("AttributeModifiers", modifiers -> (Dynamic)DataFixUtils.orElse(modifiers.asStreamOpt().result().map(s -> s.map(modifier -> modifier.update("AttributeName", this::fixName))).map(arg_0 -> ((Dynamic)modifiers).createList(arg_0)), (Object)modifiers)));
    }

    private Typed<?> fixEntity(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), tag -> tag.update("Attributes", attributeList -> (Dynamic)DataFixUtils.orElse(attributeList.asStreamOpt().result().map(s -> s.map(attribute -> attribute.update("Name", this::fixName))).map(arg_0 -> ((Dynamic)attributeList).createList(arg_0)), (Object)attributeList)));
    }
}

