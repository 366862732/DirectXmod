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

public class BlockEntityFurnaceBurnTimeFix
extends NamedEntityFix {
    public BlockEntityFurnaceBurnTimeFix(Schema outputSchema, String entityType) {
        super(outputSchema, false, "BlockEntityFurnaceBurnTimeFix" + entityType, References.BLOCK_ENTITY, entityType);
    }

    public Dynamic<?> fixBurnTime(Dynamic<?> data) {
        data = data.renameField("CookTime", "cooking_time_spent");
        data = data.renameField("CookTimeTotal", "cooking_total_time");
        data = data.renameField("BurnTime", "lit_time_remaining");
        data = data.setFieldIfPresent("lit_total_time", data.get("lit_time_remaining").result());
        return data;
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fixBurnTime);
    }
}

