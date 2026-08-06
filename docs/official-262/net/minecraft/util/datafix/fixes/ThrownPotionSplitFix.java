/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.EntityRenameFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ThrownPotionSplitFix
extends EntityRenameFix {
    private final Supplier<ItemIdFinder> itemIdFinder = Suppliers.memoize(() -> {
        Type potionType = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:potion");
        Type<?> patchedPotionType = ExtraDataFixUtils.patchSubType(potionType, this.getInputSchema().getType(References.ENTITY), this.getOutputSchema().getType(References.ENTITY));
        OpticFinder itemFinder = patchedPotionType.findField("Item");
        OpticFinder itemIdFinder = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        return new ItemIdFinder(itemFinder, (OpticFinder<Pair<String, String>>)itemIdFinder);
    });

    public ThrownPotionSplitFix(Schema outputSchema) {
        super("ThrownPotionSplitFix", outputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String name, Typed<?> entity) {
        if (!name.equals("minecraft:potion")) {
            return Pair.of((Object)name, entity);
        }
        String itemId = this.itemIdFinder.get().getItemId(entity);
        if ("minecraft:lingering_potion".equals(itemId)) {
            return Pair.of((Object)"minecraft:lingering_potion", entity);
        }
        return Pair.of((Object)"minecraft:splash_potion", entity);
    }

    private record ItemIdFinder(OpticFinder<?> itemFinder, OpticFinder<Pair<String, String>> itemIdFinder) {
        public String getItemId(Typed<?> entity) {
            return entity.getOptionalTyped(this.itemFinder).flatMap(item -> item.getOptional(this.itemIdFinder)).map(Pair::getSecond).map(NamespacedSchema::ensureNamespaced).orElse("");
        }
    }
}

