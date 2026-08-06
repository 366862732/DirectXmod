/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;

public record DropChances(Map<EquipmentSlot, Float> byEquipment) {
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085f;
    public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0f;
    public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
    public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, slot -> Float.valueOf(0.085f)));
    public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT).xmap(DropChances::toEnumMap, DropChances::filterDefaultValues).xmap(DropChances::new, DropChances::byEquipment);

    private static Map<EquipmentSlot, Float> filterDefaultValues(Map<EquipmentSlot, Float> map) {
        HashMap<EquipmentSlot, Float> filteredMap = new HashMap<EquipmentSlot, Float>(map);
        filteredMap.values().removeIf(chance -> chance.floatValue() == 0.085f);
        return filteredMap;
    }

    private static Map<EquipmentSlot, Float> toEnumMap(Map<EquipmentSlot, Float> map) {
        return Util.makeEnumMap(EquipmentSlot.class, slot -> map.getOrDefault(slot, Float.valueOf(0.085f)));
    }

    public DropChances withGuaranteedDrop(EquipmentSlot slot) {
        return this.withEquipmentChance(slot, 2.0f);
    }

    public DropChances withEquipmentChance(EquipmentSlot slot, float chance) {
        if (chance < 0.0f) {
            throw new IllegalArgumentException("Tried to set invalid equipment chance " + chance + " for " + String.valueOf(slot));
        }
        if (this.byEquipment(slot) == chance) {
            return this;
        }
        return new DropChances(Util.makeEnumMap(EquipmentSlot.class, newSlot -> Float.valueOf(newSlot == slot ? chance : this.byEquipment((EquipmentSlot)newSlot))));
    }

    public float byEquipment(EquipmentSlot slot) {
        return this.byEquipment.getOrDefault(slot, Float.valueOf(0.085f)).floatValue();
    }

    public boolean isPreserved(EquipmentSlot slot) {
        return this.byEquipment(slot) > 1.0f;
    }
}

