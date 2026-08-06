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

public class JigsawPropertiesFix
extends NamedEntityFix {
    public JigsawPropertiesFix(Schema schema, boolean changesType) {
        super(schema, changesType, "JigsawPropertiesFix", References.BLOCK_ENTITY, "minecraft:jigsaw");
    }

    private static Dynamic<?> fixTag(Dynamic<?> tag) {
        String oldName = tag.get("attachement_type").asString("minecraft:empty");
        String oldPool = tag.get("target_pool").asString("minecraft:empty");
        return tag.set("name", tag.createString(oldName)).set("target", tag.createString(oldName)).remove("attachement_type").set("pool", tag.createString(oldPool)).remove("target_pool");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), JigsawPropertiesFix::fixTag);
    }
}

