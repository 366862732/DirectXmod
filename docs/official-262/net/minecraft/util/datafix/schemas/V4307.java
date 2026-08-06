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
import net.minecraft.util.datafix.schemas.V4059;

public class V4307
extends NamespacedSchema {
    public V4307(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema schema) {
        SequencedMap<String, Supplier<TypeTemplate>> components = V4059.components(schema);
        components.put("minecraft:can_place_on", () -> V4307.adventureModePredicate(schema));
        components.put("minecraft:can_break", () -> V4307.adventureModePredicate(schema));
        return components;
    }

    private static TypeTemplate adventureModePredicate(Schema schema) {
        TypeTemplate predicate = DSL.optionalFields((String)"blocks", (TypeTemplate)DSL.or((TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_NAME.in(schema))));
        return DSL.or((TypeTemplate)predicate, (TypeTemplate)DSL.list((TypeTemplate)predicate));
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(V4307.components(schema)));
    }
}

