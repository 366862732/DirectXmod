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

public class V3325
extends NamespacedSchema {
    public V3325(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map map = super.registerEntities(schema);
        schema.register(map, "minecraft:item_display", name -> DSL.optionalFields((String)"item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register(map, "minecraft:block_display", name -> DSL.optionalFields((String)"block_state", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        schema.register(map, "minecraft:text_display", () -> DSL.optionalFields((String)"text", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        return map;
    }
}

