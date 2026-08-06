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
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class ItemCustomNameToComponentFix
extends DataFix {
    public ItemCustomNameToComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        Type textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
        OpticFinder tagFinder = itemStackType.findField("tag");
        OpticFinder displayFinder = tagFinder.type().findField("display");
        OpticFinder customNameFinder = displayFinder.type().findField("Name");
        OpticFinder textComponentFinder = DSL.typeFinder((Type)textComponentType);
        return this.fixTypeEverywhereTyped("ItemCustomNameToComponentFix", itemStackType, itemStack -> itemStack.updateTyped(tagFinder, tag -> tag.updateTyped(displayFinder, display -> display.updateTyped(customNameFinder, customName -> customName.update(textComponentFinder, textComponent -> textComponent.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))))));
    }
}

