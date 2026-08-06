/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;
import net.minecraft.util.datafix.fixes.References;

public class EntityRedundantChanceTagsFix
extends DataFix {
    private static final Codec<List<Float>> FLOAT_LIST_CODEC = Codec.FLOAT.listOf();

    public EntityRedundantChanceTagsFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("EntityRedundantChanceTagsFix", this.getInputSchema().getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), tag -> {
            if (EntityRedundantChanceTagsFix.isZeroList(tag.get("HandDropChances"), 2)) {
                tag = tag.remove("HandDropChances");
            }
            if (EntityRedundantChanceTagsFix.isZeroList(tag.get("ArmorDropChances"), 4)) {
                tag = tag.remove("ArmorDropChances");
            }
            return tag;
        }));
    }

    private static boolean isZeroList(OptionalDynamic<?> element, int size) {
        return element.flatMap(arg_0 -> FLOAT_LIST_CODEC.parse(arg_0)).map(floats -> floats.size() == size && floats.stream().allMatch(f -> f.floatValue() == 0.0f)).result().orElse(false);
    }
}

