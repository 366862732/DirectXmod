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

public class V3439
extends NamespacedSchema {
    public V3439(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map map = super.registerBlockEntities(schema);
        this.register(map, "minecraft:sign", () -> V3439.sign(schema));
        return map;
    }

    public static TypeTemplate sign(Schema schema) {
        return DSL.optionalFields((String)"front_text", (TypeTemplate)DSL.optionalFields((String)"messages", (TypeTemplate)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema)), (String)"filtered_messages", (TypeTemplate)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema))), (String)"back_text", (TypeTemplate)DSL.optionalFields((String)"messages", (TypeTemplate)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema)), (String)"filtered_messages", (TypeTemplate)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema))));
    }
}

