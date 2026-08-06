/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.EntityRenameFix;

public abstract class SimpleEntityRenameFix
extends EntityRenameFix {
    public SimpleEntityRenameFix(String name, Schema outputSchema, boolean changesType) {
        super(name, outputSchema, changesType);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String name, Typed<?> entity) {
        Pair<String, Dynamic<?>> pair = this.getNewNameAndTag(name, (Dynamic)entity.getOrCreate(DSL.remainderFinder()));
        return Pair.of((Object)((String)pair.getFirst()), (Object)entity.set(DSL.remainderFinder(), (Object)((Dynamic)pair.getSecond())));
    }

    protected abstract Pair<String, Dynamic<?>> getNewNameAndTag(String var1, Dynamic<?> var2);
}

