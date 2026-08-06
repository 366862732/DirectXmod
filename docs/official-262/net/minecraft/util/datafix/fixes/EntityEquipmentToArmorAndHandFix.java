/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;

public class EntityEquipmentToArmorAndHandFix
extends DataFix {
    public EntityEquipmentToArmorAndHandFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    public TypeRewriteRule makeRule() {
        return this.cap(this.getInputSchema().getTypeRaw(References.ITEM_STACK), this.getOutputSchema().getTypeRaw(References.ITEM_STACK));
    }

    private <ItemStackOld, ItemStackNew> TypeRewriteRule cap(Type<ItemStackOld> oldItemStackType, Type<ItemStackNew> newItemStackType) {
        Type oldEquipmentType = DSL.named((String)References.ENTITY_EQUIPMENT.typeName(), (Type)DSL.optional((Type)DSL.field((String)"Equipment", (Type)DSL.list(oldItemStackType))));
        Type newEquipmentType = DSL.named((String)References.ENTITY_EQUIPMENT.typeName(), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"ArmorItems", (Type)DSL.list(newItemStackType))), (Type)DSL.optional((Type)DSL.field((String)"HandItems", (Type)DSL.list(newItemStackType))), (Type)DSL.optional((Type)DSL.field((String)"body_armor_item", newItemStackType)), (Type)DSL.optional((Type)DSL.field((String)"saddle", newItemStackType))));
        if (!oldEquipmentType.equals((Object)this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Input entity_equipment type does not match expected");
        }
        if (!newEquipmentType.equals((Object)this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Output entity_equipment type does not match expected");
        }
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped("EntityEquipmentToArmorAndHandFix - drop chances", this.getInputSchema().getType(References.ENTITY), typed -> typed.update(DSL.remainderFinder(), EntityEquipmentToArmorAndHandFix::fixDropChances)), (TypeRewriteRule)this.fixTypeEverywhere("EntityEquipmentToArmorAndHandFix - equipment", oldEquipmentType, newEquipmentType, ops -> {
            Object emptyStack = ((Pair)newItemStackType.read(new Dynamic(ops).emptyMap()).result().orElseThrow(() -> new IllegalStateException("Could not parse newly created empty itemstack."))).getFirst();
            Either noItem = Either.right((Object)DSL.unit());
            return named -> named.mapSecond(equipmentField -> {
                List items = (List)equipmentField.map(Function.identity(), ignored -> List.of());
                Either handItems = Either.right((Object)DSL.unit());
                Either armorItems = Either.right((Object)DSL.unit());
                if (!items.isEmpty()) {
                    handItems = Either.left((Object)Lists.newArrayList((Object[])new Object[]{items.getFirst(), emptyStack}));
                }
                if (items.size() > 1) {
                    ArrayList armor = Lists.newArrayList((Object[])new Object[]{emptyStack, emptyStack, emptyStack, emptyStack});
                    for (int i = 1; i < Math.min(items.size(), 5); ++i) {
                        armor.set(i - 1, items.get(i));
                    }
                    armorItems = Either.left((Object)armor);
                }
                return Pair.of((Object)armorItems, (Object)Pair.of((Object)handItems, (Object)Pair.of((Object)noItem, (Object)noItem)));
            });
        }));
    }

    private static Dynamic<?> fixDropChances(Dynamic<?> tag) {
        Optional dropChances = tag.get("DropChances").asStreamOpt().result();
        tag = tag.remove("DropChances");
        if (dropChances.isPresent()) {
            Iterator chances = Stream.concat(((Stream)dropChances.get()).map(value -> Float.valueOf(value.asFloat(0.0f))), Stream.generate(() -> Float.valueOf(0.0f))).iterator();
            float handChance = ((Float)chances.next()).floatValue();
            if (tag.get("HandDropChances").result().isEmpty()) {
                tag = tag.set("HandDropChances", tag.createList(Stream.of(Float.valueOf(handChance), Float.valueOf(0.0f)).map(arg_0 -> ((Dynamic)tag).createFloat(arg_0))));
            }
            if (tag.get("ArmorDropChances").result().isEmpty()) {
                tag = tag.set("ArmorDropChances", tag.createList(Stream.of((Float)chances.next(), (Float)chances.next(), (Float)chances.next(), (Float)chances.next()).map(arg_0 -> ((Dynamic)tag).createFloat(arg_0))));
            }
        }
        return tag;
    }
}

