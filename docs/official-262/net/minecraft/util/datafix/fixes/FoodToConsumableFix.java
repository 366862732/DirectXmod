/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;

public class FoodToConsumableFix
extends DataFix {
    public FoodToConsumableFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead("Food to consumable fix", this.getInputSchema().getType(References.DATA_COMPONENTS), this.getOutputSchema().getType(References.DATA_COMPONENTS), components -> {
            Optional foodComponent = components.get("minecraft:food").result();
            if (foodComponent.isPresent()) {
                float eatSeconds = ((Dynamic)foodComponent.get()).get("eat_seconds").asFloat(1.6f);
                Stream effects = ((Dynamic)foodComponent.get()).get("effects").asStream();
                Stream<Dynamic> onConsumeEffects = effects.map(effect -> effect.emptyMap().set("type", effect.createString("minecraft:apply_effects")).set("effects", effect.createList(effect.get("effect").result().stream())).set("probability", effect.createFloat(effect.get("probability").asFloat(1.0f))));
                components = Dynamic.copyField((Dynamic)((Dynamic)foodComponent.get()), (String)"using_converts_to", (Dynamic)components, (String)"minecraft:use_remainder");
                components = components.set("minecraft:food", ((Dynamic)foodComponent.get()).remove("eat_seconds").remove("effects").remove("using_converts_to"));
                components = components.set("minecraft:consumable", components.emptyMap().set("consume_seconds", components.createFloat(eatSeconds)).set("on_consume_effects", components.createList(onConsumeEffects)));
                return components;
            }
            return components;
        });
    }
}

