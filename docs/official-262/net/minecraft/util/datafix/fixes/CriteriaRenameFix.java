/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.datafix.fixes.References;

public class CriteriaRenameFix
extends DataFix {
    private final String name;
    private final String advancementId;
    private final UnaryOperator<String> conversions;

    public CriteriaRenameFix(Schema outputSchema, String name, String advancementId, UnaryOperator<String> conversions) {
        super(outputSchema, false);
        this.name = name;
        this.advancementId = advancementId;
        this.conversions = conversions;
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(this.name, this.getInputSchema().getType(References.ADVANCEMENTS), input -> input.update(DSL.remainderFinder(), this::fixAdvancements));
    }

    private Dynamic<?> fixAdvancements(Dynamic<?> tag) {
        return tag.update(this.advancementId, advancement -> advancement.update("criteria", criteria -> criteria.updateMapValues(e -> e.mapFirst(k -> (Dynamic)DataFixUtils.orElse((Optional)k.asString().map(s -> k.createString((String)this.conversions.apply((String)s))).result(), (Object)k)))));
    }
}

