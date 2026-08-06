/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class ObjectiveRenderTypeFix
extends DataFix {
    public ObjectiveRenderTypeFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    private static String getRenderType(String criteriaName) {
        return criteriaName.equals("health") ? "hearts" : "integer";
    }

    protected TypeRewriteRule makeRule() {
        Type objectiveType = this.getInputSchema().getType(References.OBJECTIVE);
        return this.fixTypeEverywhereTyped("ObjectiveRenderTypeFix", objectiveType, typed -> typed.update(DSL.remainderFinder(), tag -> {
            Optional renderType = tag.get("RenderType").asString().result();
            if (renderType.isEmpty()) {
                String criteriaName = tag.get("CriteriaName").asString("");
                String defaultRenderType = ObjectiveRenderTypeFix.getRenderType(criteriaName);
                return tag.set("RenderType", tag.createString(defaultRenderType));
            }
            return tag;
        }));
    }
}

