/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;

public class MobSpawnerEntityIdentifiersFix
extends DataFix {
    public MobSpawnerEntityIdentifiersFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private Dynamic<?> fix(Dynamic<?> input) {
        Optional spawnPotentials;
        if (!"MobSpawner".equals(input.get("id").asString(""))) {
            return input;
        }
        Optional entityId = input.get("EntityId").asString().result();
        if (entityId.isPresent()) {
            Dynamic spawnData = (Dynamic)DataFixUtils.orElse((Optional)input.get("SpawnData").result(), (Object)input.emptyMap());
            spawnData = spawnData.set("id", spawnData.createString(((String)entityId.get()).isEmpty() ? "Pig" : (String)entityId.get()));
            input = input.set("SpawnData", spawnData);
            input = input.remove("EntityId");
        }
        if ((spawnPotentials = input.get("SpawnPotentials").asStreamOpt().result()).isPresent()) {
            input = input.set("SpawnPotentials", input.createList(((Stream)spawnPotentials.get()).map(spawnPotential -> {
                Optional type = spawnPotential.get("Type").asString().result();
                if (type.isPresent()) {
                    Dynamic spawnData = ((Dynamic)DataFixUtils.orElse((Optional)spawnPotential.get("Properties").result(), (Object)spawnPotential.emptyMap())).set("id", spawnPotential.createString((String)type.get()));
                    return spawnPotential.set("Entity", spawnData).remove("Type").remove("Properties");
                }
                return spawnPotential;
            })));
        }
        return input;
    }

    public TypeRewriteRule makeRule() {
        Type newType = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
        return this.fixTypeEverywhereTyped("MobSpawnerEntityIdentifiersFix", this.getInputSchema().getType(References.UNTAGGED_SPAWNER), newType, input -> {
            Dynamic tag = (Dynamic)input.get(DSL.remainderFinder());
            DataResult fixed = newType.readTyped(this.fix(tag = tag.set("id", tag.createString("MobSpawner"))));
            if (fixed.result().isEmpty()) {
                return input;
            }
            return (Typed)((Pair)fixed.result().get()).getFirst();
        });
    }
}

