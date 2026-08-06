/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.datafixers.util.Unit
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class FurnaceRecipeFix
extends DataFix {
    public FurnaceRecipeFix(Schema schema, boolean changesType) {
        super(schema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        return this.cap(this.getOutputSchema().getTypeRaw(References.RECIPE));
    }

    private <R> TypeRewriteRule cap(Type<R> recipeType) {
        Type replacedType = DSL.and((Type)DSL.optional((Type)DSL.field((String)"RecipesUsed", (Type)DSL.and((Type)DSL.compoundList(recipeType, (Type)DSL.intType()), (Type)DSL.remainderType()))), (Type)DSL.remainderType());
        OpticFinder oldFurnaceFinder = DSL.namedChoice((String)"minecraft:furnace", (Type)this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:furnace"));
        OpticFinder oldBlastFurnaceFinder = DSL.namedChoice((String)"minecraft:blast_furnace", (Type)this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:blast_furnace"));
        OpticFinder oldSmokerFinder = DSL.namedChoice((String)"minecraft:smoker", (Type)this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:smoker"));
        Type newFurnaceType = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:furnace");
        Type newBlastFurnaceFinder = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:blast_furnace");
        Type newSmokerFinder = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:smoker");
        Type oldEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type newEntityType = this.getOutputSchema().getType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhereTyped("FurnaceRecipesFix", oldEntityType, newEntityType, input -> input.updateTyped(oldFurnaceFinder, newFurnaceType, furnace -> this.updateFurnaceContents(recipeType, (Type)replacedType, (Typed<?>)furnace)).updateTyped(oldBlastFurnaceFinder, newBlastFurnaceFinder, blastFurnace -> this.updateFurnaceContents(recipeType, (Type)replacedType, (Typed<?>)blastFurnace)).updateTyped(oldSmokerFinder, newSmokerFinder, smoker -> this.updateFurnaceContents(recipeType, (Type)replacedType, (Typed<?>)smoker)));
    }

    private <R> Typed<?> updateFurnaceContents(Type<R> recipeType, Type<Pair<Either<Pair<List<Pair<R, Integer>>, Dynamic<?>>, Unit>, Dynamic<?>>> replacedType, Typed<?> input) {
        Dynamic tag = (Dynamic)input.getOrCreate(DSL.remainderFinder());
        int recipesUsedSize = tag.get("RecipesUsedSize").asInt(0);
        tag = tag.remove("RecipesUsedSize");
        ArrayList results = Lists.newArrayList();
        for (int i = 0; i < recipesUsedSize; ++i) {
            String locationKey = "RecipeLocation" + i;
            String amountKey = "RecipeAmount" + i;
            Optional maybeLocation = tag.get(locationKey).result();
            int amount = tag.get(amountKey).asInt(0);
            if (amount > 0) {
                maybeLocation.ifPresent(location -> {
                    Optional parseResult = recipeType.read(location).result();
                    parseResult.ifPresent(r -> results.add(Pair.of((Object)r.getFirst(), (Object)amount)));
                });
            }
            tag = tag.remove(locationKey).remove(amountKey);
        }
        return input.set(DSL.remainderFinder(), replacedType, (Object)Pair.of((Object)Either.left((Object)Pair.of((Object)results, (Object)tag.emptyMap())), (Object)tag));
    }
}

