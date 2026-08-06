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
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import net.minecraft.util.datafix.fixes.References;

public class AbstractArrowPickupFix
extends DataFix {
    public AbstractArrowPickupFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema inputSchema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("AbstractArrowPickupFix", inputSchema.getType(References.ENTITY), this::updateProjectiles);
    }

    private Typed<?> updateProjectiles(Typed<?> input) {
        input = this.updateEntity(input, "minecraft:arrow", AbstractArrowPickupFix::updatePickup);
        input = this.updateEntity(input, "minecraft:spectral_arrow", AbstractArrowPickupFix::updatePickup);
        input = this.updateEntity(input, "minecraft:trident", AbstractArrowPickupFix::updatePickup);
        return input;
    }

    private static Dynamic<?> updatePickup(Dynamic<?> tag) {
        if (tag.get("pickup").result().isPresent()) {
            return tag;
        }
        boolean fromPlayer = tag.get("player").asBoolean(true);
        return tag.set("pickup", tag.createByte((byte)(fromPlayer ? 1 : 0))).remove("player");
    }

    private Typed<?> updateEntity(Typed<?> input, String name, Function<Dynamic<?>, Dynamic<?>> function) {
        Type oldType = this.getInputSchema().getChoiceType(References.ENTITY, name);
        Type newType = this.getOutputSchema().getChoiceType(References.ENTITY, name);
        return input.updateTyped(DSL.namedChoice((String)name, (Type)oldType), newType, entity -> entity.update(DSL.remainderFinder(), function));
    }
}

