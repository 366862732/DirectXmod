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
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.datafixers.util.Unit
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class EmptyItemInHotbarFix
extends DataFix {
    public EmptyItemInHotbarFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        OpticFinder itemStackF = DSL.typeFinder((Type)this.getInputSchema().getType(References.ITEM_STACK));
        return this.fixTypeEverywhereTyped("EmptyItemInHotbarFix", this.getInputSchema().getType(References.HOTBAR), input -> input.update(itemStackF, namedStack -> namedStack.mapSecond(itemStack -> {
            boolean isEmpty;
            Optional<String> id = ((Either)itemStack.getFirst()).left().map(Pair::getSecond);
            Dynamic remainder = (Dynamic)((Pair)itemStack.getSecond()).getSecond();
            boolean isAir = id.isEmpty() || id.get().equals("minecraft:air");
            boolean bl = isEmpty = remainder.get("Count").asInt(0) <= 0;
            if (isAir || isEmpty) {
                return Pair.of((Object)Either.right((Object)Unit.INSTANCE), (Object)Pair.of((Object)Either.right((Object)Unit.INSTANCE), (Object)remainder.emptyMap()));
            }
            return itemStack;
        })));
    }
}

