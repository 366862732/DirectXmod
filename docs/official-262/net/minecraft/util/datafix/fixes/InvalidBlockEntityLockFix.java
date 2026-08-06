/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.InvalidLockComponentFix;
import net.minecraft.util.datafix.fixes.References;

public class InvalidBlockEntityLockFix
extends DataFix {
    public InvalidBlockEntityLockFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("BlockEntityLockToComponentFix", this.getInputSchema().getType(References.BLOCK_ENTITY), blockEntity -> blockEntity.update(DSL.remainderFinder(), remainder -> {
            Optional lock = remainder.get("lock").result();
            if (lock.isEmpty()) {
                return remainder;
            }
            Dynamic newLock = InvalidLockComponentFix.fixLock((Dynamic)lock.get());
            if (newLock != null) {
                return remainder.set("lock", newLock);
            }
            return remainder.remove("lock");
        }));
    }
}

