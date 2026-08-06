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
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class VillagerSetVillagerDataFinalized
extends NamedEntityFix {
    private static final String VILLAGER_DATA_FINALIZED = "VillagerDataFinalized";

    public VillagerSetVillagerDataFinalized(Schema outputSchema) {
        super(outputSchema, true, "Villager VillagerDataFinalized default value", References.ENTITY, "minecraft:villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), VillagerSetVillagerDataFinalized::fixValue);
    }

    private static Dynamic<?> fixValue(Dynamic<?> tag) {
        return tag.set(VILLAGER_DATA_FINALIZED, tag.createBoolean(true));
    }
}

