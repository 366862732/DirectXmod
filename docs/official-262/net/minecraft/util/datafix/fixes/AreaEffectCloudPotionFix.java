/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class AreaEffectCloudPotionFix
extends NamedEntityFix {
    public AreaEffectCloudPotionFix(Schema outputSchema) {
        super(outputSchema, false, "AreaEffectCloudPotionFix", References.ENTITY, "minecraft:area_effect_cloud");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fix);
    }

    private <T> Dynamic<T> fix(Dynamic<T> entity) {
        Optional color = entity.get("Color").result();
        Optional effects = entity.get("effects").result();
        Optional potion = entity.get("Potion").result();
        entity = entity.remove("Color").remove("effects").remove("Potion");
        if (color.isEmpty() && effects.isEmpty() && potion.isEmpty()) {
            return entity;
        }
        Dynamic potionContents = entity.emptyMap();
        if (color.isPresent()) {
            potionContents = potionContents.set("custom_color", (Dynamic)color.get());
        }
        if (effects.isPresent()) {
            potionContents = potionContents.set("custom_effects", (Dynamic)effects.get());
        }
        if (potion.isPresent()) {
            potionContents = potionContents.set("potion", (Dynamic)potion.get());
        }
        return entity.set("potion_contents", potionContents);
    }
}

