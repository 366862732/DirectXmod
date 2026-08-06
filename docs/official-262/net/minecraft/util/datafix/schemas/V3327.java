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

public class V3327
extends NamespacedSchema {
    public V3327(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map map = super.registerBlockEntities(schema);
        schema.register(map, "minecraft:decorated_pot", () -> DSL.optionalFields((String)"shards", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_NAME.in(schema)), (String)"item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register(map, "minecraft:suspicious_sand", () -> DSL.optionalFields((String)"item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        return map;
    }
}

