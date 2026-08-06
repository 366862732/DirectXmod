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
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EffectDurationFix
extends DataFix {
    private static final Set<String> POTION_ITEMS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

    public EffectDurationFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema inputSchema = this.getInputSchema();
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idFinder = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder tagFinder = itemStackType.findField("tag");
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped("EffectDurationEntity", inputSchema.getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), this::updateEntity)), (TypeRewriteRule[])new TypeRewriteRule[]{this.fixTypeEverywhereTyped("EffectDurationPlayer", inputSchema.getType(References.PLAYER), input -> input.update(DSL.remainderFinder(), this::updateEntity)), this.fixTypeEverywhereTyped("EffectDurationItem", itemStackType, input -> {
            Optional tag;
            if (input.getOptional(idFinder).filter(typeAndIdPair -> POTION_ITEMS.contains(typeAndIdPair.getSecond())).isPresent() && (tag = input.getOptionalTyped(tagFinder)).isPresent()) {
                Dynamic tagRest = (Dynamic)((Typed)tag.get()).get(DSL.remainderFinder());
                Typed newTag = ((Typed)tag.get()).set(DSL.remainderFinder(), (Object)tagRest.update("CustomPotionEffects", this::fix));
                return input.set(tagFinder, newTag);
            }
            return input;
        })});
    }

    private Dynamic<?> fixEffect(Dynamic<?> effect) {
        return effect.update("FactorCalculationData", factorData -> {
            int timestamp = factorData.get("effect_changed_timestamp").asInt(-1);
            factorData = factorData.remove("effect_changed_timestamp");
            int duration = effect.get("Duration").asInt(-1);
            int ticksActive = timestamp - duration;
            return factorData.set("ticks_active", factorData.createInt(ticksActive));
        });
    }

    private Dynamic<?> fix(Dynamic<?> input) {
        return input.createList(input.asStream().map(this::fixEffect));
    }

    private Dynamic<?> updateEntity(Dynamic<?> data) {
        data = data.update("Effects", this::fix);
        data = data.update("ActiveEffects", this::fix);
        data = data.update("CustomPotionEffects", this::fix);
        return data;
    }
}

