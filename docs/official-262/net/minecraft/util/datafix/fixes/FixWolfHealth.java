/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  org.apache.commons.lang3.mutable.MutableBoolean
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FixWolfHealth
extends NamedEntityFix {
    private static final String WOLF_ID = "minecraft:wolf";
    private static final String WOLF_HEALTH = "minecraft:generic.max_health";

    public FixWolfHealth(Schema outputSchema) {
        super(outputSchema, false, "FixWolfHealth", References.ENTITY, WOLF_ID);
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), dynamic -> {
            MutableBoolean healthAdjusted = new MutableBoolean(false);
            dynamic = dynamic.update("Attributes", attributes -> attributes.createList(attributes.asStream().map(attribute -> {
                if (WOLF_HEALTH.equals(NamespacedSchema.ensureNamespaced(attribute.get("Name").asString("")))) {
                    return attribute.update("Base", base -> {
                        if (base.asDouble(0.0) == 20.0) {
                            healthAdjusted.setTrue();
                            return base.createDouble(40.0);
                        }
                        return base;
                    });
                }
                return attribute;
            })));
            if (healthAdjusted.isTrue()) {
                dynamic = dynamic.update("Health", health -> health.createFloat(health.asFloat(0.0f) * 2.0f));
            }
            return dynamic;
        });
    }
}

