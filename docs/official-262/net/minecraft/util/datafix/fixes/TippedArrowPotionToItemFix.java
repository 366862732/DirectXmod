/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.References;

public class TippedArrowPotionToItemFix
extends NamedEntityWriteReadFix {
    public TippedArrowPotionToItemFix(Schema outputSchema) {
        super(outputSchema, false, "TippedArrowPotionToItemFix", References.ENTITY, "minecraft:arrow");
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> input) {
        Optional potion = input.get("Potion").result();
        Optional customPotionEffects = input.get("custom_potion_effects").result();
        Optional color = input.get("Color").result();
        if (potion.isEmpty() && customPotionEffects.isEmpty() && color.isEmpty()) {
            return input;
        }
        return input.remove("Potion").remove("custom_potion_effects").remove("Color").update("item", itemStack -> {
            Dynamic tag = itemStack.get("tag").orElseEmptyMap();
            if (potion.isPresent()) {
                tag = tag.set("Potion", (Dynamic)potion.get());
            }
            if (customPotionEffects.isPresent()) {
                tag = tag.set("custom_potion_effects", (Dynamic)customPotionEffects.get());
            }
            if (color.isPresent()) {
                tag = tag.set("CustomPotionColor", (Dynamic)color.get());
            }
            return itemStack.set("tag", tag);
        });
    }
}

