/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import net.minecraft.util.datafix.fixes.References;

public class SpawnerDataFix
extends DataFix {
    public SpawnerDataFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldType = this.getInputSchema().getType(References.UNTAGGED_SPAWNER);
        Type newType = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
        OpticFinder spawnDataFinder = oldType.findField("SpawnData");
        Type newSpawnDataType = newType.findField("SpawnData").type();
        OpticFinder spawnPotentialsFinder = oldType.findField("SpawnPotentials");
        Type newSpawnPotentialsType = newType.findField("SpawnPotentials").type();
        return this.fixTypeEverywhereTyped("Fix mob spawner data structure", oldType, newType, spawner -> spawner.updateTyped(spawnDataFinder, newSpawnDataType, spawnData -> this.wrapEntityToSpawnData((Type)newSpawnDataType, (Typed<?>)spawnData)).updateTyped(spawnPotentialsFinder, newSpawnPotentialsType, spawnPotentials -> this.wrapSpawnPotentialsToWeightedEntries((Type)newSpawnPotentialsType, (Typed<?>)spawnPotentials)));
    }

    private <T> Typed<T> wrapEntityToSpawnData(Type<T> newType, Typed<?> spawnData) {
        DynamicOps ops = spawnData.getOps();
        return new Typed(newType, ops, (Object)Pair.of((Object)spawnData.getValue(), (Object)new Dynamic(ops)));
    }

    private <T> Typed<T> wrapSpawnPotentialsToWeightedEntries(Type<T> newType, Typed<?> spawnPotentials) {
        DynamicOps ops = spawnPotentials.getOps();
        List entries = (List)spawnPotentials.getValue();
        List<Pair> wrappedEntries = entries.stream().map(o -> {
            Pair entry = (Pair)o;
            int weight = ((Number)((Dynamic)entry.getSecond()).get("Weight").asNumber().result().orElse(1)).intValue();
            Dynamic newEntryRemainder = new Dynamic(ops);
            newEntryRemainder = newEntryRemainder.set("weight", newEntryRemainder.createInt(weight));
            Dynamic newInnerRemainder = ((Dynamic)entry.getSecond()).remove("Weight").remove("Entity");
            return Pair.of((Object)Pair.of((Object)entry.getFirst(), (Object)newInnerRemainder), (Object)newEntryRemainder);
        }).toList();
        return new Typed(newType, ops, wrappedEntries);
    }
}

