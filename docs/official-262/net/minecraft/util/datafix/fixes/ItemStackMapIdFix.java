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
 *  com.mojang.datafixers.util.Pair
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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackMapIdFix
extends DataFix {
    public ItemStackMapIdFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder tagF = itemStackType.findField("tag");
        return this.fixTypeEverywhereTyped("ItemInstanceMapIdFix", itemStackType, input -> {
            Optional id = input.getOptional(idF);
            if (id.isPresent() && Objects.equals(((Pair)id.get()).getSecond(), "minecraft:filled_map")) {
                Dynamic rest = (Dynamic)input.get(DSL.remainderFinder());
                Typed tag = input.getOrCreateTyped(tagF);
                Dynamic tagRest = (Dynamic)tag.get(DSL.remainderFinder());
                tagRest = tagRest.set("map", tagRest.createInt(rest.get("Damage").asInt(0)));
                return input.set(tagF, tag.set(DSL.remainderFinder(), (Object)tagRest));
            }
            return input;
        });
    }
}

