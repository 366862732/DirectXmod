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

public class V3938
extends NamespacedSchema {
    public V3938(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static TypeTemplate abstractArrow(Schema schema) {
        return DSL.optionalFields((String)"inBlockState", (TypeTemplate)References.BLOCK_STATE.in(schema), (String)"item", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"weapon", (TypeTemplate)References.ITEM_STACK.in(schema));
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map map = super.registerEntities(schema);
        schema.register(map, "minecraft:spectral_arrow", () -> V3938.abstractArrow(schema));
        schema.register(map, "minecraft:arrow", () -> V3938.abstractArrow(schema));
        return map;
    }
}

