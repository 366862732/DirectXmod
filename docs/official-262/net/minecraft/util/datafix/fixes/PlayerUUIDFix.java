/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.AbstractUUIDFix;
import net.minecraft.util.datafix.fixes.EntityUUIDFix;
import net.minecraft.util.datafix.fixes.References;

public class PlayerUUIDFix
extends AbstractUUIDFix {
    public PlayerUUIDFix(Schema outputSchema) {
        super(outputSchema, References.PLAYER);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("PlayerUUIDFix", this.getInputSchema().getType(this.typeReference), input -> {
            OpticFinder rootVehicleFinder = input.getType().findField("RootVehicle");
            return input.updateTyped(rootVehicleFinder, rootVehicleFinder.type(), rootVehicle -> rootVehicle.update(DSL.remainderFinder(), tag -> PlayerUUIDFix.replaceUUIDLeastMost(tag, "Attach", "Attach").orElse((Dynamic<?>)tag))).update(DSL.remainderFinder(), tag -> EntityUUIDFix.updateEntityUUID(EntityUUIDFix.updateLivingEntity(tag)));
        });
    }
}

