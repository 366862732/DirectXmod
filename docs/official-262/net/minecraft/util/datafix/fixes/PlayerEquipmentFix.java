/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.datafix.fixes.References;

public class PlayerEquipmentFix
extends DataFix {
    private static final Map<Integer, String> SLOT_TRANSLATIONS = Map.of(100, "feet", 101, "legs", 102, "chest", 103, "head", -106, "offhand");

    public PlayerEquipmentFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldPlayerType = this.getInputSchema().getTypeRaw(References.PLAYER);
        Type newPlayerType = this.getOutputSchema().getTypeRaw(References.PLAYER);
        return this.writeFixAndRead("Player Equipment Fix", oldPlayerType, newPlayerType, tag -> {
            HashMap equipment = new HashMap();
            tag = tag.update("Inventory", inventory -> inventory.createList(inventory.asStream().filter(item -> {
                int inventorySlot = item.get("Slot").asInt(-1);
                String equipmentSlot = SLOT_TRANSLATIONS.get(inventorySlot);
                if (equipmentSlot != null) {
                    equipment.put(inventory.createString(equipmentSlot), item.remove("Slot"));
                }
                return equipmentSlot == null;
            })));
            tag = tag.set("equipment", tag.createMap(equipment));
            return tag;
        });
    }
}

