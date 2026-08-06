/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.NamedEntityFix;

public class VariantRenameFix
extends NamedEntityFix {
    private final Map<String, String> renames;

    public VariantRenameFix(Schema outputSchema, String name, DSL.TypeReference type, String entityName, Map<String, String> renames) {
        super(outputSchema, false, name, type, entityName);
        this.renames = renames;
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        return typed.update(DSL.remainderFinder(), remainder -> remainder.update("variant", variant -> (Dynamic)DataFixUtils.orElse((Optional)variant.asString().map(v -> variant.createString(this.renames.getOrDefault(v, (String)v))).result(), (Object)variant)));
    }
}

