/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.templates.TypeTemplate
 */
package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class V1458
extends NamespacedSchema {
    public V1458(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(true, References.ENTITY, () -> DSL.and((TypeTemplate)References.ENTITY_EQUIPMENT.in(schema), (TypeTemplate)DSL.optionalFields((String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", V1458.namespacedString(), (Map)entityTypes))));
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map map = super.registerBlockEntities(schema);
        schema.register(map, "minecraft:beacon", () -> V1458.nameable(schema));
        schema.register(map, "minecraft:banner", () -> V1458.nameable(schema));
        schema.register(map, "minecraft:brewing_stand", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:chest", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:trapped_chest", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:dispenser", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:dropper", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:enchanting_table", () -> V1458.nameable(schema));
        schema.register(map, "minecraft:furnace", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:hopper", () -> V1458.nameableInventory(schema));
        schema.register(map, "minecraft:shulker_box", () -> V1458.nameableInventory(schema));
        return map;
    }

    public static TypeTemplate nameableInventory(Schema schema) {
        return DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema));
    }

    public static TypeTemplate nameable(Schema schema) {
        return DSL.optionalFields((String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema));
    }
}

