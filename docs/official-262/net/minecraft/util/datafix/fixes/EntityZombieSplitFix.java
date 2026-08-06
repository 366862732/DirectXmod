/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.EntityRenameFix;
import net.minecraft.util.datafix.fixes.References;

public class EntityZombieSplitFix
extends EntityRenameFix {
    private final Supplier<Type<?>> zombieVillagerType = Suppliers.memoize(() -> this.getOutputSchema().getChoiceType(References.ENTITY, "ZombieVillager"));

    public EntityZombieSplitFix(Schema outputSchema) {
        super("EntityZombieSplitFix", outputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String name, Typed<?> entity) {
        String newName;
        if (!name.equals("Zombie")) {
            return Pair.of((Object)name, entity);
        }
        Dynamic tag = (Dynamic)entity.getOptional(DSL.remainderFinder()).orElseThrow();
        int type = tag.get("ZombieType").asInt(0);
        return Pair.of((Object)newName, (Object)(switch (type) {
            default -> {
                newName = "Zombie";
                yield entity;
            }
            case 1, 2, 3, 4, 5 -> {
                newName = "ZombieVillager";
                yield this.changeSchemaToZombieVillager(entity, type - 1);
            }
            case 6 -> {
                newName = "Husk";
                yield entity;
            }
        }).update(DSL.remainderFinder(), e -> e.remove("ZombieType")));
    }

    private Typed<?> changeSchemaToZombieVillager(Typed<?> entity, int profession) {
        return Util.writeAndReadTypedOrThrow(entity, this.zombieVillagerType.get(), serializedEntity -> serializedEntity.set("Profession", serializedEntity.createInt(profession)));
    }
}

