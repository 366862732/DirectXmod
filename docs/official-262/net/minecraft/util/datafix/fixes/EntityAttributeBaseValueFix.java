/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix
extends NamedEntityFix {
    private final String attributeId;
    private final DoubleUnaryOperator valueFixer;

    public EntityAttributeBaseValueFix(Schema outputSchema, String name, String entityName, String attributeId, DoubleUnaryOperator valueFixer) {
        super(outputSchema, false, name, References.ENTITY, entityName);
        this.attributeId = attributeId;
        this.valueFixer = valueFixer;
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fixValue);
    }

    private Dynamic<?> fixValue(Dynamic<?> tag) {
        return tag.update("attributes", attributes -> tag.createList(attributes.asStream().map(attribute -> {
            String attributeId = NamespacedSchema.ensureNamespaced(attribute.get("id").asString(""));
            if (!attributeId.equals(this.attributeId)) {
                return attribute;
            }
            double base = attribute.get("base").asDouble(0.0);
            return attribute.set("base", attribute.createDouble(this.valueFixer.applyAsDouble(base)));
        })));
    }
}

