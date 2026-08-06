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
import java.util.Map;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class EntityFieldsRenameFix
extends NamedEntityFix {
    private final Map<String, String> renames;

    public EntityFieldsRenameFix(Schema outputSchema, String name, String entityType, Map<String, String> renames) {
        super(outputSchema, false, name, References.ENTITY, entityType);
        this.renames = renames;
    }

    public Dynamic<?> fixTag(Dynamic<?> data) {
        for (Map.Entry<String, String> entry : this.renames.entrySet()) {
            data = data.renameField(entry.getKey(), entry.getValue());
        }
        return data;
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fixTag);
    }
}

