/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityHorseSaddleFix
extends NamedEntityFix {
    public EntityHorseSaddleFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityHorseSaddleFix", References.ENTITY, "EntityHorse");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        Type itemStackType = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
        OpticFinder saddleF = DSL.fieldFinder((String)"SaddleItem", (Type)itemStackType);
        Optional saddle = entity.getOptionalTyped(saddleF);
        Dynamic tag = (Dynamic)entity.get(DSL.remainderFinder());
        if (saddle.isEmpty() && tag.get("Saddle").asBoolean(false)) {
            Typed newSaddle = (Typed)itemStackType.pointTyped(entity.getOps()).orElseThrow(IllegalStateException::new);
            newSaddle = newSaddle.set(idF, (Object)Pair.of((Object)References.ITEM_NAME.typeName(), (Object)"minecraft:saddle"));
            Dynamic saddleTag = tag.emptyMap();
            saddleTag = saddleTag.set("Count", saddleTag.createByte((byte)1));
            saddleTag = saddleTag.set("Damage", saddleTag.createShort((short)0));
            newSaddle = newSaddle.set(DSL.remainderFinder(), (Object)saddleTag);
            tag.remove("Saddle");
            return entity.set(saddleF, newSaddle).set(DSL.remainderFinder(), (Object)tag);
        }
        return entity;
    }
}

