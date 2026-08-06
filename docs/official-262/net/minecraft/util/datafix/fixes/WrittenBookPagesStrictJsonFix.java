/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.ItemStackTagFix;
import net.minecraft.util.datafix.fixes.References;

public class WrittenBookPagesStrictJsonFix
extends ItemStackTagFix {
    public WrittenBookPagesStrictJsonFix(Schema outputSchema) {
        super(outputSchema, "WrittenBookPagesStrictJsonFix", id -> id.equals("minecraft:written_book"));
    }

    @Override
    protected Typed<?> fixItemStackTag(Typed<?> tag) {
        Type textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder tagF = itemStackType.findField("tag");
        OpticFinder pagesF = tagF.type().findField("pages");
        OpticFinder pageF = DSL.typeFinder((Type)textComponentType);
        return tag.updateTyped(pagesF, pages -> pages.update(pageF, page -> page.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient)));
    }
}

