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
import net.minecraft.util.datafix.fixes.References;

public class CauldronRenameFix
extends DataFix {
    public CauldronRenameFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private static Dynamic<?> fix(Dynamic<?> tag) {
        Optional name = tag.get("Name").asString().result();
        if (name.equals(Optional.of("minecraft:cauldron"))) {
            Dynamic properties = tag.get("Properties").orElseEmptyMap();
            if (properties.get("level").asString("0").equals("0")) {
                return tag.remove("Properties");
            }
            return tag.set("Name", tag.createString("minecraft:water_cauldron"));
        }
        return tag;
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("cauldron_rename_fix", this.getInputSchema().getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), CauldronRenameFix::fix));
    }
}

