/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.EntityRenameFix;
import net.minecraft.util.datafix.fixes.References;

public class EntityMinecartIdentifiersFix
extends EntityRenameFix {
    public EntityMinecartIdentifiersFix(Schema outputSchema) {
        super("EntityMinecartIdentifiersFix", outputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String name, Typed<?> entity) {
        if (!name.equals("Minecart")) {
            return Pair.of((Object)name, entity);
        }
        int id = ((Dynamic)entity.getOrCreate(DSL.remainderFinder())).get("Type").asInt(0);
        String newName = switch (id) {
            default -> "MinecartRideable";
            case 1 -> "MinecartChest";
            case 2 -> "MinecartFurnace";
        };
        Type newType = (Type)this.getOutputSchema().findChoiceType(References.ENTITY).types().get(newName);
        return Pair.of((Object)newName, Util.writeAndReadTypedOrThrow(entity, newType, dynamic -> dynamic.remove("Type")));
    }
}

