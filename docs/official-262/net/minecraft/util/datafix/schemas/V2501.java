/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TypeTemplate
 */
package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class V2501
extends NamespacedSchema {
    public V2501(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    private static void registerFurnace(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"RecipesUsed", (TypeTemplate)DSL.compoundList((TypeTemplate)References.RECIPE.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))));
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map map = super.registerBlockEntities(schema);
        V2501.registerFurnace(schema, map, "minecraft:furnace");
        V2501.registerFurnace(schema, map, "minecraft:smoker");
        V2501.registerFurnace(schema, map, "minecraft:blast_furnace");
        return map;
    }
}

