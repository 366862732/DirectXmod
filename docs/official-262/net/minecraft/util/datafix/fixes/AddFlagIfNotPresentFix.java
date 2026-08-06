/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class AddFlagIfNotPresentFix
extends DataFix {
    private final String name;
    private final boolean flagValue;
    private final String flagKey;
    private final DSL.TypeReference typeReference;

    public AddFlagIfNotPresentFix(Schema outputSchema, DSL.TypeReference typeReference, String flagKey, boolean flagValue) {
        super(outputSchema, true);
        this.flagValue = flagValue;
        this.flagKey = flagKey;
        this.name = "AddFlagIfNotPresentFix_" + this.flagKey + "=" + this.flagValue + " for " + outputSchema.getVersionKey();
        this.typeReference = typeReference;
    }

    protected TypeRewriteRule makeRule() {
        Type worldGenSettingsType = this.getInputSchema().getType(this.typeReference);
        return this.fixTypeEverywhereTyped(this.name, worldGenSettingsType, settings -> settings.update(DSL.remainderFinder(), tag -> tag.set(this.flagKey, (Dynamic)DataFixUtils.orElseGet((Optional)tag.get(this.flagKey).result(), () -> tag.createBoolean(this.flagValue)))));
    }
}

