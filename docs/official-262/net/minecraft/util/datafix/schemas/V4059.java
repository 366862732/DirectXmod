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
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V3818_3;

public class V4059
extends NamespacedSchema {
    public V4059(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema schema) {
        SequencedMap<String, Supplier<TypeTemplate>> components = V3818_3.components(schema);
        components.remove("minecraft:food");
        components.put("minecraft:use_remainder", () -> References.ITEM_STACK.in(schema));
        components.put("minecraft:equippable", () -> DSL.optionalFields((String)"allowed_entities", (TypeTemplate)DSL.or((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_NAME.in(schema)))));
        components.put("minecraft:sulfur_cube_content", () -> References.ITEM_STACK.in(schema));
        return components;
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(V4059.components(schema)));
    }
}

