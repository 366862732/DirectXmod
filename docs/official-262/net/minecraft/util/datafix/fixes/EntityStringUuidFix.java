/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.datafix.fixes.References;

public class EntityStringUuidFix
extends DataFix {
    public EntityStringUuidFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("EntityStringUuidFix", this.getInputSchema().getType(References.ENTITY), input -> input.update(DSL.remainderFinder(), tag -> {
            Optional uuidString = tag.get("UUID").asString().result();
            if (uuidString.isPresent()) {
                UUID uuid = UUID.fromString((String)uuidString.get());
                return tag.remove("UUID").set("UUIDMost", tag.createLong(uuid.getMostSignificantBits())).set("UUIDLeast", tag.createLong(uuid.getLeastSignificantBits()));
            }
            return tag;
        }));
    }
}

