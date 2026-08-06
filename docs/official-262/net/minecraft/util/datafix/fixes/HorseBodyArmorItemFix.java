/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Streams
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.References;

public class HorseBodyArmorItemFix
extends NamedEntityWriteReadFix {
    private final String previousBodyArmorTag;
    private final boolean clearArmorItems;

    public HorseBodyArmorItemFix(Schema outputSchema, String entityName, String previousBodyArmorTag, boolean clearArmorItems) {
        super(outputSchema, true, "Horse armor fix for " + entityName, References.ENTITY, entityName);
        this.previousBodyArmorTag = previousBodyArmorTag;
        this.clearArmorItems = clearArmorItems;
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> input) {
        Optional previousBodyArmor = input.get(this.previousBodyArmorTag).result();
        if (previousBodyArmor.isPresent()) {
            Dynamic bodyArmorItem = (Dynamic)previousBodyArmor.get();
            Dynamic output = input.remove(this.previousBodyArmorTag);
            if (this.clearArmorItems) {
                output = output.update("ArmorItems", armorItems -> armorItems.createList(Streams.mapWithIndex((Stream)armorItems.asStream(), (entry, index) -> index == 2L ? entry.emptyMap() : entry)));
                output = output.update("ArmorDropChances", armorDropChances -> armorDropChances.createList(Streams.mapWithIndex((Stream)armorDropChances.asStream(), (entry, index) -> index == 2L ? entry.createFloat(0.085f) : entry)));
            }
            output = output.set("body_armor_item", bodyArmorItem);
            output = output.set("body_armor_drop_chance", input.createFloat(2.0f));
            return output;
        }
        return input;
    }
}

