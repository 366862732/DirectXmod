/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.fixes.References;

public class EquippableAssetRenameFix
extends DataFix {
    public EquippableAssetRenameFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type componentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
        OpticFinder equippableField = componentsType.findField("minecraft:equippable");
        return this.fixTypeEverywhereTyped("equippable asset rename fix", componentsType, components -> components.updateTyped(equippableField, equippable -> equippable.update(DSL.remainderFinder(), tag -> tag.renameField("model", "asset_id"))));
    }
}

