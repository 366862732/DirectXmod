/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.AbstractUUIDFix;
import net.minecraft.util.datafix.fixes.References;

public class BlockEntityUUIDFix
extends AbstractUUIDFix {
    public BlockEntityUUIDFix(Schema outputSchema) {
        super(outputSchema, References.BLOCK_ENTITY);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("BlockEntityUUIDFix", this.getInputSchema().getType(this.typeReference), input -> {
            input = this.updateNamedChoice((Typed<?>)input, "minecraft:conduit", this::updateConduit);
            input = this.updateNamedChoice((Typed<?>)input, "minecraft:skull", this::updateSkull);
            return input;
        });
    }

    private Dynamic<?> updateSkull(Dynamic<?> tag) {
        return tag.get("Owner").get().map(ownerTag -> BlockEntityUUIDFix.replaceUUIDString(ownerTag, "Id", "Id").orElse((Dynamic<?>)ownerTag)).map(ownerTag -> tag.remove("Owner").set("SkullOwner", ownerTag)).result().orElse(tag);
    }

    private Dynamic<?> updateConduit(Dynamic<?> tag) {
        return BlockEntityUUIDFix.replaceUUIDMLTag(tag, "target_uuid", "Target").orElse(tag);
    }
}

