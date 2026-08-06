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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class V3818_3
extends NamespacedSchema {
    public V3818_3(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema schema) {
        LinkedHashMap<String, Supplier<TypeTemplate>> components = new LinkedHashMap<String, Supplier<TypeTemplate>>();
        components.put("minecraft:bees", () -> DSL.list((TypeTemplate)DSL.optionalFields((String)"entity_data", (TypeTemplate)References.ENTITY_TREE.in(schema))));
        components.put("minecraft:block_entity_data", () -> References.BLOCK_ENTITY.in(schema));
        components.put("minecraft:bundle_contents", () -> DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)));
        components.put("minecraft:can_break", () -> DSL.optionalFields((String)"predicates", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"blocks", (TypeTemplate)DSL.or((TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_NAME.in(schema)))))));
        components.put("minecraft:can_place_on", () -> DSL.optionalFields((String)"predicates", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"blocks", (TypeTemplate)DSL.or((TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_NAME.in(schema)))))));
        components.put("minecraft:charged_projectiles", () -> DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)));
        components.put("minecraft:container", () -> DSL.list((TypeTemplate)DSL.optionalFields((String)"item", (TypeTemplate)References.ITEM_STACK.in(schema))));
        components.put("minecraft:entity_data", () -> References.ENTITY_TREE.in(schema));
        components.put("minecraft:pot_decorations", () -> DSL.list((TypeTemplate)References.ITEM_NAME.in(schema)));
        components.put("minecraft:food", () -> DSL.optionalFields((String)"using_converts_to", (TypeTemplate)References.ITEM_STACK.in(schema)));
        components.put("minecraft:custom_name", () -> References.TEXT_COMPONENT.in(schema));
        components.put("minecraft:item_name", () -> References.TEXT_COMPONENT.in(schema));
        components.put("minecraft:lore", () -> DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        components.put("minecraft:written_book_content", () -> DSL.optionalFields((String)"pages", (TypeTemplate)DSL.list((TypeTemplate)DSL.or((TypeTemplate)DSL.optionalFields((String)"raw", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"filtered", (TypeTemplate)References.TEXT_COMPONENT.in(schema)), (TypeTemplate)References.TEXT_COMPONENT.in(schema)))));
        return components;
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(V3818_3.components(schema)));
    }
}

