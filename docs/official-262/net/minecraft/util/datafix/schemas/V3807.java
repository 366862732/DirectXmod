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

public class V3807
extends NamespacedSchema {
    public V3807(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map map = super.registerBlockEntities(schema);
        schema.register(map, "minecraft:vault", () -> DSL.optionalFields((String)"config", (TypeTemplate)DSL.optionalFields((String)"key_item", (TypeTemplate)References.ITEM_STACK.in(schema)), (String)"server_data", (TypeTemplate)DSL.optionalFields((String)"items_to_eject", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))), (String)"shared_data", (TypeTemplate)DSL.optionalFields((String)"display_item", (TypeTemplate)References.ITEM_STACK.in(schema))));
        return map;
    }
}

