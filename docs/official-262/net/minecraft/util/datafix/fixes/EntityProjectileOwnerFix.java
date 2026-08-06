/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.util.datafix.fixes.References;

public class EntityProjectileOwnerFix
extends DataFix {
    public EntityProjectileOwnerFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema inputSchema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("EntityProjectileOwner", inputSchema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> input) {
        input = this.updateEntity(input, "minecraft:egg", this::updateOwnerThrowable);
        input = this.updateEntity(input, "minecraft:ender_pearl", this::updateOwnerThrowable);
        input = this.updateEntity(input, "minecraft:experience_bottle", this::updateOwnerThrowable);
        input = this.updateEntity(input, "minecraft:snowball", this::updateOwnerThrowable);
        input = this.updateEntity(input, "minecraft:potion", this::updateOwnerThrowable);
        input = this.updateEntity(input, "minecraft:llama_spit", this::updateOwnerLlamaSpit);
        input = this.updateEntity(input, "minecraft:arrow", this::updateOwnerArrow);
        input = this.updateEntity(input, "minecraft:spectral_arrow", this::updateOwnerArrow);
        input = this.updateEntity(input, "minecraft:trident", this::updateOwnerArrow);
        return input;
    }

    private Dynamic<?> updateOwnerArrow(Dynamic<?> tag) {
        long mostSignificantBits = tag.get("OwnerUUIDMost").asLong(0L);
        long leastSignificantBits = tag.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("OwnerUUIDMost").remove("OwnerUUIDLeast");
    }

    private Dynamic<?> updateOwnerLlamaSpit(Dynamic<?> tag) {
        OptionalDynamic owner = tag.get("Owner");
        long mostSignificantBits = owner.get("OwnerUUIDMost").asLong(0L);
        long leastSignificantBits = owner.get("OwnerUUIDLeast").asLong(0L);
        return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("Owner");
    }

    private Dynamic<?> updateOwnerThrowable(Dynamic<?> tag) {
        String ownerKey = "owner";
        OptionalDynamic owner = tag.get("owner");
        long mostSignificantBits = owner.get("M").asLong(0L);
        long leastSignificantBits = owner.get("L").asLong(0L);
        return this.setUUID(tag, mostSignificantBits, leastSignificantBits).remove("owner");
    }

    private Dynamic<?> setUUID(Dynamic<?> tag, long mostSignificantBits, long leastSignificantBits) {
        String name = "OwnerUUID";
        if (mostSignificantBits != 0L && leastSignificantBits != 0L) {
            return tag.set("OwnerUUID", tag.createIntList(Arrays.stream(EntityProjectileOwnerFix.createUUIDArray(mostSignificantBits, leastSignificantBits))));
        }
        return tag;
    }

    private static int[] createUUIDArray(long mostSignificantBits, long leastSignificantBits) {
        return new int[]{(int)(mostSignificantBits >> 32), (int)mostSignificantBits, (int)(leastSignificantBits >> 32), (int)leastSignificantBits};
    }

    private Typed<?> updateEntity(Typed<?> input, String name, Function<Dynamic<?>, Dynamic<?>> function) {
        Type oldType = this.getInputSchema().getChoiceType(References.ENTITY, name);
        Type newType = this.getOutputSchema().getChoiceType(References.ENTITY, name);
        return input.updateTyped(DSL.namedChoice((String)name, (Type)oldType), newType, entity -> entity.update(DSL.remainderFinder(), function));
    }
}

