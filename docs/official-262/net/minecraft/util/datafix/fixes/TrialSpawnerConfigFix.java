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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.References;

public class TrialSpawnerConfigFix
extends NamedEntityWriteReadFix {
    public TrialSpawnerConfigFix(Schema outputSchema) {
        super(outputSchema, true, "Trial Spawner config tag fixer", References.BLOCK_ENTITY, "minecraft:trial_spawner");
    }

    private static <T> Dynamic<T> moveToConfigTag(Dynamic<T> input) {
        List<String> keysToMove = List.of("spawn_range", "total_mobs", "simultaneous_mobs", "total_mobs_added_per_player", "simultaneous_mobs_added_per_player", "ticks_between_spawn", "spawn_potentials", "loot_tables_to_eject", "items_to_drop_when_ominous");
        HashMap<Dynamic, Dynamic> map = new HashMap<Dynamic, Dynamic>(keysToMove.size());
        for (String key : keysToMove) {
            Optional maybeValueForKey = input.get(key).get().result();
            if (!maybeValueForKey.isPresent()) continue;
            map.put(input.createString(key), (Dynamic)maybeValueForKey.get());
            input = input.remove(key);
        }
        return map.isEmpty() ? input : input.set("normal_config", input.createMap(map));
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> input) {
        return TrialSpawnerConfigFix.moveToConfigTag(input);
    }
}

