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
import net.minecraft.util.datafix.fixes.References;

public class ChestedHorsesInventoryZeroIndexingFix
extends DataFix {
    public ChestedHorsesInventoryZeroIndexingFix(Schema v3807) {
        super(v3807, false);
    }

    protected TypeRewriteRule makeRule() {
        OpticFinder itemStackFinder = DSL.typeFinder((Type)this.getInputSchema().getType(References.ITEM_STACK));
        Type entityType = this.getInputSchema().getType(References.ENTITY);
        return TypeRewriteRule.seq((TypeRewriteRule)this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:llama"), (TypeRewriteRule[])new TypeRewriteRule[]{this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:trader_llama"), this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:mule"), this.horseLikeInventoryIndexingFixer(itemStackFinder, entityType, "minecraft:donkey")});
    }

    private TypeRewriteRule horseLikeInventoryIndexingFixer(OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> itemStackFinder, Type<?> schema, String horseId) {
        Type choiceType = this.getInputSchema().getChoiceType(References.ENTITY, horseId);
        OpticFinder entityFinder = DSL.namedChoice((String)horseId, (Type)choiceType);
        OpticFinder itemsFieldFinder = choiceType.findField("Items");
        return this.fixTypeEverywhereTyped("Fix non-zero indexing in chest horse type " + horseId, schema, input -> input.updateTyped(entityFinder, horseLike -> horseLike.updateTyped(itemsFieldFinder, items -> items.update(itemStackFinder, namedStack -> namedStack.mapSecond(itemStack -> itemStack.mapSecond(pair -> pair.mapSecond(remainder -> remainder.update("Slot", slot -> slot.createByte((byte)(slot.asInt(2) - 2))))))))));
    }
}

