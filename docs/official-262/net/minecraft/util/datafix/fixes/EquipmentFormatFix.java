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
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.datafix.fixes.References;

public class EquipmentFormatFix
extends DataFix {
    public EquipmentFormatFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldItemStackType = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
        Type newItemStackType = this.getOutputSchema().getTypeRaw(References.ITEM_STACK);
        OpticFinder idFinder = oldItemStackType.findField("id");
        return this.fix(oldItemStackType, newItemStackType, idFinder);
    }

    private <ItemStackOld, ItemStackNew> TypeRewriteRule fix(Type<ItemStackOld> oldItemStackType, Type<ItemStackNew> newItemStackType, OpticFinder<?> idFinder) {
        Type oldEquipmentType = DSL.named((String)References.ENTITY_EQUIPMENT.typeName(), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"ArmorItems", (Type)DSL.list(oldItemStackType))), (Type)DSL.optional((Type)DSL.field((String)"HandItems", (Type)DSL.list(oldItemStackType))), (Type)DSL.optional((Type)DSL.field((String)"body_armor_item", oldItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"saddle", oldItemStackType))));
        Type newEquipmentType = DSL.named((String)References.ENTITY_EQUIPMENT.typeName(), (Type)DSL.optional((Type)DSL.field((String)"equipment", (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"mainhand", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"offhand", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"feet", newItemStackType)), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"legs", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"chest", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"head", newItemStackType)), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"body", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"saddle", newItemStackType)), (Type)DSL.remainderType()))))));
        if (!oldEquipmentType.equals((Object)this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Input entity_equipment type does not match expected");
        }
        if (!newEquipmentType.equals((Object)this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Output entity_equipment type does not match expected");
        }
        return this.fixTypeEverywhere("EquipmentFormatFix", oldEquipmentType, newEquipmentType, ops -> {
            Predicate<Object> isPlaceholder = itemStack -> {
                Typed typed = new Typed(oldItemStackType, ops, itemStack);
                return typed.getOptional(idFinder).isEmpty();
            };
            return namedOldEquipment -> {
                String typeName = (String)namedOldEquipment.getFirst();
                Pair oldEquipment = (Pair)namedOldEquipment.getSecond();
                List armorItems = (List)((Either)oldEquipment.getFirst()).map(Function.identity(), ignored -> List.of());
                List handItems = (List)((Either)((Pair)oldEquipment.getSecond()).getFirst()).map(Function.identity(), ignored -> List.of());
                Either body = (Either)((Pair)((Pair)oldEquipment.getSecond()).getSecond()).getFirst();
                Either saddle = (Either)((Pair)((Pair)oldEquipment.getSecond()).getSecond()).getSecond();
                Either feet = EquipmentFormatFix.getItemFromList(0, armorItems, isPlaceholder);
                Either legs = EquipmentFormatFix.getItemFromList(1, armorItems, isPlaceholder);
                Either chest = EquipmentFormatFix.getItemFromList(2, armorItems, isPlaceholder);
                Either head = EquipmentFormatFix.getItemFromList(3, armorItems, isPlaceholder);
                Either mainhand = EquipmentFormatFix.getItemFromList(0, handItems, isPlaceholder);
                Either offhand = EquipmentFormatFix.getItemFromList(1, handItems, isPlaceholder);
                if (EquipmentFormatFix.areAllEmpty(body, saddle, feet, legs, chest, head, mainhand, offhand)) {
                    return Pair.of((Object)typeName, (Object)Either.right((Object)Unit.INSTANCE));
                }
                return Pair.of((Object)typeName, (Object)Either.left((Object)Pair.of(mainhand, (Object)Pair.of(offhand, (Object)Pair.of(feet, (Object)Pair.of(legs, (Object)Pair.of(chest, (Object)Pair.of(head, (Object)Pair.of((Object)body, (Object)Pair.of((Object)saddle, (Object)new Dynamic(ops)))))))))));
            };
        });
    }

    @SafeVarargs
    private static boolean areAllEmpty(Either<?, Unit> ... fields) {
        for (Either<?, Unit> field : fields) {
            if (!field.right().isEmpty()) continue;
            return false;
        }
        return true;
    }

    private static <ItemStack> Either<ItemStack, Unit> getItemFromList(int index, List<ItemStack> items, Predicate<ItemStack> isPlaceholder) {
        if (index >= items.size()) {
            return Either.right((Object)Unit.INSTANCE);
        }
        ItemStack item = items.get(index);
        if (isPlaceholder.test(item)) {
            return Either.right((Object)Unit.INSTANCE);
        }
        return Either.left(item);
    }
}

