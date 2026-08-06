/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TaggedChoice$TaggedChoiceType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class SaddleEquipmentSlotFix
extends DataFix {
    private static final Set<String> ENTITIES_WITH_SADDLE_ITEM = Set.of("minecraft:horse", "minecraft:skeleton_horse", "minecraft:zombie_horse", "minecraft:donkey", "minecraft:mule", "minecraft:camel", "minecraft:llama", "minecraft:trader_llama");
    private static final Set<String> ENTITIES_WITH_SADDLE_FLAG = Set.of("minecraft:pig", "minecraft:strider");
    private static final String SADDLE_FLAG = "Saddle";
    private static final String NEW_SADDLE = "saddle";

    public SaddleEquipmentSlotFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        TaggedChoice.TaggedChoiceType entityIdType = this.getInputSchema().findChoiceType(References.ENTITY);
        OpticFinder entityIdF = DSL.typeFinder((Type)entityIdType);
        Type inputType = this.getInputSchema().getType(References.ENTITY);
        Type outputType = this.getOutputSchema().getType(References.ENTITY);
        Type<?> patchedInputType = ExtraDataFixUtils.patchSubType(inputType, inputType, outputType);
        return this.fixTypeEverywhereTyped("SaddleEquipmentSlotFix", inputType, outputType, input -> {
            String entityId = input.getOptional(entityIdF).map(Pair::getFirst).map(NamespacedSchema::ensureNamespaced).orElse("");
            Typed fixedInput = ExtraDataFixUtils.cast(patchedInputType, input);
            if (ENTITIES_WITH_SADDLE_ITEM.contains(entityId)) {
                return Util.writeAndReadTypedOrThrow(fixedInput, outputType, SaddleEquipmentSlotFix::fixEntityWithSaddleItem);
            }
            if (ENTITIES_WITH_SADDLE_FLAG.contains(entityId)) {
                return Util.writeAndReadTypedOrThrow(fixedInput, outputType, SaddleEquipmentSlotFix::fixEntityWithSaddleFlag);
            }
            return ExtraDataFixUtils.cast(outputType, input);
        });
    }

    private static Dynamic<?> fixEntityWithSaddleItem(Dynamic<?> input) {
        if (input.get("SaddleItem").result().isEmpty()) {
            return input;
        }
        return SaddleEquipmentSlotFix.fixDropChances(input.renameField("SaddleItem", NEW_SADDLE));
    }

    private static Dynamic<?> fixEntityWithSaddleFlag(Dynamic<?> tag) {
        boolean hasSaddle = tag.get(SADDLE_FLAG).asBoolean(false);
        tag = tag.remove(SADDLE_FLAG);
        if (!hasSaddle) {
            return tag;
        }
        Dynamic saddleItem = tag.emptyMap().set("id", tag.createString("minecraft:saddle")).set("count", tag.createInt(1));
        return SaddleEquipmentSlotFix.fixDropChances(tag.set(NEW_SADDLE, saddleItem));
    }

    private static Dynamic<?> fixDropChances(Dynamic<?> tag) {
        Dynamic dropChances = tag.get("drop_chances").orElseEmptyMap().set(NEW_SADDLE, tag.createFloat(2.0f));
        return tag.set("drop_chances", dropChances);
    }
}

