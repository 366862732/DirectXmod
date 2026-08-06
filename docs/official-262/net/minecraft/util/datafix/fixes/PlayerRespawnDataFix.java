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
import net.minecraft.util.datafix.fixes.References;

public class PlayerRespawnDataFix
extends DataFix {
    public PlayerRespawnDataFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("PlayerRespawnDataFix", this.getInputSchema().getType(References.PLAYER), input -> input.update(DSL.remainderFinder(), tag -> tag.update("respawn", respawnTag -> respawnTag.set("dimension", respawnTag.createString(respawnTag.get("dimension").asString("minecraft:overworld"))).set("yaw", respawnTag.createFloat(respawnTag.get("angle").asFloat(0.0f))).set("pitch", respawnTag.createFloat(0.0f)).remove("angle"))));
    }
}

