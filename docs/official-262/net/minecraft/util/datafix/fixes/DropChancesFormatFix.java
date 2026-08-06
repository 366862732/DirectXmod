/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;
import net.minecraft.util.datafix.fixes.References;

public class DropChancesFormatFix
extends DataFix {
    private static final List<String> ARMOR_SLOT_NAMES = List.of("feet", "legs", "chest", "head");
    private static final List<String> HAND_SLOT_NAMES = List.of("mainhand", "offhand");
    private static final float DEFAULT_CHANCE = 0.085f;

    public DropChancesFormatFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("DropChancesFormatFix", this.getInputSchema().getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), remainder -> {
            List<Float> armorDropChances = DropChancesFormatFix.parseDropChances(remainder.get("ArmorDropChances"));
            List<Float> handDropChances = DropChancesFormatFix.parseDropChances(remainder.get("HandDropChances"));
            float bodyArmorDropChance = remainder.get("body_armor_drop_chance").asNumber().result().map(Number::floatValue).orElse(Float.valueOf(0.085f)).floatValue();
            remainder = remainder.remove("ArmorDropChances").remove("HandDropChances").remove("body_armor_drop_chance");
            Dynamic slotChances = remainder.emptyMap();
            slotChances = DropChancesFormatFix.addSlotChances(slotChances, armorDropChances, ARMOR_SLOT_NAMES);
            slotChances = DropChancesFormatFix.addSlotChances(slotChances, handDropChances, HAND_SLOT_NAMES);
            if (bodyArmorDropChance != 0.085f) {
                slotChances = slotChances.set("body", remainder.createFloat(bodyArmorDropChance));
            }
            if (!slotChances.equals((Object)remainder.emptyMap())) {
                return remainder.set("drop_chances", slotChances);
            }
            return remainder;
        }));
    }

    private static Dynamic<?> addSlotChances(Dynamic<?> output, List<Float> chances, List<String> slotNames) {
        for (int i = 0; i < slotNames.size() && i < chances.size(); ++i) {
            String slot = slotNames.get(i);
            float chance = chances.get(i).floatValue();
            if (chance == 0.085f) continue;
            output = output.set(slot, output.createFloat(chance));
        }
        return output;
    }

    private static List<Float> parseDropChances(OptionalDynamic<?> value) {
        return value.asStream().map(dynamic -> Float.valueOf(dynamic.asFloat(0.085f))).toList();
    }
}

