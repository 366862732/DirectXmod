/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.VillagerRebuildLevelAndXpFix;

public class ZombieVillagerRebuildXpFix
extends NamedEntityFix {
    public ZombieVillagerRebuildXpFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Zombie Villager XP rebuild", References.ENTITY, "minecraft:zombie_villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), remainder -> {
            Optional xp = remainder.get("Xp").asNumber().result();
            if (xp.isEmpty()) {
                int level = remainder.get("VillagerData").get("level").asInt(1);
                return remainder.set("Xp", remainder.createInt(VillagerRebuildLevelAndXpFix.getMinXpPerLevel(level)));
            }
            return remainder;
        });
    }
}

